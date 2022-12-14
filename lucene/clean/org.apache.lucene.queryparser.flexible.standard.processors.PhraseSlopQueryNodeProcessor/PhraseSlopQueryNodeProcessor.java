import org.apache.lucene.queryparser.flexible.standard.processors.*;


import java.util.List;

import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.SlopQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.TokenizedPhraseQueryNode;
import org.apache.lucene.queryparser.flexible.core.processors.QueryNodeProcessorImpl;
import org.apache.lucene.queryparser.flexible.standard.nodes.MultiPhraseQueryNode;

/**
 * This processor removes invalid {@link SlopQueryNode} objects in the query
 * node tree. A {@link SlopQueryNode} is invalid if its child is neither a
 * {@link TokenizedPhraseQueryNode} nor a {@link MultiPhraseQueryNode}.
 * 
 * @see SlopQueryNode
 */
public class PhraseSlopQueryNodeProcessor extends QueryNodeProcessorImpl {

  public PhraseSlopQueryNodeProcessor() {
    // empty constructor
  }

  @Override
  protected QueryNode postProcessNode(QueryNode node) throws QueryNodeException {

    if (node instanceof SlopQueryNode) {
      SlopQueryNode phraseSlopNode = (SlopQueryNode) node;

      if (!(phraseSlopNode.getChild() instanceof TokenizedPhraseQueryNode)
          && !(phraseSlopNode.getChild() instanceof MultiPhraseQueryNode)) {
        return phraseSlopNode.getChild();
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
