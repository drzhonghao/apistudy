import org.apache.poi.poifs.crypt.CipherAlgorithm;
import org.apache.poi.poifs.crypt.HashAlgorithm;
import org.apache.poi.poifs.crypt.CipherProvider;
import org.apache.poi.poifs.crypt.ChainingMode;
import org.apache.poi.poifs.crypt.*;


import org.apache.poi.EncryptedDocumentException;

/**
 * Reads and processes OOXML Encryption Headers
 * The constants are largely based on ZIP constants.
 */
public abstract class EncryptionHeader implements Cloneable {
    public static final int ALGORITHM_RC4 = CipherAlgorithm.rc4.ecmaId;
    public static final int ALGORITHM_AES_128 = CipherAlgorithm.aes128.ecmaId;
    public static final int ALGORITHM_AES_192 = CipherAlgorithm.aes192.ecmaId;
    public static final int ALGORITHM_AES_256 = CipherAlgorithm.aes256.ecmaId;
    
    public static final int HASH_NONE   = HashAlgorithm.none.ecmaId;
    public static final int HASH_SHA1   = HashAlgorithm.sha1.ecmaId;
    public static final int HASH_SHA256 = HashAlgorithm.sha256.ecmaId;
    public static final int HASH_SHA384 = HashAlgorithm.sha384.ecmaId;
    public static final int HASH_SHA512 = HashAlgorithm.sha512.ecmaId;

    public static final int PROVIDER_RC4 = CipherProvider.rc4.ecmaId;
    public static final int PROVIDER_AES = CipherProvider.aes.ecmaId;

    public static final int MODE_ECB = ChainingMode.ecb.ecmaId;
    public static final int MODE_CBC = ChainingMode.cbc.ecmaId;
    public static final int MODE_CFB = ChainingMode.cfb.ecmaId;
    
    private int flags;
    private int sizeExtra;
    private CipherAlgorithm cipherAlgorithm;
    private HashAlgorithm hashAlgorithm;
    private int keyBits;
    private int blockSize;
    private CipherProvider providerType;
    private ChainingMode chainingMode;
    private byte[] keySalt;
    private String cspName;
    
    protected EncryptionHeader() {}

    public ChainingMode getChainingMode() {
        return chainingMode;
    }
    
    protected void setChainingMode(ChainingMode chainingMode) {
        this.chainingMode = chainingMode;
    }

    public int getFlags() {
        return flags;
    }
    
    protected void setFlags(int flags) {
        this.flags = flags;
    }

    public int getSizeExtra() {
        return sizeExtra;
    }
    
    protected void setSizeExtra(int sizeExtra) {
        this.sizeExtra = sizeExtra;
    }

    public CipherAlgorithm getCipherAlgorithm() {
        return cipherAlgorithm;
    }
    
    protected void setCipherAlgorithm(CipherAlgorithm cipherAlgorithm) {
        this.cipherAlgorithm = cipherAlgorithm;
        if (cipherAlgorithm.allowedKeySize.length == 1) {
            setKeySize(cipherAlgorithm.defaultKeySize);
        }
    }

    public HashAlgorithm getHashAlgorithm() {
        return hashAlgorithm;
    }

    protected void setHashAlgorithm(HashAlgorithm hashAlgorithm) {
        this.hashAlgorithm = hashAlgorithm;
    }

    public int getKeySize() {
        return keyBits;
    }
    
    /**
     * Sets the keySize (in bits). Before calling this method, make sure
     * to set the cipherAlgorithm, as the amount of keyBits gets validated against
     * the list of allowed keyBits of the corresponding cipherAlgorithm
     *
     * @param keyBits
     */
    protected void setKeySize(int keyBits) {
        this.keyBits = keyBits;
        for (int allowedBits : getCipherAlgorithm().allowedKeySize) {
            if (allowedBits == keyBits) {
                return;
            }
        }
        throw new EncryptedDocumentException("KeySize "+keyBits+" not allowed for cipher "+getCipherAlgorithm());
    }

    public int getBlockSize() {
    	return blockSize;
    }
    
    protected void setBlockSize(int blockSize) {
        this.blockSize = blockSize;
    }
    
    public byte[] getKeySalt() {
        return keySalt;
    }
    
    protected void setKeySalt(byte salt[]) {
        this.keySalt = (salt == null) ? null : salt.clone();
    }

    public CipherProvider getCipherProvider() {
        return providerType;
    }    

    protected void setCipherProvider(CipherProvider providerType) {
        this.providerType = providerType;
    }
    
    public String getCspName() {
        return cspName;
    }
    
    protected void setCspName(String cspName) {
        this.cspName = cspName;
    }

    @Override
    public EncryptionHeader clone() throws CloneNotSupportedException {
        EncryptionHeader other = (EncryptionHeader)super.clone();
        other.keySalt = (keySalt == null) ? null : keySalt.clone();
        return other;
    }
}
