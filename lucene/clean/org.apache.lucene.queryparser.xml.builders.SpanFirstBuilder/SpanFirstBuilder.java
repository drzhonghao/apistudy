import org.apache.lucene.queryparser.xml.builders.*;


import org.apache.lucene.search.spans.SpanBoostQuery;
import org.apache.lucene.search.spans.SpanFirstQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.queryparser.xml.DOMUtils;
import org.apache.lucene.queryparser.xml.ParserException;
import org.w3c.dom.Element;
/**
 * Builder for {@link SpanFirstQuery}
 */
public class SpanFirstBuilder extends SpanBuilderBase {

  private final SpanQueryBuilder factory;

  public SpanFirstBuilder(SpanQueryBuilder factory) {
    this.factory = factory;
  }

  @Override
  public SpanQuery getSpanQuery(Element e) throws ParserException {
    int end = DOMUtils.getAttribute(e, "end", 1);
    Element child = DOMUtils.getFirstChildElement(e);
    SpanQuery q = factory.getSpanQuery(child);

    SpanFirstQuery sfq = new SpanFirstQuery(q, end);

    float boost = DOMUtils.getAttribute(e, "boost", 1.0f);
    return new SpanBoostQuery(sfq, boost);
  }

}
