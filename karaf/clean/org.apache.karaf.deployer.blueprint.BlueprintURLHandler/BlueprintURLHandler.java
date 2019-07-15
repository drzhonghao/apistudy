import org.apache.karaf.deployer.blueprint.BlueprintTransformer;
import org.apache.karaf.deployer.blueprint.*;


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
 * A URL handler that will transform a JBI artifact to an OSGi bundle
 * on the fly.  Needs to be registered in the OSGi registry.
 */
public class BlueprintURLHandler extends AbstractURLStreamHandlerService {

	private final Logger logger = LoggerFactory.getLogger(BlueprintURLHandler.class);

	private static String SYNTAX = "blueprint: bp-xml-uri";

    /**
     * Open the connection for the given URL.
     *
     * @param url the url from which to open a connection.
     * @return a connection on the specified URL.
     * @throws IOException if an error occurs or if the URL is malformed.
     */
    @Override
	public URLConnection openConnection(URL url) throws IOException {
		if (url.getPath() == null || url.getPath().trim().length() == 0) {
			throw new MalformedURLException ("Path cannot be null or empty. Syntax: " + SYNTAX );
		}

		logger.debug("Blueprint xml URL is: [" + url.getPath() + "]");
		return new Connection(url);
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
                BlueprintTransformer.transform(new URL(url.getPath()), os);
                os.close();
                return new ByteArrayInputStream(os.toByteArray());
            } catch (Exception e) {
                logger.error("Error opening blueprint xml url", e);
                throw new IOException("Error opening blueprint xml url", e);
            }
        }
    }

}
