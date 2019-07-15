import org.apache.lucene.queryparser.flexible.core.nodes.*;


import org.apache.lucene.queryparser.flexible.core.parser.EscapeQuerySyntax;

import java.util.Locale;


/**
 * A {@link FieldQueryNode} represents a element that contains field/text tuple
 */
public class FieldQueryNode extends QueryNodeImpl implements FieldValuePairQueryNode<CharSequence>, TextableQueryNode {

  /**
   * The term's field
   */
  protected CharSequence field;

  /**
   * The term's text.
   */
  protected CharSequence text;

  /**
   * The term's begin position.
   */
  protected int begin;

  /**
   * The term's end position.
   */
  protected int end;

  /**
   * The term's position increment.
   */
  protected int positionIncrement;

  /**
   * @param field
   *          - field name
   * @param text
   *          - value
   * @param begin
   *          - position in the query string
   * @param end
   *          - position in the query string
   */
  public FieldQueryNode(CharSequence field, CharSequence text, int begin,
      int end) {
    this.field = field;
    this.text = text;
    this.begin = begin;
    this.end = end;
    this.setLeaf(true);

  }

  protected CharSequence getTermEscaped(EscapeQuerySyntax escaper) {
    return escaper.escape(this.text, Locale.getDefault(), EscapeQuerySyntax.Type.NORMAL);
  }

  protected CharSequence getTermEscapeQuoted(EscapeQuerySyntax escaper) {
    return escaper.escape(this.text, Locale.getDefault(), EscapeQuerySyntax.Type.STRING);
  }

  @Override
  public CharSequence toQueryString(EscapeQuerySyntax escaper) {
    if (isDefaultField(this.field)) {
      return getTermEscaped(escaper);
    } else {
      return this.field + ":" + getTermEscaped(escaper);
    }
  }

  @Override
  public String toString() {
    return "<field start='" + this.begin + "' end='" + this.end + "' field='"
        + this.field + "' text='" + this.text + "'/>";
  }

  /**
   * @return the term
   */
  public String getTextAsString() {
    if (this.text == null)
      return null;
    else
      return this.text.toString();
  }

  /**
   * returns null if the field was not specified in the query string
   * 
   * @return the field
   */
  public String getFieldAsString() {
    if (this.field == null)
      return null;
    else
      return this.field.toString();
  }

  public int getBegin() {
    return this.begin;
  }

  public void setBegin(int begin) {
    this.begin = begin;
  }

  public int getEnd() {
    return this.end;
  }

  public void setEnd(int end) {
    this.end = end;
  }

  @Override
  public CharSequence getField() {
    return this.field;
  }

  @Override
  public void setField(CharSequence field) {
    this.field = field;
  }

  public int getPositionIncrement() {
    return this.positionIncrement;
  }

  public void setPositionIncrement(int pi) {
    this.positionIncrement = pi;
  }

  /**
   * Returns the term.
   * 
   * @return The "original" form of the term.
   */
  @Override
  public CharSequence getText() {
    return this.text;
  }

  /**
   * @param text
   *          the text to set
   */
  @Override
  public void setText(CharSequence text) {
    this.text = text;
  }

  @Override
  public FieldQueryNode cloneTree() throws CloneNotSupportedException {
    FieldQueryNode fqn = (FieldQueryNode) super.cloneTree();
    fqn.begin = this.begin;
    fqn.end = this.end;
    fqn.field = this.field;
    fqn.text = this.text;
    fqn.positionIncrement = this.positionIncrement;
    fqn.toQueryStringIgnoreFields = this.toQueryStringIgnoreFields;

    return fqn;

  }

  @Override
  public CharSequence getValue() {
    return getText();
  }

  @Override
  public void setValue(CharSequence value) {
    setText(value);
  }

}
