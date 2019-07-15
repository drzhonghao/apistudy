

import com.microsoft.schemas.office.x2006.encryption.CTDataIntegrity;
import com.microsoft.schemas.office.x2006.encryption.CTEncryption;
import com.microsoft.schemas.office.x2006.encryption.CTKeyData;
import com.microsoft.schemas.office.x2006.encryption.EncryptionDocument;
import com.microsoft.schemas.office.x2006.encryption.STCipherAlgorithm;
import com.microsoft.schemas.office.x2006.encryption.STCipherChaining;
import com.microsoft.schemas.office.x2006.encryption.STHashAlgorithm;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.poifs.crypt.ChainingMode;
import org.apache.poi.poifs.crypt.CipherAlgorithm;
import org.apache.poi.poifs.crypt.CipherProvider;
import org.apache.poi.poifs.crypt.EncryptionHeader;
import org.apache.poi.poifs.crypt.HashAlgorithm;
import org.apache.xmlbeans.StringEnumAbstractBase;


public class AgileEncryptionHeader extends EncryptionHeader implements Cloneable {
	private byte[] encryptedHmacKey;

	private byte[] encryptedHmacValue;

	public AgileEncryptionHeader(String descriptor) {
	}

	protected AgileEncryptionHeader(EncryptionDocument ed) {
		CTKeyData keyData;
		try {
			keyData = ed.getEncryption().getKeyData();
			if (keyData == null) {
				throw new NullPointerException("keyData not set");
			}
		} catch (Exception e) {
			throw new EncryptedDocumentException("Unable to parse keyData");
		}
		int keyBits = ((int) (keyData.getKeyBits()));
		CipherAlgorithm ca = CipherAlgorithm.fromXmlId(keyData.getCipherAlgorithm().toString(), keyBits);
		setCipherAlgorithm(ca);
		setCipherProvider(ca.provider);
		setKeySize(keyBits);
		setFlags(0);
		setSizeExtra(0);
		setCspName(null);
		setBlockSize(keyData.getBlockSize());
		switch (keyData.getCipherChaining().intValue()) {
			case STCipherChaining.INT_CHAINING_MODE_CBC :
				setChainingMode(ChainingMode.cbc);
				break;
			case STCipherChaining.INT_CHAINING_MODE_CFB :
				setChainingMode(ChainingMode.cfb);
				break;
			default :
				throw new EncryptedDocumentException(("Unsupported chaining mode - " + (keyData.getCipherChaining())));
		}
		int hashSize = keyData.getHashSize();
		HashAlgorithm ha = HashAlgorithm.fromEcmaId(keyData.getHashAlgorithm().toString());
		setHashAlgorithm(ha);
		if ((getHashAlgorithm().hashSize) != hashSize) {
			throw new EncryptedDocumentException((((("Unsupported hash algorithm: " + (keyData.getHashAlgorithm())) + " @ ") + hashSize) + " bytes"));
		}
		int saltLength = keyData.getSaltSize();
		setKeySalt(keyData.getSaltValue());
		if ((getKeySalt().length) != saltLength) {
			throw new EncryptedDocumentException("Invalid salt length");
		}
		CTDataIntegrity di = ed.getEncryption().getDataIntegrity();
		setEncryptedHmacKey(di.getEncryptedHmacKey());
		setEncryptedHmacValue(di.getEncryptedHmacValue());
	}

	public AgileEncryptionHeader(CipherAlgorithm algorithm, HashAlgorithm hashAlgorithm, int keyBits, int blockSize, ChainingMode chainingMode) {
		setCipherAlgorithm(algorithm);
		setHashAlgorithm(hashAlgorithm);
		setKeySize(keyBits);
		setBlockSize(blockSize);
		setChainingMode(chainingMode);
	}

	@Override
	protected void setKeySalt(byte[] salt) {
		if ((salt == null) || ((salt.length) != (getBlockSize()))) {
			throw new EncryptedDocumentException("invalid verifier salt");
		}
		super.setKeySalt(salt);
	}

	public byte[] getEncryptedHmacKey() {
		return encryptedHmacKey;
	}

	protected void setEncryptedHmacKey(byte[] encryptedHmacKey) {
		this.encryptedHmacKey = (encryptedHmacKey == null) ? null : encryptedHmacKey.clone();
	}

	public byte[] getEncryptedHmacValue() {
		return encryptedHmacValue;
	}

	protected void setEncryptedHmacValue(byte[] encryptedHmacValue) {
		this.encryptedHmacValue = (encryptedHmacValue == null) ? null : encryptedHmacValue.clone();
	}

	@Override
	public AgileEncryptionHeader clone() throws CloneNotSupportedException {
		AgileEncryptionHeader other = ((AgileEncryptionHeader) (super.clone()));
		other.encryptedHmacKey = ((encryptedHmacKey) == null) ? null : encryptedHmacKey.clone();
		other.encryptedHmacValue = ((encryptedHmacValue) == null) ? null : encryptedHmacValue.clone();
		return other;
	}
}

