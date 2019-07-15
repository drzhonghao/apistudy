import org.apache.lucene.queryparser.xml.builders.*;


import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.xml.DOMUtils;
import org.apache.lucene.queryparser.xml.ParserException;
import org.apache.lucene.queries.payloads.AveragePayloadFunction;
import org.apache.lucene.queries.payloads.PayloadScoreQuery;
import org.apache.lucene.search.spans.SpanBoostQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.w3c.dom.Element;

/**
 * Builder for {@link PayloadScoreQuery}
 */
public class BoostingTermBuilder extends SpanBuilderBase {

  @Override
  public SpanQuery getSpanQuery(Element e) throws ParserException {
    String fieldName = DOMUtils.getAttributeWithInheritanceOrFail(e, "fieldName");
    String value = DOMUtils.getNonBlankTextOrFail(e);

    SpanQuery btq = new PayloadScoreQuery(new SpanTermQuery(new Term(fieldName, value)),
        new AveragePayloadFunction());
    btq = new SpanBoostQuery(btq, DOMUtils.getAttribute(e, "boost", 1.0f));
    return btq;
  }

}
