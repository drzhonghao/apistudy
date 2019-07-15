import org.apache.lucene.queryparser.flexible.core.nodes.*;


import java.util.List;

import org.apache.lucene.queryparser.flexible.messages.MessageImpl;
import org.apache.lucene.queryparser.flexible.core.QueryNodeError;
import org.apache.lucene.queryparser.flexible.core.messages.QueryParserMessages;
import org.apache.lucene.queryparser.flexible.core.parser.EscapeQuerySyntax;

/**
 * A {@link BoostQueryNode} boosts the QueryNode tree which is under this node.
 * So, it must only and always have one child.
 * 
 * The boost value may vary from 0.0 to 1.0.
 * 
 */
public class BoostQueryNode extends QueryNodeImpl {

  private float value = 0;

  /**
   * Constructs a boost node
   * 
   * @param query
   *          the query to be boosted
   * @param value
   *          the boost value, it may vary from 0.0 to 1.0
   */
  public BoostQueryNode(QueryNode query, float value) {
    if (query == null) {
      throw new QueryNodeError(new MessageImpl(
          QueryParserMessages.NODE_ACTION_NOT_SUPPORTED, "query", "null"));
    }

    this.value = value;
    setLeaf(false);
    allocate();
    add(query);
  }

  /**
   * Returns the single child which this node boosts.
   * 
   * @return the single child which this node boosts
   */
  public QueryNode getChild() {
    List<QueryNode> children = getChildren();

    if (children == null || children.size() == 0) {
      return null;
    }

    return children.get(0);

  }

  /**
   * Returns the boost value. It may vary from 0.0 to 1.0.
   * 
   * @return the boost value
   */
  public float getValue() {
    return this.value;
  }

  /**
   * Returns the boost value parsed to a string.
   * 
   * @return the parsed value
   */
  private CharSequence getValueString() {
    Float f = Float.valueOf(this.value);
    if (f == f.longValue())
      return "" + f.longValue();
    else
      return "" + f;

  }

  @Override
  public String toString() {
    return "<boost value='" + getValueString() + "'>" + "\n"
        + getChild().toString() + "\n</boost>";
  }

  @Override
  public CharSequence toQueryString(EscapeQuerySyntax escapeSyntaxParser) {
    if (getChild() == null)
      return "";
    return getChild().toQueryString(escapeSyntaxParser) + "^"
        + getValueString();
  }

  @Override
  public QueryNode cloneTree() throws CloneNotSupportedException {
    BoostQueryNode clone = (BoostQueryNode) super.cloneTree();

    clone.value = this.value;

    return clone;
  }

}
