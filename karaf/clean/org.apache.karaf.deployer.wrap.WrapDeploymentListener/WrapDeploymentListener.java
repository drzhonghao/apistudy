import org.apache.karaf.system.commands.Name;
import org.apache.karaf.deployer.wrap.*;


import java.io.File;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.felix.fileinstall.ArtifactUrlTransformer;
import org.apache.karaf.util.DeployerUtils;

/**
 * A deployment listener that listens for non OSGi jar deployements.
 */
public class WrapDeploymentListener implements ArtifactUrlTransformer {

    public boolean canHandle(File artifact) {
        try {
            // only handle .jar files
            if (!artifact.getPath().endsWith(".jar")) {
                return false;
            }
            try (JarFile jar = new JarFile(artifact)) {
                // only handle non OSGi jar
                Manifest manifest = jar.getManifest();
                return manifest == null
                        || manifest.getMainAttributes().getValue(new Attributes.Name("Bundle-SymbolicName")) == null
                        || manifest.getMainAttributes().getValue(new Attributes.Name("Bundle-Version")) == null;
            }
        } catch (Exception e) {
            return false;
        }
    }

    public URL transform(URL artifact) throws Exception {
        try
        {
            String path = artifact.getPath();
            String name = path.substring( path.lastIndexOf('/') + 1);
            String[] nv = DeployerUtils.extractNameVersionType( name );
            return new URL("wrap", null, artifact.toExternalForm() + "$Bundle-SymbolicName=" + nv[0] + "&Bundle-Version=" + nv[1]);
        } catch (Exception e) {
            return new URL("wrap", null, artifact.toExternalForm());
        }
    }

}
