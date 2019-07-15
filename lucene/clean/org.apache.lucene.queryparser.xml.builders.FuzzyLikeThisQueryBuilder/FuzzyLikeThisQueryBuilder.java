import org.apache.lucene.queryparser.xml.builders.*;


import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryparser.xml.DOMUtils;
import org.apache.lucene.queryparser.xml.ParserException;
import org.apache.lucene.queryparser.xml.QueryBuilder;
import org.apache.lucene.sandbox.queries.FuzzyLikeThisQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.Query;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Builder for {@link FuzzyLikeThisQuery}
 */
public class FuzzyLikeThisQueryBuilder implements QueryBuilder {

  private static final int DEFAULT_MAX_NUM_TERMS = 50;
  private static final float DEFAULT_MIN_SIMILARITY = FuzzyQuery.defaultMinSimilarity;
  private static final int DEFAULT_PREFIX_LENGTH = 1;
  private static final boolean DEFAULT_IGNORE_TF = false;

  private final Analyzer analyzer;

  public FuzzyLikeThisQueryBuilder(Analyzer analyzer) {
    this.analyzer = analyzer;
  }

  @Override
  public Query getQuery(Element e) throws ParserException {
    NodeList nl = e.getElementsByTagName("Field");
    int maxNumTerms = DOMUtils.getAttribute(e, "maxNumTerms", DEFAULT_MAX_NUM_TERMS);
    FuzzyLikeThisQuery fbq = new FuzzyLikeThisQuery(maxNumTerms, analyzer);
    fbq.setIgnoreTF(DOMUtils.getAttribute(e, "ignoreTF", DEFAULT_IGNORE_TF));

    final int nlLen = nl.getLength();
    for (int i = 0; i < nlLen; i++) {
      Element fieldElem = (Element) nl.item(i);
      float minSimilarity = DOMUtils.getAttribute(fieldElem, "minSimilarity", DEFAULT_MIN_SIMILARITY);
      int prefixLength = DOMUtils.getAttribute(fieldElem, "prefixLength", DEFAULT_PREFIX_LENGTH);
      String fieldName = DOMUtils.getAttributeWithInheritance(fieldElem, "fieldName");

      String value = DOMUtils.getText(fieldElem);
      fbq.addTerms(value, fieldName, minSimilarity, prefixLength);
    }

    Query q = fbq;
    float boost = DOMUtils.getAttribute(e, "boost", 1.0f);
    if (boost != 1f) {
      q = new BoostQuery(fbq, boost);
    }
    return q;
  }

}
