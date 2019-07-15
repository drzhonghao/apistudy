

import com.datastax.driver.core.DataType;
import com.datastax.driver.core.TypeCodec;
import com.google.common.base.Objects;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.apache.cassandra.cql3.functions.AbstractFunction;
import org.apache.cassandra.cql3.functions.AggregateFunction;
import org.apache.cassandra.cql3.functions.Function;
import org.apache.cassandra.cql3.functions.FunctionName;
import org.apache.cassandra.cql3.functions.ScalarFunction;
import org.apache.cassandra.cql3.functions.UDFunction;
import org.apache.cassandra.cql3.functions.UDHelper;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.exceptions.InvalidRequestException;
import org.apache.cassandra.schema.Functions;
import org.apache.cassandra.tracing.Tracing;
import org.apache.cassandra.transport.ProtocolVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class UDAggregate extends AbstractFunction implements AggregateFunction {
	protected static final Logger logger = LoggerFactory.getLogger(UDAggregate.class);

	private final AbstractType<?> stateType;

	private final TypeCodec stateTypeCodec;

	private final TypeCodec returnTypeCodec;

	protected final ByteBuffer initcond;

	private final ScalarFunction stateFunction;

	private final ScalarFunction finalFunction;

	public UDAggregate(FunctionName name, List<AbstractType<?>> argTypes, AbstractType<?> returnType, ScalarFunction stateFunc, ScalarFunction finalFunc, ByteBuffer initcond) {
		super(name, argTypes, returnType);
		this.stateFunction = stateFunc;
		this.finalFunction = finalFunc;
		this.stateType = (stateFunc != null) ? stateFunc.returnType() : null;
		this.stateTypeCodec = ((stateType) != null) ? UDHelper.codecFor(UDHelper.driverType(stateType)) : null;
		this.returnTypeCodec = (returnType != null) ? UDHelper.codecFor(UDHelper.driverType(returnType)) : null;
		this.initcond = initcond;
	}

	public static UDAggregate create(Functions functions, FunctionName name, List<AbstractType<?>> argTypes, AbstractType<?> returnType, FunctionName stateFunc, FunctionName finalFunc, AbstractType<?> stateType, ByteBuffer initcond) throws InvalidRequestException {
		List<AbstractType<?>> stateTypes = new ArrayList<>(((argTypes.size()) + 1));
		stateTypes.add(stateType);
		stateTypes.addAll(argTypes);
		List<AbstractType<?>> finalTypes = Collections.singletonList(stateType);
		return new UDAggregate(name, argTypes, returnType, UDAggregate.resolveScalar(functions, name, stateFunc, stateTypes), (finalFunc != null ? UDAggregate.resolveScalar(functions, name, finalFunc, finalTypes) : null), initcond);
	}

	public static UDAggregate createBroken(FunctionName name, List<AbstractType<?>> argTypes, AbstractType<?> returnType, ByteBuffer initcond, InvalidRequestException reason) {
		return new UDAggregate(name, argTypes, returnType, null, null, initcond) {
			public AggregateFunction.Aggregate newAggregate() throws InvalidRequestException {
				throw new InvalidRequestException(String.format(("Aggregate '%s' exists but hasn't been loaded successfully for the following reason: %s. " + "Please see the server log for more details"), this, reason.getMessage()));
			}
		};
	}

	public boolean hasReferenceTo(Function function) {
		return ((stateFunction) == function) || ((finalFunction) == function);
	}

	@Override
	public void addFunctionsTo(List<Function> functions) {
		functions.add(this);
		if ((stateFunction) != null) {
			stateFunction.addFunctionsTo(functions);
			if ((finalFunction) != null)
				finalFunction.addFunctionsTo(functions);

		}
	}

	public boolean isAggregate() {
		return true;
	}

	public boolean isNative() {
		return false;
	}

	public ScalarFunction stateFunction() {
		return stateFunction;
	}

	public ScalarFunction finalFunction() {
		return finalFunction;
	}

	public ByteBuffer initialCondition() {
		return initcond;
	}

	public AbstractType<?> stateType() {
		return stateType;
	}

	public AggregateFunction.Aggregate newAggregate() throws InvalidRequestException {
		return new AggregateFunction.Aggregate() {
			private long stateFunctionCount;

			private long stateFunctionDuration;

			private Object state;

			private boolean needsInit = true;

			public void addInput(ProtocolVersion protocolVersion, List<ByteBuffer> values) throws InvalidRequestException {
				maybeInit(protocolVersion);
				long startTime = System.nanoTime();
				(stateFunctionCount)++;
				if ((stateFunction) instanceof UDFunction) {
					UDFunction udf = ((UDFunction) (stateFunction));
					if (udf.isCallableWrtNullable(values))
						state = udf.executeForAggregate(protocolVersion, state, values);

				}else {
					throw new UnsupportedOperationException("UDAs only support UDFs");
				}
				stateFunctionDuration += ((System.nanoTime()) - startTime) / 1000;
			}

			private void maybeInit(ProtocolVersion protocolVersion) {
				if (needsInit) {
					state = ((initcond) != null) ? UDHelper.deserialize(stateTypeCodec, protocolVersion, initcond.duplicate()) : null;
					stateFunctionDuration = 0;
					stateFunctionCount = 0;
					needsInit = false;
				}
			}

			public ByteBuffer compute(ProtocolVersion protocolVersion) throws InvalidRequestException {
				maybeInit(protocolVersion);
				Tracing.trace("Executed UDA {}: {} call(s) to state function {} in {}\u03bcs", name(), stateFunctionCount, stateFunction.name(), stateFunctionDuration);
				if ((finalFunction) == null) {
				}
				if ((finalFunction) instanceof UDFunction) {
					UDFunction udf = ((UDFunction) (finalFunction));
					Object result = udf.executeForAggregate(protocolVersion, state, Collections.emptyList());
				}
				throw new UnsupportedOperationException("UDAs only support UDFs");
			}

			public void reset() {
				needsInit = true;
			}
		};
	}

	private static ScalarFunction resolveScalar(Functions functions, FunctionName aName, FunctionName fName, List<AbstractType<?>> argTypes) throws InvalidRequestException {
		Optional<Function> fun = functions.find(fName, argTypes);
		if (!(fun.isPresent()))
			throw new InvalidRequestException(String.format("Referenced state function '%s %s' for aggregate '%s' does not exist", fName, Arrays.toString(UDHelper.driverTypes(argTypes)), aName));

		if (!((fun.get()) instanceof ScalarFunction))
			throw new InvalidRequestException(String.format("Referenced state function '%s %s' for aggregate '%s' is not a scalar function", fName, Arrays.toString(UDHelper.driverTypes(argTypes)), aName));

		return ((ScalarFunction) (fun.get()));
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof UDAggregate))
			return false;

		UDAggregate that = ((UDAggregate) (o));
		return ((((((Objects.equal(name, that.name)) && (Functions.typesMatch(argTypes, that.argTypes))) && (Functions.typesMatch(returnType, that.returnType))) && (Objects.equal(stateFunction, that.stateFunction))) && (Objects.equal(finalFunction, that.finalFunction))) && (((stateType) == (that.stateType)) || (((stateType) != null) && (stateType.equals(that.stateType, true))))) && (Objects.equal(initcond, that.initcond));
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(name, Functions.typeHashCode(argTypes), Functions.typeHashCode(returnType), stateFunction, finalFunction, stateType, initcond);
	}
}

