import org.apache.accumulo.core.security.crypto.*;


import java.io.FilterOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;

/**
 * This class defines several parameters needed by by a module providing cryptographic stream
 * support in Accumulo. The following Javadoc details which parameters are used for which operations
 * (encryption vs. decryption), which ones return values (i.e. are "out" parameters from the
 * {@link CryptoModule}), and which ones are required versus optional in certain situations.
 *
 * Most of the time, these classes can be constructed using
 * {@link CryptoModuleFactory#createParamsObjectFromAccumuloConfiguration(org.apache.accumulo.core.conf.AccumuloConfiguration)}.
 */
public class CryptoModuleParameters {

  /**
   * Gets the name of the symmetric algorithm to use for encryption.
   *
   * @see CryptoModuleParameters#setAlgorithmName(String)
   */

  public String getAlgorithmName() {
    return algorithmName;
  }

  /**
   * Sets the name of the symmetric algorithm to use for an encryption stream.
   * <p>
   * Valid names are names recognized by your cryptographic engine provider. For the default Java
   * provider, valid names would include things like "AES", "RC4", "DESede", etc.
   * <p>
   * For <b>encryption</b>, this value is <b>required</b> and is always used. Its value should be
   * prepended or otherwise included with the ciphertext for future decryption. <br>
   * For <b>decryption</b>, this value is often disregarded in favor of the value encoded with the
   * ciphertext.
   *
   * @param algorithmName
   *          the name of the cryptographic algorithm to use.
   * @see <a href=
   *      "http://docs.oracle.com/javase/1.5.0/docs/guide/security/jce/JCERefGuide.html#AppA">Standard
   *      Algorithm Names in JCE</a>
   *
   */

  public void setAlgorithmName(String algorithmName) {
    this.algorithmName = algorithmName;
  }

  /**
   * Gets the name of the encryption mode to use for encryption.
   *
   * @see CryptoModuleParameters#setEncryptionMode(String)
   */

  public String getEncryptionMode() {
    return encryptionMode;
  }

  /**
   * Sets the name of the encryption mode to use for an encryption stream.
   * <p>
   * Valid names are names recognized by your cryptographic engine provider. For the default Java
   * provider, valid names would include things like "EBC", "CBC", "CFB", etc.
   * <p>
   * For <b>encryption</b>, this value is <b>required</b> and is always used. Its value should be
   * prepended or otherwise included with the ciphertext for future decryption. <br>
   * For <b>decryption</b>, this value is often disregarded in favor of the value encoded with the
   * ciphertext.
   *
   * @param encryptionMode
   *          the name of the encryption mode to use.
   * @see <a href=
   *      "http://docs.oracle.com/javase/1.5.0/docs/guide/security/jce/JCERefGuide.html#AppA">Standard
   *      Mode Names in JCE</a>
   *
   */

  public void setEncryptionMode(String encryptionMode) {
    this.encryptionMode = encryptionMode;
  }

  /**
   * Gets the name of the padding type to use for encryption.
   *
   * @see CryptoModuleParameters#setPadding(String)
   */

  public String getPadding() {
    return padding;
  }

  /**
   * Sets the name of the padding type to use for an encryption stream.
   * <p>
   * Valid names are names recognized by your cryptographic engine provider. For the default Java
   * provider, valid names would include things like "NoPadding", "None", etc.
   * <p>
   * For <b>encryption</b>, this value is <b>required</b> and is always used. Its value should be
   * prepended or otherwise included with the ciphertext for future decryption. <br>
   * For <b>decryption</b>, this value is often disregarded in favor of the value encoded with the
   * ciphertext.
   *
   * @param padding
   *          the name of the padding type to use.
   * @see <a href=
   *      "http://docs.oracle.com/javase/1.5.0/docs/guide/security/jce/JCERefGuide.html#AppA">Standard
   *      Padding Names in JCE</a>
   *
   */
  public void setPadding(String padding) {
    this.padding = padding;
  }

