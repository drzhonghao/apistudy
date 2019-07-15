import org.apache.lucene.analysis.uima.ae.*;



import java.io.IOException;

import org.apache.lucene.util.IOUtils;
import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.XMLInputSource;

/**
 * Basic {@link AEProvider} which just instantiates a UIMA {@link AnalysisEngine} with no additional metadata,
 * parameters or resources
 */
public class BasicAEProvider implements AEProvider {

  private final String aePath;
  private AnalysisEngineDescription cachedDescription;

  public BasicAEProvider(String aePath) {
    this.aePath = aePath;
  }

  @Override
  public AnalysisEngine getAE() throws ResourceInitializationException {
    synchronized(this) {
      if (cachedDescription == null) {
        XMLInputSource in = null;
        boolean success = false;
        try {
          // get Resource Specifier from XML file
          in = getInputSource();

          // get AE description
          cachedDescription = UIMAFramework.getXMLParser()
              .parseAnalysisEngineDescription(in);
          configureDescription(cachedDescription);
          success = true;
        } catch (Exception e) {
            throw new ResourceInitializationException(e);
        } finally {
          if (success) {
            try {
              IOUtils.close(in.getInputStream());
            } catch (IOException e) {
              throw new ResourceInitializationException(e);
            }
          } else if (in != null) {
            IOUtils.closeWhileHandlingException(in.getInputStream());
          }
        }
      } 
    }

    return UIMAFramework.produceAnalysisEngine(cachedDescription);
  }
  
  protected void configureDescription(AnalysisEngineDescription description) {
    // no configuration
  }
  
  private XMLInputSource getInputSource() throws IOException {
    try {
      return new XMLInputSource(aePath);
    } catch (Exception e) {
      return new XMLInputSource(getClass().getResource(aePath));
    }
  }
}
