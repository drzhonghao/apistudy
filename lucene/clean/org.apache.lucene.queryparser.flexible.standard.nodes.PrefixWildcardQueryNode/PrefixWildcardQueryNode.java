import org.apache.lucene.queryparser.flexible.standard.nodes.*;


import org.apache.lucene.queryparser.flexible.core.nodes.FieldQueryNode;

/**
 * A {@link PrefixWildcardQueryNode} represents wildcardquery that matches abc*
 * or *. This does not apply to phrases, this is a special case on the original
 * lucene parser. TODO: refactor the code to remove this special case from the
 * parser. and probably do it on a Processor
 */
public class PrefixWildcardQueryNode extends WildcardQueryNode {

  /**
   * @param field
   *          - field name
   * @param text
   *          - value including the wildcard
   * @param begin
   *          - position in the query string
   * @param end
   *          - position in the query string
   */
  public PrefixWildcardQueryNode(CharSequence field, CharSequence text,
      int begin, int end) {
    super(field, text, begin, end);
  }

  public PrefixWildcardQueryNode(FieldQueryNode fqn) {
    this(fqn.getField(), fqn.getText(), fqn.getBegin(), fqn.getEnd());
  }

  @Override
  public String toString() {
    return "<prefixWildcard field='" + this.field + "' term='" + this.text
        + "'/>";
  }

  @Override
  public PrefixWildcardQueryNode cloneTree() throws CloneNotSupportedException {
    PrefixWildcardQueryNode clone = (PrefixWildcardQueryNode) super.cloneTree();

    // nothing to do here

    return clone;
  }
}
