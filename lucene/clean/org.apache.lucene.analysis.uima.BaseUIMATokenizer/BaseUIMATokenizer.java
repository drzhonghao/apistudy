import org.apache.lucene.analysis.uima.*;



import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.uima.ae.AEProviderFactory;
import org.apache.lucene.util.AttributeFactory;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.IOException;
import java.io.Reader;
import java.util.Map;

/**
 * Abstract base implementation of a {@link Tokenizer} which is able to analyze the given input with a
 * UIMA {@link AnalysisEngine}
 */
public abstract class BaseUIMATokenizer extends Tokenizer {

  protected FSIterator<AnnotationFS> iterator;

  private final String descriptorPath;
  private final Map<String, Object> configurationParameters;

  protected AnalysisEngine ae;
  protected CAS cas;

  protected BaseUIMATokenizer
      (AttributeFactory factory, String descriptorPath, Map<String, Object> configurationParameters) {
    super(factory);
    this.descriptorPath = descriptorPath;
    this.configurationParameters = configurationParameters;
  }

  /**
   * analyzes the tokenizer input using the given analysis engine
   * <p>
   * {@link #cas} will be filled with  extracted metadata (UIMA annotations, feature structures)
   *
   * @throws IOException If there is a low-level I/O error.
   */
  protected void analyzeInput() throws ResourceInitializationException, AnalysisEngineProcessException, IOException {
    if (ae == null) {
      ae = AEProviderFactory.getInstance().getAEProvider(null, descriptorPath, configurationParameters).getAE();
    }
    if (cas == null) {
      cas = ae.newCAS();
    } else {
      cas.reset();
    }
    cas.setDocumentText(toString(input));
    ae.process(cas);
  }

  /**
   * initialize the FSIterator which is used to build tokens at each incrementToken() method call
   *
   * @throws IOException If there is a low-level I/O error.
   */
  protected abstract void initializeIterator() throws IOException;

  private String toString(Reader reader) throws IOException {
    StringBuilder stringBuilder = new StringBuilder();
    int ch;
    while ((ch = reader.read()) > -1) {
      stringBuilder.append((char) ch);
    }
    return stringBuilder.toString();
  }

  @Override
  public void reset() throws IOException {
    super.reset();
    iterator = null;
  }
}
