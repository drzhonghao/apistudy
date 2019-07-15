

import java.io.IOException;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.poifs.crypt.ChainingMode;
import org.apache.poi.poifs.crypt.CipherAlgorithm;
import org.apache.poi.poifs.crypt.EncryptionInfo;
import org.apache.poi.poifs.crypt.EncryptionInfoBuilder;
import org.apache.poi.poifs.crypt.HashAlgorithm;
import org.apache.poi.util.LittleEndianInput;


public class StandardEncryptionInfoBuilder implements EncryptionInfoBuilder {
	@Override
	public void initialize(EncryptionInfo info, LittleEndianInput dis) throws IOException {
		dis.readInt();
		if (((info.getVersionMinor()) == 2) && (((info.getVersionMajor()) == 3) || ((info.getVersionMajor()) == 4))) {
		}
	}

	@Override
	public void initialize(EncryptionInfo info, CipherAlgorithm cipherAlgorithm, HashAlgorithm hashAlgorithm, int keyBits, int blockSize, ChainingMode chainingMode) {
		if (cipherAlgorithm == null) {
			cipherAlgorithm = CipherAlgorithm.aes128;
		}
		if (((cipherAlgorithm != (CipherAlgorithm.aes128)) && (cipherAlgorithm != (CipherAlgorithm.aes192))) && (cipherAlgorithm != (CipherAlgorithm.aes256))) {
			throw new EncryptedDocumentException("Standard encryption only supports AES128/192/256.");
		}
		if (hashAlgorithm == null) {
			hashAlgorithm = HashAlgorithm.sha1;
		}
		if (hashAlgorithm != (HashAlgorithm.sha1)) {
			throw new EncryptedDocumentException("Standard encryption only supports SHA-1.");
		}
		if (chainingMode == null) {
			chainingMode = ChainingMode.ecb;
		}
		if (chainingMode != (ChainingMode.ecb)) {
			throw new EncryptedDocumentException("Standard encryption only supports ECB chaining.");
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
	}
}