  /**
   * Gets the plaintext secret key.
   * <p>
   * For <b>decryption</b>, this value is often the out parameter of using a secret key encryption
   * strategy to decrypt an encrypted version of this secret key. (See
   * {@link CryptoModuleParameters#setKeyEncryptionStrategyClass(String)}.)
   *
   *
   * @see CryptoModuleParameters#setPlaintextKey(byte[])
   */
  public byte[] getPlaintextKey() {
    return plaintextKey;
  }

  /**
   * Sets the plaintext secret key that will be used to encrypt and decrypt bytes.
   * <p>
   * Valid values and lengths for this secret key depend entirely on the algorithm type. Refer to
   * the documentation about the algorithm for further information.
   * <p>
   * For <b>encryption</b>, this value is <b>optional</b>. If it is not provided, it will be
   * automatically generated by the underlying cryptographic module. <br>
   * For <b>decryption</b>, this value is often obtained from the underlying cipher stream, or
   * derived from the encrypted version of the key (see
   * {@link CryptoModuleParameters#setEncryptedKey(byte[])}).
   *
   * @param plaintextKey
   *          the value of the plaintext secret key
   */

  public void setPlaintextKey(byte[] plaintextKey) {
    this.plaintextKey = plaintextKey;
  }

  /**
   * Gets the length of the secret key.
   *
   * @see CryptoModuleParameters#setKeyLength(int)
   */
  public int getKeyLength() {
    return keyLength;
  }

  /**
   * Sets the length of the secret key that will be used to encrypt and decrypt bytes.
   * <p>
   * Valid lengths depend entirely on the algorithm type. Refer to the documentation about the
   * algorithm for further information. (For example, AES may use either 128 or 256 bit keys in the
   * default Java cryptography provider.)
   * <p>
   * For <b>encryption</b>, this value is <b>required if the secret key is not set</b>. <br>
   * For <b>decryption</b>, this value is often obtained from the underlying cipher stream, or
   * derived from the encrypted version of the key (see
   * {@link CryptoModuleParameters#setEncryptedKey(byte[])}).
   *
   * @param keyLength
   *          the length of the secret key to be generated
   */

  public void setKeyLength(int keyLength) {
    this.keyLength = keyLength;
  }

  /**
   * Gets the random number generator name.
   *
   * @see CryptoModuleParameters#setRandomNumberGenerator(String)
   */

  public String getRandomNumberGenerator() {
    return randomNumberGenerator;
  }

  /**
   * Sets the name of the random number generator to use. The default for this for the baseline JCE
   * implementation is "SHA1PRNG".
   * <p>
   * For <b>encryption</b>, this value is <b>required</b>.<br>
   * For <b>decryption</b>, this value is often obtained from the underlying cipher stream.
   *
   * @param randomNumberGenerator
   *          the name of the random number generator to use
   */

  public void setRandomNumberGenerator(String randomNumberGenerator) {
    this.randomNumberGenerator = randomNumberGenerator;
  }

  /**
   * Gets the random number generator provider name.
   *
   * @see CryptoModuleParameters#setRandomNumberGeneratorProvider(String)
   */
  public String getRandomNumberGeneratorProvider() {
    return randomNumberGeneratorProvider;
  }

  /**
   * Sets the name of the random number generator provider to use. The default for this for the
   * baseline JCE implementation is "SUN".
   * <p>
   * The provider, as the name implies, provides the RNG implementation specified by
   * {@link CryptoModuleParameters#getRandomNumberGenerator()}.
   * <p>
   * For <b>encryption</b>, this value is <b>required</b>. <br>
   * For <b>decryption</b>, this value is often obtained from the underlying cipher stream.
   *
   * @param randomNumberGeneratorProvider
   *          the name of the provider to use
   */

  public void setRandomNumberGeneratorProvider(String randomNumberGeneratorProvider) {
    this.randomNumberGeneratorProvider = randomNumberGeneratorProvider;
  }

  /**
   * Gets the key encryption strategy class.
   *
   * @see CryptoModuleParameters#setKeyEncryptionStrategyClass(String)
   */

