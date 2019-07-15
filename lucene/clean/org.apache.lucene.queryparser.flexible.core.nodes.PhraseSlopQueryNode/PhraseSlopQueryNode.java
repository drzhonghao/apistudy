import org.apache.lucene.queryparser.flexible.core.nodes.QueryNodeImpl;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.*;


import org.apache.lucene.search.PhraseQuery; // javadocs
import org.apache.lucene.queryparser.flexible.messages.MessageImpl;
import org.apache.lucene.queryparser.flexible.core.QueryNodeError;
import org.apache.lucene.queryparser.flexible.core.messages.QueryParserMessages;
import org.apache.lucene.queryparser.flexible.core.parser.EscapeQuerySyntax;

/**
 * Query node for {@link PhraseQuery}'s slop factor.
 */
public class PhraseSlopQueryNode extends QueryNodeImpl implements FieldableNode {

  private int value = 0;

  /**
   * @exception QueryNodeError throw in overridden method to disallow
   */
  public PhraseSlopQueryNode(QueryNode query, int value) {
    if (query == null) {
      throw new QueryNodeError(new MessageImpl(
          QueryParserMessages.NODE_ACTION_NOT_SUPPORTED, "query", "null"));
    }

    this.value = value;
    setLeaf(false);
    allocate();
    add(query);
  }

  public QueryNode getChild() {
    return getChildren().get(0);
  }

  public int getValue() {
    return this.value;
  }

  private CharSequence getValueString() {
    Float f = Float.valueOf(this.value);
    if (f == f.longValue())
      return "" + f.longValue();
    else
      return "" + f;

  }

  @Override
  public String toString() {
    return "<phraseslop value='" + getValueString() + "'>" + "\n"
        + getChild().toString() + "\n</phraseslop>";
  }

  @Override
  public CharSequence toQueryString(EscapeQuerySyntax escapeSyntaxParser) {
    if (getChild() == null)
      return "";
    return getChild().toQueryString(escapeSyntaxParser) + "~"
        + getValueString();
  }

  @Override
  public QueryNode cloneTree() throws CloneNotSupportedException {
    PhraseSlopQueryNode clone = (PhraseSlopQueryNode) super.cloneTree();

    clone.value = this.value;

    return clone;
  }

  @Override
  public CharSequence getField() {
    QueryNode child = getChild();

    if (child instanceof FieldableNode) {
      return ((FieldableNode) child).getField();
    }

    return null;

  }

  @Override
  public void setField(CharSequence fieldName) {
    QueryNode child = getChild();

    if (child instanceof FieldableNode) {
      ((FieldableNode) child).setField(fieldName);
    }

  }

}
