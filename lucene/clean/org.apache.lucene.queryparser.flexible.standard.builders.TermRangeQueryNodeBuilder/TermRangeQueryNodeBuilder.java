import org.apache.lucene.queryparser.flexible.standard.builders.*;


import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.core.nodes.FieldQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.core.util.StringUtils;
import org.apache.lucene.queryparser.flexible.standard.nodes.TermRangeQueryNode;
import org.apache.lucene.queryparser.flexible.standard.processors.MultiTermRewriteMethodProcessor;
import org.apache.lucene.search.MultiTermQuery;
import org.apache.lucene.search.TermRangeQuery;

/**
 * Builds a {@link TermRangeQuery} object from a {@link TermRangeQueryNode}
 * object.
 */
public class TermRangeQueryNodeBuilder implements StandardQueryBuilder {
  
  public TermRangeQueryNodeBuilder() {
  // empty constructor
  }
  
  @Override
  public TermRangeQuery build(QueryNode queryNode) throws QueryNodeException {
    TermRangeQueryNode rangeNode = (TermRangeQueryNode) queryNode;
    FieldQueryNode upper = rangeNode.getUpperBound();
    FieldQueryNode lower = rangeNode.getLowerBound();
    
    String field = StringUtils.toString(rangeNode.getField());
    String lowerText = lower.getTextAsString();
    String upperText = upper.getTextAsString();
    
    if (lowerText.length() == 0) {
      lowerText = null;
    }
    
    if (upperText.length() == 0) {
      upperText = null;
    }
    
    TermRangeQuery rangeQuery = TermRangeQuery.newStringRange(field, lowerText, upperText, rangeNode
        .isLowerInclusive(), rangeNode.isUpperInclusive());
    
    MultiTermQuery.RewriteMethod method = (MultiTermQuery.RewriteMethod) queryNode
        .getTag(MultiTermRewriteMethodProcessor.TAG_ID);
    if (method != null) {
      rangeQuery.setRewriteMethod(method);
    }
    
    return rangeQuery;
    
  }
  
}
