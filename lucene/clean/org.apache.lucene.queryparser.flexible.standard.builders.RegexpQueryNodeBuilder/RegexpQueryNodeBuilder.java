import org.apache.lucene.queryparser.flexible.standard.builders.*;


import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.standard.nodes.RegexpQueryNode;
import org.apache.lucene.queryparser.flexible.standard.processors.MultiTermRewriteMethodProcessor;
import org.apache.lucene.search.MultiTermQuery;
import org.apache.lucene.search.RegexpQuery;

/**
 * Builds a {@link RegexpQuery} object from a {@link RegexpQueryNode} object.
 */
public class RegexpQueryNodeBuilder implements StandardQueryBuilder {

  public RegexpQueryNodeBuilder() {
    // empty constructor
  }

  @Override
  public RegexpQuery build(QueryNode queryNode) throws QueryNodeException {
    RegexpQueryNode regexpNode = (RegexpQueryNode) queryNode;

    // TODO: make the maxStates configurable w/ a reasonable default (QueryParserBase uses 10000)
    RegexpQuery q = new RegexpQuery(new Term(regexpNode.getFieldAsString(),
        regexpNode.textToBytesRef()));

    MultiTermQuery.RewriteMethod method = (MultiTermQuery.RewriteMethod) queryNode
        .getTag(MultiTermRewriteMethodProcessor.TAG_ID);
    if (method != null) {
      q.setRewriteMethod(method);
    }

    return q;
  }

}
