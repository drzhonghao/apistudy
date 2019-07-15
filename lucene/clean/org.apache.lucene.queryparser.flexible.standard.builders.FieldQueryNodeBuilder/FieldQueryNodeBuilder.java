import org.apache.lucene.queryparser.flexible.standard.builders.*;


import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.core.nodes.FieldQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.search.TermQuery;

/**
 * Builds a {@link TermQuery} object from a {@link FieldQueryNode} object.
 */
public class FieldQueryNodeBuilder implements StandardQueryBuilder {

  public FieldQueryNodeBuilder() {
    // empty constructor
  }

  @Override
  public TermQuery build(QueryNode queryNode) throws QueryNodeException {
    FieldQueryNode fieldNode = (FieldQueryNode) queryNode;

    return new TermQuery(new Term(fieldNode.getFieldAsString(), fieldNode
        .getTextAsString()));

  }

}
