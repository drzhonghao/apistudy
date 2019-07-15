import org.apache.lucene.queryparser.flexible.standard.processors.*;


import java.util.List;

import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.core.nodes.FieldQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.MatchAllDocsQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.core.processors.QueryNodeProcessorImpl;
import org.apache.lucene.queryparser.flexible.standard.nodes.WildcardQueryNode;
import org.apache.lucene.search.MatchAllDocsQuery;

/**
 * This processor converts every {@link WildcardQueryNode} that is "*:*" to
 * {@link MatchAllDocsQueryNode}.
 * 
 * @see MatchAllDocsQueryNode
 * @see MatchAllDocsQuery
 */
public class MatchAllDocsQueryNodeProcessor extends QueryNodeProcessorImpl {

  public MatchAllDocsQueryNodeProcessor() {
    // empty constructor
  }

  @Override
  protected QueryNode postProcessNode(QueryNode node) throws QueryNodeException {

    if (node instanceof FieldQueryNode) {
      FieldQueryNode fqn = (FieldQueryNode) node;

      if (fqn.getField().toString().equals("*")
          && fqn.getText().toString().equals("*")) {

        return new MatchAllDocsQueryNode();

      }

    }

    return node;

  }

  @Override
  protected QueryNode preProcessNode(QueryNode node) throws QueryNodeException {

    return node;

  }

  @Override
  protected List<QueryNode> setChildrenOrder(List<QueryNode> children)
      throws QueryNodeException {

    return children;

  }

}
