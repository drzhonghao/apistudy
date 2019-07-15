

import com.microsoft.schemas.office.x2006.encryption.CTDataIntegrity;
import com.microsoft.schemas.office.x2006.encryption.CTEncryption;
import com.microsoft.schemas.office.x2006.encryption.CTKeyData;
import com.microsoft.schemas.office.x2006.encryption.CTKeyEncryptor;
import com.microsoft.schemas.office.x2006.encryption.CTKeyEncryptors;
import com.microsoft.schemas.office.x2006.encryption.EncryptionDocument;
import com.microsoft.schemas.office.x2006.encryption.STCipherAlgorithm;
import com.microsoft.schemas.office.x2006.encryption.STCipherChaining;
import com.microsoft.schemas.office.x2006.encryption.STHashAlgorithm;
import com.microsoft.schemas.office.x2006.keyEncryptor.certificate.CTCertificateKeyEncryptor;
import com.microsoft.schemas.office.x2006.keyEncryptor.password.CTPasswordKeyEncryptor;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.poifs.crypt.ChainingMode;
import org.apache.poi.poifs.crypt.ChunkedCipherOutputStream;
import org.apache.poi.poifs.crypt.CipherAlgorithm;
import org.apache.poi.poifs.crypt.CryptoFunctions;
import org.apache.poi.poifs.crypt.DataSpaceMapUtils;
import org.apache.poi.poifs.crypt.EncryptionHeader;
import org.apache.poi.poifs.crypt.EncryptionInfo;
import org.apache.poi.poifs.crypt.EncryptionVerifier;
import org.apache.poi.poifs.crypt.Encryptor;
import org.apache.poi.poifs.crypt.HashAlgorithm;
import org.apache.poi.poifs.crypt.agile.AgileEncryptionHeader;
import org.apache.poi.poifs.crypt.agile.AgileEncryptionVerifier;
import org.apache.poi.poifs.crypt.agile.AgileEncryptionVerifier.AgileCertificateEntry;
import org.apache.poi.poifs.crypt.standard.EncryptionRecord;
import org.apache.poi.poifs.filesystem.DirectoryNode;
import org.apache.poi.poifs.filesystem.DocumentEntry;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.LittleEndian;
import org.apache.poi.util.LittleEndianByteArrayOutputStream;
import org.apache.poi.util.LittleEndianConsts;
import org.apache.xmlbeans.StringEnumAbstractBase;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.XmlTokenSource;

import static com.microsoft.schemas.office.x2006.encryption.CTKeyEncryptor.Uri.HTTP_SCHEMAS_MICROSOFT_COM_OFFICE_2006_KEY_ENCRYPTOR_CERTIFICATE;
import static com.microsoft.schemas.office.x2006.encryption.CTKeyEncryptor.Uri.HTTP_SCHEMAS_MICROSOFT_COM_OFFICE_2006_KEY_ENCRYPTOR_PASSWORD;
import static com.microsoft.schemas.office.x2006.encryption.EncryptionDocument.Factory.newInstance;


public class AgileEncryptor extends Encryptor implements Cloneable {
	private static final int MAX_RECORD_LENGTH = 1000000;

	private byte[] integritySalt;

	private byte[] pwHash;

	protected AgileEncryptor() {
	}

	@Override
	public void confirmPassword(String password) {
		Random r = new SecureRandom();
		AgileEncryptionHeader header = ((AgileEncryptionHeader) (getEncryptionInfo().getHeader()));
		int blockSize = header.getBlockSize();
		int keySize = (header.getKeySize()) / 8;
		int hashSize = header.getHashAlgorithm().hashSize;
		byte[] newVerifierSalt = IOUtils.safelyAllocate(blockSize, AgileEncryptor.MAX_RECORD_LENGTH);
		byte[] newVerifier = IOUtils.safelyAllocate(blockSize, AgileEncryptor.MAX_RECORD_LENGTH);
		byte[] newKeySalt = IOUtils.safelyAllocate(blockSize, AgileEncryptor.MAX_RECORD_LENGTH);
		byte[] newKeySpec = IOUtils.safelyAllocate(keySize, AgileEncryptor.MAX_RECORD_LENGTH);
		byte[] newIntegritySalt = IOUtils.safelyAllocate(hashSize, AgileEncryptor.MAX_RECORD_LENGTH);
		r.nextBytes(newVerifierSalt);
		r.nextBytes(newVerifier);
		r.nextBytes(newKeySalt);
		r.nextBytes(newKeySpec);
		r.nextBytes(newIntegritySalt);
		confirmPassword(password, newKeySpec, newKeySalt, newVerifierSalt, newVerifier, newIntegritySalt);
	}

