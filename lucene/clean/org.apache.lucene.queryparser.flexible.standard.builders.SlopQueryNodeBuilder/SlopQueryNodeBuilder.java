import org.apache.lucene.queryparser.flexible.standard.builders.*;


import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.core.builders.QueryTreeBuilder;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.SlopQueryNode;
import org.apache.lucene.search.MultiPhraseQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;

/**
 * This builder basically reads the {@link Query} object set on the
 * {@link SlopQueryNode} child using
 * {@link QueryTreeBuilder#QUERY_TREE_BUILDER_TAGID} and applies the slop value
 * defined in the {@link SlopQueryNode}.
 */
public class SlopQueryNodeBuilder implements StandardQueryBuilder {

  public SlopQueryNodeBuilder() {
    // empty constructor
  }

  @Override
  public Query build(QueryNode queryNode) throws QueryNodeException {
    SlopQueryNode phraseSlopNode = (SlopQueryNode) queryNode;

    Query query = (Query) phraseSlopNode.getChild().getTag(
        QueryTreeBuilder.QUERY_TREE_BUILDER_TAGID);

    if (query instanceof PhraseQuery) {
      PhraseQuery.Builder builder = new PhraseQuery.Builder();
      builder.setSlop(phraseSlopNode.getValue());
      PhraseQuery pq = (PhraseQuery) query;
      org.apache.lucene.index.Term[] terms = pq.getTerms();
      int[] positions = pq.getPositions();
      for (int i = 0; i < terms.length; ++i) {
        builder.add(terms[i], positions[i]);
      }
      query = builder.build();

    } else {
      MultiPhraseQuery mpq = (MultiPhraseQuery)query;
      
      int slop = phraseSlopNode.getValue();
      
      if (slop != mpq.getSlop()) {
        query = new MultiPhraseQuery.Builder(mpq).setSlop(slop).build();
      }
    }

    return query;

  }

}
