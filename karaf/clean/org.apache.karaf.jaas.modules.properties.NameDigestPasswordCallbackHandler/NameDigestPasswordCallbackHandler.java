import org.apache.karaf.jaas.modules.properties.*;


import java.io.IOException;
import java.lang.reflect.Method;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NameDigestPasswordCallbackHandler implements CallbackHandler {  
    
    private static final Logger LOG = LoggerFactory.getLogger(NameDigestPasswordCallbackHandler.class);
    private static final String PASSWORD_CALLBACK_NAME = "setObject";
    private static final Class<?>[] PASSWORD_CALLBACK_TYPES = 
        new Class[]{Object.class, char[].class, String.class};
    
    private String username;  
    private String password;  
    private String nonce;
    private String createdTime;
    
    private String passwordCallbackName;
    
    public NameDigestPasswordCallbackHandler(String username, String password, String nonce, String createdTime) {  
        this(username, password, nonce, createdTime, null);  
    }  
     
    public NameDigestPasswordCallbackHandler(String username, 
                                              String password, 
                                              String nonce, 
                                              String createdTime, 
                                              String passwordCallbackName) {  
        this.username = username;  
        this.password = password;
        this.nonce = nonce;
        this.createdTime = createdTime;
        this.passwordCallbackName = passwordCallbackName;
    }  

    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        for (Callback callback : callbacks) {
            if (handleCallback(callback)) {
                continue;
            } else if (callback instanceof NameCallback) {
                ((NameCallback) callback).setName(username);
            } else if (callback instanceof PasswordCallback) {
                PasswordCallback pwCallback = (PasswordCallback) callback;
                pwCallback.setPassword(password.toCharArray());
            } else if (!invokePasswordCallback(callback)) {
                String msg = "Unsupported callback type" + callback.getClass().getName();
                LOG.info(msg);
                throw new UnsupportedCallbackException(callback, msg);
            }
        }  
    }      
    
    protected boolean handleCallback(Callback callback) {
        return false;
    }
    
    /*
     * This method is called from the handle(Callback[]) method when the specified callback 
     * did not match any of the known callback classes. It looks for the callback method 
     * having the specified method name with one of the suppported parameter types.
     * If found, it invokes the callback method on the object and returns true. 
     * If not, it returns false.
     */
    private boolean invokePasswordCallback(Callback callback) {
        String cbname = passwordCallbackName == null
                        ? PASSWORD_CALLBACK_NAME : passwordCallbackName;
        for (Class<?> arg : PASSWORD_CALLBACK_TYPES) {
            try {
                Method method = callback.getClass().getMethod(cbname, arg);
                method.invoke(callback, arg == String.class ? password : password.toCharArray());
                return true;
            } catch (Exception e) {
                // ignore and continue
                LOG.debug(e.getMessage(), e);
            }
        }
        return false;
    }
    
    public String getNonce() {
        return this.nonce;
    }
    
    public String getCreatedTime() {
        return this.createdTime;
    }
 
}
