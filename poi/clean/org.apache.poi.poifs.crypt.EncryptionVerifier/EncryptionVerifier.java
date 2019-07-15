import org.apache.poi.poifs.crypt.CipherAlgorithm;
import org.apache.poi.poifs.crypt.ChainingMode;
import org.apache.poi.poifs.crypt.HashAlgorithm;
import org.apache.poi.poifs.crypt.*;


/**
 * Used when checking if a key is valid for a document 
 */
public abstract class EncryptionVerifier implements Cloneable {
    private byte[] salt;
    private byte[] encryptedVerifier;
    private byte[] encryptedVerifierHash;
    private byte[] encryptedKey;
    // protected int verifierHashSize;
    private int spinCount;
    private CipherAlgorithm cipherAlgorithm;
    private ChainingMode chainingMode;
    private HashAlgorithm hashAlgorithm;
    
    protected EncryptionVerifier() {}

    public byte[] getSalt() {
        return salt;
    }

    public byte[] getEncryptedVerifier() {
        return encryptedVerifier;
    }    
    
    public byte[] getEncryptedVerifierHash() {
        return encryptedVerifierHash;
    }    
    
    public int getSpinCount() {
        return spinCount;
    }

    public byte[] getEncryptedKey() {
        return encryptedKey;
    }
    
    public CipherAlgorithm getCipherAlgorithm() {
        return cipherAlgorithm;
    }
    
    public HashAlgorithm getHashAlgorithm() {
        return hashAlgorithm;
    }
    
    public ChainingMode getChainingMode() {
        return chainingMode;
    }

    protected void setSalt(byte[] salt) {
        this.salt = (salt == null) ? null : salt.clone();
    }

    protected void setEncryptedVerifier(byte[] encryptedVerifier) {
        this.encryptedVerifier = (encryptedVerifier == null) ? null : encryptedVerifier.clone();
    }

    protected void setEncryptedVerifierHash(byte[] encryptedVerifierHash) {
        this.encryptedVerifierHash = (encryptedVerifierHash == null) ? null : encryptedVerifierHash.clone();
    }

    protected void setEncryptedKey(byte[] encryptedKey) {
        this.encryptedKey = (encryptedKey == null) ? null : encryptedKey.clone();
    }

    protected void setSpinCount(int spinCount) {
        this.spinCount = spinCount;
    }

    protected void setCipherAlgorithm(CipherAlgorithm cipherAlgorithm) {
        this.cipherAlgorithm = cipherAlgorithm;
    }

    protected void setChainingMode(ChainingMode chainingMode) {
        this.chainingMode = chainingMode;
    }

    protected void setHashAlgorithm(HashAlgorithm hashAlgorithm) {
        this.hashAlgorithm = hashAlgorithm;
    }
    
    @Override
    public EncryptionVerifier clone() throws CloneNotSupportedException {
        EncryptionVerifier other = (EncryptionVerifier)super.clone();
        other.salt = (salt == null) ? null : salt.clone();
        other.encryptedVerifier = (encryptedVerifier == null) ? null : encryptedVerifier.clone();
        other.encryptedVerifierHash = (encryptedVerifierHash == null) ? null : encryptedVerifierHash.clone();
        other.encryptedKey = (encryptedKey == null) ? null : encryptedKey.clone();
        return other;
    }
}
