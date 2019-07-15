

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Random;
import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.poifs.crypt.ChainingMode;
import org.apache.poi.poifs.crypt.CipherAlgorithm;
import org.apache.poi.poifs.crypt.CryptoFunctions;
import org.apache.poi.poifs.crypt.DataSpaceMapUtils;
import org.apache.poi.poifs.crypt.EncryptionHeader;
import org.apache.poi.poifs.crypt.EncryptionInfo;
import org.apache.poi.poifs.crypt.EncryptionVerifier;
import org.apache.poi.poifs.crypt.Encryptor;
import org.apache.poi.poifs.crypt.HashAlgorithm;
import org.apache.poi.poifs.crypt.standard.EncryptionRecord;
import org.apache.poi.poifs.crypt.standard.StandardEncryptionHeader;
import org.apache.poi.poifs.crypt.standard.StandardEncryptionVerifier;
import org.apache.poi.poifs.filesystem.DirectoryNode;
import org.apache.poi.poifs.filesystem.DocumentEntry;
import org.apache.poi.poifs.filesystem.DocumentOutputStream;
import org.apache.poi.poifs.filesystem.POIFSWriterEvent;
import org.apache.poi.poifs.filesystem.POIFSWriterListener;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.LittleEndianByteArrayOutputStream;
import org.apache.poi.util.LittleEndianConsts;
import org.apache.poi.util.LittleEndianOutputStream;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;
import org.apache.poi.util.TempFile;


public class StandardEncryptor extends Encryptor implements Cloneable {
	private static final POILogger logger = POILogFactory.getLogger(StandardEncryptor.class);

	protected StandardEncryptor() {
	}

	@Override
	public void confirmPassword(String password) {
		Random r = new SecureRandom();
		byte[] salt = new byte[16];
		byte[] verifier = new byte[16];
		r.nextBytes(salt);
		r.nextBytes(verifier);
		confirmPassword(password, null, null, salt, verifier, null);
	}

	@Override
	public void confirmPassword(String password, byte[] keySpec, byte[] keySalt, byte[] verifier, byte[] verifierSalt, byte[] integritySalt) {
		StandardEncryptionVerifier ver = ((StandardEncryptionVerifier) (getEncryptionInfo().getVerifier()));
		MessageDigest hashAlgo = CryptoFunctions.getMessageDigest(ver.getHashAlgorithm());
		byte[] calcVerifierHash = hashAlgo.digest(verifier);
		int encVerHashSize = ver.getCipherAlgorithm().encryptedVerifierHashLength;
	}

	private Cipher getCipher(SecretKey key, String padding) {
		EncryptionVerifier ver = getEncryptionInfo().getVerifier();
		return CryptoFunctions.getCipher(key, ver.getCipherAlgorithm(), ver.getChainingMode(), null, Cipher.ENCRYPT_MODE, padding);
	}

	@Override
	public OutputStream getDataStream(final DirectoryNode dir) throws IOException, GeneralSecurityException {
		createEncryptionInfoEntry(dir);
		DataSpaceMapUtils.addDefaultDataSpace(dir);
		return new StandardEncryptor.StandardCipherOutputStream(dir);
	}

	protected class StandardCipherOutputStream extends FilterOutputStream implements POIFSWriterListener {
		protected long countBytes;

		protected final File fileOut;

		protected final DirectoryNode dir;

		@SuppressWarnings("resource")
		private StandardCipherOutputStream(DirectoryNode dir, File fileOut) throws IOException {
			super(new CipherOutputStream(new FileOutputStream(fileOut), getCipher(getSecretKey(), "PKCS5Padding")));
			this.fileOut = fileOut;
			this.dir = dir;
		}

		protected StandardCipherOutputStream(DirectoryNode dir) throws IOException {
			this(dir, TempFile.createTempFile("encrypted_package", "crypt"));
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			out.write(b, off, len);
			countBytes += len;
		}

		@Override
		public void write(int b) throws IOException {
			out.write(b);
			(countBytes)++;
		}

		@Override
		public void close() throws IOException {
			super.close();
			writeToPOIFS();
		}

		void writeToPOIFS() throws IOException {
			int oleStreamSize = ((int) ((fileOut.length()) + (LittleEndianConsts.LONG_SIZE)));
			dir.createDocument(Encryptor.DEFAULT_POIFS_ENTRY, oleStreamSize, this);
		}

		@Override
		public void processPOIFSWriterEvent(POIFSWriterEvent event) {
			try {
				LittleEndianOutputStream leos = new LittleEndianOutputStream(event.getStream());
				leos.writeLong(countBytes);
				FileInputStream fis = new FileInputStream(fileOut);
				try {
					IOUtils.copy(fis, leos);
				} finally {
					fis.close();
				}
				if (!(fileOut.delete())) {
					StandardEncryptor.logger.log(POILogger.ERROR, ("Can't delete temporary encryption file: " + (fileOut)));
				}
				leos.close();
			} catch (IOException e) {
				throw new EncryptedDocumentException(e);
			}
		}
	}

	protected int getKeySizeInBytes() {
		return (getEncryptionInfo().getHeader().getKeySize()) / 8;
	}

	protected void createEncryptionInfoEntry(DirectoryNode dir) throws IOException {
		final EncryptionInfo info = getEncryptionInfo();
		final StandardEncryptionHeader header = ((StandardEncryptionHeader) (info.getHeader()));
		final StandardEncryptionVerifier verifier = ((StandardEncryptionVerifier) (info.getVerifier()));
		EncryptionRecord er = new EncryptionRecord() {
			@Override
			public void write(LittleEndianByteArrayOutputStream bos) {
				bos.writeShort(info.getVersionMajor());
				bos.writeShort(info.getVersionMinor());
				bos.writeInt(info.getEncryptionFlags());
				header.write(bos);
				verifier.write(bos);
			}
		};
		DataSpaceMapUtils.createEncryptionEntry(dir, "EncryptionInfo", er);
	}

	@Override
	public StandardEncryptor clone() throws CloneNotSupportedException {
		return ((StandardEncryptor) (super.clone()));
	}
}

