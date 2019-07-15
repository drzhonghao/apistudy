import org.apache.lucene.queryparser.xml.builders.*;


import org.apache.lucene.index.Term;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.queryparser.xml.DOMUtils;
import org.apache.lucene.queryparser.xml.ParserException;
import org.apache.lucene.queryparser.xml.QueryBuilder;
import org.w3c.dom.Element;
/**
 * Builder for {@link TermQuery}
 */
public class TermQueryBuilder implements QueryBuilder {

  @Override
  public Query getQuery(Element e) throws ParserException {
    String field = DOMUtils.getAttributeWithInheritanceOrFail(e, "fieldName");
    String value = DOMUtils.getNonBlankTextOrFail(e);
    Query tq = new TermQuery(new Term(field, value));
    float boost = DOMUtils.getAttribute(e, "boost", 1.0f);
    if (boost != 1f) {
      tq = new BoostQuery(tq, boost);
    }
    return tq;
  }

}
