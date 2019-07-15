import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.*;


import org.apache.lucene.queryparser.flexible.core.parser.EscapeQuerySyntax;

import java.util.List;


/**
 * A {@link AndQueryNode} represents an AND boolean operation performed on a
 * list of nodes.
 */
public class AndQueryNode extends BooleanQueryNode {

  /**
   * @param clauses
   *          - the query nodes to be and'ed
   */
  public AndQueryNode(List<QueryNode> clauses) {
    super(clauses);
    if ((clauses == null) || (clauses.size() == 0)) {
      throw new IllegalArgumentException(
          "AND query must have at least one clause");
    }
  }

  @Override
  public String toString() {
    if (getChildren() == null || getChildren().size() == 0)
      return "<boolean operation='and'/>";
    StringBuilder sb = new StringBuilder();
    sb.append("<boolean operation='and'>");
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
    for (QueryNode child : getChildren()) {
      sb.append(filler).append(child.toQueryString(escapeSyntaxParser));
      filler = " AND ";
    }

    // in case is root or the parent is a group node avoid parenthesis
    if ((getParent() != null && getParent() instanceof GroupQueryNode)
        || isRoot())
      return sb.toString();
    else
      return "( " + sb.toString() + " )";
  }

}
