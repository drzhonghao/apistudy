import org.apache.poi.poifs.crypt.cryptoapi.CryptoAPIDecryptor;
import org.apache.poi.poifs.crypt.cryptoapi.*;


import java.io.ByteArrayInputStream;
import java.security.GeneralSecurityException;

import javax.crypto.Cipher;
import javax.crypto.ShortBufferException;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.util.Internal;

/**
 * A seekable InputStream, which is used to decrypt/extract the document entries
 * within the encrypted stream 
 */
@Internal
/* package */ class CryptoAPIDocumentInputStream extends ByteArrayInputStream {
    private Cipher cipher;
    private final CryptoAPIDecryptor decryptor;
    private byte oneByte[] = { 0 };
    
    public void seek(int newpos) {
        if (newpos > count) {
            throw new ArrayIndexOutOfBoundsException(newpos);
        }
        
        this.pos = newpos;
        mark = newpos;
    }

    public void setBlock(int block) throws GeneralSecurityException {
        cipher = decryptor.initCipherForBlock(cipher, block);
    }

    @Override
    public synchronized int read() {
        int ch = super.read();
        if (ch == -1) {
            return -1;
        }
        oneByte[0] = (byte) ch;
        try {
            cipher.update(oneByte, 0, 1, oneByte);
        } catch (ShortBufferException e) {
            throw new EncryptedDocumentException(e);
        }
        return oneByte[0];
    }

    @Override
    public synchronized int read(byte b[], int off, int len) {
        int readLen = super.read(b, off, len);
        if (readLen ==-1) {
            return -1;
        }
        try {
            cipher.update(b, off, readLen, b, off);
        } catch (ShortBufferException e) {
            throw new EncryptedDocumentException(e);
        }
        return readLen;
    }

    public CryptoAPIDocumentInputStream(CryptoAPIDecryptor decryptor, byte buf[])
    throws GeneralSecurityException {
        super(buf);
        this.decryptor = decryptor;
        cipher = decryptor.initCipherForBlock(null, 0);
    }
}
