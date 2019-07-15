

import com.datastax.driver.core.TupleValue;
import com.datastax.driver.core.TypeCodec;
import com.datastax.driver.core.UDTValue;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;
import java.util.List;
import java.util.concurrent.ExecutorService;
import javax.script.Bindings;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;
import jdk.nashorn.api.scripting.AbstractJSObject;
import jdk.nashorn.api.scripting.NashornScriptEngine;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import org.apache.cassandra.cql3.ColumnIdentifier;
import org.apache.cassandra.cql3.functions.AbstractFunction;
import org.apache.cassandra.cql3.functions.FunctionName;
import org.apache.cassandra.cql3.functions.UDFContext;
import org.apache.cassandra.cql3.functions.UDFunction;
import org.apache.cassandra.cql3.functions.UDHelper;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.exceptions.InvalidRequestException;
import org.apache.cassandra.transport.ProtocolVersion;
import org.slf4j.Logger;


final class ScriptBasedUDFunction extends UDFunction {
	private static final ProtectionDomain protectionDomain = null;

	private static final AccessControlContext accessControlContext;

	private static final String[] allowedPackagesArray = new String[]{ "", "com", "edu", "java", "javax", "javafx", "org", "java.lang", "java.lang.invoke", "java.lang.reflect", "java.nio.charset", "java.util", "java.util.concurrent", "javax.script", "sun.reflect", "jdk.internal.org.objectweb.asm.commons", "jdk.nashorn.internal.runtime", "jdk.nashorn.internal.runtime.linker", "java.math", "java.nio", "java.text", "com.google.common.base", "com.google.common.collect", "com.google.common.reflect", "com.datastax.driver.core", "com.datastax.driver.core.utils" };

	private static final NashornScriptEngine scriptEngine = null;

	static {
		ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
		ScriptEngine engine = scriptEngineManager.getEngineByName("nashorn");
		NashornScriptEngineFactory factory = (engine != null) ? ((NashornScriptEngineFactory) (engine.getFactory())) : null;
		accessControlContext = new AccessControlContext(new ProtectionDomain[]{ ScriptBasedUDFunction.protectionDomain });
	}

	private final CompiledScript script;

	private final Object udfContextBinding;

	ScriptBasedUDFunction(FunctionName name, List<ColumnIdentifier> argNames, List<AbstractType<?>> argTypes, AbstractType<?> returnType, boolean calledOnNullInput, String language, String body) {
		super(name, argNames, argTypes, returnType, calledOnNullInput, language, body);
		if ((!("JavaScript".equalsIgnoreCase(language))) || ((ScriptBasedUDFunction.scriptEngine) == null))
			throw new InvalidRequestException(String.format("Invalid language '%s' for function '%s'", language, name));

		try {
			this.script = AccessController.doPrivileged(((PrivilegedExceptionAction<CompiledScript>) (() -> ScriptBasedUDFunction.scriptEngine.compile(body))), ScriptBasedUDFunction.accessControlContext);
		} catch (PrivilegedActionException x) {
			Throwable e = x.getCause();
			UDFunction.logger.info("Failed to compile function '{}' for language {}: ", name, language, e);
			throw new InvalidRequestException(String.format("Failed to compile function '%s' for language %s: %s", name, language, e));
		}
		udfContextBinding = new ScriptBasedUDFunction.UDFContextWrapper();
	}

	protected ExecutorService executor() {
		return null;
	}

	public ByteBuffer executeUserDefined(ProtocolVersion protocolVersion, List<ByteBuffer> parameters) {
		Object[] params = new Object[argTypes.size()];
		for (int i = 0; i < (params.length); i++)
			params[i] = compose(protocolVersion, i, parameters.get(i));

		Object result = executeScriptInternal(params);
		return decompose(protocolVersion, result);
	}

	protected Object executeAggregateUserDefined(ProtocolVersion protocolVersion, Object firstParam, List<ByteBuffer> parameters) {
		Object[] params = new Object[argTypes.size()];
		params[0] = firstParam;
		for (int i = 1; i < (params.length); i++)
			params[i] = compose(protocolVersion, i, parameters.get((i - 1)));

		return executeScriptInternal(params);
	}

