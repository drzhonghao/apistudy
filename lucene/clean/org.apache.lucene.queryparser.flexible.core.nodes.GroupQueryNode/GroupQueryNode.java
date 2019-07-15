import org.apache.lucene.queryparser.flexible.core.nodes.*;


import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.queryparser.flexible.messages.MessageImpl;
import org.apache.lucene.queryparser.flexible.core.QueryNodeError;
import org.apache.lucene.queryparser.flexible.core.messages.QueryParserMessages;
import org.apache.lucene.queryparser.flexible.core.parser.EscapeQuerySyntax;

/**
 * A {@link GroupQueryNode} represents a location where the original user typed
 * real parenthesis on the query string. This class is useful for queries like:
 * a) a AND b OR c b) ( a AND b) OR c
 * 
 * Parenthesis might be used to define the boolean operation precedence.
 */
public class GroupQueryNode extends QueryNodeImpl {

  /**
   * This QueryNode is used to identify parenthesis on the original query string
   */
  public GroupQueryNode(QueryNode query) {
    if (query == null) {
      throw new QueryNodeError(new MessageImpl(
          QueryParserMessages.PARAMETER_VALUE_NOT_SUPPORTED, "query", "null"));
    }

    allocate();
    setLeaf(false);
    add(query);
  }

  public QueryNode getChild() {
    return getChildren().get(0);
  }

  @Override
  public String toString() {
    return "<group>" + "\n" + getChild().toString() + "\n</group>";
  }

  @Override
  public CharSequence toQueryString(EscapeQuerySyntax escapeSyntaxParser) {
    if (getChild() == null)
      return "";

    return "( " + getChild().toQueryString(escapeSyntaxParser) + " )";
  }

  @Override
  public QueryNode cloneTree() throws CloneNotSupportedException {
    GroupQueryNode clone = (GroupQueryNode) super.cloneTree();

    return clone;
  }

  public void setChild(QueryNode child) {
    List<QueryNode> list = new ArrayList<>();
    list.add(child);
    this.set(list);
  }

}
