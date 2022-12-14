import org.apache.poi.poifs.crypt.Decryptor;
import org.apache.poi.poifs.crypt.EncryptionVerifier;
import org.apache.poi.poifs.crypt.HashAlgorithm;
import org.apache.poi.poifs.crypt.EncryptionInfo;
import org.apache.poi.poifs.crypt.EncryptionHeader;
import org.apache.poi.poifs.crypt.binaryrc4.*;


import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.poifs.crypt.*;
import org.apache.poi.poifs.filesystem.DirectoryNode;
import org.apache.poi.poifs.filesystem.DocumentInputStream;
import org.apache.poi.util.LittleEndian;
import org.apache.poi.util.StringUtil;

public class BinaryRC4Decryptor extends Decryptor implements Cloneable {
    private long length = -1L;
    private int chunkSize = 512;
    
    private class BinaryRC4CipherInputStream extends ChunkedCipherInputStream {

        @Override
        protected Cipher initCipherForBlock(Cipher existing, int block)
                throws GeneralSecurityException {
            return BinaryRC4Decryptor.this.initCipherForBlock(existing, block);
        }

        public BinaryRC4CipherInputStream(DocumentInputStream stream, long size)
                throws GeneralSecurityException {
            super(stream, size, chunkSize);
        }

        public BinaryRC4CipherInputStream(InputStream stream, int size, int initialPos)
                throws GeneralSecurityException {
            super(stream, size, chunkSize, initialPos);
        }    
    }

    protected BinaryRC4Decryptor() {
    }

    @Override
    public boolean verifyPassword(String password) {
        EncryptionVerifier ver = getEncryptionInfo().getVerifier();
        SecretKey skey = generateSecretKey(password, ver);
        try {
            Cipher cipher = initCipherForBlock(null, 0, getEncryptionInfo(), skey, Cipher.DECRYPT_MODE);
            byte encryptedVerifier[] = ver.getEncryptedVerifier();
            byte verifier[] = new byte[encryptedVerifier.length];
            cipher.update(encryptedVerifier, 0, encryptedVerifier.length, verifier);
            setVerifier(verifier);
            byte encryptedVerifierHash[] = ver.getEncryptedVerifierHash();
            byte verifierHash[] = cipher.doFinal(encryptedVerifierHash);
            HashAlgorithm hashAlgo = ver.getHashAlgorithm();
            MessageDigest hashAlg = CryptoFunctions.getMessageDigest(hashAlgo);
            byte calcVerifierHash[] = hashAlg.digest(verifier);
            if (Arrays.equals(calcVerifierHash, verifierHash)) {
                setSecretKey(skey);
                return true;
            }
        } catch (GeneralSecurityException e) {
            throw new EncryptedDocumentException(e);
        }
        return false;
    }

    @Override
    public Cipher initCipherForBlock(Cipher cipher, int block)
    throws GeneralSecurityException {
        return initCipherForBlock(cipher, block, getEncryptionInfo(), getSecretKey(), Cipher.DECRYPT_MODE);
    }    
    
    protected static Cipher initCipherForBlock(Cipher cipher, int block,
        EncryptionInfo encryptionInfo, SecretKey skey, int encryptMode)
    throws GeneralSecurityException {
        EncryptionVerifier ver = encryptionInfo.getVerifier();
        HashAlgorithm hashAlgo = ver.getHashAlgorithm();
        byte blockKey[] = new byte[4];
        LittleEndian.putUInt(blockKey, 0, block);
        byte encKey[] = CryptoFunctions.generateKey(skey.getEncoded(), hashAlgo, blockKey, 16);
        SecretKey key = new SecretKeySpec(encKey, skey.getAlgorithm());
        if (cipher == null) {
            EncryptionHeader em = encryptionInfo.getHeader();
            cipher = CryptoFunctions.getCipher(key, em.getCipherAlgorithm(), null, null, encryptMode);
        } else {
            cipher.init(encryptMode, key);
        }
        return cipher;
    }

    protected static SecretKey generateSecretKey(String password, EncryptionVerifier ver) {
        if (password.length() > 255) {
            password = password.substring(0, 255);
        }
        HashAlgorithm hashAlgo = ver.getHashAlgorithm();
        MessageDigest hashAlg = CryptoFunctions.getMessageDigest(hashAlgo);
        byte hash[] = hashAlg.digest(StringUtil.getToUnicodeLE(password));
        byte salt[] = ver.getSalt();
        hashAlg.reset();
        for (int i = 0; i < 16; i++) {
            hashAlg.update(hash, 0, 5);
            hashAlg.update(salt);
        }

        hash = new byte[5];
        System.arraycopy(hashAlg.digest(), 0, hash, 0, 5);
        return new SecretKeySpec(hash, ver.getCipherAlgorithm().jceId);
    }

    @Override
    @SuppressWarnings("resource")
    public ChunkedCipherInputStream getDataStream(DirectoryNode dir) throws IOException,
            GeneralSecurityException {
        DocumentInputStream dis = dir.createDocumentInputStream(DEFAULT_POIFS_ENTRY);
        length = dis.readLong();
        return new BinaryRC4CipherInputStream(dis, length);
    }
    
    @Override
    public InputStream getDataStream(InputStream stream, int size, int initialPos)
            throws IOException, GeneralSecurityException {
        return new BinaryRC4CipherInputStream(stream, size, initialPos);
    }
    

    @Override
    public long getLength() {
        if (length == -1L) {
            throw new IllegalStateException("Decryptor.getDataStream() was not called");
        }
        
        return length;
    }

    @Override
    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }
    
    @Override
    public BinaryRC4Decryptor clone() throws CloneNotSupportedException {
        return (BinaryRC4Decryptor)super.clone();
    }
}
