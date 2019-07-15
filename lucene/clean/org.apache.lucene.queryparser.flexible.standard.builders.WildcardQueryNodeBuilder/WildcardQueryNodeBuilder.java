import org.apache.lucene.queryparser.flexible.standard.builders.*;


import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.standard.nodes.WildcardQueryNode;
import org.apache.lucene.queryparser.flexible.standard.processors.MultiTermRewriteMethodProcessor;
import org.apache.lucene.search.MultiTermQuery;
import org.apache.lucene.search.WildcardQuery;

/**
 * Builds a {@link WildcardQuery} object from a {@link WildcardQueryNode}
 * object.
 */
public class WildcardQueryNodeBuilder implements StandardQueryBuilder {

  public WildcardQueryNodeBuilder() {
    // empty constructor
  }

  @Override
  public WildcardQuery build(QueryNode queryNode) throws QueryNodeException {
    WildcardQueryNode wildcardNode = (WildcardQueryNode) queryNode;

    WildcardQuery q = new WildcardQuery(new Term(wildcardNode.getFieldAsString(),
                                                 wildcardNode.getTextAsString()));
    
    MultiTermQuery.RewriteMethod method = (MultiTermQuery.RewriteMethod)queryNode.getTag(MultiTermRewriteMethodProcessor.TAG_ID);
    if (method != null) {
      q.setRewriteMethod(method);
    }
    
    return q;
  }

}
