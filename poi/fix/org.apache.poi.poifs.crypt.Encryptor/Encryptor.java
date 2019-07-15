

import java.io.IOException;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.Key;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.poifs.crypt.ChunkedCipherOutputStream;
import org.apache.poi.poifs.crypt.Decryptor;
import org.apache.poi.poifs.crypt.EncryptionInfo;
import org.apache.poi.poifs.filesystem.DirectoryNode;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;


public abstract class Encryptor implements Cloneable {
	protected static final String DEFAULT_POIFS_ENTRY = Decryptor.DEFAULT_POIFS_ENTRY;

	private EncryptionInfo encryptionInfo;

	private SecretKey secretKey;

	public abstract OutputStream getDataStream(DirectoryNode dir) throws IOException, GeneralSecurityException;

	public abstract void confirmPassword(String password, byte[] keySpec, byte[] keySalt, byte[] verifier, byte[] verifierSalt, byte[] integritySalt);

	public abstract void confirmPassword(String password);

	public static Encryptor getInstance(EncryptionInfo info) {
		return null;
	}

	public OutputStream getDataStream(POIFSFileSystem fs) throws IOException, GeneralSecurityException {
		return getDataStream(fs.getRoot());
	}

	public ChunkedCipherOutputStream getDataStream(OutputStream stream, int initialOffset) throws IOException, GeneralSecurityException {
		throw new EncryptedDocumentException("this decryptor doesn't support writing directly to a stream");
	}

	public SecretKey getSecretKey() {
		return secretKey;
	}

	public void setSecretKey(SecretKey secretKey) {
		this.secretKey = secretKey;
	}

	public EncryptionInfo getEncryptionInfo() {
		return encryptionInfo;
	}

	public void setEncryptionInfo(EncryptionInfo encryptionInfo) {
		this.encryptionInfo = encryptionInfo;
	}

	public void setChunkSize(int chunkSize) {
		throw new EncryptedDocumentException("this decryptor doesn't support changing the chunk size");
	}

	@Override
	public Encryptor clone() throws CloneNotSupportedException {
		Encryptor other = ((Encryptor) (super.clone()));
		other.secretKey = new SecretKeySpec(secretKey.getEncoded(), secretKey.getAlgorithm());
		return other;
	}
}

