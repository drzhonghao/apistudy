import org.apache.poi.poifs.crypt.EncryptionVerifier;
import org.apache.poi.poifs.crypt.CipherAlgorithm;
import org.apache.poi.poifs.crypt.HashAlgorithm;
import org.apache.poi.poifs.crypt.binaryrc4.*;


import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.poifs.crypt.*;
import org.apache.poi.poifs.crypt.standard.EncryptionRecord;
import org.apache.poi.util.LittleEndianByteArrayOutputStream;
import org.apache.poi.util.LittleEndianInput;

public class BinaryRC4EncryptionVerifier extends EncryptionVerifier implements EncryptionRecord, Cloneable {

    protected BinaryRC4EncryptionVerifier() {
        setSpinCount(-1);
        setCipherAlgorithm(CipherAlgorithm.rc4);
        setChainingMode(null);
        setEncryptedKey(null);
        setHashAlgorithm(HashAlgorithm.md5);
    }

    protected BinaryRC4EncryptionVerifier(LittleEndianInput is) {
        byte salt[] = new byte[16];
        is.readFully(salt);
        setSalt(salt);
        byte encryptedVerifier[] = new byte[16];
        is.readFully(encryptedVerifier);
        setEncryptedVerifier(encryptedVerifier);
        byte encryptedVerifierHash[] = new byte[16];
        is.readFully(encryptedVerifierHash);
        setEncryptedVerifierHash(encryptedVerifierHash);
        setSpinCount(-1);
        setCipherAlgorithm(CipherAlgorithm.rc4);
        setChainingMode(null);
        setEncryptedKey(null);
        setHashAlgorithm(HashAlgorithm.md5);
    }

    @Override
    protected void setSalt(byte salt[]) {
        if (salt == null || salt.length != 16) {
            throw new EncryptedDocumentException("invalid verifier salt");
        }
        
        super.setSalt(salt);
    }

    @Override
    protected void setEncryptedVerifier(byte encryptedVerifier[]) {
        super.setEncryptedVerifier(encryptedVerifier);
    }

    @Override
    protected void setEncryptedVerifierHash(byte encryptedVerifierHash[]) {
        super.setEncryptedVerifierHash(encryptedVerifierHash);
    }

    @Override
    public void write(LittleEndianByteArrayOutputStream bos) {
        byte salt[] = getSalt();
        assert (salt.length == 16);
        bos.write(salt);
        byte encryptedVerifier[] = getEncryptedVerifier();
        assert (encryptedVerifier.length == 16);
        bos.write(encryptedVerifier);
        byte encryptedVerifierHash[] = getEncryptedVerifierHash();
        assert (encryptedVerifierHash.length == 16);
        bos.write(encryptedVerifierHash);
    }

    @Override
    public BinaryRC4EncryptionVerifier clone() throws CloneNotSupportedException {
        return (BinaryRC4EncryptionVerifier)super.clone();
    }
}
