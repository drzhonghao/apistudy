

import com.datastax.driver.core.DataType;
import com.datastax.driver.core.TypeCodec;
import com.google.common.reflect.TypeToken;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.ByteBuffer;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.ProtectionDomain;
import java.security.SecureClassLoader;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.cassandra.cql3.ColumnIdentifier;
import org.apache.cassandra.cql3.functions.FunctionName;
import org.apache.cassandra.cql3.functions.JavaUDF;
import org.apache.cassandra.cql3.functions.UDFByteCodeVerifier;
import org.apache.cassandra.cql3.functions.UDFContext;
import org.apache.cassandra.cql3.functions.UDFunction;
import org.apache.cassandra.cql3.functions.UDHelper;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.exceptions.InvalidRequestException;
import org.apache.cassandra.transport.ProtocolVersion;
import org.apache.cassandra.utils.FBUtilities;
import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.internal.compiler.ClassFile;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.Compiler;
import org.eclipse.jdt.internal.compiler.DefaultErrorHandlingPolicies;
import org.eclipse.jdt.internal.compiler.ICompilerRequestor;
import org.eclipse.jdt.internal.compiler.IErrorHandlingPolicy;
import org.eclipse.jdt.internal.compiler.IProblemFactory;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.jdt.internal.compiler.env.INameEnvironment;
import org.eclipse.jdt.internal.compiler.env.NameEnvironmentAnswer;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class JavaBasedUDFunction extends UDFunction {
	private static final String BASE_PACKAGE = "org.apache.cassandra.cql3.udf.gen";

	private static final Pattern JAVA_LANG_PREFIX = Pattern.compile("\\bjava\\.lang\\.");

	static final Logger logger = LoggerFactory.getLogger(JavaBasedUDFunction.class);

	private static final AtomicInteger classSequence = new AtomicInteger();

	private static final JavaBasedUDFunction.EcjTargetClassLoader targetClassLoader = new JavaBasedUDFunction.EcjTargetClassLoader();

	private static final UDFByteCodeVerifier udfByteCodeVerifier = new UDFByteCodeVerifier();

	private static final ProtectionDomain protectionDomain = null;

	private static final IErrorHandlingPolicy errorHandlingPolicy = DefaultErrorHandlingPolicies.proceedWithAllProblems();

	private static final IProblemFactory problemFactory = new DefaultProblemFactory(Locale.ENGLISH);

	private static final CompilerOptions compilerOptions;

	private static final String[] javaSourceTemplate;

	static {
		JavaBasedUDFunction.udfByteCodeVerifier.addDisallowedMethodCall("java/lang/Class", "forName");
		JavaBasedUDFunction.udfByteCodeVerifier.addDisallowedMethodCall("java/lang/Class", "getClassLoader");
		JavaBasedUDFunction.udfByteCodeVerifier.addDisallowedMethodCall("java/lang/Class", "getResource");
		JavaBasedUDFunction.udfByteCodeVerifier.addDisallowedMethodCall("java/lang/Class", "getResourceAsStream");
		JavaBasedUDFunction.udfByteCodeVerifier.addDisallowedMethodCall("java/lang/ClassLoader", "clearAssertionStatus");
		JavaBasedUDFunction.udfByteCodeVerifier.addDisallowedMethodCall("java/lang/ClassLoader", "getResource");
		JavaBasedUDFunction.udfByteCodeVerifier.addDisallowedMethodCall("java/lang/ClassLoader", "getResourceAsStream");
		JavaBasedUDFunction.udfByteCodeVerifier.addDisallowedMethodCall("java/lang/ClassLoader", "getResources");
		JavaBasedUDFunction.udfByteCodeVerifier.addDisallowedMethodCall("java/lang/ClassLoader", "getSystemClassLoader");
		JavaBasedUDFunction.udfByteCodeVerifier.addDisallowedMethodCall("java/lang/ClassLoader", "getSystemResource");
		JavaBasedUDFunction.udfByteCodeVerifier.addDisallowedMethodCall("java/lang/ClassLoader", "getSystemResourceAsStream");
		JavaBasedUDFunction.udfByteCodeVerifier.addDisallowedMethodCall("java/lang/ClassLoader", "getSystemResources");
		JavaBasedUDFunction.udfByteCodeVerifier.addDisallowedMethodCall("java/lang/ClassLoader", "loadClass");
		JavaBasedUDFunction.udfByteCodeVerifier.addDisallowedMethodCall("java/lang/ClassLoader", "setClassAssertionStatus");
		JavaBasedUDFunction.udfByteCodeVerifier.addDisallowedMethodCall("java/lang/ClassLoader", "setDefaultAssertionStatus");
		JavaBasedUDFunction.udfByteCodeVerifier.addDisallowedMethodCall("java/lang/ClassLoader", "setPackageAssertionStatus");
		JavaBasedUDFunction.udfByteCodeVerifier.addDisallowedMethodCall("java/nio/ByteBuffer", "allocateDirect");
		for (String ia : new String[]{ "java/net/InetAddress", "java/net/Inet4Address", "java/net/Inet6Address" }) {
			JavaBasedUDFunction.udfByteCodeVerifier.addDisallowedMethodCall(ia, "getByAddress");
			JavaBasedUDFunction.udfByteCodeVerifier.addDisallowedMethodCall(ia, "getAllByName");
			JavaBasedUDFunction.udfByteCodeVerifier.addDisallowedMethodCall(ia, "getByName");
			JavaBasedUDFunction.udfByteCodeVerifier.addDisallowedMethodCall(ia, "getLocalHost");
			JavaBasedUDFunction.udfByteCodeVerifier.addDisallowedMethodCall(ia, "getHostName");
			JavaBasedUDFunction.udfByteCodeVerifier.addDisallowedMethodCall(ia, "getCanonicalHostName");
			JavaBasedUDFunction.udfByteCodeVerifier.addDisallowedMethodCall(ia, "isReachable");
		}
		JavaBasedUDFunction.udfByteCodeVerifier.addDisallowedClass("java/net/NetworkInterface");
		JavaBasedUDFunction.udfByteCodeVerifier.addDisallowedClass("java/net/SocketException");
		Map<String, String> settings = new HashMap<>();
		settings.put(CompilerOptions.OPTION_LineNumberAttribute, CompilerOptions.GENERATE);
		settings.put(CompilerOptions.OPTION_SourceFileAttribute, CompilerOptions.DISABLED);
		settings.put(CompilerOptions.OPTION_ReportDeprecation, CompilerOptions.IGNORE);
		settings.put(CompilerOptions.OPTION_Source, CompilerOptions.VERSION_1_8);
		settings.put(CompilerOptions.OPTION_TargetPlatform, CompilerOptions.VERSION_1_8);
		compilerOptions = new CompilerOptions(settings);
		JavaBasedUDFunction.compilerOptions.parseLiteralExpressionsAsConstants = true;
		try (final InputStream input = JavaBasedUDFunction.class.getResource("JavaSourceUDF.txt").openConnection().getInputStream()) {
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			FBUtilities.copy(input, output, Long.MAX_VALUE);
			String template = output.toString();
			StringTokenizer st = new StringTokenizer(template, "#");
			javaSourceTemplate = new String[st.countTokens()];
			for (int i = 0; st.hasMoreElements(); i++)
				JavaBasedUDFunction.javaSourceTemplate[i] = st.nextToken();

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		CodeSource codeSource;
		try {
			codeSource = new CodeSource(new URL("udf", "localhost", 0, "/java", new URLStreamHandler() {
				protected URLConnection openConnection(URL u) {
					return null;
				}
			}), ((Certificate[]) (null)));
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	private final JavaUDF javaUDF;

	JavaBasedUDFunction(FunctionName name, List<ColumnIdentifier> argNames, List<AbstractType<?>> argTypes, AbstractType<?> returnType, boolean calledOnNullInput, String body) {
		super(name, argNames, argTypes, UDHelper.driverTypes(argTypes), returnType, UDHelper.driverType(returnType), calledOnNullInput, "java", body);
		TypeToken<?>[] javaParamTypes = UDHelper.typeTokens(argCodecs, calledOnNullInput);
		TypeToken<?> javaReturnType = returnCodec.getJavaType();
		String pkgName = ((JavaBasedUDFunction.BASE_PACKAGE) + '.') + (JavaBasedUDFunction.generateClassName(name, 'p'));
		String clsName = JavaBasedUDFunction.generateClassName(name, 'C');
		String executeInternalName = JavaBasedUDFunction.generateClassName(name, 'x');
		StringBuilder javaSourceBuilder = new StringBuilder();
		int lineOffset = 1;
		for (int i = 0; i < (JavaBasedUDFunction.javaSourceTemplate.length); i++) {
			String s = JavaBasedUDFunction.javaSourceTemplate[i];
			if ((i & 1) == 1) {
				switch (s) {
					case "package_name" :
						s = pkgName;
						break;
					case "class_name" :
						s = clsName;
						break;
					case "body" :
						lineOffset = JavaBasedUDFunction.countNewlines(javaSourceBuilder);
						s = body;
						break;
					case "arguments" :
						s = JavaBasedUDFunction.generateArguments(javaParamTypes, argNames, false);
						break;
					case "arguments_aggregate" :
						s = JavaBasedUDFunction.generateArguments(javaParamTypes, argNames, true);
						break;
					case "argument_list" :
						s = JavaBasedUDFunction.generateArgumentList(javaParamTypes, argNames);
						break;
					case "return_type" :
						s = JavaBasedUDFunction.javaSourceName(javaReturnType);
						break;
					case "execute_internal_name" :
						s = executeInternalName;
						break;
				}
			}
			javaSourceBuilder.append(s);
		}
		String targetClassName = (pkgName + '.') + clsName;
		String javaSource = javaSourceBuilder.toString();
		JavaBasedUDFunction.logger.trace("Compiling Java source UDF \'{}\' as class \'{}\' using source:\n{}", name, targetClassName, javaSource);
		try {
			JavaBasedUDFunction.EcjCompilationUnit compilationUnit = new JavaBasedUDFunction.EcjCompilationUnit(javaSource, targetClassName);
			Compiler compiler = new Compiler(compilationUnit, JavaBasedUDFunction.errorHandlingPolicy, JavaBasedUDFunction.compilerOptions, compilationUnit, JavaBasedUDFunction.problemFactory);
			compiler.compile(new ICompilationUnit[]{ compilationUnit });
			if (((compilationUnit.problemList) != null) && (!(compilationUnit.problemList.isEmpty()))) {
				boolean fullSource = false;
				StringBuilder problems = new StringBuilder();
				for (IProblem problem : compilationUnit.problemList) {
					long ln = (problem.getSourceLineNumber()) - lineOffset;
					if (ln < 1L) {
						if (problem.isError()) {
							problems.append("GENERATED SOURCE ERROR: line ").append(problem.getSourceLineNumber()).append(" (in generated source): ").append(problem.getMessage()).append('\n');
							fullSource = true;
						}
					}else {
						problems.append("Line ").append(Long.toString(ln)).append(": ").append(problem.getMessage()).append('\n');
					}
				}
				if (fullSource)
					throw new InvalidRequestException(((("Java source compilation failed:\n" + problems) + "\n generated source:\n") + javaSource));
				else
					throw new InvalidRequestException(("Java source compilation failed:\n" + problems));

			}
			Set<String> errors = JavaBasedUDFunction.udfByteCodeVerifier.verify(targetClassName, JavaBasedUDFunction.targetClassLoader.classData(targetClassName));
			String validDeclare = ("not allowed method declared: " + executeInternalName) + '(';
			for (Iterator<String> i = errors.iterator(); i.hasNext();) {
				String error = i.next();
				if (error.startsWith(validDeclare))
					i.remove();

			}
			if (!(errors.isEmpty()))
				throw new InvalidRequestException(("Java UDF validation failed: " + errors));

			Thread thread = Thread.currentThread();
			ClassLoader orig = thread.getContextClassLoader();
			try {
				Class cls = Class.forName(targetClassName, false, JavaBasedUDFunction.targetClassLoader);
				int nonSyntheticMethodCount = 0;
				for (Method m : cls.getDeclaredMethods()) {
					if (!(m.isSynthetic())) {
						nonSyntheticMethodCount += 1;
					}
				}
				if ((nonSyntheticMethodCount != 3) || ((cls.getDeclaredConstructors().length) != 1))
					throw new InvalidRequestException("Check your source to not define additional Java methods or constructors");

				MethodType methodType = MethodType.methodType(void.class).appendParameterTypes(TypeCodec.class, TypeCodec[].class, UDFContext.class);
				MethodHandle ctor = MethodHandles.lookup().findConstructor(cls, methodType);
				this.javaUDF = ((JavaUDF) (ctor.invokeWithArguments(returnCodec, argCodecs, udfContext)));
			} finally {
				thread.setContextClassLoader(orig);
			}
		} catch (InvocationTargetException e) {
			throw new InvalidRequestException(String.format("Could not compile function '%s' from Java source: %s", name, e.getCause()));
		} catch (InvalidRequestException | VirtualMachineError e) {
			throw e;
		} catch (Throwable e) {
			JavaBasedUDFunction.logger.error(String.format("Could not compile function '%s' from Java source:%n%s", name, javaSource), e);
			throw new InvalidRequestException(String.format("Could not compile function '%s' from Java source: %s", name, e));
		}
	}

	protected ExecutorService executor() {
		return null;
	}

	protected ByteBuffer executeUserDefined(ProtocolVersion protocolVersion, List<ByteBuffer> params) {
		return null;
	}

	protected Object executeAggregateUserDefined(ProtocolVersion protocolVersion, Object firstParam, List<ByteBuffer> params) {
		return null;
	}

	private static int countNewlines(StringBuilder javaSource) {
		int ln = 0;
		for (int i = 0; i < (javaSource.length()); i++)
			if ((javaSource.charAt(i)) == '\n')
				ln++;


		return ln;
	}

	private static String generateClassName(FunctionName name, char prefix) {
		String qualifiedName = name.toString();
		StringBuilder sb = new StringBuilder(((qualifiedName.length()) + 10));
		sb.append(prefix);
		for (int i = 0; i < (qualifiedName.length()); i++) {
			char c = qualifiedName.charAt(i);
			if (Character.isJavaIdentifierPart(c))
				sb.append(c);
			else
				sb.append(Integer.toHexString((((short) (c)) & 65535)));

		}
		sb.append('_').append(((ThreadLocalRandom.current().nextInt()) & 16777215)).append('_').append(JavaBasedUDFunction.classSequence.incrementAndGet());
		return sb.toString();
	}

	@com.google.common.annotations.VisibleForTesting
	public static String javaSourceName(TypeToken<?> type) {
		String n = type.toString();
		return JavaBasedUDFunction.JAVA_LANG_PREFIX.matcher(n).replaceAll("");
	}

	private static String generateArgumentList(TypeToken<?>[] paramTypes, List<ColumnIdentifier> argNames) {
		StringBuilder code = new StringBuilder((32 * (paramTypes.length)));
		for (int i = 0; i < (paramTypes.length); i++) {
			if (i > 0)
				code.append(", ");

			code.append(JavaBasedUDFunction.javaSourceName(paramTypes[i])).append(' ').append(argNames.get(i));
		}
		return code.toString();
	}

	private static String generateArguments(TypeToken<?>[] paramTypes, List<ColumnIdentifier> argNames, boolean forAggregate) {
		StringBuilder code = new StringBuilder((64 * (paramTypes.length)));
		for (int i = 0; i < (paramTypes.length); i++) {
			if (i > 0)
				code.append(",\n");

			if (JavaBasedUDFunction.logger.isTraceEnabled())
				code.append("            /* parameter '").append(argNames.get(i)).append("\' */\n");

			code.append("            (").append(JavaBasedUDFunction.javaSourceName(paramTypes[i])).append(") ");
			if (forAggregate && (i == 0))
				code.append("firstParam");
			else
				code.append(JavaBasedUDFunction.composeMethod(paramTypes[i])).append("(protocolVersion, ").append(i).append(", params.get(").append((forAggregate ? i - 1 : i)).append("))");

		}
		return code.toString();
	}

	private static String composeMethod(TypeToken<?> type) {
		return type.isPrimitive() ? "super.compose_" + (type.getRawType().getName()) : "super.compose";
	}

	static final class EcjCompilationUnit implements ICompilerRequestor , ICompilationUnit , INameEnvironment {
		List<IProblem> problemList;

		private final String className;

		private final char[] sourceCode;

		EcjCompilationUnit(String sourceCode, String className) {
			this.className = className;
			this.sourceCode = sourceCode.toCharArray();
		}

		@Override
		public char[] getFileName() {
			return sourceCode;
		}

		@Override
		public char[] getContents() {
			return sourceCode;
		}

		@Override
		public char[] getMainTypeName() {
			int dot = className.lastIndexOf('.');
			return (dot > 0 ? className.substring((dot + 1)) : className).toCharArray();
		}

		@Override
		public char[][] getPackageName() {
			StringTokenizer izer = new StringTokenizer(className, ".");
			char[][] result = new char[(izer.countTokens()) - 1][];
			for (int i = 0; i < (result.length); i++)
				result[i] = izer.nextToken().toCharArray();

			return result;
		}

		@Override
		public boolean ignoreOptionalProblems() {
			return false;
		}

		@Override
		public void acceptResult(CompilationResult result) {
			if (result.hasErrors()) {
				IProblem[] problems = result.getProblems();
				if ((problemList) == null)
					problemList = new ArrayList<>(problems.length);

				Collections.addAll(problemList, problems);
			}else {
				ClassFile[] classFiles = result.getClassFiles();
				for (ClassFile classFile : classFiles)
					JavaBasedUDFunction.targetClassLoader.addClass(className, classFile.getBytes());

			}
		}

		@Override
		public NameEnvironmentAnswer findType(char[][] compoundTypeName) {
			StringBuilder result = new StringBuilder();
			for (int i = 0; i < (compoundTypeName.length); i++) {
				if (i > 0)
					result.append('.');

				result.append(compoundTypeName[i]);
			}
			return findType(result.toString());
		}

		@Override
		public NameEnvironmentAnswer findType(char[] typeName, char[][] packageName) {
			StringBuilder result = new StringBuilder();
			int i = 0;
			for (; i < (packageName.length); i++) {
				if (i > 0)
					result.append('.');

				result.append(packageName[i]);
			}
			if (i > 0)
				result.append('.');

			result.append(typeName);
			return findType(result.toString());
		}

		private NameEnvironmentAnswer findType(String className) {
			if (className.equals(this.className)) {
				return new NameEnvironmentAnswer(this, null);
			}
			String resourceName = (className.replace('.', '/')) + ".class";
			return null;
		}

		private boolean isPackage(String result) {
			if (result.equals(this.className))
				return false;

			String resourceName = (result.replace('.', '/')) + ".class";
			return false;
		}

		@Override
		public boolean isPackage(char[][] parentPackageName, char[] packageName) {
			StringBuilder result = new StringBuilder();
			int i = 0;
			if (parentPackageName != null)
				for (; i < (parentPackageName.length); i++) {
					if (i > 0)
						result.append('.');

					result.append(parentPackageName[i]);
				}

			if ((Character.isUpperCase(packageName[0])) && (!(isPackage(result.toString()))))
				return false;

			if (i > 0)
				result.append('.');

			result.append(packageName);
			return isPackage(result.toString());
		}

		@Override
		public void cleanup() {
		}
	}

	static final class EcjTargetClassLoader extends SecureClassLoader {
		EcjTargetClassLoader() {
		}

		private final Map<String, byte[]> classes = new ConcurrentHashMap<>();

		void addClass(String className, byte[] classData) {
			classes.put(className, classData);
		}

		byte[] classData(String className) {
			return classes.get(className);
		}

		protected Class<?> findClass(String name) throws ClassNotFoundException {
			byte[] classData = classes.remove(name);
			if (classData != null)
				return defineClass(name, classData, 0, classData.length, JavaBasedUDFunction.protectionDomain);

			return getParent().loadClass(name);
		}

		protected PermissionCollection getPermissions(CodeSource codesource) {
			return null;
		}
	}
}

