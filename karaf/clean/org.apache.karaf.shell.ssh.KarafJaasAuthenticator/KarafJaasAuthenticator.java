import org.apache.karaf.shell.ssh.*;


import java.security.Principal;
import java.security.PublicKey;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginContext;

import org.apache.karaf.jaas.boot.principal.ClientPrincipal;
import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.apache.karaf.jaas.modules.publickey.PublickeyCallback;
import org.apache.sshd.common.session.Session;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.auth.pubkey.PublickeyAuthenticator;
import org.apache.sshd.server.session.ServerSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KarafJaasAuthenticator implements PasswordAuthenticator, PublickeyAuthenticator {

    public static final Session.AttributeKey<Subject> SUBJECT_ATTRIBUTE_KEY = new Session.AttributeKey<>();
    private final Logger LOGGER = LoggerFactory.getLogger(KarafJaasAuthenticator.class);

    private String realm;
    private String role;
    private Class<?>[] roleClasses;

    public KarafJaasAuthenticator(String realm, String role, Class<?>[] roleClasses) {
        this.realm = realm;
        this.role = role;
        this.roleClasses = roleClasses;
    }

    public boolean authenticate(final String username, final String password, final ServerSession session) {
        CallbackHandler callbackHandler = callbacks -> {
            for (Callback callback : callbacks) {
                if (callback instanceof NameCallback) {
                    ((NameCallback) callback).setName(username);
                } else if (callback instanceof PasswordCallback) {
                    ((PasswordCallback) callback).setPassword(password.toCharArray());
                } else {
                    throw new UnsupportedCallbackException(callback);
                }
            }
        };
        return doLogin(session, callbackHandler);
    }

    public boolean authenticate(final String username, final PublicKey key, final ServerSession session) {
        CallbackHandler callbackHandler = callbacks -> {
            for (Callback callback : callbacks) {
                if (callback instanceof NameCallback) {
                    ((NameCallback) callback).setName(username);
                } else if (callback instanceof PublickeyCallback) {
                    ((PublickeyCallback) callback).setPublicKey(key);
                } else {
                    throw new UnsupportedCallbackException(callback);
                }
            }
        };
        return doLogin(session, callbackHandler);
    }

    private boolean doLogin(final ServerSession session, CallbackHandler callbackHandler) {
        try {
            Subject subject = new Subject();
            subject.getPrincipals().add(new ClientPrincipal("ssh", session.getClientAddress().toString()));
            LoginContext loginContext = new LoginContext(realm, subject, callbackHandler);
            loginContext.login();
            assertRolePresent(subject);
            session.setAttribute(SUBJECT_ATTRIBUTE_KEY, subject);
            return true;
        } catch (Exception e) {
            LOGGER.debug("User authentication failed with " + e.getMessage(), e);
            return false;
        }
    }

    private void assertRolePresent(Subject subject) throws FailedLoginException {
        boolean hasCorrectRole = role == null || role.isEmpty() || roleClasses.length == 0;
        int roleCount = 0;
        for (Principal principal : subject.getPrincipals()) {
            for (Class<?> roleClass : roleClasses) {
                if (roleClass.isInstance(principal)) {
                    if (!hasCorrectRole) {
                        hasCorrectRole = role.equals(principal.getName());
                    }
                    roleCount++;
                }
            }
        }
        if (roleCount == 0) {
            throw new FailedLoginException("User doesn't have role defined");
        }
        if (!hasCorrectRole) {
            throw new FailedLoginException("User doesn't have the required role " + role);
        }
    }

}
