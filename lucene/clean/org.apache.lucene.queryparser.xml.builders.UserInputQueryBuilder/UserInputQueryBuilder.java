import org.apache.lucene.queryparser.xml.builders.*;


import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.queryparser.xml.DOMUtils;
import org.apache.lucene.queryparser.xml.ParserException;
import org.apache.lucene.queryparser.xml.QueryBuilder;
import org.w3c.dom.Element;

/**
 * UserInputQueryBuilder uses 1 of 2 strategies for thread-safe parsing:
 * 1) Synchronizing access to "parse" calls on a previously supplied QueryParser
 * or..
 * 2) creating a new QueryParser object for each parse request
 */
public class UserInputQueryBuilder implements QueryBuilder {

  private QueryParser unSafeParser;
  private Analyzer analyzer;
  private String defaultField;

  /**
   * This constructor has the disadvantage of not being able to change choice of default field name
   *
   * @param parser thread un-safe query parser
   */
  public UserInputQueryBuilder(QueryParser parser) {
    this.unSafeParser = parser;
  }

  public UserInputQueryBuilder(String defaultField, Analyzer analyzer) {
    this.analyzer = analyzer;
    this.defaultField = defaultField;
  }

  /* (non-Javadoc)
    * @see org.apache.lucene.xmlparser.QueryObjectBuilder#process(org.w3c.dom.Element)
    */

  @Override
  public Query getQuery(Element e) throws ParserException {
    String text = DOMUtils.getText(e);
    try {
      Query q = null;
      if (unSafeParser != null) {
        //synchronize on unsafe parser
        synchronized (unSafeParser) {
          q = unSafeParser.parse(text);
        }
      } else {
        String fieldName = DOMUtils.getAttribute(e, "fieldName", defaultField);
        //Create new parser
        QueryParser parser = createQueryParser(fieldName, analyzer);
        q = parser.parse(text);
      }
      float boost = DOMUtils.getAttribute(e, "boost", 1.0f);
      return new BoostQuery(q, boost);
    } catch (ParseException e1) {
      throw new ParserException(e1.getMessage());
    }
  }

  /**
   * Method to create a QueryParser - designed to be overridden
   *
   * @return QueryParser
   */
  protected QueryParser createQueryParser(String fieldName, Analyzer analyzer) {
    return new QueryParser(fieldName, analyzer);
  }

}
