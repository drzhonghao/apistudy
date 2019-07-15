import org.apache.lucene.analysis.uima.*;



import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.util.AttributeFactory;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FeaturePath;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.IOException;
import java.util.Map;

/**
 * A {@link Tokenizer} which creates token from UIMA Annotations filling also their {@link TypeAttribute} according to
 * {@link org.apache.uima.cas.FeaturePath}s specified
 */
public final class UIMATypeAwareAnnotationsTokenizer extends BaseUIMATokenizer {

  private final TypeAttribute typeAttr;

  private final CharTermAttribute termAttr;

  private final OffsetAttribute offsetAttr;

  private final String tokenTypeString;

  private final String typeAttributeFeaturePath;

  private FeaturePath featurePath;

  private int finalOffset = 0;

  public UIMATypeAwareAnnotationsTokenizer(String descriptorPath, String tokenType, String typeAttributeFeaturePath, Map<String, Object> configurationParameters) {
    this(descriptorPath, tokenType, typeAttributeFeaturePath, configurationParameters, DEFAULT_TOKEN_ATTRIBUTE_FACTORY);
  }

  public UIMATypeAwareAnnotationsTokenizer(String descriptorPath, String tokenType, String typeAttributeFeaturePath, 
                                           Map<String, Object> configurationParameters, AttributeFactory factory) {
    super(factory, descriptorPath, configurationParameters);
    this.tokenTypeString = tokenType;
    this.termAttr = addAttribute(CharTermAttribute.class);
    this.typeAttr = addAttribute(TypeAttribute.class);
    this.offsetAttr = addAttribute(OffsetAttribute.class);
    this.typeAttributeFeaturePath = typeAttributeFeaturePath;
  }

  @Override
  protected void initializeIterator() throws IOException {
    try {
      analyzeInput();
    } catch (AnalysisEngineProcessException | ResourceInitializationException e) {
      throw new IOException(e);
    }
    featurePath = cas.createFeaturePath();
    try {
      featurePath.initialize(typeAttributeFeaturePath);
    } catch (CASException e) {
      featurePath = null;
      throw new IOException(e);
    }
    finalOffset = correctOffset(cas.getDocumentText().length());
    Type tokenType = cas.getTypeSystem().getType(tokenTypeString);
    iterator = cas.getAnnotationIndex(tokenType).iterator();

  }

  @Override
  public boolean incrementToken() throws IOException {
    if (iterator == null) {
      initializeIterator();
    }
    if (iterator.hasNext()) {
      clearAttributes();
      AnnotationFS next = iterator.next();
      termAttr.append(next.getCoveredText());
      offsetAttr.setOffset(correctOffset(next.getBegin()), correctOffset(next.getEnd()));
      typeAttr.setType(featurePath.getValueAsString(next));
      return true;
    } else {
      return false;
    }
  }

  @Override
  public void end() throws IOException {
    super.end();
    offsetAttr.setOffset(finalOffset, finalOffset);
  }


}
