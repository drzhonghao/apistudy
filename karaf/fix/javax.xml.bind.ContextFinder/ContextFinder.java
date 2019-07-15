

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import org.apache.karaf.specs.locator.OsgiLocator;


class ContextFinder {
	private static final Logger logger;

	static {
		logger = Logger.getLogger("javax.xml.bind");
		try {
		} catch (Throwable t) {
		}
	}

	private static void handleInvocationTargetException(InvocationTargetException x) throws JAXBException {
		Throwable t = x.getTargetException();
		if (t != null) {
			if (t instanceof JAXBException)
				throw ((JAXBException) (t));

			if (t instanceof RuntimeException)
				throw ((RuntimeException) (t));

			if (t instanceof Error)
				throw ((Error) (t));

		}
	}

	private static JAXBException handleClassCastException(Class originalType, Class targetType) {
		final URL targetTypeURL = ContextFinder.which(targetType);
		ClassLoader cl = ((originalType.getClassLoader()) != null) ? originalType.getClassLoader() : ClassLoader.getSystemClassLoader();
		return null;
	}

	static JAXBContext newInstance(String contextPath, String className, ClassLoader classLoader, Map properties) throws JAXBException {
		try {
			Class spiClass = ContextFinder.safeLoadClass(className, classLoader);
			Object context = null;
			try {
				Method m = spiClass.getMethod("createContext", String.class, ClassLoader.class, Map.class);
				if ((m.getReturnType()) != (JAXBContext.class)) {
					throw ContextFinder.handleClassCastException(m.getReturnType(), JAXBContext.class);
				}
				context = m.invoke(null, contextPath, classLoader, properties);
			} catch (NoSuchMethodException e) {
			}
			if (context == null) {
				Method m = spiClass.getMethod("createContext", String.class, ClassLoader.class);
				if ((m.getReturnType()) != (JAXBContext.class)) {
					throw ContextFinder.handleClassCastException(m.getReturnType(), JAXBContext.class);
				}
				context = m.invoke(null, contextPath, classLoader);
			}
			if (!(context instanceof JAXBContext)) {
				throw ContextFinder.handleClassCastException(context.getClass(), JAXBContext.class);
			}
			return ((JAXBContext) (context));
		} catch (ClassNotFoundException x) {
		} catch (InvocationTargetException x) {
			ContextFinder.handleInvocationTargetException(x);
			Throwable e = x;
			if ((x.getTargetException()) != null)
				e = x.getTargetException();

		} catch (RuntimeException x) {
			throw x;
		} catch (Exception x) {
		}
		return null;
	}

	static JAXBContext newInstance(Class[] classes, Map properties, String className) throws JAXBException {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		Class spi;
		try {
			spi = ContextFinder.safeLoadClass(className, cl);
		} catch (ClassNotFoundException e) {
			throw new JAXBException(e);
		}
		if (ContextFinder.logger.isLoggable(Level.FINE)) {
			ContextFinder.logger.fine(((("loaded " + className) + " from ") + (ContextFinder.which(spi))));
		}
		Method m;
		try {
			m = spi.getMethod("createContext", Class[].class, Map.class);
		} catch (NoSuchMethodException e) {
			throw new JAXBException(e);
		}
		try {
			Object context = m.invoke(null, classes, properties);
			if (!(context instanceof JAXBContext)) {
				throw ContextFinder.handleClassCastException(context.getClass(), JAXBContext.class);
			}
			return ((JAXBContext) (context));
		} catch (IllegalAccessException e) {
			throw new JAXBException(e);
		} catch (InvocationTargetException e) {
			ContextFinder.handleInvocationTargetException(e);
			Throwable x = e;
			if ((e.getTargetException()) != null)
				x = e.getTargetException();

			throw new JAXBException(x);
		}
	}

	static JAXBContext find(String factoryId, String contextPath, ClassLoader classLoader, Map properties) throws JAXBException {
		final String jaxbContextFQCN = JAXBContext.class.getName();
		StringBuilder propFileName;
		StringTokenizer packages = new StringTokenizer(contextPath, ":");
		String factoryClassName;
		if (!(packages.hasMoreTokens())) {
		}
		ContextFinder.logger.fine("Searching jaxb.properties");
		while (packages.hasMoreTokens()) {
			String packageName = packages.nextToken(":").replace('.', '/');
			propFileName = new StringBuilder().append(packageName).append("/jaxb.properties");
			Properties props = ContextFinder.loadJAXBProperties(classLoader, propFileName.toString());
			if (props != null) {
				if (props.containsKey(factoryId)) {
					factoryClassName = props.getProperty(factoryId);
					return ContextFinder.newInstance(contextPath, factoryClassName, classLoader, properties);
				}else {
				}
			}
		} 
		ContextFinder.logger.fine("Searching the system property");
		factoryClassName = null;
		if (factoryClassName != null) {
			return ContextFinder.newInstance(contextPath, factoryClassName, classLoader, properties);
		}
		ContextFinder.logger.fine("Searching META-INF/services");
		BufferedReader r;
		try {
			final StringBuilder resource = new StringBuilder().append("META-INF/services/").append(jaxbContextFQCN);
			final InputStream resourceStream = classLoader.getResourceAsStream(resource.toString());
			if (resourceStream != null) {
				r = new BufferedReader(new InputStreamReader(resourceStream, "UTF-8"));
				factoryClassName = r.readLine().trim();
				r.close();
				return ContextFinder.newInstance(contextPath, factoryClassName, classLoader, properties);
			}else {
				ContextFinder.logger.fine(("Unable to load:" + (resource.toString())));
			}
		} catch (UnsupportedEncodingException e) {
			throw new JAXBException(e);
		} catch (IOException e) {
			throw new JAXBException(e);
		}
		ContextFinder.logger.fine("Trying to create the platform default provider");
		return ContextFinder.newInstance(contextPath, ContextFinder.PLATFORM_DEFAULT_FACTORY_CLASS, classLoader, properties);
	}

