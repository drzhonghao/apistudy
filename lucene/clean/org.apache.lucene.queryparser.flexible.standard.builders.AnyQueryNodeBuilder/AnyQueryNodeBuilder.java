import org.apache.lucene.queryparser.flexible.standard.builders.*;


import java.util.List;

import org.apache.lucene.queryparser.flexible.messages.MessageImpl;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.core.builders.QueryTreeBuilder;
import org.apache.lucene.queryparser.flexible.core.messages.QueryParserMessages;
import org.apache.lucene.queryparser.flexible.core.nodes.AnyQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.BooleanQuery.TooManyClauses;

/**
 * Builds a BooleanQuery of SHOULD clauses, possibly with
 * some minimum number to match.
 */
public class AnyQueryNodeBuilder implements StandardQueryBuilder {

  public AnyQueryNodeBuilder() {
    // empty constructor
  }

  @Override
  public BooleanQuery build(QueryNode queryNode) throws QueryNodeException {
    AnyQueryNode andNode = (AnyQueryNode) queryNode;

    BooleanQuery.Builder bQuery = new BooleanQuery.Builder();
    List<QueryNode> children = andNode.getChildren();

    if (children != null) {

      for (QueryNode child : children) {
        Object obj = child.getTag(QueryTreeBuilder.QUERY_TREE_BUILDER_TAGID);

        if (obj != null) {
          Query query = (Query) obj;

          try {
            bQuery.add(query, BooleanClause.Occur.SHOULD);
          } catch (TooManyClauses ex) {

            throw new QueryNodeException(new MessageImpl(
            /*
             * IQQQ.Q0028E_TOO_MANY_BOOLEAN_CLAUSES,
             * BooleanQuery.getMaxClauseCount()
             */QueryParserMessages.EMPTY_MESSAGE), ex);

          }

        }

      }

    }

    bQuery.setMinimumNumberShouldMatch(andNode.getMinimumMatchingElements());

    return bQuery.build();

  }

}
