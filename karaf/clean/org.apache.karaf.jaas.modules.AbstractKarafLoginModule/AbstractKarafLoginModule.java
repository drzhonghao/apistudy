import org.apache.karaf.jaas.modules.*;


import java.security.Principal;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.apache.karaf.jaas.boot.principal.RolePolicy;
import org.apache.karaf.jaas.modules.encryption.EncryptionSupport;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Abstract JAAS login module extended by all Karaf Login Modules.
 */
public abstract class AbstractKarafLoginModule implements LoginModule {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(AbstractKarafLoginModule.class);

    protected Set<Principal> principals = new HashSet<>();
    protected Subject subject;
    protected String user;
    protected CallbackHandler callbackHandler;
    protected boolean debug;
    protected Map<String, ?> options;
   
    protected String rolePolicy;
    protected String roleDiscriminator;
    protected boolean detailedLoginExcepion;

    /** the authentication status*/
    protected boolean succeeded = false;
    protected boolean commitSucceeded = false;

    /**
     * the bundle context is required to use the encryption service
     */
    protected BundleContext bundleContext;

    private EncryptionSupport encryptionSupport;

    @Override
    public boolean commit() throws LoginException {
        if (!succeeded || principals.isEmpty()) {
            clear();
            succeeded = false;
            return false;
        }
        RolePolicy policy = RolePolicy.getPolicy(rolePolicy);
        if (policy != null && roleDiscriminator != null) {
            policy.handleRoles(subject, principals, roleDiscriminator);
        } else {
            subject.getPrincipals().addAll(principals);
        }
        commitSucceeded = true;
        return true;
    }

    @Override
    public boolean abort() throws LoginException {
        if (debug) {
            LOGGER.debug("abort");
        }
        if (!succeeded) {
            return false;
        } else if (succeeded && commitSucceeded) {
            // we succeeded, but another required module failed
            logout();
        } else {
            // our commit failed
            clear();
            succeeded = false;
        }
        return true;
    }

    @Override
    public boolean logout() throws LoginException {
        if (debug) {
            LOGGER.debug("logout");
        }

        subject.getPrincipals().removeAll(principals);
        clear();

        succeeded = false;
        commitSucceeded = false;

        return true;
    }

    protected void clear() {
        user = null;
        principals.clear();
    }

    public void initialize(Subject sub, CallbackHandler handler, Map<String, ?> options) {
        this.subject = sub;
        this.callbackHandler = handler;
        this.options = options;
        this.rolePolicy = (String) options.get("role.policy");
        this.roleDiscriminator = (String) options.get("role.discriminator");
        this.debug = Boolean.parseBoolean((String) options.get("debug"));
        this.detailedLoginExcepion = Boolean.parseBoolean((String) options.get("detailed.login.exception"));
        // the bundle context is set in the Config JaasRealm by default
        this.bundleContext = (BundleContext) options.get(BundleContext.class.getName());
        encryptionSupport = new EncryptionSupport(options);
    }

    public boolean checkPassword(String plain, String encrypted) {
        String newEncrypted = encryptionSupport.encrypt(plain);
        String prefix = encryptionSupport.getEncryptionPrefix();
        String suffix = encryptionSupport.getEncryptionSuffix();
        boolean isMatch = encryptionSupport.getEncryption() != null 
            ? encryptionSupport.getEncryption().checkPassword(plain, 
                encrypted.substring(prefix.length(), encrypted.length() - suffix.length())) : false;
        return encrypted.equals(newEncrypted) 
            || isMatch;
    }

}
