

import java.io.IOException;
import java.security.Principal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import javax.naming.Context;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.apache.karaf.jaas.boot.principal.UserPrincipal;
import org.apache.karaf.jaas.modules.AbstractKarafLoginModule;
import org.apache.karaf.jaas.modules.ldap.LDAPCache;
import org.apache.karaf.jaas.modules.ldap.LDAPOptions;
import org.apache.karaf.jaas.modules.ldap.ManagedSSLSocketFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class LDAPLoginModule extends AbstractKarafLoginModule {
	private static Logger logger = LoggerFactory.getLogger(LDAPLoginModule.class);

	public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
		super.initialize(subject, callbackHandler, options);
		LDAPCache.clear();
	}

	public boolean login() throws LoginException {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			return doLogin();
		} finally {
			ManagedSSLSocketFactory.setSocketFactory(null);
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	protected boolean doLogin() throws LoginException {
		Callback[] callbacks = new Callback[2];
		callbacks[0] = new NameCallback("Username: ");
		callbacks[1] = new PasswordCallback("Password: ", false);
		try {
			callbackHandler.handle(callbacks);
		} catch (IOException ioException) {
			throw new LoginException(ioException.getMessage());
		} catch (UnsupportedCallbackException unsupportedCallbackException) {
			throw new LoginException(((unsupportedCallbackException.getMessage()) + " not available to obtain information from user."));
		}
		char[] tmpPassword = ((PasswordCallback) (callbacks[1])).getPassword();
		LDAPOptions options = new LDAPOptions(this.options);
		if (options.isUsernameTrim()) {
			if ((user) != null) {
				user = user.trim();
			}
		}
		String authentication = options.getAuthentication();
		if (("none".equals(authentication)) && (((user) != null) || (tmpPassword != null))) {
			LDAPLoginModule.logger.debug("Changing from authentication = none to simple since user or password was specified.");
			authentication = "simple";
			Map<String, Object> opts = new HashMap<>(this.options);
			opts.put(LDAPOptions.AUTHENTICATION, authentication);
			options = new LDAPOptions(opts);
		}
		boolean allowEmptyPasswords = options.getAllowEmptyPasswords();
		if (((!("none".equals(authentication))) && (!allowEmptyPasswords)) && ((tmpPassword == null) || ((tmpPassword.length) == 0))) {
			throw new LoginException("Empty passwords not allowed");
		}
		if (tmpPassword == null) {
			tmpPassword = new char[0];
		}
		String password = new String(tmpPassword);
		principals = new HashSet<>();
		LDAPCache cache = LDAPCache.getCache(options);
		final String[] userDnAndNamespace;
		try {
			LDAPLoginModule.logger.debug("Get the user DN.");
			userDnAndNamespace = cache.getUserDnAndNamespace(user);
			if (userDnAndNamespace == null) {
				return false;
			}
		} catch (Exception e) {
			LDAPLoginModule.logger.warn("Can't connect to the LDAP server: {}", e.getMessage(), e);
			throw new LoginException(("Can't connect to the LDAP server: " + (e.getMessage())));
		}
		DirContext context = null;
		try {
			LDAPLoginModule.logger.debug("Bind user (authentication).");
			Hashtable<String, Object> env = options.getEnv();
			env.put(Context.SECURITY_AUTHENTICATION, authentication);
			LDAPLoginModule.logger.debug(((("Set the security principal for " + (userDnAndNamespace[0])) + ",") + (options.getUserBaseDn())));
			env.put(Context.SECURITY_PRINCIPAL, (((userDnAndNamespace[0]) + ",") + (options.getUserBaseDn())));
			env.put(Context.SECURITY_CREDENTIALS, password);
			LDAPLoginModule.logger.debug("Binding the user.");
			context = new InitialDirContext(env);
			LDAPLoginModule.logger.debug((("User " + (user)) + " successfully bound."));
			context.close();
		} catch (Exception e) {
			LDAPLoginModule.logger.warn((("User " + (user)) + " authentication failed."), e);
			throw new LoginException(("Authentication failed: " + (e.getMessage())));
		} finally {
			if (context != null) {
				try {
					context.close();
				} catch (Exception e) {
				}
			}
		}
		principals.add(new UserPrincipal(user));
		try {
			String[] roles = cache.getUserRoles(user, userDnAndNamespace[0], userDnAndNamespace[1]);
			for (String role : roles) {
				principals.add(new RolePrincipal(role));
			}
		} catch (Exception e) {
			throw new LoginException(((("Can't get user " + (user)) + " roles: ") + (e.getMessage())));
		}
		succeeded = true;
		return true;
	}
}

