import org.apache.lucene.queryparser.flexible.standard.builders.*;


import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.core.builders.QueryTreeBuilder;
import org.apache.lucene.queryparser.flexible.core.nodes.GroupQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.search.Query;

/**
 * Builds no object, it only returns the {@link Query} object set on the
 * {@link GroupQueryNode} object using a
 * {@link QueryTreeBuilder#QUERY_TREE_BUILDER_TAGID} tag.
 */
public class GroupQueryNodeBuilder implements StandardQueryBuilder {

  public GroupQueryNodeBuilder() {
    // empty constructor
  }

  @Override
  public Query build(QueryNode queryNode) throws QueryNodeException {
    GroupQueryNode groupNode = (GroupQueryNode) queryNode;

    return (Query) (groupNode).getChild().getTag(
        QueryTreeBuilder.QUERY_TREE_BUILDER_TAGID);

  }

}
