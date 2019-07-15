import org.apache.lucene.queryparser.flexible.standard.builders.*;


import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.core.builders.QueryTreeBuilder;
import org.apache.lucene.queryparser.flexible.core.nodes.ModifierQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.search.Query;

/**
 * Builds no object, it only returns the {@link Query} object set on the
 * {@link ModifierQueryNode} object using a
 * {@link QueryTreeBuilder#QUERY_TREE_BUILDER_TAGID} tag.
 */
public class ModifierQueryNodeBuilder implements StandardQueryBuilder {

  public ModifierQueryNodeBuilder() {
    // empty constructor
  }

  @Override
  public Query build(QueryNode queryNode) throws QueryNodeException {
    ModifierQueryNode modifierNode = (ModifierQueryNode) queryNode;

    return (Query) (modifierNode).getChild().getTag(
        QueryTreeBuilder.QUERY_TREE_BUILDER_TAGID);

  }

}