  public String getKeyEncryptionStrategyClass() {
    return keyEncryptionStrategyClass;
  }

  /**
   * Sets the class name of the key encryption strategy class. The class obeys the
   * {@link SecretKeyEncryptionStrategy} interface. It instructs the {@link DefaultCryptoModule} on
   * how to encrypt the keys it uses to secure the streams.
   * <p>
   * The default implementation of this interface, {@link CachingHDFSSecretKeyEncryptionStrategy},
   * creates a random key encryption key (KEK) as another symmetric key and places the KEK into
   * HDFS. <i>This is not really very secure.</i> Users of the crypto modules are encouraged to
   * either safeguard that KEK carefully or to obtain and use another
   * {@link SecretKeyEncryptionStrategy} class.
   * <p>
   * For <b>encryption</b>, this value is <b>optional</b>. If it is not specified, then it assumed
   * that the secret keys used for encrypting files will not be encrypted. This is not a secure
   * approach, thus setting this is highly recommended.<br>
   * For <b>decryption</b>, this value is often obtained from the underlying cipher stream. However,
   * the underlying stream's value can be overridden (at least when using
   * {@link DefaultCryptoModule}) by setting the
   * {@link CryptoModuleParameters#setOverrideStreamsSecretKeyEncryptionStrategy(boolean)} to true.
   *
   * @param keyEncryptionStrategyClass
   *          the name of the key encryption strategy class to use
   */
  public void setKeyEncryptionStrategyClass(String keyEncryptionStrategyClass) {
    this.keyEncryptionStrategyClass = keyEncryptionStrategyClass;
  }

  /**
   * Gets the encrypted version of the plaintext key. This parameter is generally either obtained
   * from an underlying stream or computed in the process of employed the
   * {@link CryptoModuleParameters#getKeyEncryptionStrategyClass()}.
   *
   * @see CryptoModuleParameters#setEncryptedKey(byte[])
   */
  public byte[] getEncryptedKey() {
    return encryptedKey;
  }

  /**
   * Sets the encrypted version of the plaintext key
   * ({@link CryptoModuleParameters#getPlaintextKey()}). Generally this operation will be done
   * either by:
   * <ul>
   * <li>the code reading an encrypted stream and coming across the encrypted version of one of
   * these keys, OR
   * <li>the {@link CryptoModuleParameters#getKeyEncryptionStrategyClass()} that encrypted the
   * plaintext key (see {@link CryptoModuleParameters#getPlaintextKey()}).
   * </ul>
   * <p>
   * For <b>encryption</b>, this value is generally not required, but is usually set by the
   * underlying module during encryption. <br>
   * For <b>decryption</b>, this value is <b>usually required</b>.
   *
   * @param encryptedKey
   *          the encrypted value of the plaintext key
   */
  public void setEncryptedKey(byte[] encryptedKey) {
    this.encryptedKey = encryptedKey;
  }

  /**
   * Gets the opaque ID associated with the encrypted version of the plaintext key.
   *
   * @see CryptoModuleParameters#setOpaqueKeyEncryptionKeyID(String)
   */
  public String getOpaqueKeyEncryptionKeyID() {
    return opaqueKeyEncryptionKeyID;
  }

  /**
   * Sets an opaque ID assocaited with the encrypted version of the plaintext key.
   * <p>
   * Often, implementors of the {@link SecretKeyEncryptionStrategy} will need to record some
   * information about how they encrypted a particular plaintext key. For example, if the strategy
   * employs several keys for its encryption, it will want to record which key it used. The caller
   * should not have to worry about the format or contents of this internal ID; thus, the strategy
   * class will encode whatever information it needs into this string. It is then beholden to the
   * calling code to record this opqaue string properly to the underlying cryptographically-encoded
   * stream, and then set the opaque ID back into this parameter object upon reading.
   * <p>
   * For <b>encryption</b>, this value is generally not required, but will be typically generated
   * and set by the {@link SecretKeyEncryptionStrategy} class (see
   * {@link CryptoModuleParameters#getKeyEncryptionStrategyClass()}). <br>
   * For <b>decryption</b>, this value is <b>required</b>, though it will typically be read from the
   * underlying stream.
   *
   * @param opaqueKeyEncryptionKeyID
   *          the opaque ID assoicated with the encrypted version of the plaintext key (see
   *          {@link CryptoModuleParameters#getEncryptedKey()}).
   */

