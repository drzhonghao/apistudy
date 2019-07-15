

import com.microsoft.schemas.office.x2006.encryption.EncryptionDocument;
import java.io.IOException;
import java.io.InputStream;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ooxml.POIXMLTypeLoader;
import org.apache.poi.poifs.crypt.ChainingMode;
import org.apache.poi.poifs.crypt.CipherAlgorithm;
import org.apache.poi.poifs.crypt.EncryptionInfo;
import org.apache.poi.poifs.crypt.EncryptionInfoBuilder;
import org.apache.poi.poifs.crypt.EncryptionMode;
import org.apache.poi.poifs.crypt.HashAlgorithm;
import org.apache.poi.poifs.crypt.agile.AgileEncryptionHeader;
import org.apache.poi.poifs.crypt.agile.AgileEncryptionVerifier;
import org.apache.poi.util.LittleEndianInput;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;

import static com.microsoft.schemas.office.x2006.encryption.EncryptionDocument.Factory.parse;


public class AgileEncryptionInfoBuilder implements EncryptionInfoBuilder {
	@Override
	public void initialize(EncryptionInfo info, LittleEndianInput dis) throws IOException {
		EncryptionDocument ed = AgileEncryptionInfoBuilder.parseDescriptor(((InputStream) (dis)));
		if (((info.getVersionMajor()) == (EncryptionMode.agile.versionMajor)) && ((info.getVersionMinor()) == (EncryptionMode.agile.versionMinor))) {
		}
	}

	@Override
	public void initialize(EncryptionInfo info, CipherAlgorithm cipherAlgorithm, HashAlgorithm hashAlgorithm, int keyBits, int blockSize, ChainingMode chainingMode) {
		if (cipherAlgorithm == null) {
			cipherAlgorithm = CipherAlgorithm.aes128;
		}
		if (cipherAlgorithm == (CipherAlgorithm.rc4)) {
			throw new EncryptedDocumentException("RC4 must not be used with agile encryption.");
		}
		if (hashAlgorithm == null) {
			hashAlgorithm = HashAlgorithm.sha1;
		}
		if (chainingMode == null) {
			chainingMode = ChainingMode.cbc;
		}
		if (!((chainingMode == (ChainingMode.cbc)) || (chainingMode == (ChainingMode.cfb)))) {
			throw new EncryptedDocumentException("Agile encryption only supports CBC/CFB chaining.");
		}
		if (keyBits == (-1)) {
			keyBits = cipherAlgorithm.defaultKeySize;
		}
		if (blockSize == (-1)) {
			blockSize = cipherAlgorithm.blockSize;
		}
		boolean found = false;
		for (int ks : cipherAlgorithm.allowedKeySize) {
			found |= ks == keyBits;
		}
		if (!found) {
			throw new EncryptedDocumentException(((("KeySize " + keyBits) + " not allowed for Cipher ") + cipherAlgorithm));
		}
		info.setHeader(new AgileEncryptionHeader(cipherAlgorithm, hashAlgorithm, keyBits, blockSize, chainingMode));
		info.setVerifier(new AgileEncryptionVerifier(cipherAlgorithm, hashAlgorithm, keyBits, blockSize, chainingMode));
	}

	protected static EncryptionDocument parseDescriptor(String descriptor) {
		try {
			return EncryptionDocument.Factory.parse(descriptor, POIXMLTypeLoader.DEFAULT_XML_OPTIONS);
		} catch (XmlException e) {
			throw new EncryptedDocumentException("Unable to parse encryption descriptor", e);
		}
	}

	protected static EncryptionDocument parseDescriptor(InputStream descriptor) {
		try {
			return parse(descriptor, POIXMLTypeLoader.DEFAULT_XML_OPTIONS);
		} catch (Exception e) {
			throw new EncryptedDocumentException("Unable to parse encryption descriptor", e);
		}
	}
}

