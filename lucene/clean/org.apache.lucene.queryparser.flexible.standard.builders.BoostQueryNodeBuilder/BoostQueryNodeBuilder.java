import org.apache.lucene.queryparser.flexible.standard.builders.*;


import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.core.builders.QueryTreeBuilder;
import org.apache.lucene.queryparser.flexible.core.nodes.BoostQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.Query;

/**
 * This builder basically reads the {@link Query} object set on the
 * {@link BoostQueryNode} child using
 * {@link QueryTreeBuilder#QUERY_TREE_BUILDER_TAGID} and applies the boost value
 * defined in the {@link BoostQueryNode}.
 */
public class BoostQueryNodeBuilder implements StandardQueryBuilder {

  public BoostQueryNodeBuilder() {
    // empty constructor
  }

  @Override
  public Query build(QueryNode queryNode) throws QueryNodeException {
    BoostQueryNode boostNode = (BoostQueryNode) queryNode;
    QueryNode child = boostNode.getChild();

    if (child == null) {
      return null;
    }

    Query query = (Query) child
        .getTag(QueryTreeBuilder.QUERY_TREE_BUILDER_TAGID);

    return new BoostQuery(query, boostNode.getValue());

  }

}
