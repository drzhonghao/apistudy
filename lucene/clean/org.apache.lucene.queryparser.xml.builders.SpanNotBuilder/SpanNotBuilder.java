import org.apache.lucene.queryparser.xml.builders.*;


import org.apache.lucene.search.spans.SpanBoostQuery;
import org.apache.lucene.search.spans.SpanNotQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.queryparser.xml.DOMUtils;
import org.apache.lucene.queryparser.xml.ParserException;
import org.w3c.dom.Element;
/**
 * Builder for {@link SpanNotQuery}
 */
public class SpanNotBuilder extends SpanBuilderBase {

  private final SpanQueryBuilder factory;

  public SpanNotBuilder(SpanQueryBuilder factory) {
    this.factory = factory;
  }

  @Override
  public SpanQuery getSpanQuery(Element e) throws ParserException {
    Element includeElem = DOMUtils.getChildByTagOrFail(e, "Include");
    includeElem = DOMUtils.getFirstChildOrFail(includeElem);

    Element excludeElem = DOMUtils.getChildByTagOrFail(e, "Exclude");
    excludeElem = DOMUtils.getFirstChildOrFail(excludeElem);

    SpanQuery include = factory.getSpanQuery(includeElem);
    SpanQuery exclude = factory.getSpanQuery(excludeElem);

    SpanNotQuery snq = new SpanNotQuery(include, exclude);

    float boost = DOMUtils.getAttribute(e, "boost", 1.0f);
    return new SpanBoostQuery(snq, boost);
  }

}
