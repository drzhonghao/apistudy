import org.apache.poi.hslf.record.*;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.poifs.crypt.CipherAlgorithm;
import org.apache.poi.poifs.crypt.EncryptionInfo;
import org.apache.poi.poifs.crypt.EncryptionMode;
import org.apache.poi.poifs.crypt.HashAlgorithm;
import org.apache.poi.poifs.crypt.cryptoapi.CryptoAPIEncryptionHeader;
import org.apache.poi.poifs.crypt.cryptoapi.CryptoAPIEncryptionVerifier;
import org.apache.poi.util.LittleEndian;
import org.apache.poi.util.LittleEndianByteArrayOutputStream;
import org.apache.poi.util.LittleEndianInputStream;

/**
 * A Document Encryption Atom (type 12052). Holds information
 *  on the Encryption of a Document
 *
 * @author Nick Burch
 */
public final class DocumentEncryptionAtom extends PositionDependentRecordAtom {
    private static final long _type = 12052l;
	private final byte[] _header;
	private EncryptionInfo ei;

	/**
	 * For the Document Encryption Atom
	 */
	protected DocumentEncryptionAtom(byte[] source, int start, int len) {
		// Get the header
		_header = new byte[8];
		System.arraycopy(source,start,_header,0,8);

		ByteArrayInputStream bis = new ByteArrayInputStream(source, start+8, len-8);
		try (LittleEndianInputStream leis = new LittleEndianInputStream(bis)) {
			ei = new EncryptionInfo(leis, EncryptionMode.cryptoAPI);
		} catch (IOException e) {
			throw new EncryptedDocumentException(e);
		}
	}

	public DocumentEncryptionAtom() {
	    _header = new byte[8];
	    LittleEndian.putShort(_header, 0, (short)0x000F);
	    LittleEndian.putShort(_header, 2, (short)_type);
	    // record length not yet known ...
	    
	    ei = new EncryptionInfo(EncryptionMode.cryptoAPI);
	}
	
	/**
	 * Initializes the encryption settings
	 *
	 * @param keyBits see {@link CipherAlgorithm#rc4} for allowed values, use -1 for default size
	 */
	public void initializeEncryptionInfo(int keyBits) {
	    ei = new EncryptionInfo(EncryptionMode.cryptoAPI, CipherAlgorithm.rc4, HashAlgorithm.sha1, keyBits, -1, null);
	}
	
	/**
	 * Return the length of the encryption key, in bits
	 */
	public int getKeyLength() {
		return ei.getHeader().getKeySize();
	}

	/**
	 * Return the name of the encryption provider used
	 */
	public String getEncryptionProviderName() {
		return ei.getHeader().getCspName();
	}

	/**
	 * @return the {@link EncryptionInfo} object for details about encryption settings
	 */
	public EncryptionInfo getEncryptionInfo() {
	    return ei;
	}
	
	
	/**
	 * We are of type 12052
	 */
	public long getRecordType() { return _type; }

	/**
	 * Write the contents of the record back, so it can be written
	 *  to disk
	 */
	public void writeOut(OutputStream out) throws IOException {

		// Data
		byte data[] = new byte[1024];
		LittleEndianByteArrayOutputStream bos = new LittleEndianByteArrayOutputStream(data, 0);
		bos.writeShort(ei.getVersionMajor());
		bos.writeShort(ei.getVersionMinor());
		bos.writeInt(ei.getEncryptionFlags());
		
		((CryptoAPIEncryptionHeader)ei.getHeader()).write(bos);
		((CryptoAPIEncryptionVerifier)ei.getVerifier()).write(bos);
		
        // Header
		LittleEndian.putInt(_header, 4, bos.getWriteIndex());
        out.write(_header);
		out.write(data, 0, bos.getWriteIndex());
		bos.close();
	}

    @Override
    public void updateOtherRecordReferences(Map<Integer,Integer> oldToNewReferencesLookup) {
        // nothing to update
    }
}
