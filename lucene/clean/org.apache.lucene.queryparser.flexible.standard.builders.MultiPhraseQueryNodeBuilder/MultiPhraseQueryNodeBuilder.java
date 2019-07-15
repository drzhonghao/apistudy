import org.apache.lucene.queryparser.flexible.standard.builders.*;


import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.core.builders.QueryTreeBuilder;
import org.apache.lucene.queryparser.flexible.core.nodes.FieldQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.standard.nodes.MultiPhraseQueryNode;
import org.apache.lucene.search.MultiPhraseQuery;
import org.apache.lucene.search.TermQuery;

/**
 * Builds a {@link MultiPhraseQuery} object from a {@link MultiPhraseQueryNode}
 * object.
 */
public class MultiPhraseQueryNodeBuilder implements StandardQueryBuilder {

  public MultiPhraseQueryNodeBuilder() {
    // empty constructor
  }

  @Override
  public MultiPhraseQuery build(QueryNode queryNode) throws QueryNodeException {
    MultiPhraseQueryNode phraseNode = (MultiPhraseQueryNode) queryNode;

    MultiPhraseQuery.Builder phraseQueryBuilder = new MultiPhraseQuery.Builder();

    List<QueryNode> children = phraseNode.getChildren();

    if (children != null) {
      TreeMap<Integer, List<Term>> positionTermMap = new TreeMap<>();

      for (QueryNode child : children) {
        FieldQueryNode termNode = (FieldQueryNode) child;
        TermQuery termQuery = (TermQuery) termNode
            .getTag(QueryTreeBuilder.QUERY_TREE_BUILDER_TAGID);
        List<Term> termList = positionTermMap.get(termNode
            .getPositionIncrement());

        if (termList == null) {
          termList = new LinkedList<>();
          positionTermMap.put(termNode.getPositionIncrement(), termList);

        }

        termList.add(termQuery.getTerm());

      }

      for (int positionIncrement : positionTermMap.keySet()) {
        List<Term> termList = positionTermMap.get(positionIncrement);

        phraseQueryBuilder.add(termList.toArray(new Term[termList.size()]),
            positionIncrement);

      }

    }

    return phraseQueryBuilder.build();

  }

}
