

import java.io.IOException;
import java.security.Principal;
import java.security.PublicKey;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.naming.NamingException;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.apache.karaf.jaas.boot.principal.UserPrincipal;
import org.apache.karaf.jaas.modules.AbstractKarafLoginModule;
import org.apache.karaf.jaas.modules.ldap.LDAPCache;
import org.apache.karaf.jaas.modules.ldap.LDAPOptions;
import org.apache.karaf.jaas.modules.ldap.ManagedSSLSocketFactory;
import org.apache.karaf.jaas.modules.publickey.PublickeyCallback;
import org.apache.karaf.jaas.modules.publickey.PublickeyLoginModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class LDAPPubkeyLoginModule extends AbstractKarafLoginModule {
	private static Logger logger = LoggerFactory.getLogger(LDAPPubkeyLoginModule.class);

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
		callbacks[1] = new PublickeyCallback();
		try {
			callbackHandler.handle(callbacks);
		} catch (IOException ioException) {
			throw new LoginException(ioException.getMessage());
		} catch (UnsupportedCallbackException unsupportedCallbackException) {
			throw new LoginException(((unsupportedCallbackException.getMessage()) + " not available to obtain information from user."));
		}
		PublicKey remotePubkey = ((PublickeyCallback) (callbacks[1])).getPublicKey();
		LDAPOptions options = new LDAPOptions(this.options);
		if (options.isUsernameTrim()) {
			if ((user) != null) {
				user = user.trim();
			}
		}
		principals = new HashSet<>();
		LDAPCache cache = LDAPCache.getCache(options);
		final String[] userDnAndNamespace;
		try {
			LDAPPubkeyLoginModule.logger.debug("Get the user DN.");
			userDnAndNamespace = cache.getUserDnAndNamespace(user);
			if (userDnAndNamespace == null) {
				return false;
			}
		} catch (Exception e) {
			LDAPPubkeyLoginModule.logger.warn("Can't connect to the LDAP server: {}", e.getMessage(), e);
			throw new LoginException(("Can't connect to the LDAP server: " + (e.getMessage())));
		}
		String userFullDn = ((userDnAndNamespace[0]) + ",") + (options.getUserBaseDn());
		try {
			authenticatePubkey(userFullDn, remotePubkey, cache);
		} catch (NamingException e) {
			LDAPPubkeyLoginModule.logger.warn("Can't connect to the LDAP server: {}", e.getMessage(), e);
			throw new LoginException(("Can't connect to the LDAP server: " + (e.getMessage())));
		} catch (FailedLoginException e) {
			if (!(this.detailedLoginExcepion)) {
				throw new LoginException("Authentication failed");
			}else {
				LDAPPubkeyLoginModule.logger.warn("Public key authentication failed for user {}: {}", user, e.getMessage(), e);
				throw new LoginException(((("Public key authentication failed for user " + (user)) + ": ") + (e.getMessage())));
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

	private void authenticatePubkey(String userDn, PublicKey key, LDAPCache cache) throws NamingException, FailedLoginException {
		if (key == null)
			throw new FailedLoginException("no public key supplied by the client");

		String[] storedKeys = cache.getUserPubkeys(userDn);
		if ((storedKeys.length) > 0) {
			String keyString = PublickeyLoginModule.getString(key);
			for (String storedKey : storedKeys) {
				if (keyString.equals(storedKey)) {
					return;
				}
			}
		}
		throw new FailedLoginException("no matching public key found");
	}
}

