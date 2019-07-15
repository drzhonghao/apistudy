import org.apache.lucene.queryparser.flexible.core.processors.QueryNodeProcessorImpl;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.TokenizedPhraseQueryNode;
import org.apache.lucene.queryparser.flexible.core.processors.*;


import java.util.List;

import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.core.nodes.*;

/**
 * <p>
 * A {@link NoChildOptimizationQueryNodeProcessor} removes every
 * BooleanQueryNode, BoostQueryNode, TokenizedPhraseQueryNode or
 * ModifierQueryNode that do not have a valid children.
 * </p>
 * <p>
 * Example: When the children of these nodes are removed for any reason then the
 * nodes may become invalid.
 * </p>
 */
public class NoChildOptimizationQueryNodeProcessor extends
    QueryNodeProcessorImpl {

  public NoChildOptimizationQueryNodeProcessor() {
    // empty constructor
  }

  @Override
  protected QueryNode postProcessNode(QueryNode node) throws QueryNodeException {

    if (node instanceof BooleanQueryNode || node instanceof BoostQueryNode
        || node instanceof TokenizedPhraseQueryNode
        || node instanceof ModifierQueryNode) {

      List<QueryNode> children = node.getChildren();

      if (children != null && children.size() > 0) {

        for (QueryNode child : children) {

          if (!(child instanceof DeletedQueryNode)) {
            return node;
          }

        }

      }

      return new MatchNoDocsQueryNode();

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
