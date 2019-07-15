import org.apache.lucene.queryparser.xml.builders.*;


import org.apache.lucene.search.spans.SpanBoostQuery;
import org.apache.lucene.search.spans.SpanOrQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.queryparser.xml.DOMUtils;
import org.apache.lucene.queryparser.xml.ParserException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;
/**
 * Builder for {@link SpanOrQuery}
 */
public class SpanOrBuilder extends SpanBuilderBase {

  private final SpanQueryBuilder factory;

  public SpanOrBuilder(SpanQueryBuilder factory) {
    this.factory = factory;
  }

  @Override
  public SpanQuery getSpanQuery(Element e) throws ParserException {
    List<SpanQuery> clausesList = new ArrayList<>();
    for (Node kid = e.getFirstChild(); kid != null; kid = kid.getNextSibling()) {
      if (kid.getNodeType() == Node.ELEMENT_NODE) {
        SpanQuery clause = factory.getSpanQuery((Element) kid);
        clausesList.add(clause);
      }
    }
    SpanQuery[] clauses = clausesList.toArray(new SpanQuery[clausesList.size()]);
    SpanOrQuery soq = new SpanOrQuery(clauses);
    float boost = DOMUtils.getAttribute(e, "boost", 1.0f);
    return new SpanBoostQuery(soq, boost);
  }

}
