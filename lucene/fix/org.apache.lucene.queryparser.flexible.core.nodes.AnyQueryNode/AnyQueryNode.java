

import java.util.List;
import org.apache.lucene.queryparser.flexible.core.nodes.AndQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.BooleanQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.FieldQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.FieldableNode;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNodeImpl;
import org.apache.lucene.queryparser.flexible.core.parser.EscapeQuerySyntax;


public class AnyQueryNode extends AndQueryNode {
	private CharSequence field = null;

	private int minimumMatchingmElements = 0;

	public AnyQueryNode(List<QueryNode> clauses, CharSequence field, int minimumMatchingElements) {
		super(clauses);
		this.field = field;
		this.minimumMatchingmElements = minimumMatchingElements;
		if (clauses != null) {
			for (QueryNode clause : clauses) {
				if (clause instanceof FieldQueryNode) {
					if (clause instanceof QueryNodeImpl) {
					}
					if (clause instanceof FieldableNode) {
						((FieldableNode) (clause)).setField(field);
					}
				}
			}
		}
	}

	public int getMinimumMatchingElements() {
		return this.minimumMatchingmElements;
	}

	public CharSequence getField() {
		return this.field;
	}

	public String getFieldAsString() {
		if ((this.field) == null)
			return null;
		else
			return this.field.toString();

	}

	public void setField(CharSequence field) {
		this.field = field;
	}

	@Override
	public QueryNode cloneTree() throws CloneNotSupportedException {
		AnyQueryNode clone = ((AnyQueryNode) (super.cloneTree()));
		clone.field = this.field;
		clone.minimumMatchingmElements = this.minimumMatchingmElements;
		return clone;
	}

	@Override
	public String toString() {
		if (((getChildren()) == null) || ((getChildren().size()) == 0))
			return ((("<any field='" + (this.field)) + "'  matchelements=") + (this.minimumMatchingmElements)) + "/>";

		StringBuilder sb = new StringBuilder();
		sb.append((((("<any field='" + (this.field)) + "'  matchelements=") + (this.minimumMatchingmElements)) + ">"));
		for (QueryNode clause : getChildren()) {
			sb.append("\n");
			sb.append(clause.toString());
		}
		sb.append("\n</any>");
		return sb.toString();
	}

	@Override
	public CharSequence toQueryString(EscapeQuerySyntax escapeSyntaxParser) {
		String anySTR = "ANY " + (this.minimumMatchingmElements);
		StringBuilder sb = new StringBuilder();
		if (((getChildren()) == null) || ((getChildren().size()) == 0)) {
		}else {
			String filler = "";
			for (QueryNode clause : getChildren()) {
				sb.append(filler).append(clause.toQueryString(escapeSyntaxParser));
				filler = " ";
			}
		}
		if (isDefaultField(this.field)) {
			return (("( " + (sb.toString())) + " ) ") + anySTR;
		}else {
			return (((((this.field) + ":(( ") + (sb.toString())) + " ) ") + anySTR) + ")";
		}
	}
}

