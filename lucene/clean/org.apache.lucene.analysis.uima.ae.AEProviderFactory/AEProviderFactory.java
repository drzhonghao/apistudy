import org.apache.lucene.analysis.uima.ae.OverridingParamsAEProvider;
import org.apache.lucene.analysis.uima.ae.BasicAEProvider;
import org.apache.lucene.analysis.uima.ae.*;



import java.util.HashMap;
import java.util.Map;

/**
 * Singleton factory class responsible of {@link AEProvider}s' creation
 */
public class AEProviderFactory {

  private static final AEProviderFactory instance = new AEProviderFactory();

  private final Map<String, AEProvider> providerCache = new HashMap<>();

  private AEProviderFactory() {
    // Singleton
  }

  public static AEProviderFactory getInstance() {
    return instance;
  }

  /**
   * @param keyPrefix         a prefix of the key used to cache the AEProvider
   * @param aePath            the AnalysisEngine descriptor path
   * @param runtimeParameters map of runtime parameters to configure inside the AnalysisEngine
   * @return AEProvider
   */
  public synchronized AEProvider getAEProvider(String keyPrefix, String aePath, Map<String, Object> runtimeParameters) {
    String key = new StringBuilder(keyPrefix != null ? keyPrefix : "").append(aePath).append(runtimeParameters != null ?
        runtimeParameters.toString() : "").toString();
    if (providerCache.get(key) == null) {
      AEProvider aeProvider;
      if (runtimeParameters != null)
        aeProvider = new OverridingParamsAEProvider(aePath, runtimeParameters);
      else
        aeProvider = new BasicAEProvider(aePath);
      providerCache.put(key, aeProvider);
    }
    return providerCache.get(key);
  }

  /**
   * @param aePath the AnalysisEngine descriptor path
   * @return AEProvider
   */
  public synchronized AEProvider getAEProvider(String aePath) {
    return getAEProvider(null, aePath, null);
  }

  /**
   * @param aePath            the AnalysisEngine descriptor path
   * @param runtimeParameters map of runtime parameters to configure inside the AnalysisEngine
   * @return AEProvider
   */
  public synchronized AEProvider getAEProvider(String aePath, Map<String, Object> runtimeParameters) {
    return getAEProvider(null, aePath, runtimeParameters);
  }
}
