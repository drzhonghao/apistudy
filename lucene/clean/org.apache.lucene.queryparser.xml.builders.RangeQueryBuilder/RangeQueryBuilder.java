import org.apache.lucene.queryparser.xml.builders.*;


import org.apache.lucene.queryparser.xml.DOMUtils;
import org.apache.lucene.queryparser.xml.ParserException;
import org.apache.lucene.queryparser.xml.QueryBuilder;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermRangeQuery;
import org.w3c.dom.Element;

/**
 * Builder for {@link TermRangeQuery}
 */
public class RangeQueryBuilder implements QueryBuilder {

  @Override
  public Query getQuery(Element e) throws ParserException {
    String fieldName = DOMUtils.getAttributeWithInheritance(e, "fieldName");

    String lowerTerm = e.getAttribute("lowerTerm");
    String upperTerm = e.getAttribute("upperTerm");
    boolean includeLower = DOMUtils.getAttribute(e, "includeLower", true);
    boolean includeUpper = DOMUtils.getAttribute(e, "includeUpper", true);
    return TermRangeQuery.newStringRange(fieldName, lowerTerm, upperTerm, includeLower, includeUpper);
  }

}
