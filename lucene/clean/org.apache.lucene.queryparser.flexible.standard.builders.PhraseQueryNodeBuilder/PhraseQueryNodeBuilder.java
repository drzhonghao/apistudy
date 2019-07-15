import org.apache.lucene.queryparser.flexible.standard.builders.*;


import java.util.List;

import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.core.builders.QueryTreeBuilder;
import org.apache.lucene.queryparser.flexible.core.nodes.FieldQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.TokenizedPhraseQueryNode;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

/**
 * Builds a {@link PhraseQuery} object from a {@link TokenizedPhraseQueryNode}
 * object.
 */
public class PhraseQueryNodeBuilder implements StandardQueryBuilder {

  public PhraseQueryNodeBuilder() {
    // empty constructor
  }

  @Override
  public Query build(QueryNode queryNode) throws QueryNodeException {
    TokenizedPhraseQueryNode phraseNode = (TokenizedPhraseQueryNode) queryNode;

    PhraseQuery.Builder builder = new PhraseQuery.Builder();

    List<QueryNode> children = phraseNode.getChildren();

    if (children != null) {

      for (QueryNode child : children) {
        TermQuery termQuery = (TermQuery) child
            .getTag(QueryTreeBuilder.QUERY_TREE_BUILDER_TAGID);
        FieldQueryNode termNode = (FieldQueryNode) child;

        builder.add(termQuery.getTerm(), termNode.getPositionIncrement());
      }

    }

    return builder.build();

  }

}
