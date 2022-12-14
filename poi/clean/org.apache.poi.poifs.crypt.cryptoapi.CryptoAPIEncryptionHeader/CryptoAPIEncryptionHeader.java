import org.apache.poi.poifs.crypt.cryptoapi.*;


import java.io.IOException;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.poifs.crypt.ChainingMode;
import org.apache.poi.poifs.crypt.CipherAlgorithm;
import org.apache.poi.poifs.crypt.CipherProvider;
import org.apache.poi.poifs.crypt.HashAlgorithm;
import org.apache.poi.poifs.crypt.standard.StandardEncryptionHeader;
import org.apache.poi.util.LittleEndianInput;

public class CryptoAPIEncryptionHeader extends StandardEncryptionHeader implements Cloneable {

    public CryptoAPIEncryptionHeader(LittleEndianInput is) throws IOException {
        super(is);
    }

    protected CryptoAPIEncryptionHeader(CipherAlgorithm cipherAlgorithm,
            HashAlgorithm hashAlgorithm, int keyBits, int blockSize,
            ChainingMode chainingMode) {
        super(cipherAlgorithm, hashAlgorithm, keyBits, blockSize, chainingMode);
    }

    @Override
    public void setKeySize(int keyBits) {
        // Microsoft Base Cryptographic Provider is limited up to 40 bits
        // http://msdn.microsoft.com/en-us/library/windows/desktop/aa375599(v=vs.85).aspx
        boolean found = false;
        for (int size : getCipherAlgorithm().allowedKeySize) {
            if (size == keyBits) {
                found = true;
                break;
            }
        }
        if (!found) {
            throw new EncryptedDocumentException("invalid keysize "+keyBits+" for cipher algorithm "+getCipherAlgorithm());
        }
        super.setKeySize(keyBits);
        if (keyBits > 40) {
            setCspName("Microsoft Enhanced Cryptographic Provider v1.0");
        } else {
            setCspName(CipherProvider.rc4.cipherProviderName);
        }
    }

    @Override
    public CryptoAPIEncryptionHeader clone() throws CloneNotSupportedException {
        return (CryptoAPIEncryptionHeader)super.clone();
    }
}
