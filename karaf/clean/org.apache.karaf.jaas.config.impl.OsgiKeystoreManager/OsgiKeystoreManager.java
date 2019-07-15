import org.apache.karaf.jaas.config.impl.*;


import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocketFactory;

import org.apache.karaf.jaas.config.KeystoreInstance;
import org.apache.karaf.jaas.config.KeystoreIsLocked;
import org.apache.karaf.jaas.config.KeystoreManager;
import org.apache.karaf.util.collections.CopyOnWriteArrayIdentityList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of KeystoreManager
 */
public class OsgiKeystoreManager implements KeystoreManager {

    private final static transient Logger logger = LoggerFactory.getLogger(OsgiKeystoreManager.class);

    private List<KeystoreInstance> keystores = new CopyOnWriteArrayIdentityList<>();

    public void register(KeystoreInstance keystore, Map<String, ?> properties) {
        keystores.add(keystore);
    }

    public void unregister(KeystoreInstance keystore, Map<String, ?> properties) {
        keystores.remove(keystore);
    }

    public KeystoreInstance getKeystore(String name) {
        KeystoreInstance keystore = null;
        for (KeystoreInstance ks : keystores) {
            if (ks.getName().equals(name)) {
                if (keystore == null || keystore.getRank() < ks.getRank()) {
                    keystore = ks;
                }
            }
        }
        return keystore;
    }

    public SSLContext createSSLContext(String provider, String protocol, String algorithm, String keyStore, String keyAlias, String trustStore) throws GeneralSecurityException {
        return createSSLContext(provider, protocol, algorithm, keyStore, keyAlias, trustStore, 0);
    }

    public SSLContext createSSLContext(String provider, String protocol, String algorithm, String keyStore, String keyAlias, String trustStore, long timeout) throws GeneralSecurityException {

        if (!this.checkForKeystoresAvailability(keyStore, keyAlias, trustStore, timeout)) {
            throw new GeneralSecurityException("Unable to lookup configured keystore and/or truststore");
        }

        KeystoreInstance keyInstance = getKeystore(keyStore);
        if (keyInstance != null && keyInstance.isKeystoreLocked()) {
            throw new KeystoreIsLocked("Keystore '" + keyStore + "' is locked");
        }
        if (keyInstance != null && keyInstance.isKeyLocked(keyAlias)) {
            throw new KeystoreIsLocked("Key '" + keyAlias + "' in keystore '" + keyStore + "' is locked");
        }
        KeystoreInstance trustInstance = trustStore == null ? null : getKeystore(trustStore);
        if (trustInstance != null && trustInstance.isKeystoreLocked()) {
            throw new KeystoreIsLocked("Keystore '" + trustStore + "' is locked");
        }
        SSLContext context;
        if (provider == null) {
            context = SSLContext.getInstance(protocol);
        } else {
            context = SSLContext.getInstance(protocol, provider);
        }
        context.init(keyInstance == null ? null : keyInstance.getKeyManager(algorithm, keyAlias),
                trustInstance == null ? null : trustInstance.getTrustManager(algorithm), new SecureRandom());
        return context;
    }

    public SSLServerSocketFactory createSSLServerFactory(String provider, String protocol, String algorithm, String keyStore, String keyAlias, String trustStore) throws GeneralSecurityException {
        return createSSLServerFactory(provider, protocol, algorithm, keyStore, keyAlias, trustStore, 0);
    }

    public SSLServerSocketFactory createSSLServerFactory(String provider, String protocol, String algorithm, String keyStore, String keyAlias, String trustStore, long timeout) throws GeneralSecurityException {
        SSLContext context = createSSLContext(provider, protocol, algorithm, keyStore, keyAlias, trustStore, timeout);
        return context.getServerSocketFactory();
    }

    public SSLSocketFactory createSSLFactory(String provider, String protocol, String algorithm, String keyStore, String keyAlias, String trustStore) throws GeneralSecurityException {
        return createSSLFactory(provider, protocol, algorithm, keyStore, keyAlias, trustStore, 0);
    }

    public SSLSocketFactory createSSLFactory(String provider, String protocol, String algorithm, String keyStore, String keyAlias, String trustStore, long timeout) throws GeneralSecurityException {
        SSLContext context = createSSLContext(provider, protocol, algorithm, keyStore, keyAlias, trustStore, timeout);
        return context.getSocketFactory();
    }

    /**
     * Purely check for the availability of provided key stores and key
     *
     * @param keyStore
     * @param keyAlias
     * @param trustStore
     * @param timeout
     */
    private boolean checkForKeystoresAvailability( String keyStore, String keyAlias, String trustStore, long timeout ) throws GeneralSecurityException {
        long start = System.currentTimeMillis();
        while (true) {
            KeystoreInstance keyInstance = getKeystore(keyStore);
            KeystoreInstance trustInstance = trustStore == null ? null : getKeystore(trustStore);
            if (keyStore != null && keyInstance == null) {
                logger.info( "Keystore {} not found", keyStore );
            } else if (keyStore != null && keyInstance.isKeystoreLocked()) {
                logger.info( "Keystore {} locked", keyStore );
            } else if (keyStore != null && keyAlias != null && keyInstance.isKeyLocked(keyAlias)) {
                logger.info( "Keystore's key {} locked", keyAlias );
            } else if (trustStore != null && trustInstance == null) {
                logger.info( "Truststore {} not found", trustStore );
            } else if (trustStore != null && trustInstance.isKeystoreLocked()) {
                logger.info( "Truststore {} locked", keyStore );
            } else {
                return true;
            }
            if (System.currentTimeMillis() - start < timeout) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new GeneralSecurityException("Interrupted", e);
                }
            } else {
                return false;
            }
        }
    }

}
