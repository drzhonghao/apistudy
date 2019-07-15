import org.apache.poi.poifs.crypt.cryptoapi.CryptoAPIEncryptor;
import org.apache.poi.poifs.crypt.cryptoapi.*;


import java.io.ByteArrayOutputStream;
import java.security.GeneralSecurityException;

import javax.crypto.Cipher;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.util.Internal;

/**
 * An OutputStream for the document entries within the encrypted stream
 */
@Internal
/* package */ class CryptoAPIDocumentOutputStream extends ByteArrayOutputStream {
    private final Cipher cipher;
    private final CryptoAPIEncryptor encryptor;
    private final byte oneByte[] = { 0 };

    public CryptoAPIDocumentOutputStream(CryptoAPIEncryptor encryptor) throws GeneralSecurityException {
        this.encryptor = encryptor;
        cipher = encryptor.initCipherForBlock(null, 0);
    }
    
    public byte[] getBuf() {
        return buf;
    }
    
    public void setSize(int count) {
        this.count = count;
    }
    
    public void setBlock(int block) throws GeneralSecurityException {
        encryptor.initCipherForBlock(cipher, block);
    }
    
    @Override
    public void write(int b) {
        try {
            oneByte[0] = (byte)b;
            cipher.update(oneByte, 0, 1, oneByte, 0);
            super.write(oneByte);
        } catch (Exception e) {
            throw new EncryptedDocumentException(e);
        }
    }

    @Override
    public void write(byte[] b, int off, int len) {
        try {
            cipher.update(b, off, len, b, off);
            super.write(b, off, len);
        } catch (Exception e) {
            throw new EncryptedDocumentException(e);
        }
    }

}
