

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.util.Set;
import javax.security.auth.Subject;
import org.apache.karaf.jaas.boot.principal.ClientPrincipal;
import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.apache.karaf.jaas.boot.principal.UserPrincipal;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.api.console.SessionFactory;
import org.apache.karaf.shell.impl.console.JLineTerminal;
import org.apache.karaf.shell.support.ShellUtil;
import org.apache.karaf.util.jaas.JaasHelper;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import static org.jline.terminal.Terminal.SignalHandler.SIG_IGN;


public class LocalConsoleManager {
	private static final String INPUT_ENCODING = "input.encoding";

	private static final String KARAF_DELAY_CONSOLE = "karaf.delay.console";

	private static final String KARAF_LOCAL_USER = "karaf.local.user";

	private static final String KARAF_LOCAL_ROLES = "karaf.local.roles";

	private static final String KARAF_LOCAL_ROLES_DEFAULT = "admin,manager,viewer,systembundles";

	private SessionFactory sessionFactory;

	private BundleContext bundleContext;

	private Session session;

	private ServiceRegistration<?> registration;

	private boolean closing;

	public LocalConsoleManager(BundleContext bundleContext, SessionFactory sessionFactory) throws Exception {
		this.bundleContext = bundleContext;
		this.sessionFactory = sessionFactory;
	}

	public void start() throws Exception {
		final Terminal terminal = TerminalBuilder.builder().nativeSignals(true).signalHandler(SIG_IGN).build();
		final Subject subject = createLocalKarafSubject();
		this.session = JaasHelper.doAs(subject, ((PrivilegedAction<Session>) (() -> {
			String encoding = LocalConsoleManager.getEncoding();
			PrintStream pout = new PrintStream(terminal.output()) {
				@Override
				public void close() {
				}
			};
			session = sessionFactory.create(terminal.input(), pout, pout, new JLineTerminal(terminal), encoding, this::close);
			session.put(Session.IS_LOCAL, true);
			registration = bundleContext.registerService(Session.class, session, null);
			String name = "Karaf local console user " + (ShellUtil.getCurrentUserName());
			boolean delayconsole = Boolean.parseBoolean(System.getProperty(LocalConsoleManager.KARAF_DELAY_CONSOLE));
			if (delayconsole) {
			}else {
				new Thread(session, name).start();
			}
			return session;
		})));
	}

	public static String getEncoding() {
		String envEncoding = LocalConsoleManager.extractEncodingFromCtype(System.getenv("LC_CTYPE"));
		if (envEncoding != null) {
			return envEncoding;
		}
		return System.getProperty(LocalConsoleManager.INPUT_ENCODING, Charset.defaultCharset().name());
	}

	static String extractEncodingFromCtype(String ctype) {
		if ((ctype != null) && ((ctype.indexOf('.')) > 0)) {
			String encodingAndModifier = ctype.substring(((ctype.indexOf('.')) + 1));
			if ((encodingAndModifier.indexOf('@')) > 0) {
				return encodingAndModifier.substring(0, encodingAndModifier.indexOf('@'));
			}else {
				return encodingAndModifier;
			}
		}
		return null;
	}

	private Subject createLocalKarafSubject() {
		String userName = System.getProperty(LocalConsoleManager.KARAF_LOCAL_USER);
		if (userName == null) {
			userName = "karaf";
		}
		final Subject subject = new Subject();
		subject.getPrincipals().add(new UserPrincipal(userName));
		subject.getPrincipals().add(new ClientPrincipal("local", "localhost"));
		String roles = System.getProperty(LocalConsoleManager.KARAF_LOCAL_ROLES, LocalConsoleManager.KARAF_LOCAL_ROLES_DEFAULT);
		if (roles != null) {
			for (String role : roles.split("[,]")) {
				subject.getPrincipals().add(new RolePrincipal(role.trim()));
			}
		}
		return subject;
	}

	public void stop() throws Exception {
		closing = true;
		if ((registration) != null) {
			registration.unregister();
		}
		if ((session) != null) {
			session.close();
		}
	}

	protected void close() {
		try {
			if (!(closing)) {
				bundleContext.getBundle(0).stop();
			}
		} catch (Exception e) {
		}
	}
}

