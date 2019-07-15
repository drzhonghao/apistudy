import org.apache.lucene.queryparser.flexible.core.nodes.FieldQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.*;


import org.apache.lucene.queryparser.flexible.core.parser.EscapeQuerySyntax;

/**
 * A {@link QuotedFieldQueryNode} represents phrase query. Example:
 * "life is great"
 */
public class QuotedFieldQueryNode extends FieldQueryNode {

  /**
   * @param field
   *          - field name
   * @param text
   *          - value
   * @param begin
   *          - position in the query string
   * @param end
   *          - position in the query string
   */
  public QuotedFieldQueryNode(CharSequence field, CharSequence text, int begin,
      int end) {
    super(field, text, begin, end);
  }

  @Override
  public CharSequence toQueryString(EscapeQuerySyntax escaper) {
    if (isDefaultField(this.field)) {
      return "\"" + getTermEscapeQuoted(escaper) + "\"";
    } else {
      return this.field + ":" + "\"" + getTermEscapeQuoted(escaper) + "\"";
    }
  }

  @Override
  public String toString() {
    return "<quotedfield start='" + this.begin + "' end='" + this.end
        + "' field='" + this.field + "' term='" + this.text + "'/>";
  }

  @Override
  public QuotedFieldQueryNode cloneTree() throws CloneNotSupportedException {
    QuotedFieldQueryNode clone = (QuotedFieldQueryNode) super.cloneTree();
    // nothing to do here
    return clone;
  }

}
