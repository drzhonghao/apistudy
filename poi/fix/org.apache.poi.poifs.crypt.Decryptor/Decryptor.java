

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.Key;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.poifs.crypt.EncryptionHeader;
import org.apache.poi.poifs.crypt.EncryptionInfo;
import org.apache.poi.poifs.filesystem.DirectoryNode;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;


public abstract class Decryptor implements Cloneable {
	public static final String DEFAULT_PASSWORD = "VelvetSweatshop";

	public static final String DEFAULT_POIFS_ENTRY = "EncryptedPackage";

	protected EncryptionInfo encryptionInfo;

	private SecretKey secretKey;

	private byte[] verifier;

	private byte[] integrityHmacKey;

	private byte[] integrityHmacValue;

	protected Decryptor() {
	}

	public abstract InputStream getDataStream(DirectoryNode dir) throws IOException, GeneralSecurityException;

	public InputStream getDataStream(InputStream stream, int size, int initialPos) throws IOException, GeneralSecurityException {
		throw new EncryptedDocumentException("this decryptor doesn't support reading from a stream");
	}

	public void setChunkSize(int chunkSize) {
		throw new EncryptedDocumentException("this decryptor doesn't support changing the chunk size");
	}

	public Cipher initCipherForBlock(Cipher cipher, int block) throws GeneralSecurityException {
		throw new EncryptedDocumentException("this decryptor doesn't support initCipherForBlock");
	}

	public abstract boolean verifyPassword(String password) throws GeneralSecurityException;

	public abstract long getLength();

	public static Decryptor getInstance(EncryptionInfo info) {
		return null;
	}

	public InputStream getDataStream(POIFSFileSystem fs) throws IOException, GeneralSecurityException {
		return getDataStream(fs.getRoot());
	}

	public byte[] getVerifier() {
		return verifier;
	}

	public SecretKey getSecretKey() {
		return secretKey;
	}

	public byte[] getIntegrityHmacKey() {
		return integrityHmacKey;
	}

	@SuppressWarnings("unused")
	public byte[] getIntegrityHmacValue() {
		return integrityHmacValue;
	}

	protected void setSecretKey(SecretKey secretKey) {
		this.secretKey = secretKey;
	}

	protected void setVerifier(byte[] verifier) {
		this.verifier = (verifier == null) ? null : verifier.clone();
	}

	protected void setIntegrityHmacKey(byte[] integrityHmacKey) {
		this.integrityHmacKey = (integrityHmacKey == null) ? null : integrityHmacKey.clone();
	}

	protected void setIntegrityHmacValue(byte[] integrityHmacValue) {
		this.integrityHmacValue = (integrityHmacValue == null) ? null : integrityHmacValue.clone();
	}

	@SuppressWarnings("unused")
	protected int getBlockSizeInBytes() {
		return encryptionInfo.getHeader().getBlockSize();
	}

	protected int getKeySizeInBytes() {
		return (encryptionInfo.getHeader().getKeySize()) / 8;
	}

	public EncryptionInfo getEncryptionInfo() {
		return encryptionInfo;
	}

	public void setEncryptionInfo(EncryptionInfo encryptionInfo) {
		this.encryptionInfo = encryptionInfo;
	}

	@Override
	public Decryptor clone() throws CloneNotSupportedException {
		Decryptor other = ((Decryptor) (super.clone()));
		other.integrityHmacKey = integrityHmacKey.clone();
		other.integrityHmacValue = integrityHmacValue.clone();
		other.verifier = verifier.clone();
		other.secretKey = new SecretKeySpec(secretKey.getEncoded(), secretKey.getAlgorithm());
		return other;
	}
}