  public void setOpaqueKeyEncryptionKeyID(String opaqueKeyEncryptionKeyID) {
    this.opaqueKeyEncryptionKeyID = opaqueKeyEncryptionKeyID;
  }

  /**
   * Gets the flag that indicates whether or not the module should record its cryptographic
   * parameters to the stream automatically, or rely on the calling code to do so.
   *
   * @see CryptoModuleParameters#setRecordParametersToStream(boolean)
   */
  public boolean getRecordParametersToStream() {
    return recordParametersToStream;
  }

  /**
   * Gets the flag that indicates whether or not the module should record its cryptographic
   * parameters to the stream automatically, or rely on the calling code to do so.
   *
   * <p>
   *
   * If this is set to <i>true</i>, then the stream passed to
   * {@link CryptoModule#getEncryptingOutputStream(CryptoModuleParameters)} will be <i>written to by
   * the module</i> before it is returned to the caller. There are situations where it is easier to
   * let the crypto module do this writing on behalf of the caller, and other times where it is not
   * appropriate (if the format of the underlying stream must be carefully maintained, for
   * instance).
   *
   * @param recordParametersToStream
   *          whether or not to require the module to record its parameters to the stream by itself
   */
  public void setRecordParametersToStream(boolean recordParametersToStream) {
    this.recordParametersToStream = recordParametersToStream;
  }

  /**
   * Gets the flag that indicates whether or not to close the underlying stream when the cipher
   * stream is closed.
   *
   * @see CryptoModuleParameters#setCloseUnderylingStreamAfterCryptoStreamClose(boolean)
   */
  public boolean getCloseUnderylingStreamAfterCryptoStreamClose() {
    return closeUnderylingStreamAfterCryptoStreamClose;
  }

  /**
   * Sets the flag that indicates whether or not to close the underlying stream when the cipher
   * stream is closed.
   *
   * <p>
   *
   * {@link CipherOutputStream} will only output its padding bytes when its
   * {@link CipherOutputStream#close()} method is called. However, there are times when a caller
   * doesn't want its underlying stream closed at the time that the {@link CipherOutputStream} is
   * closed. This flag indicates that the {@link CryptoModule} should wrap the underlying stream in
   * a basic {@link FilterOutputStream} which will swallow any close() calls and prevent them from
   * propogating to the underlying stream.
   *
   * @param closeUnderylingStreamAfterCryptoStreamClose
   *          the flag that indicates whether or not to close the underlying stream when the cipher
   *          stream is closed
   */
  public void setCloseUnderylingStreamAfterCryptoStreamClose(
      boolean closeUnderylingStreamAfterCryptoStreamClose) {
    this.closeUnderylingStreamAfterCryptoStreamClose = closeUnderylingStreamAfterCryptoStreamClose;
  }

  /**
   * Gets the flag that indicates if the underlying stream's key encryption strategy should be
   * overridden by the currently configured key encryption strategy.
   *
   * @see CryptoModuleParameters#setOverrideStreamsSecretKeyEncryptionStrategy(boolean)
   */
  public boolean getOverrideStreamsSecretKeyEncryptionStrategy() {
    return overrideStreamsSecretKeyEncryptionStrategy;
  }

