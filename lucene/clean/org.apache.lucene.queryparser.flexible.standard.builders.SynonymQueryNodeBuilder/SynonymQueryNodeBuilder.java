import org.apache.lucene.queryparser.flexible.standard.builders.*;


import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.core.builders.QueryTreeBuilder;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.standard.nodes.SynonymQueryNode;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;

/** Builer for {@link SynonymQueryNode}. */
public class SynonymQueryNodeBuilder implements StandardQueryBuilder {

  /** Sole constructor. */
  public SynonymQueryNodeBuilder() {}

  @Override
  public Query build(QueryNode queryNode) throws QueryNodeException {
    // TODO: use SynonymQuery instead
    SynonymQueryNode node = (SynonymQueryNode) queryNode;
    BooleanQuery.Builder builder = new BooleanQuery.Builder();
    for (QueryNode child : node.getChildren()) {
      Object obj = child.getTag(QueryTreeBuilder.QUERY_TREE_BUILDER_TAGID);

      if (obj != null) {
        Query query = (Query) obj;
        builder.add(query, Occur.SHOULD);
      }
    }
    return builder.build();
  }
}
