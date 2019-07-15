

import java.io.IOException;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.poifs.crypt.ChainingMode;
import org.apache.poi.poifs.crypt.CipherAlgorithm;
import org.apache.poi.poifs.crypt.Decryptor;
import org.apache.poi.poifs.crypt.EncryptionHeader;
import org.apache.poi.poifs.crypt.EncryptionInfoBuilder;
import org.apache.poi.poifs.crypt.EncryptionMode;
import org.apache.poi.poifs.crypt.EncryptionVerifier;
import org.apache.poi.poifs.crypt.Encryptor;
import org.apache.poi.poifs.crypt.HashAlgorithm;
import org.apache.poi.poifs.filesystem.DirectoryNode;
import org.apache.poi.poifs.filesystem.DocumentInputStream;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.util.BitField;
import org.apache.poi.util.BitFieldFactory;
import org.apache.poi.util.LittleEndianInput;


public class EncryptionInfo implements Cloneable {
	private final EncryptionMode encryptionMode;

	private final int versionMajor;

	private final int versionMinor;

	private final int encryptionFlags;

	private EncryptionHeader header;

	private EncryptionVerifier verifier;

	private Decryptor decryptor;

	private Encryptor encryptor;

	public static final BitField flagCryptoAPI = BitFieldFactory.getInstance(4);

	@SuppressWarnings("WeakerAccess")
	public static final BitField flagDocProps = BitFieldFactory.getInstance(8);

	@SuppressWarnings("WeakerAccess")
	public static final BitField flagExternal = BitFieldFactory.getInstance(16);

	public static final BitField flagAES = BitFieldFactory.getInstance(32);

	public EncryptionInfo(POIFSFileSystem fs) throws IOException {
		this(fs.getRoot());
	}

	public EncryptionInfo(DirectoryNode dir) throws IOException {
		this(dir.createDocumentInputStream("EncryptionInfo"), null);
	}

	public EncryptionInfo(LittleEndianInput dis, EncryptionMode preferredEncryptionMode) throws IOException {
		if (preferredEncryptionMode == (EncryptionMode.xor)) {
			versionMajor = EncryptionMode.xor.versionMajor;
			versionMinor = EncryptionMode.xor.versionMinor;
		}else {
			versionMajor = dis.readUShort();
			versionMinor = dis.readUShort();
		}
		if (((versionMajor) == (EncryptionMode.xor.versionMajor)) && ((versionMinor) == (EncryptionMode.xor.versionMinor))) {
			encryptionMode = EncryptionMode.xor;
			encryptionFlags = -1;
		}else
			if (((versionMajor) == (EncryptionMode.binaryRC4.versionMajor)) && ((versionMinor) == (EncryptionMode.binaryRC4.versionMinor))) {
				encryptionMode = EncryptionMode.binaryRC4;
				encryptionFlags = -1;
			}else
				if (((2 <= (versionMajor)) && ((versionMajor) <= 4)) && ((versionMinor) == 2)) {
					encryptionFlags = dis.readInt();
					encryptionMode = ((preferredEncryptionMode == (EncryptionMode.cryptoAPI)) || (!(EncryptionInfo.flagAES.isSet(encryptionFlags)))) ? EncryptionMode.cryptoAPI : EncryptionMode.standard;
				}else
					if (((versionMajor) == (EncryptionMode.agile.versionMajor)) && ((versionMinor) == (EncryptionMode.agile.versionMinor))) {
						encryptionMode = EncryptionMode.agile;
						encryptionFlags = dis.readInt();
					}else {
						encryptionFlags = dis.readInt();
						throw new EncryptedDocumentException(((((((((((("Unknown encryption: version major: " + (versionMajor)) + " / version minor: ") + (versionMinor)) + " / fCrypto: ") + (EncryptionInfo.flagCryptoAPI.isSet(encryptionFlags))) + " / fExternal: ") + (EncryptionInfo.flagExternal.isSet(encryptionFlags))) + " / fDocProps: ") + (EncryptionInfo.flagDocProps.isSet(encryptionFlags))) + " / fAES: ") + (EncryptionInfo.flagAES.isSet(encryptionFlags))));
					}



		EncryptionInfoBuilder eib;
		try {
			eib = EncryptionInfo.getBuilder(encryptionMode);
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	public EncryptionInfo(EncryptionMode encryptionMode) {
		this(encryptionMode, null, null, (-1), (-1), null);
	}

	public EncryptionInfo(EncryptionMode encryptionMode, CipherAlgorithm cipherAlgorithm, HashAlgorithm hashAlgorithm, int keyBits, int blockSize, ChainingMode chainingMode) {
		this.encryptionMode = encryptionMode;
		versionMajor = encryptionMode.versionMajor;
		versionMinor = encryptionMode.versionMinor;
		encryptionFlags = encryptionMode.encryptionFlags;
		EncryptionInfoBuilder eib;
		try {
			eib = EncryptionInfo.getBuilder(encryptionMode);
		} catch (Exception e) {
			throw new EncryptedDocumentException(e);
		}
	}

	@SuppressWarnings({ "WeakerAccess", "JavadocReference" })
	protected static EncryptionInfoBuilder getBuilder(EncryptionMode encryptionMode) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
		ClassLoader cl = EncryptionInfo.class.getClassLoader();
		EncryptionInfoBuilder eib;
		eib = ((EncryptionInfoBuilder) (cl.loadClass(encryptionMode.builder).newInstance()));
		return eib;
	}

	public int getVersionMajor() {
		return versionMajor;
	}

	public int getVersionMinor() {
		return versionMinor;
	}

	public int getEncryptionFlags() {
		return encryptionFlags;
	}

	public EncryptionHeader getHeader() {
		return header;
	}

	public EncryptionVerifier getVerifier() {
		return verifier;
	}

	public Decryptor getDecryptor() {
		return decryptor;
	}

	public Encryptor getEncryptor() {
		return encryptor;
	}

	public void setHeader(EncryptionHeader header) {
		this.header = header;
	}

	public void setVerifier(EncryptionVerifier verifier) {
		this.verifier = verifier;
	}

	public void setDecryptor(Decryptor decryptor) {
		this.decryptor = decryptor;
	}

	public void setEncryptor(Encryptor encryptor) {
		this.encryptor = encryptor;
	}

	public EncryptionMode getEncryptionMode() {
		return encryptionMode;
	}

	public boolean isDocPropsEncrypted() {
		return !(EncryptionInfo.flagDocProps.isSet(getEncryptionFlags()));
	}

	@Override
	public EncryptionInfo clone() throws CloneNotSupportedException {
		EncryptionInfo other = ((EncryptionInfo) (super.clone()));
		other.header = header.clone();
		other.verifier = verifier.clone();
		other.decryptor = decryptor.clone();
		other.encryptor = encryptor.clone();
		return other;
	}
}

