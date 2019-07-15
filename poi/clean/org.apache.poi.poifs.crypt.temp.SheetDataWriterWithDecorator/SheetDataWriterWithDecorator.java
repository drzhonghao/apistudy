import org.apache.poi.poifs.crypt.temp.*;


import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.SecretKeySpec;

import org.apache.poi.poifs.crypt.ChainingMode;
import org.apache.poi.poifs.crypt.CipherAlgorithm;
import org.apache.poi.poifs.crypt.CryptoFunctions;
import org.apache.poi.util.Beta;
import org.apache.poi.xssf.streaming.SheetDataWriter;

@Beta
public class SheetDataWriterWithDecorator extends SheetDataWriter {
    final static CipherAlgorithm cipherAlgorithm = CipherAlgorithm.aes128;
    SecretKeySpec skeySpec;
    byte[] ivBytes;
    
    public SheetDataWriterWithDecorator() throws IOException {
        super();
    }
    
    void init() {
        if(skeySpec == null) {
            SecureRandom sr = new SecureRandom();
            ivBytes = new byte[16];
            byte[] keyBytes = new byte[16];
            sr.nextBytes(ivBytes);
            sr.nextBytes(keyBytes);
            skeySpec = new SecretKeySpec(keyBytes, cipherAlgorithm.jceId);
        }
    }

    @Override
    protected OutputStream decorateOutputStream(FileOutputStream fos) {
        init();
        Cipher ciEnc = CryptoFunctions.getCipher(skeySpec, cipherAlgorithm, ChainingMode.cbc, ivBytes, Cipher.ENCRYPT_MODE, "PKCS5Padding");
        return new CipherOutputStream(fos, ciEnc);
    }
    
    @Override
    protected InputStream decorateInputStream(FileInputStream fis) {
        Cipher ciDec = CryptoFunctions.getCipher(skeySpec, cipherAlgorithm, ChainingMode.cbc, ivBytes, Cipher.DECRYPT_MODE, "PKCS5Padding");
        return new CipherInputStream(fis, ciDec);
    }
}
