import org.apache.lucene.queryparser.flexible.standard.processors.*;


import java.util.List;

import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.core.nodes.BooleanQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.ModifierQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.ModifierQueryNode.Modifier;
import org.apache.lucene.queryparser.flexible.core.processors.QueryNodeProcessorImpl;
import org.apache.lucene.queryparser.flexible.standard.nodes.BooleanModifierNode;

/**
 * This processor removes every {@link BooleanQueryNode} that contains only one
 * child and returns this child. If this child is {@link ModifierQueryNode} that
 * was defined by the user. A modifier is not defined by the user when it's a
 * {@link BooleanModifierNode}
 * 
 * @see ModifierQueryNode
 */
public class BooleanSingleChildOptimizationQueryNodeProcessor extends
    QueryNodeProcessorImpl {

  public BooleanSingleChildOptimizationQueryNodeProcessor() {
    // empty constructor
  }

  @Override
  protected QueryNode postProcessNode(QueryNode node) throws QueryNodeException {

    if (node instanceof BooleanQueryNode) {
      List<QueryNode> children = node.getChildren();

      if (children != null && children.size() == 1) {
        QueryNode child = children.get(0);

        if (child instanceof ModifierQueryNode) {
          ModifierQueryNode modNode = (ModifierQueryNode) child;

          if (modNode instanceof BooleanModifierNode
              || modNode.getModifier() == Modifier.MOD_NONE) {

            return child;

          }

        } else {
          return child;
        }

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
