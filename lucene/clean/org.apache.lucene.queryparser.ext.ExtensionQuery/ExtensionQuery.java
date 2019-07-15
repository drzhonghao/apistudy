import org.apache.lucene.queryparser.ext.*;


import org.apache.lucene.queryparser.classic.QueryParser;

/**
 * {@link ExtensionQuery} holds all query components extracted from the original
 * query string like the query field and the extension query string.
 * 
 * @see Extensions
 * @see ExtendableQueryParser
 * @see ParserExtension
 */
public class ExtensionQuery {

  private final String field;
  private final String rawQueryString;
  private final QueryParser topLevelParser;

  /**
   * Creates a new {@link ExtensionQuery}
   * 
   * @param field
   *          the query field
   * @param rawQueryString
   *          the raw extension query string
   */
  public ExtensionQuery(QueryParser topLevelParser, String field, String rawQueryString) {
    this.field = field;
    this.rawQueryString = rawQueryString;
    this.topLevelParser = topLevelParser;
  }

  /**
   * Returns the query field
   * 
   * @return the query field
   */
  public String getField() {
    return field;
  }

  /**
   * Returns the raw extension query string
   * 
   * @return the raw extension query string
   */
  public String getRawQueryString() {
    return rawQueryString;
  }
  
  /**
   * Returns the top level parser which created this {@link ExtensionQuery} 
   * @return the top level parser which created this {@link ExtensionQuery}
   */
  public QueryParser getTopLevelParser() {
    return topLevelParser;
  }
}
