

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.apache.karaf.deployer.blueprint.BlueprintTransformer;
import org.apache.karaf.features.internal.download.impl.AbstractDownloadTask;
import org.apache.karaf.features.internal.download.impl.AbstractRetryableDownloadTask;


public class CustomSimpleDownloadTask extends AbstractRetryableDownloadTask {
	private static final String WRAP_URI_PREFIX = "wrap";

	private static final String SPRING_URI_PREFIX = "spring";

	private static final String BLUEPRINT_URI_PREFIX = "blueprint";

	private static final String WAR_URI_PREFIX = "war";

	private static final String PROFILE_URI_PREFIX = "profile";

	@Override
	protected File download(Exception previousExceptionNotUsed) throws Exception {
		URL url = createUrl(getUrl());
		Path path = Files.createTempFile("download-", null);
		try (final InputStream is = url.openStream()) {
			Files.copy(is, path, StandardCopyOption.REPLACE_EXISTING);
		}
		return path.toFile();
	}

	private URL createUrl(String url) throws MalformedURLException, URISyntaxException {
		URLStreamHandler handler = getUrlStreamHandler(url);
		if (handler != null) {
			return new URL(null, url, handler);
		}else {
			return new URL(url);
		}
	}

	private URLStreamHandler getUrlStreamHandler(String url) throws URISyntaxException {
		if (url.contains("\\")) {
			url = url.replace("\\", "/");
		}
		String scheme = url.substring(0, url.indexOf(':'));
		return null;
	}

	public class SpringURLHandler extends URLStreamHandler {
		@Override
		protected URLConnection openConnection(URL u) throws IOException {
			return new URLConnection(u) {
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
						throw new IOException("Error opening spring xml url", e);
					}
				}
			};
		}
	}

	public class BlueprintURLHandler extends URLStreamHandler {
		@Override
		protected URLConnection openConnection(URL u) throws IOException {
			return new URLConnection(u) {
				@Override
				public void connect() throws IOException {
				}

				@Override
				public InputStream getInputStream() throws IOException {
					try {
						ByteArrayOutputStream os = new ByteArrayOutputStream();
						BlueprintTransformer.transform(createUrl(url.getPath()), os);
						os.close();
						return new ByteArrayInputStream(os.toByteArray());
					} catch (Exception e) {
						throw new IOException("Error opening blueprint xml url", e);
					}
				}
			};
		}
	}

	public class ProfileURLHandler extends URLStreamHandler {
		@Override
		protected URLConnection openConnection(URL u) throws IOException {
			return new URLConnection(u) {
				@Override
				public void connect() throws IOException {
				}

				@Override
				public InputStream getInputStream() throws IOException {
					String path = url.getPath();
					return null;
				}
			};
		}
	}
}

