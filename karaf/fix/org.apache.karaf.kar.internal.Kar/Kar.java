

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import org.apache.karaf.kar.internal.KarServiceImpl;
import org.apache.karaf.util.StreamUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Kar {
	public static final Logger LOGGER = LoggerFactory.getLogger(KarServiceImpl.class);

	public static final String MANIFEST_ATTR_KARAF_FEATURE_START = "Karaf-Feature-Start";

	public static final String MANIFEST_ATTR_KARAF_FEATURE_REPOS = "Karaf-Feature-Repos";

	private final URI karUri;

	private boolean shouldInstallFeatures;

	private List<URI> featureRepos;

	public Kar(URI karUri) {
		this.karUri = karUri;
	}

	public void extract(File repoDir, File resourceDir) {
		InputStream is = null;
		JarInputStream zipIs = null;
		this.featureRepos = new ArrayList<>();
		this.shouldInstallFeatures = true;
		try {
			is = karUri.toURL().openStream();
			repoDir.mkdirs();
			if (!(repoDir.isDirectory())) {
				throw new RuntimeException((("The KAR file " + (karUri)) + " is already installed"));
			}
			Kar.LOGGER.debug("Uncompress the KAR file {} into directory {}", karUri, repoDir);
			zipIs = new JarInputStream(is);
			boolean scanForRepos = true;
			Manifest manifest = zipIs.getManifest();
			if (manifest != null) {
				Attributes attr = manifest.getMainAttributes();
				String featureStartSt = ((String) (attr.get(new Attributes.Name(Kar.MANIFEST_ATTR_KARAF_FEATURE_START))));
				if ("false".equals(featureStartSt)) {
					shouldInstallFeatures = false;
				}
				String featureReposAttr = ((String) (attr.get(new Attributes.Name(Kar.MANIFEST_ATTR_KARAF_FEATURE_REPOS))));
				if (featureReposAttr != null) {
					featureRepos.add(new URI(featureReposAttr));
					scanForRepos = false;
				}
			}
			ZipEntry entry = zipIs.getNextEntry();
			while (entry != null) {
				if ((entry.getName().contains("..")) || (entry.getName().contains("%2e%2e"))) {
					Kar.LOGGER.warn("kar entry {} contains a .. relative path. For security reasons, it's not allowed.", entry.getName());
				}else {
					if (entry.getName().startsWith("repository/")) {
						String path = entry.getName().substring("repository/".length());
						File destFile = new File(repoDir, path);
						Kar.extract(zipIs, entry, destFile);
					}
					if (entry.getName().startsWith("resources/")) {
						String path = entry.getName().substring("resources/".length());
						File destFile = new File(resourceDir, path);
						Kar.extract(zipIs, entry, destFile);
					}
				}
				entry = zipIs.getNextEntry();
			} 
		} catch (Exception e) {
			throw new RuntimeException(((((("Error extracting kar file " + (karUri)) + " into dir ") + repoDir) + ": ") + (e.getMessage())), e);
		} finally {
			Kar.closeStream(zipIs);
			Kar.closeStream(is);
		}
	}

	private static File extract(InputStream is, ZipEntry zipEntry, File dest) throws Exception {
		if (zipEntry.isDirectory()) {
			Kar.LOGGER.debug("Creating directory {}", dest.getName());
			dest.mkdirs();
		}else {
			dest.getParentFile().mkdirs();
			FileOutputStream out = new FileOutputStream(dest);
			StreamUtils.copy(is, out);
			out.close();
		}
		return dest;
	}

	private static void closeStream(InputStream is) {
		if (is != null) {
			try {
				is.close();
			} catch (IOException e) {
				Kar.LOGGER.warn("Error closing stream", e);
			}
		}
	}

	public String getKarName() {
		try {
			String url = karUri.toURL().toString();
			if (url.startsWith("mvn")) {
				int index = url.indexOf("/");
				url = url.substring((index + 1));
				index = url.indexOf("/");
				url = url.substring(0, index);
				return url;
			}else {
				String karName = new File(karUri.toURL().getFile()).getName();
				karName = karName.substring(0, karName.lastIndexOf("."));
				return karName;
			}
		} catch (MalformedURLException e) {
			throw new RuntimeException(("Invalid kar URI " + (karUri)), e);
		}
	}

	public URI getKarUri() {
		return karUri;
	}

	public boolean isShouldInstallFeatures() {
		return shouldInstallFeatures;
	}

	public List<URI> getFeatureRepos() {
		return featureRepos;
	}
}

