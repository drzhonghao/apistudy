import org.apache.cassandra.security.*;


import java.io.IOException;
import java.lang.reflect.Constructor;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.util.concurrent.FastThreadLocal;
import org.apache.cassandra.config.TransparentDataEncryptionOptions;

/**
 * A factory for loading encryption keys from {@link KeyProvider} instances.
 * Maintains a cache of loaded keys to avoid invoking the key provider on every call.
 */
public class CipherFactory
{
    private final Logger logger = LoggerFactory.getLogger(CipherFactory.class);

    /**
     * Keep around thread local instances of Cipher as they are quite expensive to instantiate (@code Cipher#getInstance).
     * Bonus points if you can avoid calling (@code Cipher#init); hence, the point of the supporting struct
     * for caching Cipher instances.
     */
    private static final FastThreadLocal<CachedCipher> cipherThreadLocal = new FastThreadLocal<>();

    private final SecureRandom secureRandom;
    private final LoadingCache<String, Key> cache;
    private final int ivLength;
    private final KeyProvider keyProvider;

    public CipherFactory(TransparentDataEncryptionOptions options)
    {
        logger.info("initializing CipherFactory");
        ivLength = options.iv_length;

        try
        {
            secureRandom = SecureRandom.getInstance("SHA1PRNG");
            Class<KeyProvider> keyProviderClass = (Class<KeyProvider>)Class.forName(options.key_provider.class_name);
            Constructor ctor = keyProviderClass.getConstructor(TransparentDataEncryptionOptions.class);
            keyProvider = (KeyProvider)ctor.newInstance(options);
        }
        catch (Exception e)
        {
            throw new RuntimeException("couldn't load cipher factory", e);
        }

        cache = CacheBuilder.newBuilder() // by default cache is unbounded
                .maximumSize(64) // a value large enough that we should never even get close (so nothing gets evicted)
                .concurrencyLevel(Runtime.getRuntime().availableProcessors())
                .removalListener(new RemovalListener<String, Key>()
                {
                    public void onRemoval(RemovalNotification<String, Key> notice)
                    {
                        // maybe reload the key? (to avoid the reload being on the user's dime)
                        logger.info("key {} removed from cipher key cache", notice.getKey());
                    }
                })
                .build(new CacheLoader<String, Key>()
                {
                    @Override
                    public Key load(String alias) throws Exception
                    {
                        logger.info("loading secret key for alias {}", alias);
                        return keyProvider.getSecretKey(alias);
                    }
                });
    }

    public Cipher getEncryptor(String transformation, String keyAlias) throws IOException
    {
        byte[] iv = new byte[ivLength];
        secureRandom.nextBytes(iv);
        return buildCipher(transformation, keyAlias, iv, Cipher.ENCRYPT_MODE);
    }

    public Cipher getDecryptor(String transformation, String keyAlias, byte[] iv) throws IOException
    {
        assert iv != null && iv.length > 0 : "trying to decrypt, but the initialization vector is empty";
        return buildCipher(transformation, keyAlias, iv, Cipher.DECRYPT_MODE);
    }

    @VisibleForTesting
    Cipher buildCipher(String transformation, String keyAlias, byte[] iv, int cipherMode) throws IOException
    {
        try
        {
            CachedCipher cachedCipher = cipherThreadLocal.get();
            if (cachedCipher != null)
            {
                Cipher cipher = cachedCipher.cipher;
                // rigorous checks to make sure we've absolutely got the correct instance (with correct alg/key/iv/...)
                if (cachedCipher.mode == cipherMode && cipher.getAlgorithm().equals(transformation)
                    && cachedCipher.keyAlias.equals(keyAlias) && Arrays.equals(cipher.getIV(), iv))
                    return cipher;
            }

            Key key = retrieveKey(keyAlias);
            Cipher cipher = Cipher.getInstance(transformation);
            cipher.init(cipherMode, key, new IvParameterSpec(iv));
            cipherThreadLocal.set(new CachedCipher(cipherMode, keyAlias, cipher));
            return cipher;
        }
        catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException | InvalidKeyException e)
        {
            logger.error("could not build cipher", e);
            throw new IOException("cannot load cipher", e);
        }
    }

    private Key retrieveKey(String keyAlias) throws IOException
    {
        try
        {
            return cache.get(keyAlias);
        }
        catch (ExecutionException e)
        {
            if (e.getCause() instanceof IOException)
                throw (IOException)e.getCause();
            throw new IOException("failed to load key from cache: " + keyAlias, e);
        }
    }

    /**
     * A simple struct to use with the thread local caching of Cipher as we can't get the mode (encrypt/decrypt) nor
     * key_alias (or key!) from the Cipher itself to use for comparisons
     */
    private static class CachedCipher
    {
        public final int mode;
        public final String keyAlias;
        public final Cipher cipher;

        private CachedCipher(int mode, String keyAlias, Cipher cipher)
        {
            this.mode = mode;
            this.keyAlias = keyAlias;
            this.cipher = cipher;
        }
    }
}
