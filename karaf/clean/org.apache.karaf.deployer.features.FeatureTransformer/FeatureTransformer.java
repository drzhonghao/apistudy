import org.apache.karaf.deployer.features.*;


import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.apache.karaf.util.DeployerUtils;
import org.apache.karaf.util.StreamUtils;
import org.osgi.framework.Constants;

/**
 * Transform a feature descriptor into an bundles bundle
 */
public class FeatureTransformer {

    public static void transform(URL url, OutputStream os) throws Exception {
        // Heuristicly retrieve name and version
        String name = getPath(url);
        int idx = name.lastIndexOf('/');
        if (idx >= 0) {
            name = name.substring(idx + 1);
        }
        String[] str = DeployerUtils.extractNameVersionType(name);
        // Create manifest
        Manifest m = new Manifest();
        m.getMainAttributes().putValue("Manifest-Version", "2");
        m.getMainAttributes().putValue(Constants.BUNDLE_MANIFESTVERSION, "2");
        m.getMainAttributes().putValue(Constants.BUNDLE_SYMBOLICNAME, str[0]);
        m.getMainAttributes().putValue(Constants.BUNDLE_VERSION, str[1]);
        // Put content
        JarOutputStream out = new JarOutputStream(os);
        ZipEntry e = new ZipEntry(JarFile.MANIFEST_NAME);
        out.putNextEntry(e);
        m.write(out);
        out.closeEntry();
        e = new ZipEntry("META-INF/");
        out.putNextEntry(e);
        e = new ZipEntry("META-INF/" + FeatureDeploymentListener.FEATURE_PATH + "/");
        out.putNextEntry(e);
        out.closeEntry();
        e = new ZipEntry("META-INF/" + FeatureDeploymentListener.FEATURE_PATH + "/" + name);
        out.putNextEntry(e);
        try (
            InputStream fis = url.openStream()
        ) {
            StreamUtils.copy(fis, out);
        }
        out.closeEntry();
        out.close();
        os.close();
    }

    private static String getPath(URL url) {
        if (url.getProtocol().equals("mvn")) {
            String[] parts = url.toExternalForm().substring(4).split("/");
            String groupId;
            String artifactId;
            String version;
            String type;
            String qualifier;
            if (parts.length < 3 || parts.length > 5) {
                return url.getPath();
            }
            groupId = parts[0];
            artifactId = parts[1];
            version = parts[2];
            type = (parts.length >= 4) ?  "." + parts[3] : ".jar";
            qualifier = (parts.length >= 5) ? "-" + parts[4] :  "";
            return groupId.replace('.', '/') + "/" + artifactId + "/"
                    + version + "/" + artifactId + "-" + version + qualifier + type;
        }
        return url.getPath();
    }

}
