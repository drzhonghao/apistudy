import org.apache.karaf.shell.impl.console.*;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Properties;


public final class Branding {
    static final Logger LOGGER = LoggerFactory.getLogger(Branding.class);

    private Branding() { }

    public static Properties loadBrandingProperties(boolean ssh) {
        Properties props = new Properties();
        loadPropsFromResource(props, "org/apache/karaf/shell/console/", ssh);
        loadPropsFromResource(props, "org/apache/karaf/branding/", ssh);
        loadPropsFromFile(props, System.getProperty("karaf.etc") + "/", ssh);
        return props;
    }

    private static void loadPropsFromFile(Properties props, String fileName, boolean ssh) {
        loadPropsFromFile(props, fileName + "branding.properties");
        if (ssh) {
            loadPropsFromFile(props, fileName + "branding-ssh.properties");
        }
    }

    private static void loadPropsFromFile(Properties props, String fileName) {
        try (FileInputStream is = new FileInputStream(fileName)) {
            loadProps(props, is);
        } catch (IOException e) {
            LOGGER.trace("Could not load branding.", e);
        }
    }

    private static void loadPropsFromResource(Properties props, String resource, boolean ssh) {
        loadPropsFromResource(props, resource + "branding.properties");
        if (ssh) {
            loadPropsFromResource(props, resource + "branding-ssh.properties");
        }
    }

    private static void loadPropsFromResource(Properties props, String resource) {
        try (InputStream is = Branding.class.getClassLoader().getResourceAsStream(resource)) {
            loadProps(props, is);
        } catch (IOException e) {
            LOGGER.trace("Could not load branding.", e);
        }
    }

    private static void loadProps(Properties props, InputStream is) throws IOException {
        if (is != null) {
            props.load(is);
        }
    }

}
