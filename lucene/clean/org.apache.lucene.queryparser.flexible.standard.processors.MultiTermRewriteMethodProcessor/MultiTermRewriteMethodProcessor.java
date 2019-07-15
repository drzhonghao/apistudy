import org.apache.lucene.queryparser.flexible.standard.processors.*;


import java.util.List;

import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.core.processors.QueryNodeProcessorImpl;
import org.apache.lucene.queryparser.flexible.standard.config.StandardQueryConfigHandler.ConfigurationKeys;
import org.apache.lucene.queryparser.flexible.standard.nodes.AbstractRangeQueryNode;
import org.apache.lucene.queryparser.flexible.standard.nodes.RegexpQueryNode;
import org.apache.lucene.queryparser.flexible.standard.nodes.WildcardQueryNode;
import org.apache.lucene.search.MultiTermQuery;

/**
 * This processor instates the default
 * {@link org.apache.lucene.search.MultiTermQuery.RewriteMethod},
 * {@link MultiTermQuery#CONSTANT_SCORE_REWRITE}, for multi-term
 * query nodes.
 */
public class MultiTermRewriteMethodProcessor extends QueryNodeProcessorImpl {

  public static final String TAG_ID = "MultiTermRewriteMethodConfiguration";

  @Override
  protected QueryNode postProcessNode(QueryNode node) {

    // set setMultiTermRewriteMethod for WildcardQueryNode and
    // PrefixWildcardQueryNode
    if (node instanceof WildcardQueryNode
        || node instanceof AbstractRangeQueryNode || node instanceof RegexpQueryNode) {
      
      MultiTermQuery.RewriteMethod rewriteMethod = getQueryConfigHandler().get(ConfigurationKeys.MULTI_TERM_REWRITE_METHOD);

      if (rewriteMethod == null) {
        // This should not happen, this configuration is set in the
        // StandardQueryConfigHandler
        throw new IllegalArgumentException(
            "StandardQueryConfigHandler.ConfigurationKeys.MULTI_TERM_REWRITE_METHOD should be set on the QueryConfigHandler");
      }

      // use a TAG to take the value to the Builder
      node.setTag(MultiTermRewriteMethodProcessor.TAG_ID, rewriteMethod);

    }

    return node;
  }

  @Override
  protected QueryNode preProcessNode(QueryNode node) {
    return node;
  }

  @Override
  protected List<QueryNode> setChildrenOrder(List<QueryNode> children) {
    return children;
  }
}