	@Override
	public void confirmPassword(String password, byte[] keySpec, byte[] keySalt, byte[] verifier, byte[] verifierSalt, byte[] integritySalt) {
		AgileEncryptionVerifier ver = ((AgileEncryptionVerifier) (getEncryptionInfo().getVerifier()));
		AgileEncryptionHeader header = ((AgileEncryptionHeader) (getEncryptionInfo().getHeader()));
		int blockSize = header.getBlockSize();
		pwHash = CryptoFunctions.hashPassword(password, ver.getHashAlgorithm(), verifierSalt, ver.getSpinCount());
		MessageDigest hashMD = CryptoFunctions.getMessageDigest(ver.getHashAlgorithm());
		byte[] hashedVerifier = hashMD.digest(verifier);
		SecretKey secretKey = new SecretKeySpec(keySpec, header.getCipherAlgorithm().jceId);
		setSecretKey(secretKey);
		this.integritySalt = integritySalt.clone();
		try {
			for (AgileEncryptionVerifier.AgileCertificateEntry ace : ver.getCertificates()) {
				Mac x509Hmac = CryptoFunctions.getMac(header.getHashAlgorithm());
				x509Hmac.init(getSecretKey());
			}
		} catch (GeneralSecurityException e) {
			throw new EncryptedDocumentException(e);
		}
	}

	@Override
	public OutputStream getDataStream(DirectoryNode dir) throws IOException, GeneralSecurityException {
		return new AgileEncryptor.AgileCipherOutputStream(dir);
	}

	protected void updateIntegrityHMAC(File tmpFile, int oleStreamSize) throws IOException, GeneralSecurityException {
		AgileEncryptionHeader header = ((AgileEncryptionHeader) (getEncryptionInfo().getHeader()));
		int blockSize = header.getBlockSize();
		HashAlgorithm hashAlgo = header.getHashAlgorithm();
		Mac integrityMD = CryptoFunctions.getMac(hashAlgo);
		byte[] buf = new byte[1024];
		LittleEndian.putLong(buf, 0, oleStreamSize);
		integrityMD.update(buf, 0, LittleEndianConsts.LONG_SIZE);
		InputStream fis = new FileInputStream(tmpFile);
		try {
			int readBytes;
			while ((readBytes = fis.read(buf)) != (-1)) {
				integrityMD.update(buf, 0, readBytes);
			} 
		} finally {
			fis.close();
		}
		byte[] hmacValue = integrityMD.doFinal();
	}

	private final CTKeyEncryptor.Uri.Enum passwordUri = HTTP_SCHEMAS_MICROSOFT_COM_OFFICE_2006_KEY_ENCRYPTOR_PASSWORD;

	private final CTKeyEncryptor.Uri.Enum certificateUri = HTTP_SCHEMAS_MICROSOFT_COM_OFFICE_2006_KEY_ENCRYPTOR_CERTIFICATE;

	protected EncryptionDocument createEncryptionDocument() {
		AgileEncryptionVerifier ver = ((AgileEncryptionVerifier) (getEncryptionInfo().getVerifier()));
		AgileEncryptionHeader header = ((AgileEncryptionHeader) (getEncryptionInfo().getHeader()));
		EncryptionDocument ed = newInstance();
		CTEncryption edRoot = ed.addNewEncryption();
		CTKeyData keyData = edRoot.addNewKeyData();
		CTKeyEncryptors keyEncList = edRoot.addNewKeyEncryptors();
		CTKeyEncryptor keyEnc = keyEncList.addNewKeyEncryptor();
		keyEnc.setUri(passwordUri);
		CTPasswordKeyEncryptor keyPass = keyEnc.addNewEncryptedPasswordKey();
		keyPass.setSpinCount(ver.getSpinCount());
		keyData.setSaltSize(header.getBlockSize());
		keyPass.setSaltSize(ver.getBlockSize());
		keyData.setBlockSize(header.getBlockSize());
		keyPass.setBlockSize(ver.getBlockSize());
		keyData.setKeyBits(header.getKeySize());
		keyPass.setKeyBits(ver.getKeySize());
		keyData.setHashSize(header.getHashAlgorithm().hashSize);
		keyPass.setHashSize(ver.getHashAlgorithm().hashSize);
		if (!(header.getCipherAlgorithm().xmlId.equals(ver.getCipherAlgorithm().xmlId))) {
			throw new EncryptedDocumentException("Cipher algorithm of header and verifier have to match");
		}
		STCipherAlgorithm.Enum xmlCipherAlgo = STCipherAlgorithm.Enum.forString(header.getCipherAlgorithm().xmlId);
		if (xmlCipherAlgo == null) {
			throw new EncryptedDocumentException((("CipherAlgorithm " + (header.getCipherAlgorithm())) + " not supported."));
		}
		keyData.setCipherAlgorithm(xmlCipherAlgo);
		keyPass.setCipherAlgorithm(xmlCipherAlgo);
		switch (header.getChainingMode()) {
			case cbc :
				keyData.setCipherChaining(STCipherChaining.CHAINING_MODE_CBC);
				keyPass.setCipherChaining(STCipherChaining.CHAINING_MODE_CBC);
				break;
			case cfb :
				keyData.setCipherChaining(STCipherChaining.CHAINING_MODE_CFB);
				keyPass.setCipherChaining(STCipherChaining.CHAINING_MODE_CFB);
				break;
			default :
				throw new EncryptedDocumentException((("ChainingMode " + (header.getChainingMode())) + " not supported."));
		}
		keyData.setHashAlgorithm(AgileEncryptor.mapHashAlgorithm(header.getHashAlgorithm()));
		keyPass.setHashAlgorithm(AgileEncryptor.mapHashAlgorithm(ver.getHashAlgorithm()));
		keyData.setSaltValue(header.getKeySalt());
		keyPass.setSaltValue(ver.getSalt());
		keyPass.setEncryptedVerifierHashInput(ver.getEncryptedVerifier());
		keyPass.setEncryptedVerifierHashValue(ver.getEncryptedVerifierHash());
		keyPass.setEncryptedKeyValue(ver.getEncryptedKey());
		CTDataIntegrity hmacData = edRoot.addNewDataIntegrity();
		hmacData.setEncryptedHmacKey(header.getEncryptedHmacKey());
		hmacData.setEncryptedHmacValue(header.getEncryptedHmacValue());
		for (AgileEncryptionVerifier.AgileCertificateEntry ace : ver.getCertificates()) {
			keyEnc = keyEncList.addNewKeyEncryptor();
			keyEnc.setUri(certificateUri);
			CTCertificateKeyEncryptor certData = keyEnc.addNewEncryptedCertificateKey();
		}
		return ed;
	}

