

import java.io.File;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.util.BitSet;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.poifs.crypt.ChunkedCipherOutputStream;
import org.apache.poi.poifs.crypt.CryptoFunctions;
import org.apache.poi.poifs.crypt.EncryptionInfo;
import org.apache.poi.poifs.crypt.EncryptionVerifier;
import org.apache.poi.poifs.crypt.Encryptor;
import org.apache.poi.poifs.crypt.xor.XOREncryptionVerifier;
import org.apache.poi.poifs.filesystem.DirectoryNode;
import org.apache.poi.util.LittleEndian;


public class XOREncryptor extends Encryptor implements Cloneable {
	protected XOREncryptor() {
	}

	@Override
	public void confirmPassword(String password) {
		int keyComp = CryptoFunctions.createXorKey1(password);
		int verifierComp = CryptoFunctions.createXorVerifier1(password);
		byte[] xorArray = CryptoFunctions.createXorArray1(password);
		byte[] shortBuf = new byte[2];
		XOREncryptionVerifier ver = ((XOREncryptionVerifier) (getEncryptionInfo().getVerifier()));
		LittleEndian.putUShort(shortBuf, 0, keyComp);
		LittleEndian.putUShort(shortBuf, 0, verifierComp);
		setSecretKey(new SecretKeySpec(xorArray, "XOR"));
	}

	@Override
	public void confirmPassword(String password, byte[] keySpec, byte[] keySalt, byte[] verifier, byte[] verifierSalt, byte[] integritySalt) {
		confirmPassword(password);
	}

	@Override
	public OutputStream getDataStream(DirectoryNode dir) throws IOException, GeneralSecurityException {
		return new XOREncryptor.XORCipherOutputStream(dir);
	}

	@Override
	public XOREncryptor.XORCipherOutputStream getDataStream(OutputStream stream, int initialOffset) throws IOException, GeneralSecurityException {
		return new XOREncryptor.XORCipherOutputStream(stream, initialOffset);
	}

	protected int getKeySizeInBytes() {
		return -1;
	}

	@Override
	public void setChunkSize(int chunkSize) {
	}

	@Override
	public XOREncryptor clone() throws CloneNotSupportedException {
		return ((XOREncryptor) (super.clone()));
	}

	private class XORCipherOutputStream extends ChunkedCipherOutputStream {
		private int recordStart;

		private int recordEnd;

		public XORCipherOutputStream(OutputStream stream, int initialPos) throws IOException, GeneralSecurityException {
			super(stream, (-1));
		}

		public XORCipherOutputStream(DirectoryNode dir) throws IOException, GeneralSecurityException {
			super(dir, (-1));
		}

		@Override
		protected Cipher initCipherForBlock(Cipher cipher, int block, boolean lastChunk) throws GeneralSecurityException {
			return null;
		}

		@Override
		protected void calculateChecksum(File file, int i) {
		}

		@Override
		protected void createEncryptionInfoEntry(DirectoryNode dir, File tmpFile) {
			throw new EncryptedDocumentException("createEncryptionInfoEntry not supported");
		}

		@Override
		public void setNextRecordSize(int recordSize, boolean isPlain) {
			if (((recordEnd) > 0) && (!isPlain)) {
				invokeCipher(((int) (getPos())), true);
			}
			recordStart = ((int) (getTotalPos())) + 4;
			recordEnd = (recordStart) + recordSize;
		}

		@Override
		public void flush() throws IOException {
			setNextRecordSize(0, true);
			super.flush();
		}

		@Override
		protected int invokeCipher(int posInChunk, boolean doFinal) {
			if (posInChunk == 0) {
				return 0;
			}
			final int start = Math.max((posInChunk - ((recordEnd) - (recordStart))), 0);
			final BitSet plainBytes = getPlainByteFlags();
			final byte[] xorArray = getEncryptionInfo().getEncryptor().getSecretKey().getEncoded();
			final byte[] chunk = getChunk();
			final byte[] plain = (plainBytes.isEmpty()) ? null : chunk.clone();
			int xorArrayIndex = (recordEnd) + (start - (recordStart));
			for (int i = start; i < posInChunk; i++) {
				byte value = chunk[i];
				value ^= xorArray[((xorArrayIndex++) & 15)];
				value = rotateLeft(value, (8 - 3));
				chunk[i] = value;
			}
			if (plain != null) {
				int i = plainBytes.nextSetBit(start);
				while ((i >= 0) && (i < posInChunk)) {
					chunk[i] = plain[i];
					i = plainBytes.nextSetBit((i + 1));
				} 
			}
			return posInChunk;
		}

		private byte rotateLeft(byte bits, int shift) {
			return ((byte) (((bits & 255) << shift) | ((bits & 255) >>> (8 - shift))));
		}
	}
}

