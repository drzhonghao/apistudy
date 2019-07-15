import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.*;


import java.util.Iterator;
import java.util.List;

import org.apache.lucene.queryparser.flexible.core.parser.EscapeQuerySyntax;

/**
 * A {@link OrQueryNode} represents an OR boolean operation performed on a list
 * of nodes.
 * 
 */
public class OrQueryNode extends BooleanQueryNode {

  /**
   * @param clauses
   *          - the query nodes to be or'ed
   */
  public OrQueryNode(List<QueryNode> clauses) {
    super(clauses);
    if ((clauses == null) || (clauses.size() == 0)) {
      throw new IllegalArgumentException(
          "OR query must have at least one clause");
    }
  }

  @Override
  public String toString() {
    if (getChildren() == null || getChildren().size() == 0)
      return "<boolean operation='or'/>";
    StringBuilder sb = new StringBuilder();
    sb.append("<boolean operation='or'>");
    for (QueryNode child : getChildren()) {
      sb.append("\n");
      sb.append(child.toString());

    }
    sb.append("\n</boolean>");
    return sb.toString();
  }

  @Override
  public CharSequence toQueryString(EscapeQuerySyntax escapeSyntaxParser) {
    if (getChildren() == null || getChildren().size() == 0)
      return "";

    StringBuilder sb = new StringBuilder();
    String filler = "";
    for (Iterator<QueryNode> it = getChildren().iterator(); it.hasNext();) {
      sb.append(filler).append(it.next().toQueryString(escapeSyntaxParser));
      filler = " OR ";
    }

    // in case is root or the parent is a group node avoid parenthesis
    if ((getParent() != null && getParent() instanceof GroupQueryNode)
        || isRoot())
      return sb.toString();
    else
      return "( " + sb.toString() + " )";
  }
}