	private static STHashAlgorithm.Enum mapHashAlgorithm(HashAlgorithm hashAlgo) {
		STHashAlgorithm.Enum xmlHashAlgo = STHashAlgorithm.Enum.forString(hashAlgo.ecmaString);
		if (xmlHashAlgo == null) {
			throw new EncryptedDocumentException((("HashAlgorithm " + hashAlgo) + " not supported."));
		}
		return xmlHashAlgo;
	}

	protected void marshallEncryptionDocument(EncryptionDocument ed, LittleEndianByteArrayOutputStream os) {
		XmlOptions xo = new XmlOptions();
		xo.setCharacterEncoding("UTF-8");
		Map<String, String> nsMap = new HashMap<>();
		nsMap.put(passwordUri.toString(), "p");
		nsMap.put(certificateUri.toString(), "c");
		xo.setUseDefaultNamespace();
		xo.setSaveSuggestedPrefixes(nsMap);
		xo.setSaveNamespacesFirst();
		xo.setSaveAggressiveNamespaces();
		xo.setSaveNoXmlDecl();
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			bos.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\r\n".getBytes("UTF-8"));
			ed.save(bos, xo);
			bos.writeTo(os);
		} catch (IOException e) {
			throw new EncryptedDocumentException("error marshalling encryption info document", e);
		}
	}

	protected void createEncryptionInfoEntry(DirectoryNode dir, File tmpFile) throws IOException, GeneralSecurityException {
		DataSpaceMapUtils.addDefaultDataSpace(dir);
		final EncryptionInfo info = getEncryptionInfo();
		EncryptionRecord er = new EncryptionRecord() {
			@Override
			public void write(LittleEndianByteArrayOutputStream bos) {
				bos.writeShort(info.getVersionMajor());
				bos.writeShort(info.getVersionMinor());
				bos.writeInt(info.getEncryptionFlags());
				EncryptionDocument ed = createEncryptionDocument();
				marshallEncryptionDocument(ed, bos);
			}
		};
		DataSpaceMapUtils.createEncryptionEntry(dir, "EncryptionInfo", er);
	}

	private class AgileCipherOutputStream extends ChunkedCipherOutputStream {
		public AgileCipherOutputStream(DirectoryNode dir) throws IOException, GeneralSecurityException {
			super(dir, 4096);
		}

		@Override
		protected Cipher initCipherForBlock(Cipher existing, int block, boolean lastChunk) throws GeneralSecurityException {
			return null;
		}

		@Override
		protected void calculateChecksum(File fileOut, int oleStreamSize) throws IOException, GeneralSecurityException {
			updateIntegrityHMAC(fileOut, oleStreamSize);
		}

		@Override
		protected void createEncryptionInfoEntry(DirectoryNode dir, File tmpFile) throws IOException, GeneralSecurityException {
			AgileEncryptor.this.createEncryptionInfoEntry(dir, tmpFile);
		}
	}

	@Override
	public AgileEncryptor clone() throws CloneNotSupportedException {
		AgileEncryptor other = ((AgileEncryptor) (super.clone()));
		other.integritySalt = ((integritySalt) == null) ? null : integritySalt.clone();
		other.pwHash = ((pwHash) == null) ? null : pwHash.clone();
		return other;
	}
}

