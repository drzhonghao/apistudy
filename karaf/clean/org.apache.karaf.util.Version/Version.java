import org.apache.karaf.util.*;


import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Version {

    private static String version = "unknown";

    static {
        Properties versions = new Properties();
        try (InputStream is = Version.class.getResourceAsStream("versions.properties")) {
            versions.load(is);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        version = versions.getProperty("karaf-version");
    }

    public static String karafVersion() {
        return version;
    }

}
