import org.apache.karaf.shell.ssh.keygenerator.PemWriter;
import org.apache.karaf.shell.ssh.keygenerator.OpenSSHKeyPairGenerator;
import org.apache.karaf.shell.ssh.keygenerator.*;


import static java.util.Collections.singleton;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.spec.InvalidKeySpecException;

import org.apache.commons.ssl.PKCS8Key;
import org.apache.sshd.common.keyprovider.AbstractKeyPairProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenSSHKeyPairProvider extends AbstractKeyPairProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenSSHKeyPairProvider.class);
    private File keyFile;
    private String password;
    private KeyPair cachedKey;
    private String algorithm;
    private int keySize;

    public OpenSSHKeyPairProvider(File keyFile, String algorithm, int keySize) {
        this.keyFile = keyFile;
        this.algorithm = algorithm;
        this.keySize = keySize;
    }

    @Override
    public synchronized Iterable<KeyPair> loadKeys() {
        if (cachedKey != null) {
            return singleton(cachedKey);
        }
        if (!keyFile.exists()) {
            createServerKey();
        }
        try (FileInputStream is = new FileInputStream(keyFile)) {
            KeyPair kp = getKeyPair(is);
            cachedKey = kp;
            return singleton(kp);
        } catch (Exception e) {
            LOGGER.warn("Failed to parse keypair in {}. Attempting to parse it as a legacy 'simple' key", keyFile);
            try {
                KeyPair kp = convertLegacyKey(keyFile);
                LOGGER.info("Successfully loaded legacy simple key. Converted to PEM format");
                cachedKey = kp;
                return singleton(kp);
            } catch (Exception nested) {
                LOGGER.warn(keyFile+" is not a 'simple' key either",nested);
            }
            throw new RuntimeException(e);
        }
    }

    private KeyPair getKeyPair(FileInputStream is) throws GeneralSecurityException, IOException {
        PKCS8Key pkcs8 = new PKCS8Key(is, password == null ? null : password.toCharArray());
        KeyPair kp = new KeyPair(pkcs8.getPublicKey(), pkcs8.getPrivateKey());
        return kp;
    }


    private KeyPair convertLegacyKey(File keyFile) throws GeneralSecurityException, IOException {
        KeyPair keypair = null;
        try (ObjectInputStream r = new ObjectInputStream(new FileInputStream(keyFile))) {
            keypair = (KeyPair)r.readObject();
        }
        catch (ClassNotFoundException e) {
            throw new InvalidKeySpecException("Missing classes: " + e.getMessage(), e);
        }
        new PemWriter(keyFile).writeKeyPair(algorithm, keypair);
        return keypair;
    }

    private void createServerKey() {
        try {
            LOGGER.info("Creating ssh server key at " + keyFile);
            KeyPair kp = new OpenSSHKeyPairGenerator(algorithm, keySize).generate();
            new PemWriter(keyFile).writeKeyPair(algorithm, kp);
        } catch (Exception e) {
            throw new RuntimeException("Key file generation failed", e);
        }
    }
}