	private Object executeScriptInternal(Object[] params) {
		ScriptContext scriptContext = new SimpleScriptContext();
		scriptContext.setAttribute("javax.script.filename", this.name.toString(), ScriptContext.ENGINE_SCOPE);
		Bindings bindings = scriptContext.getBindings(ScriptContext.ENGINE_SCOPE);
		for (int i = 0; i < (params.length); i++)
			bindings.put(argNames.get(i).toString(), params[i]);

		bindings.put("udfContext", udfContextBinding);
		Object result;
		try {
			result = script.eval(scriptContext);
		} catch (ScriptException e) {
			throw new RuntimeException(e);
		}
		if (result == null)
			return null;

		Class<?> javaReturnType = UDHelper.asJavaClass(returnCodec);
		Class<?> resultType = result.getClass();
		if (!(javaReturnType.isAssignableFrom(resultType))) {
			if (result instanceof Number) {
				Number rNumber = ((Number) (result));
				if (javaReturnType == (Integer.class))
					result = rNumber.intValue();
				else
					if (javaReturnType == (Long.class))
						result = rNumber.longValue();
					else
						if (javaReturnType == (Short.class))
							result = rNumber.shortValue();
						else
							if (javaReturnType == (Byte.class))
								result = rNumber.byteValue();
							else
								if (javaReturnType == (Float.class))
									result = rNumber.floatValue();
								else
									if (javaReturnType == (Double.class))
										result = rNumber.doubleValue();
									else
										if (javaReturnType == (BigInteger.class)) {
											if (javaReturnType == (Integer.class))
												result = rNumber.intValue();
											else
												if (javaReturnType == (Short.class))
													result = rNumber.shortValue();
												else
													if (javaReturnType == (Byte.class))
														result = rNumber.byteValue();
													else
														if (javaReturnType == (Long.class))
															result = rNumber.longValue();
														else
															if (javaReturnType == (Float.class))
																result = rNumber.floatValue();
															else
																if (javaReturnType == (Double.class))
																	result = rNumber.doubleValue();
																else
																	if (javaReturnType == (BigInteger.class)) {
																		if (rNumber instanceof BigDecimal)
																			result = ((BigDecimal) (rNumber)).toBigInteger();
																		else
																			if ((rNumber instanceof Double) || (rNumber instanceof Float))
																				result = new BigDecimal(rNumber.toString()).toBigInteger();
																			else
																				result = BigInteger.valueOf(rNumber.longValue());


																	}else
																		if (javaReturnType == (BigDecimal.class))
																			result = new BigDecimal(rNumber.toString());








										}else
											if (javaReturnType == (BigDecimal.class))
												result = new BigDecimal(rNumber.toString());








			}
		}
		return result;
	}

	private final class UDFContextWrapper extends AbstractJSObject {
		private final AbstractJSObject fRetUDT;

		private final AbstractJSObject fArgUDT;

		private final AbstractJSObject fRetTup;

		private final AbstractJSObject fArgTup;

		UDFContextWrapper() {
			fRetUDT = new AbstractJSObject() {
				public Object call(Object thiz, Object... args) {
					return udfContext.newReturnUDTValue();
				}
			};
			fArgUDT = new AbstractJSObject() {
				public Object call(Object thiz, Object... args) {
					if ((args[0]) instanceof String)
						return udfContext.newArgUDTValue(((String) (args[0])));

					if ((args[0]) instanceof Number)
						return udfContext.newArgUDTValue(((Number) (args[0])).intValue());

					return super.call(thiz, args);
				}
			};
			fRetTup = new AbstractJSObject() {
				public Object call(Object thiz, Object... args) {
					return udfContext.newReturnTupleValue();
				}
			};
			fArgTup = new AbstractJSObject() {
				public Object call(Object thiz, Object... args) {
					if ((args[0]) instanceof String)
						return udfContext.newArgTupleValue(((String) (args[0])));

					if ((args[0]) instanceof Number)
						return udfContext.newArgTupleValue(((Number) (args[0])).intValue());

					return super.call(thiz, args);
				}
			};
		}

		public Object getMember(String name) {
			switch (name) {
				case "newReturnUDTValue" :
					return fRetUDT;
				case "newArgUDTValue" :
					return fArgUDT;
				case "newReturnTupleValue" :
					return fRetTup;
				case "newArgTupleValue" :
					return fArgTup;
			}
			return super.getMember(name);
		}
	}
}

