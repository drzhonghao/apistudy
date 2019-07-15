import org.apache.lucene.queryparser.xml.builders.*;

import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.queryparser.xml.DOMUtils;
import org.apache.lucene.queryparser.xml.ParserException;
import org.apache.lucene.queryparser.xml.QueryBuilder;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.DisjunctionMaxQuery;
import org.apache.lucene.search.Query;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Builder for {@link DisjunctionMaxQuery}
 */
public class DisjunctionMaxQueryBuilder implements QueryBuilder {

  private final QueryBuilder factory;

  public DisjunctionMaxQueryBuilder(QueryBuilder factory) {
    this.factory = factory;
  }

  /* (non-Javadoc)
    * @see org.apache.lucene.xmlparser.QueryObjectBuilder#process(org.w3c.dom.Element)
    */

  @Override
  public Query getQuery(Element e) throws ParserException {
    float tieBreaker = DOMUtils.getAttribute(e, "tieBreaker", 0.0f); 

    List<Query> disjuncts = new ArrayList<>();
    NodeList nl = e.getChildNodes();
    final int nlLen = nl.getLength();
    for (int i = 0; i < nlLen; i++) {
      Node node = nl.item(i);
      if (node instanceof Element) { // all elements are disjuncts.
        Element queryElem = (Element) node;
        Query q = factory.getQuery(queryElem);
        disjuncts.add(q);
      }
    }

    Query q = new DisjunctionMaxQuery(disjuncts, tieBreaker);
    float boost = DOMUtils.getAttribute(e, "boost", 1.0f);
    if (boost != 1f) {
      q = new BoostQuery(q, boost);
    }
    return q;
  }
}
