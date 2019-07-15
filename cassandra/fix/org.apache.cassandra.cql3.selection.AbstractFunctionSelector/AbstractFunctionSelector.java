

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import org.apache.cassandra.cql3.functions.Function;
import org.apache.cassandra.cql3.functions.FunctionName;
import org.apache.cassandra.cql3.selection.Selector;
import org.apache.cassandra.cql3.statements.RequestValidations;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.exceptions.InvalidRequestException;
import org.apache.commons.lang3.text.StrBuilder;


abstract class AbstractFunctionSelector<T extends Function> extends Selector {
	protected final T fun;

	private final List<ByteBuffer> args;

	protected final List<Selector> argSelectors;

	protected AbstractFunctionSelector(T fun, List<Selector> argSelectors) {
		this.fun = fun;
		this.argSelectors = argSelectors;
		this.args = Arrays.asList(new ByteBuffer[argSelectors.size()]);
	}

	protected void setArg(int i, ByteBuffer value) throws InvalidRequestException {
		RequestValidations.checkBindValueSet(value, "Invalid unset value for argument in call to function %s", fun.name().name);
		args.set(i, value);
	}

	protected List<ByteBuffer> args() {
		return args;
	}

	public AbstractType<?> getType() {
		return fun.returnType();
	}

	@Override
	public String toString() {
		return new StrBuilder().append(fun.name()).append("(").appendWithSeparators(argSelectors, ", ").append(")").toString();
	}
}

