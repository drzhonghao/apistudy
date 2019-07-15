import org.apache.lucene.queryparser.xml.builders.*;


import org.apache.lucene.queryparser.xml.DOMUtils;
import org.apache.lucene.queryparser.xml.ParserException;
import org.apache.lucene.queryparser.xml.QueryBuilder;
import org.apache.lucene.queryparser.xml.QueryBuilderFactory;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.Query;
import org.w3c.dom.Element;
/**
 * Builder for {@link ConstantScoreQuery}
 */
public class ConstantScoreQueryBuilder implements QueryBuilder {

  private final QueryBuilderFactory queryFactory;

  public ConstantScoreQueryBuilder(QueryBuilderFactory queryFactory) {
    this.queryFactory = queryFactory;
  }

  @Override
  public Query getQuery(Element e) throws ParserException {
    Element queryElem = DOMUtils.getFirstChildOrFail(e);

    Query q = new ConstantScoreQuery(queryFactory.getQuery(queryElem));
    float boost = DOMUtils.getAttribute(e, "boost", 1.0f);
    if (boost != 1f) {
      q = new BoostQuery(q, boost);
    }
    return q;
  }

}
