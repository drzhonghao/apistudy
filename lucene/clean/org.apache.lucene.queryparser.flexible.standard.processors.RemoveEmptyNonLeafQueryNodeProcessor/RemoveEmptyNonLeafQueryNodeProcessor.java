import org.apache.lucene.queryparser.flexible.standard.processors.*;


import java.util.LinkedList;
import java.util.List;

import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.core.nodes.GroupQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.MatchNoDocsQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.ModifierQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.core.processors.QueryNodeProcessorImpl;

/**
 * This processor removes every {@link QueryNode} that is not a leaf and has not
 * children. If after processing the entire tree the root node is not a leaf and
 * has no children, a {@link MatchNoDocsQueryNode} object is returned.
 * <br>
 * This processor is used at the end of a pipeline to avoid invalid query node
 * tree structures like a {@link GroupQueryNode} or {@link ModifierQueryNode}
 * with no children.
 * 
 * @see QueryNode
 * @see MatchNoDocsQueryNode
 */
public class RemoveEmptyNonLeafQueryNodeProcessor extends
    QueryNodeProcessorImpl {

  private LinkedList<QueryNode> childrenBuffer = new LinkedList<>();

  public RemoveEmptyNonLeafQueryNodeProcessor() {
    // empty constructor
  }

  @Override
  public QueryNode process(QueryNode queryTree) throws QueryNodeException {
    queryTree = super.process(queryTree);

    if (!queryTree.isLeaf()) {

      List<QueryNode> children = queryTree.getChildren();

      if (children == null || children.size() == 0) {
        return new MatchNoDocsQueryNode();
      }

    }

    return queryTree;

  }

  @Override
  protected QueryNode postProcessNode(QueryNode node) throws QueryNodeException {

    return node;

  }

  @Override
  protected QueryNode preProcessNode(QueryNode node) throws QueryNodeException {

    return node;

  }

  @Override
  protected List<QueryNode> setChildrenOrder(List<QueryNode> children)
      throws QueryNodeException {

    try {

      for (QueryNode child : children) {

        if (!child.isLeaf()) {

          List<QueryNode> grandChildren = child.getChildren();

          if (grandChildren != null && grandChildren.size() > 0) {
            this.childrenBuffer.add(child);
          }

        } else {
          this.childrenBuffer.add(child);
        }

      }

      children.clear();
      children.addAll(this.childrenBuffer);

    } finally {
      this.childrenBuffer.clear();
    }

    return children;

  }

}
