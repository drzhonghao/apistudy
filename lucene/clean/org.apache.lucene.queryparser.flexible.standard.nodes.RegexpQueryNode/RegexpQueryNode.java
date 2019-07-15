import org.apache.lucene.queryparser.flexible.standard.nodes.*;


import org.apache.lucene.queryparser.flexible.core.nodes.FieldableNode;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNodeImpl;
import org.apache.lucene.queryparser.flexible.core.nodes.TextableQueryNode;
import org.apache.lucene.queryparser.flexible.core.parser.EscapeQuerySyntax;
import org.apache.lucene.search.RegexpQuery;
import org.apache.lucene.util.BytesRef;

/**
 * A {@link RegexpQueryNode} represents {@link RegexpQuery} query Examples: /[a-z]|[0-9]/
 */
public class RegexpQueryNode extends QueryNodeImpl  implements TextableQueryNode,
FieldableNode {
  private CharSequence text;
  private CharSequence field;
  /**
   * @param field
   *          - field name
   * @param text
   *          - value that contains a regular expression
   * @param begin
   *          - position in the query string
   * @param end
   *          - position in the query string
   */
  public RegexpQueryNode(CharSequence field, CharSequence text, int begin,
      int end) {
    this.field = field;
    this.text = text.subSequence(begin, end);
  }

  public BytesRef textToBytesRef() {
    return new BytesRef(text);
  }

  @Override
  public String toString() {
    return "<regexp field='" + this.field + "' term='" + this.text + "'/>";
  }

  @Override
  public RegexpQueryNode cloneTree() throws CloneNotSupportedException {
    RegexpQueryNode clone = (RegexpQueryNode) super.cloneTree();
    clone.field = this.field;
    clone.text = this.text;
    return clone;
  }

  @Override
  public CharSequence getText() {
    return text;
  }

  @Override
  public void setText(CharSequence text) {
    this.text = text;
  }

  @Override
  public CharSequence getField() {
    return field;
  }
  
  public String getFieldAsString() {
    return field.toString();
  }

  @Override
  public void setField(CharSequence field) {
    this.field = field;
  }

  @Override
  public CharSequence toQueryString(EscapeQuerySyntax escapeSyntaxParser) {
    return isDefaultField(field)? "/"+text+"/": field + ":/" + text + "/";
  }

}
