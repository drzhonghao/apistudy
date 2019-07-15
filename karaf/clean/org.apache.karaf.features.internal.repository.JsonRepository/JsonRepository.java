import org.apache.karaf.features.internal.repository.*;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Repository using a JSON representation of resource metadata.
 * The json should be a map: the key is the resource uri and the
 * value is a map of resource headers.
 * The content of the URL can be gzipped.
 */
public class JsonRepository extends org.apache.felix.utils.repository.JsonRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonRepository.class);

    private final boolean ignoreFailures;

    public JsonRepository(String url, long expiration, boolean ignoreFailures) {
        super(url, expiration);
        this.ignoreFailures = ignoreFailures;
    }

    protected void checkAndLoadCache() {
        try {
            super.checkAndLoadCache();
        } catch (Exception e) {
            if (ignoreFailures) {
                LOGGER.warn("Ignoring failure: " + e.getMessage(), e);
            } else {
                throw e;
            }
        }
    }

}
