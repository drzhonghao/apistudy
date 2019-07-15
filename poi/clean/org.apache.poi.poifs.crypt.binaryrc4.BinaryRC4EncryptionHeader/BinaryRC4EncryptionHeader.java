import org.apache.poi.poifs.crypt.binaryrc4.*;


import org.apache.poi.poifs.crypt.CipherAlgorithm;
import org.apache.poi.poifs.crypt.CipherProvider;
import org.apache.poi.poifs.crypt.EncryptionHeader;
import org.apache.poi.poifs.crypt.HashAlgorithm;
import org.apache.poi.poifs.crypt.standard.EncryptionRecord;
import org.apache.poi.util.LittleEndianByteArrayOutputStream;

public class BinaryRC4EncryptionHeader extends EncryptionHeader implements EncryptionRecord, Cloneable {

    protected BinaryRC4EncryptionHeader() {
        setCipherAlgorithm(CipherAlgorithm.rc4);
        setKeySize(40);
        setBlockSize(-1);
        setCipherProvider(CipherProvider.rc4);
        setHashAlgorithm(HashAlgorithm.md5);
        setSizeExtra(0);
        setFlags(0);
        setCspName("");
        setChainingMode(null);
    }

    @Override
    public void write(LittleEndianByteArrayOutputStream littleendianbytearrayoutputstream) {
    }

    @Override
    public BinaryRC4EncryptionHeader clone() throws CloneNotSupportedException {
        return (BinaryRC4EncryptionHeader)super.clone();
    }
    
    
}
