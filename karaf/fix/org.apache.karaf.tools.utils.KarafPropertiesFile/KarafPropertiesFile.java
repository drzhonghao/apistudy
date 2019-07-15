

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Properties;
import org.apache.commons.io.FileUtils;


public class KarafPropertiesFile {
	private final Properties properties;

	private final File propertyFile;

	public KarafPropertiesFile(File karafHome, String location) {
		this(KarafPropertiesFile.homedPropFile(karafHome, location));
	}

	public KarafPropertiesFile(File propertyFile) {
		this.propertyFile = propertyFile;
		properties = new Properties();
	}

	private static File homedPropFile(File karafHome, String location) {
		File propFile;
		if (location.startsWith("/")) {
			propFile = new File((karafHome + location));
		}else {
			propFile = new File(((karafHome + "/") + location));
		}
		return propFile;
	}

	public void load() throws IOException {
		if (!(propertyFile.exists())) {
			return;
		}
		properties.load(new FileInputStream(propertyFile));
	}

	public void put(String key, String value) {
		properties.put(key, value);
	}

	public void remove(String key) {
		properties.remove(key);
	}

	public void extend(String key, String value, boolean prepend) {
		if ((properties.get(key)) == null) {
			properties.put(key, value);
			return;
		}else
			if (prepend) {
			}else {
			}

	}

	public String get(String key) {
		return properties.getProperty(key);
	}

	public void store() throws IOException {
		store(propertyFile);
	}

	public void store(File destinationFile) throws IOException {
		try (final FileOutputStream outputStream = new FileOutputStream(destinationFile)) {
			properties.store(outputStream, String.format("Modified by %s", getClass().getName()));
		}
	}

	public void replace(File source) {
		try {
			FileUtils.copyFile(source, propertyFile);
		} catch (IOException e) {
			throw new IllegalStateException(String.format("Failed to replace %s", propertyFile.getAbsolutePath()), e);
		}
	}
}

