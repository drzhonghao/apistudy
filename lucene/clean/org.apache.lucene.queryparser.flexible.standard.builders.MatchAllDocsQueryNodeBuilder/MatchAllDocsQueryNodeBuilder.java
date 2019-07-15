import org.apache.lucene.queryparser.flexible.standard.builders.*;


import org.apache.lucene.queryparser.flexible.messages.MessageImpl;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.core.messages.QueryParserMessages;
import org.apache.lucene.queryparser.flexible.core.nodes.MatchAllDocsQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.standard.parser.EscapeQuerySyntaxImpl;
import org.apache.lucene.search.MatchAllDocsQuery;

/**
 * Builds a {@link MatchAllDocsQuery} object from a
 * {@link MatchAllDocsQueryNode} object.
 */
public class MatchAllDocsQueryNodeBuilder implements StandardQueryBuilder {

  public MatchAllDocsQueryNodeBuilder() {
    // empty constructor
  }

  @Override
  public MatchAllDocsQuery build(QueryNode queryNode) throws QueryNodeException {

    // validates node
    if (!(queryNode instanceof MatchAllDocsQueryNode)) {
      throw new QueryNodeException(new MessageImpl(
          QueryParserMessages.LUCENE_QUERY_CONVERSION_ERROR, queryNode
              .toQueryString(new EscapeQuerySyntaxImpl()), queryNode.getClass()
              .getName()));
    }

    return new MatchAllDocsQuery();

  }

}