	static JAXBContext find(Class[] classes, Map properties) throws JAXBException {
		final String jaxbContextFQCN = JAXBContext.class.getName();
		String factoryClassName;
		for (final Class c : classes) {
			ClassLoader classLoader = AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
				public ClassLoader run() {
					return c.getClassLoader();
				}
			});
			Package pkg = c.getPackage();
			if (pkg == null)
				continue;

			String packageName = pkg.getName().replace('.', '/');
			String resourceName = packageName + "/jaxb.properties";
			ContextFinder.logger.fine(("Trying to locate " + resourceName));
			Properties props = ContextFinder.loadJAXBProperties(classLoader, resourceName);
			if (props == null) {
				ContextFinder.logger.fine("  not found");
			}else {
				ContextFinder.logger.fine("  found");
				if (props.containsKey(JAXBContext.JAXB_CONTEXT_FACTORY)) {
					factoryClassName = props.getProperty(JAXBContext.JAXB_CONTEXT_FACTORY).trim();
					return ContextFinder.newInstance(classes, properties, factoryClassName);
				}else {
				}
			}
		}
		ContextFinder.logger.fine(("Checking system property " + jaxbContextFQCN));
		factoryClassName = null;
		if (factoryClassName != null) {
			ContextFinder.logger.fine(("  found " + factoryClassName));
			return ContextFinder.newInstance(classes, properties, factoryClassName);
		}
		ContextFinder.logger.fine("  not found");
		ContextFinder.logger.fine("Checking META-INF/services");
		BufferedReader r;
		try {
			final String resource = new StringBuilder("META-INF/services/").append(jaxbContextFQCN).toString();
			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
			URL resourceURL;
			if (classLoader == null)
				resourceURL = ClassLoader.getSystemResource(resource);
			else
				resourceURL = classLoader.getResource(resource);

			if (resourceURL != null) {
				ContextFinder.logger.fine(("Reading " + resourceURL));
				r = new BufferedReader(new InputStreamReader(resourceURL.openStream(), "UTF-8"));
				factoryClassName = r.readLine().trim();
				return ContextFinder.newInstance(classes, properties, factoryClassName);
			}else {
				ContextFinder.logger.fine(("Unable to find: " + resource));
			}
		} catch (UnsupportedEncodingException e) {
			throw new JAXBException(e);
		} catch (IOException e) {
			throw new JAXBException(e);
		}
		ContextFinder.logger.fine("Trying to create the platform default provider");
		return ContextFinder.newInstance(classes, properties, ContextFinder.PLATFORM_DEFAULT_FACTORY_CLASS);
	}

	private static Properties loadJAXBProperties(ClassLoader classLoader, String propFileName) throws JAXBException {
		Properties props = null;
		try {
			URL url;
			if (classLoader == null)
				url = ClassLoader.getSystemResource(propFileName);
			else
				url = classLoader.getResource(propFileName);

			if (url != null) {
				ContextFinder.logger.fine(("loading props from " + url));
				props = new Properties();
				InputStream is = url.openStream();
				props.load(is);
				is.close();
			}
		} catch (IOException ioe) {
			ContextFinder.logger.log(Level.FINE, ("Unable to load " + propFileName), ioe);
			throw new JAXBException(ioe.toString(), ioe);
		}
		return props;
	}

	static URL which(Class clazz, ClassLoader loader) {
		String classnameAsResource = (clazz.getName().replace('.', '/')) + ".class";
		if (loader == null) {
			loader = ClassLoader.getSystemClassLoader();
		}
		return loader.getResource(classnameAsResource);
	}

	static URL which(Class clazz) {
		return ContextFinder.which(clazz, clazz.getClassLoader());
	}

	private static final String PLATFORM_DEFAULT_FACTORY_CLASS = "com.sun.xml.internal.bind.v2.ContextFactory";

	private static Class safeLoadClass(String className, ClassLoader classLoader) throws ClassNotFoundException {
		try {
			Class spiClass = OsgiLocator.locate(JAXBContext.class);
			if (spiClass != null) {
				return spiClass;
			}
		} catch (Throwable t) {
		}
		ContextFinder.logger.fine(("Trying to load " + className));
		try {
			SecurityManager s = System.getSecurityManager();
			if (s != null) {
				int i = className.lastIndexOf('.');
				if (i != (-1)) {
					s.checkPackageAccess(className.substring(0, i));
				}
			}
			if (classLoader == null) {
				return Class.forName(className);
			}else {
				return classLoader.loadClass(className);
			}
		} catch (SecurityException se) {
			if (ContextFinder.PLATFORM_DEFAULT_FACTORY_CLASS.equals(className)) {
				return Class.forName(className);
			}
			throw se;
		}
	}
}

