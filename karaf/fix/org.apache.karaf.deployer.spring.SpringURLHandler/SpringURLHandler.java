

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


public class SpringURLHandler extends AbstractURLStreamHandlerService {
	private final Logger logger = LoggerFactory.getLogger(SpringURLHandler.class);

	private static String SYNTAX = "spring: spring-xml-uri";

	@Override
	public URLConnection openConnection(URL url) throws IOException {
		if (((url.getPath()) == null) || ((url.getPath().trim().length()) == 0)) {
			throw new MalformedURLException(("Path cannot be null or empty. Syntax: " + (SpringURLHandler.SYNTAX)));
		}
		logger.debug((("Spring xml URL is: [" + (url.getPath())) + "]"));
		return new SpringURLHandler.Connection(url);
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
				os.close();
				return new ByteArrayInputStream(os.toByteArray());
			} catch (Exception e) {
				logger.error("Error opening Spring xml url", e);
				throw new IOException("Error opening Spring xml url", e);
			}
		}
	}
}

