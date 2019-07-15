import org.apache.lucene.queryparser.flexible.standard.builders.*;


import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.core.nodes.FuzzyQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.search.FuzzyQuery;

/**
 * Builds a {@link FuzzyQuery} object from a {@link FuzzyQueryNode} object.
 */
public class FuzzyQueryNodeBuilder implements StandardQueryBuilder {

  public FuzzyQueryNodeBuilder() {
    // empty constructor
  }

  @Override
  public FuzzyQuery build(QueryNode queryNode) throws QueryNodeException {
    FuzzyQueryNode fuzzyNode = (FuzzyQueryNode) queryNode;
    String text = fuzzyNode.getTextAsString();
    
    int numEdits = FuzzyQuery.floatToEdits(fuzzyNode.getSimilarity(), 
        text.codePointCount(0, text.length()));
    
    return new FuzzyQuery(new Term(fuzzyNode.getFieldAsString(), fuzzyNode
        .getTextAsString()), numEdits, fuzzyNode
        .getPrefixLength());

  }

}