  /**
   * Sets the flag that indicates if the underlying stream's key encryption strategy should be
   * overridden by the currently configured key encryption strategy.
   *
   * <p>
   *
   * So, why is this important? Say you started out with the default secret key encryption strategy.
   * So, now you have a secret key in HDFS that encrypts all the other secret keys. <i>Then</i> you
   * deploy a key management solution. You want to move that secret key up to the key management
   * server. Great! No problem. Except, all your encrypted files now contain a setting that says
   * "hey I was encrypted by the default strategy, so find decrypt my key using that, not the key
   * management server". This setting signals the {@link CryptoModule} that it should ignore the
   * setting in the file and prefer the one from the configuration.
   *
   * @param overrideStreamsSecretKeyEncryptionStrategy
   *          the flag that indicates if the underlying stream's key encryption strategy should be
   *          overridden by the currently configured key encryption strategy
   */

  public void setOverrideStreamsSecretKeyEncryptionStrategy(
      boolean overrideStreamsSecretKeyEncryptionStrategy) {
    this.overrideStreamsSecretKeyEncryptionStrategy = overrideStreamsSecretKeyEncryptionStrategy;
  }

  /**
   * Gets the plaintext output stream to wrap for encryption.
   *
   * @see CryptoModuleParameters#setPlaintextOutputStream(OutputStream)
   */
  public OutputStream getPlaintextOutputStream() {
    return plaintextOutputStream;
  }

  /**
   * Sets the plaintext output stream to wrap for encryption.
   *
   * <p>
   *
   * For <b>encryption</b>, this parameter is <b>required</b>. <br>
   * For <b>decryption</b>, this parameter is ignored.
   */
  public void setPlaintextOutputStream(OutputStream plaintextOutputStream) {
    this.plaintextOutputStream = plaintextOutputStream;
  }

  /**
   * Gets the encrypted output stream, which is nearly always a wrapped version of the output stream
   * from {@link CryptoModuleParameters#getPlaintextOutputStream()}.
   *
   * <p>
   *
   * Generally this method is used by {@link CryptoModule} classes as an <i>out</i> parameter from
   * calling {@link CryptoModule#getEncryptingOutputStream(CryptoModuleParameters)}.
   *
   * @see CryptoModuleParameters#setEncryptedOutputStream(OutputStream)
   */

  public OutputStream getEncryptedOutputStream() {
    return encryptedOutputStream;
  }

  /**
   * Sets the encrypted output stream. This method should really only be called by
   * {@link CryptoModule} implementations unless something very unusual is going on.
   *
   * @param encryptedOutputStream
   *          the encrypted version of the stream from output stream from
   *          {@link CryptoModuleParameters#getPlaintextOutputStream()}.
   */
  public void setEncryptedOutputStream(OutputStream encryptedOutputStream) {
    this.encryptedOutputStream = encryptedOutputStream;
  }

  /**
   * Gets the plaintext input stream, which is nearly always a wrapped version of the output from
   * {@link CryptoModuleParameters#getEncryptedInputStream()}.
   *
   * <p>
   *
   * Generally this method is used by {@link CryptoModule} classes as an <i>out</i> parameter from
   * calling {@link CryptoModule#getDecryptingInputStream(CryptoModuleParameters)}.
   *
   *
   * @see CryptoModuleParameters#setPlaintextInputStream(InputStream)
   */
  public InputStream getPlaintextInputStream() {
    return plaintextInputStream;
  }

  /**
   * Sets the plaintext input stream, which is nearly always a wrapped version of the output from
   * {@link CryptoModuleParameters#getEncryptedInputStream()}.
   *
   * <p>
   *
   * This method should really only be called by {@link CryptoModule} implementations.
   */

  public void setPlaintextInputStream(InputStream plaintextInputStream) {
    this.plaintextInputStream = plaintextInputStream;
  }

  /**
   * Gets the encrypted input stream to wrap for decryption.
   *
   * @see CryptoModuleParameters#setEncryptedInputStream(InputStream)
   */
  public InputStream getEncryptedInputStream() {
    return encryptedInputStream;
  }

  /**
   * Sets the encrypted input stream to wrap for decryption.
   */

  public void setEncryptedInputStream(InputStream encryptedInputStream) {
    this.encryptedInputStream = encryptedInputStream;
  }

  /**
   * Gets the initialized cipher object.
   *
   *
   * @see CryptoModuleParameters#setCipher(Cipher)
   */
  public Cipher getCipher() {
    return cipher;
  }

