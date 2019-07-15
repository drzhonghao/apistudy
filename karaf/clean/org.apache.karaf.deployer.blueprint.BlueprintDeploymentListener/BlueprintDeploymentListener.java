import org.apache.karaf.deployer.blueprint.*;


import java.io.File;
import java.net.URL;

import org.apache.karaf.util.XmlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import org.apache.felix.fileinstall.ArtifactUrlTransformer;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * A deployment listener that listens for spring xml applications
 * and creates bundles for these.
 */
public class BlueprintDeploymentListener implements ArtifactUrlTransformer {

    private final Logger logger = LoggerFactory.getLogger(BlueprintDeploymentListener.class);

    public boolean canHandle(File artifact) {
        try {
            if (artifact.isFile() && artifact.getName().endsWith(".xml")) {
                Document doc = parse(artifact);
                String name = doc.getDocumentElement().getLocalName();
                String uri  = doc.getDocumentElement().getNamespaceURI();
                if ("blueprint".equals(name) && "http://www.osgi.org/xmlns/blueprint/v1.0.0".equals(uri)) {
                    return true;
                }
            }
        } catch (Exception e) {
            logger.error("Unable to parse deployed file " + artifact.getAbsolutePath(), e);
        }
        return false;
    }

    public URL transform(URL artifact) {
        try {
            return new URL("blueprint", null, artifact.toString());
        } catch (Exception e) {
            logger.error("Unable to build blueprint application bundle", e);
            return null;
        }
    }

    protected Document parse(File artifact) throws Exception {
        return XmlUtils.parse(artifact, new ErrorHandler() {
            public void warning(SAXParseException exception) throws SAXException {
            }

            public void error(SAXParseException exception) throws SAXException {
            }

            public void fatalError(SAXParseException exception) throws SAXException {
                throw exception;
            }
        });
    }

}
