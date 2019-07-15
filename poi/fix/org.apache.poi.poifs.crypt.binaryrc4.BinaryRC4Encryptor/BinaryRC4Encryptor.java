

import java.io.File;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Random;
import javax.crypto.Cipher;
import org.apache.poi.poifs.crypt.ChunkedCipherOutputStream;
import org.apache.poi.poifs.crypt.CryptoFunctions;
import org.apache.poi.poifs.crypt.DataSpaceMapUtils;
import org.apache.poi.poifs.crypt.EncryptionHeader;
import org.apache.poi.poifs.crypt.EncryptionInfo;
import org.apache.poi.poifs.crypt.EncryptionVerifier;
import org.apache.poi.poifs.crypt.Encryptor;
import org.apache.poi.poifs.crypt.HashAlgorithm;
import org.apache.poi.poifs.crypt.binaryrc4.BinaryRC4EncryptionHeader;
import org.apache.poi.poifs.crypt.binaryrc4.BinaryRC4EncryptionVerifier;
import org.apache.poi.poifs.crypt.standard.EncryptionRecord;
import org.apache.poi.poifs.filesystem.DirectoryNode;
import org.apache.poi.poifs.filesystem.DocumentEntry;
import org.apache.poi.util.LittleEndianByteArrayOutputStream;


public class BinaryRC4Encryptor extends Encryptor implements Cloneable {
	private int chunkSize = 512;

	protected BinaryRC4Encryptor() {
	}

	@Override
	public void confirmPassword(String password) {
		Random r = new SecureRandom();
		byte[] salt = new byte[16];
		byte[] verifier = new byte[16];
		r.nextBytes(salt);
		r.nextBytes(verifier);
		confirmPassword(password, null, null, verifier, salt, null);
	}

	@Override
	public void confirmPassword(String password, byte[] keySpec, byte[] keySalt, byte[] verifier, byte[] verifierSalt, byte[] integritySalt) {
		BinaryRC4EncryptionVerifier ver = ((BinaryRC4EncryptionVerifier) (getEncryptionInfo().getVerifier()));
		byte[] encryptedVerifier = new byte[16];
		HashAlgorithm hashAlgo = ver.getHashAlgorithm();
		MessageDigest hashAlg = CryptoFunctions.getMessageDigest(hashAlgo);
		byte[] calcVerifierHash = hashAlg.digest(verifier);
	}

	@Override
	public OutputStream getDataStream(DirectoryNode dir) throws IOException, GeneralSecurityException {
		return new BinaryRC4Encryptor.BinaryRC4CipherOutputStream(dir);
	}

	@Override
	public BinaryRC4Encryptor.BinaryRC4CipherOutputStream getDataStream(OutputStream stream, int initialOffset) throws IOException, GeneralSecurityException {
		return new BinaryRC4Encryptor.BinaryRC4CipherOutputStream(stream);
	}

	protected int getKeySizeInBytes() {
		return (getEncryptionInfo().getHeader().getKeySize()) / 8;
	}

	protected void createEncryptionInfoEntry(DirectoryNode dir) throws IOException {
		DataSpaceMapUtils.addDefaultDataSpace(dir);
		final EncryptionInfo info = getEncryptionInfo();
		final BinaryRC4EncryptionHeader header = ((BinaryRC4EncryptionHeader) (info.getHeader()));
		final BinaryRC4EncryptionVerifier verifier = ((BinaryRC4EncryptionVerifier) (info.getVerifier()));
		EncryptionRecord er = new EncryptionRecord() {
			@Override
			public void write(LittleEndianByteArrayOutputStream bos) {
				bos.writeShort(info.getVersionMajor());
				bos.writeShort(info.getVersionMinor());
				header.write(bos);
				verifier.write(bos);
			}
		};
		DataSpaceMapUtils.createEncryptionEntry(dir, "EncryptionInfo", er);
	}

	@Override
	public void setChunkSize(int chunkSize) {
		this.chunkSize = chunkSize;
	}

	@Override
	public BinaryRC4Encryptor clone() throws CloneNotSupportedException {
		return ((BinaryRC4Encryptor) (super.clone()));
	}

	protected class BinaryRC4CipherOutputStream extends ChunkedCipherOutputStream {
		public BinaryRC4CipherOutputStream(OutputStream stream) throws IOException, GeneralSecurityException {
			super(stream, BinaryRC4Encryptor.this.chunkSize);
		}

		public BinaryRC4CipherOutputStream(DirectoryNode dir) throws IOException, GeneralSecurityException {
			super(dir, BinaryRC4Encryptor.this.chunkSize);
		}

		@Override
		protected Cipher initCipherForBlock(Cipher cipher, int block, boolean lastChunk) throws GeneralSecurityException {
			return null;
		}

		@Override
		protected void calculateChecksum(File file, int i) {
		}

		@Override
		protected void createEncryptionInfoEntry(DirectoryNode dir, File tmpFile) throws IOException, GeneralSecurityException {
			BinaryRC4Encryptor.this.createEncryptionInfoEntry(dir);
		}

		@Override
		public void flush() throws IOException {
			writeChunk(false);
			super.flush();
		}
	}
}

