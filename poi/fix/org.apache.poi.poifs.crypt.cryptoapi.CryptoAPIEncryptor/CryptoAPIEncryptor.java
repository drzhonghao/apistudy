

import java.io.File;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Random;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.poifs.crypt.ChunkedCipherOutputStream;
import org.apache.poi.poifs.crypt.CryptoFunctions;
import org.apache.poi.poifs.crypt.EncryptionInfo;
import org.apache.poi.poifs.crypt.EncryptionVerifier;
import org.apache.poi.poifs.crypt.Encryptor;
import org.apache.poi.poifs.crypt.HashAlgorithm;
import org.apache.poi.poifs.crypt.cryptoapi.CryptoAPIEncryptionVerifier;
import org.apache.poi.poifs.filesystem.DirectoryNode;
import org.apache.poi.poifs.filesystem.DocumentInputStream;
import org.apache.poi.poifs.filesystem.Entry;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;


public class CryptoAPIEncryptor extends Encryptor implements Cloneable {
	private int chunkSize = 512;

	CryptoAPIEncryptor() {
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
		assert (verifier != null) && (verifierSalt != null);
		CryptoAPIEncryptionVerifier ver = ((CryptoAPIEncryptionVerifier) (getEncryptionInfo().getVerifier()));
		try {
			Cipher cipher = initCipherForBlock(null, 0);
			byte[] encryptedVerifier = new byte[verifier.length];
			cipher.update(verifier, 0, verifier.length, encryptedVerifier);
			HashAlgorithm hashAlgo = ver.getHashAlgorithm();
			MessageDigest hashAlg = CryptoFunctions.getMessageDigest(hashAlgo);
			byte[] calcVerifierHash = hashAlg.digest(verifier);
			byte[] encryptedVerifierHash = cipher.doFinal(calcVerifierHash);
		} catch (GeneralSecurityException e) {
			throw new EncryptedDocumentException("Password confirmation failed", e);
		}
	}

	public Cipher initCipherForBlock(Cipher cipher, int block) throws GeneralSecurityException {
		return null;
	}

	@Override
	public ChunkedCipherOutputStream getDataStream(DirectoryNode dir) throws IOException {
		throw new IOException("not supported");
	}

	@Override
	public CryptoAPIEncryptor.CryptoAPICipherOutputStream getDataStream(OutputStream stream, int initialOffset) throws IOException, GeneralSecurityException {
		return new CryptoAPIEncryptor.CryptoAPICipherOutputStream(stream);
	}

	public void setSummaryEntries(DirectoryNode dir, String encryptedStream, POIFSFileSystem entries) throws IOException, GeneralSecurityException {
		byte[] buf = new byte[8];
		int block = 0;
		for (Entry entry : entries.getRoot()) {
			if (entry.isDirectoryEntry()) {
				continue;
			}
			DocumentInputStream dis = dir.createDocumentInputStream(entry);
			dis.close();
			block++;
		}
	}

	@Override
	public void setChunkSize(int chunkSize) {
		this.chunkSize = chunkSize;
	}

	@Override
	public CryptoAPIEncryptor clone() throws CloneNotSupportedException {
		return ((CryptoAPIEncryptor) (super.clone()));
	}

	protected class CryptoAPICipherOutputStream extends ChunkedCipherOutputStream {
		@Override
		protected Cipher initCipherForBlock(Cipher cipher, int block, boolean lastChunk) throws IOException, GeneralSecurityException {
			flush();
			EncryptionInfo ei = getEncryptionInfo();
			SecretKey sk = getSecretKey();
			return null;
		}

		@Override
		protected void calculateChecksum(File file, int i) {
		}

		@Override
		protected void createEncryptionInfoEntry(DirectoryNode dir, File tmpFile) {
			throw new EncryptedDocumentException("createEncryptionInfoEntry not supported");
		}

		CryptoAPICipherOutputStream(OutputStream stream) throws IOException, GeneralSecurityException {
			super(stream, CryptoAPIEncryptor.this.chunkSize);
		}

		@Override
		public void flush() throws IOException {
			writeChunk(false);
			super.flush();
		}
	}
}

