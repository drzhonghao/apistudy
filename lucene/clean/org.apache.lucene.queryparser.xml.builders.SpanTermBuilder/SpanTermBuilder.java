import org.apache.lucene.queryparser.xml.builders.*;


import org.apache.lucene.index.Term;
import org.apache.lucene.search.spans.SpanBoostQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.queryparser.xml.DOMUtils;
import org.apache.lucene.queryparser.xml.ParserException;
import org.w3c.dom.Element;
/**
 * Builder for {@link SpanTermQuery}
 */
public class SpanTermBuilder extends SpanBuilderBase {

  @Override
  public SpanQuery getSpanQuery(Element e) throws ParserException {
    String fieldName = DOMUtils.getAttributeWithInheritanceOrFail(e, "fieldName");
    String value = DOMUtils.getNonBlankTextOrFail(e);
    SpanTermQuery stq = new SpanTermQuery(new Term(fieldName, value));

    float boost = DOMUtils.getAttribute(e, "boost", 1.0f);
    return new SpanBoostQuery(stq, boost);
  }

}