  /**
   * Sets the initialized cipher object. Generally speaking, callers do not have to create and set
   * this object. There may be circumstances where the cipher object is created outside of the
   * module (to determine IV lengths, for one). If it is created and you want the module to use the
   * cipher you already initialized, set it here.
   *
   * @param cipher
   *          the cipher object
   */
  public void setCipher(Cipher cipher) {
    this.cipher = cipher;
  }

  /**
   * Gets the initialized secure random object.
   *
   * @see CryptoModuleParameters#setSecureRandom(SecureRandom)
   */
  public SecureRandom getSecureRandom() {
    return secureRandom;
  }

  /**
   * Sets the initialized secure random object. Generally speaking, callers do not have to create
   * and set this object. There may be circumstances where the random object is created outside of
   * the module (for instance, to create a random secret key). If it is created outside the module
   * and you want the module to use the random object you already created, set it here.
   *
   * @param secureRandom
   *          the {@link SecureRandom} object
   */

  public void setSecureRandom(SecureRandom secureRandom) {
    this.secureRandom = secureRandom;
  }

  /**
   * Gets the initialization vector to use for this crypto module.
   *
   * @see CryptoModuleParameters#setInitializationVector(byte[])
   */
  public byte[] getInitializationVector() {
    return initializationVector;
  }

  /**
   * Sets the initialization vector to use for this crypto module.
   *
   * <p>
   *
   * For <b>encryption</b>, this parameter is <i>optional</i>. If the initialization vector is
   * created by the caller, for whatever reasons, it can be set here and the crypto module will use
   * it. <br>
   *
   * For <b>decryption</b>, this parameter is <b>required</b>. It should be read from the underlying
   * stream that contains the encrypted data.
   *
   * @param initializationVector
   *          the initialization vector to use for this crypto operation.
   */
  public void setInitializationVector(byte[] initializationVector) {
    this.initializationVector = initializationVector;
  }

  /**
   * Gets the size of the buffering stream that sits above the cipher stream
   */
  public int getBlockStreamSize() {
    return blockStreamSize;
  }

  /**
   * Sets the size of the buffering stream that sits above the cipher stream
   */
  public void setBlockStreamSize(int blockStreamSize) {
    this.blockStreamSize = blockStreamSize;
  }

  /**
   * Gets the overall set of options for the {@link CryptoModule}.
   *
   * @see CryptoModuleParameters#setAllOptions(Map)
   */
  public Map<String,String> getAllOptions() {
    return allOptions;
  }

  /**
   * Sets the overall set of options for the {@link CryptoModule}.
   *
   * <p>
   *
   * Often, options for the cryptographic modules will be encoded as key/value pairs in a
   * configuration file. This map represents those values. It may include some of the parameters
   * already called out as members of this class. It may contain any number of additional parameters
   * which may be required by different module or key encryption strategy implementations.
   *
   * @param allOptions
   *          the set of key/value pairs that confiure a module, based on a configuration file
   */
  public void setAllOptions(Map<String,String> allOptions) {
    this.allOptions = allOptions;
  }

  private String algorithmName = null;
  private String encryptionMode = null;
  private String padding = null;
  private byte[] plaintextKey;
  private int keyLength = 0;
  private String randomNumberGenerator = null;
  private String randomNumberGeneratorProvider = null;

  private String keyEncryptionStrategyClass;
  private byte[] encryptedKey;
  private String opaqueKeyEncryptionKeyID;

  private boolean recordParametersToStream = true;
  private boolean closeUnderylingStreamAfterCryptoStreamClose = true;
  private boolean overrideStreamsSecretKeyEncryptionStrategy = false;

  private OutputStream plaintextOutputStream;
  private OutputStream encryptedOutputStream;
  private InputStream plaintextInputStream;
  private InputStream encryptedInputStream;

  private Cipher cipher;
  private SecureRandom secureRandom;
  private byte[] initializationVector;

  private Map<String,String> allOptions;
  private int blockStreamSize;
}
