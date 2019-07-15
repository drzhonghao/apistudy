import org.apache.lucene.queryparser.flexible.standard.builders.*;


import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.standard.nodes.PrefixWildcardQueryNode;
import org.apache.lucene.queryparser.flexible.standard.processors.MultiTermRewriteMethodProcessor;
import org.apache.lucene.search.MultiTermQuery;
import org.apache.lucene.search.PrefixQuery;

/**
 * Builds a {@link PrefixQuery} object from a {@link PrefixWildcardQueryNode}
 * object.
 */
public class PrefixWildcardQueryNodeBuilder implements StandardQueryBuilder {

  public PrefixWildcardQueryNodeBuilder() {
    // empty constructor
  }

  @Override
  public PrefixQuery build(QueryNode queryNode) throws QueryNodeException {    

    PrefixWildcardQueryNode wildcardNode = (PrefixWildcardQueryNode) queryNode;

    String text = wildcardNode.getText().subSequence(0, wildcardNode.getText().length() - 1).toString();
    PrefixQuery q = new PrefixQuery(new Term(wildcardNode.getFieldAsString(), text));
    
    MultiTermQuery.RewriteMethod method = (MultiTermQuery.RewriteMethod)queryNode.getTag(MultiTermRewriteMethodProcessor.TAG_ID);
    if (method != null) {
      q.setRewriteMethod(method);
    }
    
    return q;
  }

}
