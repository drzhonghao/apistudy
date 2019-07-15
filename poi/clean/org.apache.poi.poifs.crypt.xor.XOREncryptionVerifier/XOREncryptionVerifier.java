import org.apache.poi.poifs.crypt.xor.*;


import org.apache.poi.poifs.crypt.EncryptionVerifier;
import org.apache.poi.poifs.crypt.standard.EncryptionRecord;
import org.apache.poi.util.LittleEndianByteArrayOutputStream;
import org.apache.poi.util.LittleEndianInput;

public class XOREncryptionVerifier extends EncryptionVerifier implements EncryptionRecord, Cloneable {

    protected XOREncryptionVerifier() {
        setEncryptedKey(new byte[2]);
        setEncryptedVerifier(new byte[2]);
    }

    protected XOREncryptionVerifier(LittleEndianInput is) {
        /**
         * key (2 bytes): An unsigned integer that specifies the obfuscation key. 
         * See [MS-OFFCRYPTO], 2.3.6.2 section, the first step of initializing XOR
         * array where it describes the generation of 16-bit XorKey value.
         */
        byte key[] = new byte[2];
        is.readFully(key);
        setEncryptedKey(key);
        
        /**
         * verificationBytes (2 bytes): An unsigned integer that specifies
         * the password verification identifier.
         */
        byte verifier[] = new byte[2];
        is.readFully(verifier);
        setEncryptedVerifier(verifier);
    }
    
    @Override
    public void write(LittleEndianByteArrayOutputStream bos) {
        bos.write(getEncryptedKey());
        bos.write(getEncryptedVerifier());
    }

    @Override
    public XOREncryptionVerifier clone() throws CloneNotSupportedException {
        return (XOREncryptionVerifier)super.clone();
    }

    @Override
    protected final void setEncryptedVerifier(byte[] encryptedVerifier) {
        super.setEncryptedVerifier(encryptedVerifier);
    }

    @Override
    protected final void setEncryptedKey(byte[] encryptedKey) {
        super.setEncryptedKey(encryptedKey);
    }
}
