import org.apache.lucene.queryparser.xml.builders.*;


import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.queryparser.xml.DOMUtils;
import org.apache.lucene.queryparser.xml.ParserException;
import org.apache.lucene.queryparser.xml.QueryBuilder;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Builder for {@link BooleanQuery}
 */
public class BooleanQueryBuilder implements QueryBuilder {

  private final QueryBuilder factory;

  public BooleanQueryBuilder(QueryBuilder factory) {
    this.factory = factory;
  }

  /* (non-Javadoc)
    * @see org.apache.lucene.xmlparser.QueryObjectBuilder#process(org.w3c.dom.Element)
    */

  @Override
  public Query getQuery(Element e) throws ParserException {
    BooleanQuery.Builder bq = new BooleanQuery.Builder();
    bq.setMinimumNumberShouldMatch(DOMUtils.getAttribute(e, "minimumNumberShouldMatch", 0));

    NodeList nl = e.getChildNodes();
    final int nlLen = nl.getLength();
    for (int i = 0; i < nlLen; i++) {
      Node node = nl.item(i);
      if (node.getNodeName().equals("Clause")) {
        Element clauseElem = (Element) node;
        BooleanClause.Occur occurs = getOccursValue(clauseElem);

        Element clauseQuery = DOMUtils.getFirstChildOrFail(clauseElem);
        Query q = factory.getQuery(clauseQuery);
        bq.add(new BooleanClause(q, occurs));
      }
    }

    Query q = bq.build();
    float boost = DOMUtils.getAttribute(e, "boost", 1.0f);
    if (boost != 1f) {
      q = new BoostQuery(q, boost);
    }
    return q;
  }

  static BooleanClause.Occur getOccursValue(Element clauseElem) throws ParserException {
    String occs = clauseElem.getAttribute("occurs");
    if (occs == null || "should".equalsIgnoreCase(occs)) {
      return BooleanClause.Occur.SHOULD;
    } else if ("must".equalsIgnoreCase(occs)) {
      return BooleanClause.Occur.MUST;
    } else if ("mustNot".equalsIgnoreCase(occs)) {
      return BooleanClause.Occur.MUST_NOT;
    } else if ("filter".equals(occs)) {
      return BooleanClause.Occur.FILTER;
    }
    throw new ParserException("Invalid value for \"occurs\" attribute of clause:" + occs);
  }

}
