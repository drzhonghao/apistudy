

import java.io.IOException;
import java.io.Reader;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.lucene.expressions.Expression;
import org.apache.lucene.search.DoubleValues;
import org.apache.lucene.util.IOUtils;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;


public final class JavascriptCompiler {
	static final class Loader extends ClassLoader {
		Loader(ClassLoader parent) {
			super(parent);
		}

		public Class<? extends Expression> define(String className, byte[] bytecode) {
			return defineClass(className, bytecode, 0, bytecode.length).asSubclass(Expression.class);
		}
	}

	private static final int CLASSFILE_VERSION = Opcodes.V1_8;

	private static final String COMPILED_EXPRESSION_CLASS = (JavascriptCompiler.class.getName()) + "$CompiledExpression";

	private static final String COMPILED_EXPRESSION_INTERNAL = JavascriptCompiler.COMPILED_EXPRESSION_CLASS.replace('.', '/');

	static final Type EXPRESSION_TYPE = Type.getType(Expression.class);

	static final Type FUNCTION_VALUES_TYPE = Type.getType(DoubleValues.class);

	private static final Method EXPRESSION_CTOR = JavascriptCompiler.getAsmMethod(void.class, "<init>", String.class, String[].class);

	private static final Method EVALUATE_METHOD = JavascriptCompiler.getAsmMethod(double.class, "evaluate", DoubleValues[].class);

	static final Method DOUBLE_VAL_METHOD = JavascriptCompiler.getAsmMethod(double.class, "doubleValue");

	private static Method getAsmMethod(Class<?> rtype, String name, Class<?>... ptypes) {
		return null;
	}

	private static final int MAX_SOURCE_LENGTH = 16384;

	final String sourceText;

	final Map<String, Method> functions;

	public static Expression compile(String sourceText) throws ParseException {
		return new JavascriptCompiler(sourceText).compileExpression(JavascriptCompiler.class.getClassLoader());
	}

	public static Expression compile(String sourceText, Map<String, Method> functions, ClassLoader parent) throws ParseException {
		if (parent == null) {
			throw new NullPointerException("A parent ClassLoader must be given.");
		}
		for (Method m : functions.values()) {
			JavascriptCompiler.checkFunctionClassLoader(m, parent);
			JavascriptCompiler.checkFunction(m);
		}
		return new JavascriptCompiler(sourceText, functions).compileExpression(parent);
	}

	@SuppressWarnings({ "unused", "null" })
	private static void unusedTestCompile() throws IOException {
		DoubleValues f = null;
		double ret = f.doubleValue();
	}

	private JavascriptCompiler(String sourceText) {
		this(sourceText, JavascriptCompiler.DEFAULT_FUNCTIONS);
	}

	private JavascriptCompiler(String sourceText, Map<String, Method> functions) {
		if (sourceText == null) {
			throw new NullPointerException();
		}
		this.sourceText = sourceText;
		this.functions = functions;
	}

	private Expression compileExpression(ClassLoader parent) throws ParseException {
		final Map<String, Integer> externalsMap = new LinkedHashMap<>();
		final ClassWriter classWriter = new ClassWriter(((ClassWriter.COMPUTE_FRAMES) | (ClassWriter.COMPUTE_MAXS)));
		try {
			generateClass(getAntlrParseTree(), classWriter, externalsMap);
			final Class<? extends Expression> evaluatorClass = new JavascriptCompiler.Loader(parent).define(JavascriptCompiler.COMPILED_EXPRESSION_CLASS, classWriter.toByteArray());
			final Constructor<? extends Expression> constructor = evaluatorClass.getConstructor(String.class, String[].class);
			return constructor.newInstance(sourceText, externalsMap.keySet().toArray(new String[externalsMap.size()]));
		} catch (RuntimeException re) {
			if ((re.getCause()) instanceof ParseException) {
				throw ((ParseException) (re.getCause()));
			}
			throw re;
		} catch (ReflectiveOperationException exception) {
			throw new IllegalStateException((("An internal error occurred attempting to compile the expression (" + (sourceText)) + ")."), exception);
		}
	}

	private ParseTree getAntlrParseTree() throws ParseException {
		final ANTLRInputStream antlrInputStream = new ANTLRInputStream(sourceText);
		return null;
	}

