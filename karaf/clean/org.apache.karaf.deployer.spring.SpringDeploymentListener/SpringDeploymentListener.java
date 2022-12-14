import org.apache.karaf.deployer.spring.*;


import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.apache.felix.fileinstall.ArtifactUrlTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A deployment listener that listens for spring xml applications
 * and creates bundles for these.
 */
public class SpringDeploymentListener implements ArtifactUrlTransformer {
	private static final QName SPRING_DM_ROOT = new QName("http://www.springframework.org/schema/beans", "beans");

    private final Logger logger = LoggerFactory.getLogger(SpringDeploymentListener.class);
	private XMLInputFactory factory;
	
    public boolean canHandle(File artifact) {
        try {
            if (artifact.isFile() && artifact.getName().endsWith(".xml")) {
                StartElement element = getRootElement(artifact);
                if (element != null && SPRING_DM_ROOT.equals(element.getName())) {
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
            return new URL("spring", null, artifact.toString());
        } catch (Exception e) {
            logger.error("Unable to build spring application bundle", e);
            return null;
        }
    }

	protected StartElement getRootElement(File artifact) throws Exception {
    	XMLEventReader parser = null;
    	InputStream in = null;
    	try {
			if (factory == null) {
				factory = XMLInputFactory.newInstance();
			}
			in = new FileInputStream(artifact);
			parser = factory.createXMLEventReader(in);
			while (parser.hasNext()) {
				XMLEvent event = parser.nextEvent();
				if (event.isStartElement()) {
					return event.asStartElement();
				}
			}
			return null;
		} finally {
			if (parser != null) {
				parser.close();
			}
			if (in != null) {
				in.close();
			}
		}
    }

}
