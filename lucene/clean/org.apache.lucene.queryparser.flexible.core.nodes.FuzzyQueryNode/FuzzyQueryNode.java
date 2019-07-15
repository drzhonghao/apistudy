import org.apache.lucene.queryparser.flexible.core.nodes.FieldQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.*;


import org.apache.lucene.queryparser.flexible.core.parser.EscapeQuerySyntax;

/**
 * A {@link FuzzyQueryNode} represents a element that contains
 * field/text/similarity tuple
 */
public class FuzzyQueryNode extends FieldQueryNode {

  private float similarity;

  private int prefixLength;

  /**
   * @param field
   *          Name of the field query will use.
   * @param termStr
   *          Term token to use for building term for the query
   */
  /**
   * @param field
   *          - Field name
   * @param term
   *          - Value
   * @param minSimilarity
   *          - similarity value
   * @param begin
   *          - position in the query string
   * @param end
   *          - position in the query string
   */
  public FuzzyQueryNode(CharSequence field, CharSequence term,
      float minSimilarity, int begin, int end) {
    super(field, term, begin, end);
    this.similarity = minSimilarity;
    setLeaf(true);
  }

  public void setPrefixLength(int prefixLength) {
    this.prefixLength = prefixLength;
  }

  public int getPrefixLength() {
    return this.prefixLength;
  }

  @Override
  public CharSequence toQueryString(EscapeQuerySyntax escaper) {
    if (isDefaultField(this.field)) {
      return getTermEscaped(escaper) + "~" + this.similarity;
    } else {
      return this.field + ":" + getTermEscaped(escaper) + "~" + this.similarity;
    }
  }

  @Override
  public String toString() {
    return "<fuzzy field='" + this.field + "' similarity='" + this.similarity
        + "' term='" + this.text + "'/>";
  }

  public void setSimilarity(float similarity) {
    this.similarity = similarity;
  }

  @Override
  public FuzzyQueryNode cloneTree() throws CloneNotSupportedException {
    FuzzyQueryNode clone = (FuzzyQueryNode) super.cloneTree();

    clone.similarity = this.similarity;

    return clone;
  }

  /**
   * @return the similarity
   */
  public float getSimilarity() {
    return this.similarity;
  }
}
