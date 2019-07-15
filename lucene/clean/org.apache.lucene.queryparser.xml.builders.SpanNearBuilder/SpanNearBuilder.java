import org.apache.lucene.queryparser.xml.builders.*;


import org.apache.lucene.search.spans.SpanBoostQuery;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.queryparser.xml.DOMUtils;
import org.apache.lucene.queryparser.xml.ParserException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;
/**
 * Builder for {@link SpanNearQuery}
 */
public class SpanNearBuilder extends SpanBuilderBase {

  private final SpanQueryBuilder factory;

  public SpanNearBuilder(SpanQueryBuilder factory) {
    this.factory = factory;
  }

  @Override
  public SpanQuery getSpanQuery(Element e) throws ParserException {
    String slopString = DOMUtils.getAttributeOrFail(e, "slop");
    int slop = Integer.parseInt(slopString);
    boolean inOrder = DOMUtils.getAttribute(e, "inOrder", false);
    List<SpanQuery> spans = new ArrayList<>();
    for (Node kid = e.getFirstChild(); kid != null; kid = kid.getNextSibling()) {
      if (kid.getNodeType() == Node.ELEMENT_NODE) {
        spans.add(factory.getSpanQuery((Element) kid));
      }
    }
    SpanQuery[] spanQueries = spans.toArray(new SpanQuery[spans.size()]);
    SpanQuery snq = new SpanNearQuery(spanQueries, slop, inOrder);
    float boost = DOMUtils.getAttribute(e, "boost", 1.0f);
    return new SpanBoostQuery(snq, boost);
  }

}
