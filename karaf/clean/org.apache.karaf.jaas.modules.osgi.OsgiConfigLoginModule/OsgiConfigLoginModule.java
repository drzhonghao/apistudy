import org.apache.karaf.jaas.modules.osgi.*;


import java.io.IOException;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;

import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.apache.karaf.jaas.boot.principal.UserPrincipal;
import org.apache.karaf.jaas.modules.AbstractKarafLoginModule;

import org.osgi.service.cm.Configuration;

public class OsgiConfigLoginModule extends AbstractKarafLoginModule {

    public static final String PID = "pid";
    public static final String USER_PREFIX = "user.";

    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        super.initialize(subject, callbackHandler, options);
    }

    public boolean login() throws LoginException {
        try {
            String pid = (String) options.get(PID);
            Configuration config = ConfigAdminHolder.getService().getConfiguration(pid, null);
            Dictionary<String, Object> properties = config.getProperties();

            Callback[] callbacks = new Callback[2];

            callbacks[0] = new NameCallback("Username: ");
            callbacks[1] = new PasswordCallback("Password: ", false);
            try {
                callbackHandler.handle(callbacks);
            } catch (IOException ioe) {
                throw new LoginException(ioe.getMessage());
            } catch (UnsupportedCallbackException uce) {
                throw new LoginException(uce.getMessage() + " not available to obtain information from user");
            }
            String user = ((NameCallback) callbacks[0]).getName();
            String password = new String(((PasswordCallback) callbacks[1]).getPassword());

            String userInfos = (String) properties.get(USER_PREFIX + user);
            if (userInfos == null) {
            	if (!this.detailedLoginExcepion) {
            		throw new FailedLoginException("login failed");
            	} else {
            		throw new FailedLoginException("User does not exist");
            	}
            }
            String[] infos = userInfos.split(",");
            String storedPassword = infos[0];

            // check the provided password
            if (!checkPassword(password, storedPassword)) {
            	if (!this.detailedLoginExcepion) {
            		throw new FailedLoginException("login failed");
            	} else {
            		throw new FailedLoginException("Password for " + user + " does not match");
            	}
            }

            principals = new HashSet<>();
            principals.add(new UserPrincipal(user));
            for (int i = 1; i < infos.length; i++) {
                principals.add(new RolePrincipal(infos[i]));
            }

            succeeded = true;
            return true;
        } catch (LoginException e) {
            throw e;
        } catch (Exception e) {
            throw (LoginException) new LoginException("Unable to authenticate user").initCause(e);
        } finally {
            callbackHandler = null;
            options = null;
        }
    }

}
