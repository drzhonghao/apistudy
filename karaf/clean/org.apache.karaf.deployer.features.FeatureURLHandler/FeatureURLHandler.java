import org.apache.karaf.deployer.features.FeatureTransformer;
import org.apache.karaf.deployer.features.*;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.osgi.service.url.AbstractURLStreamHandlerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * URL handler for features
 */
public class FeatureURLHandler extends AbstractURLStreamHandlerService {

    private final Logger logger = LoggerFactory.getLogger(FeatureURLHandler.class);

    private static String SYNTAX = "feature: xml-uri";

    private URL featureXmlURL;

    /**
     * Open the connection for the given URL.
     *
     * @param url the url from which to open a connection.
     * @return a connection on the specified URL.
     * @throws java.io.IOException if an error occurs or if the URL is malformed.
     */
    @Override
    public URLConnection openConnection(URL url) throws IOException {
        if (url.getPath() == null || url.getPath().trim().length() == 0) {
            throw new MalformedURLException("Path cannot be null or empty. Syntax: " + SYNTAX );
        }
        featureXmlURL = new URL(url.getPath());

        logger.debug("Features xml URL is: [" + featureXmlURL + "]");
        return new Connection(url);
    }

    public URL getFeatureXmlURL() {
        return featureXmlURL;
    }

    public class Connection extends URLConnection {

        public Connection(URL url) {
            super(url);
        }

        @Override
        public void connect() throws IOException {
        }

        @Override
        public InputStream getInputStream() throws IOException {
            try {
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                FeatureTransformer.transform(featureXmlURL, os);
                os.close();
                return new ByteArrayInputStream(os.toByteArray());
            } catch (Exception e) {
                logger.error("Error opening features xml url", e);
                throw new IOException("Error opening features xml url", e);
            }
        }
    }


}