	private void generateClass(final ParseTree parseTree, final ClassWriter classWriter, final Map<String, Integer> externalsMap) throws ParseException {
		classWriter.visit(JavascriptCompiler.CLASSFILE_VERSION, (((Opcodes.ACC_PUBLIC) | (Opcodes.ACC_SUPER)) | (Opcodes.ACC_FINAL)), JavascriptCompiler.COMPILED_EXPRESSION_INTERNAL, null, JavascriptCompiler.EXPRESSION_TYPE.getInternalName(), null);
		final String clippedSourceText = ((sourceText.length()) <= (JavascriptCompiler.MAX_SOURCE_LENGTH)) ? sourceText : (sourceText.substring(0, ((JavascriptCompiler.MAX_SOURCE_LENGTH) - 3))) + "...";
		classWriter.visitSource(clippedSourceText, null);
		classWriter.visitEnd();
	}

	static String normalizeQuotes(String text) {
		StringBuilder out = new StringBuilder(text.length());
		boolean inDoubleQuotes = false;
		for (int i = 0; i < (text.length()); ++i) {
			char c = text.charAt(i);
			if (c == '\\') {
				c = text.charAt((++i));
				if (c == '\\') {
					out.append('\\');
				}
			}else
				if (c == '\'') {
					if (inDoubleQuotes) {
						out.append('\\');
					}else {
						int j = JavascriptCompiler.findSingleQuoteStringEnd(text, i);
						out.append(text, i, j);
						i = j;
					}
				}else
					if (c == '"') {
						c = '\'';
						inDoubleQuotes = !inDoubleQuotes;
					}


			out.append(c);
		}
		return out.toString();
	}

	static int findSingleQuoteStringEnd(String text, int start) {
		++start;
		while ((text.charAt(start)) != '\'') {
			if ((text.charAt(start)) == '\\') {
				++start;
			}
			++start;
		} 
		return start;
	}

	public static final Map<String, Method> DEFAULT_FUNCTIONS;

	static {
		Map<String, Method> map = new HashMap<>();
		try {
			final Properties props = new Properties();
			try (Reader in = IOUtils.getDecodingReader(JavascriptCompiler.class, ((JavascriptCompiler.class.getSimpleName()) + ".properties"), StandardCharsets.UTF_8)) {
				props.load(in);
			}
			for (final String call : props.stringPropertyNames()) {
				final String[] vals = props.getProperty(call).split(",");
				if ((vals.length) != 3) {
					throw new Error("Syntax error while reading Javascript functions from resource");
				}
				final Class<?> clazz = Class.forName(vals[0].trim());
				final String methodName = vals[1].trim();
				final int arity = Integer.parseInt(vals[2].trim());
				@SuppressWarnings({ "rawtypes", "unchecked" })
				Class[] args = new Class[arity];
				Arrays.fill(args, double.class);
				Method method = clazz.getMethod(methodName, args);
				JavascriptCompiler.checkFunction(method);
				map.put(call, method);
			}
		} catch (ReflectiveOperationException | IOException e) {
			throw new Error("Cannot resolve function", e);
		}
		DEFAULT_FUNCTIONS = Collections.unmodifiableMap(map);
	}

	private static void checkFunction(Method method) {
		final MethodType type;
		try {
			type = MethodHandles.publicLookup().unreflect(method).type();
		} catch (IllegalAccessException iae) {
			throw new IllegalArgumentException((method + " is not accessible (declaring class or method not public)."));
		}
		if (!(Modifier.isStatic(method.getModifiers()))) {
			throw new IllegalArgumentException((method + " is not static."));
		}
		for (int arg = 0, arity = type.parameterCount(); arg < arity; arg++) {
			if ((type.parameterType(arg)) != (double.class)) {
				throw new IllegalArgumentException((method + " must take only double parameters."));
			}
		}
		if ((type.returnType()) != (double.class)) {
			throw new IllegalArgumentException((method + " does not return a double."));
		}
	}

	private static void checkFunctionClassLoader(Method method, ClassLoader parent) {
		boolean ok = false;
		try {
			final Class<?> clazz = method.getDeclaringClass();
			ok = (Class.forName(clazz.getName(), false, parent)) == clazz;
		} catch (ClassNotFoundException e) {
			ok = false;
		}
		if (!ok) {
			throw new IllegalArgumentException((method + " is not declared by a class which is accessible by the given parent ClassLoader."));
		}
	}
}

