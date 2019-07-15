

import java.util.List;
import org.apache.lucene.queryparser.flexible.core.QueryNodeError;
import org.apache.lucene.queryparser.flexible.core.messages.QueryParserMessages;
import org.apache.lucene.queryparser.flexible.core.nodes.BooleanQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.FieldQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNodeImpl;
import org.apache.lucene.queryparser.flexible.core.parser.EscapeQuerySyntax;
import org.apache.lucene.queryparser.flexible.messages.MessageImpl;


public class ProximityQueryNode extends BooleanQueryNode {
	public enum Type {

		PARAGRAPH() {
			@Override
			CharSequence toQueryString() {
				return "WITHIN PARAGRAPH";
			}
		},
		SENTENCE() {
			@Override
			CharSequence toQueryString() {
				return "WITHIN SENTENCE";
			}
		},
		NUMBER() {
			@Override
			CharSequence toQueryString() {
				return "WITHIN";
			}
		};
		abstract CharSequence toQueryString();
	}

	public static class ProximityType {
		int pDistance = 0;

		ProximityQueryNode.Type pType = null;

		public ProximityType(ProximityQueryNode.Type type) {
			this(type, 0);
		}

		public ProximityType(ProximityQueryNode.Type type, int distance) {
			this.pType = type;
			this.pDistance = distance;
		}
	}

	private ProximityQueryNode.Type proximityType = ProximityQueryNode.Type.SENTENCE;

	private int distance = -1;

	private boolean inorder = false;

	private CharSequence field = null;

	public ProximityQueryNode(List<QueryNode> clauses, CharSequence field, ProximityQueryNode.Type type, int distance, boolean inorder) {
		super(clauses);
		setLeaf(false);
		this.proximityType = type;
		this.inorder = inorder;
		this.field = field;
		if (type == (ProximityQueryNode.Type.NUMBER)) {
			if (distance <= 0) {
				throw new QueryNodeError(new MessageImpl(QueryParserMessages.PARAMETER_VALUE_NOT_SUPPORTED, "distance", distance));
			}else {
				this.distance = distance;
			}
		}
		ProximityQueryNode.clearFields(clauses, field);
	}

	public ProximityQueryNode(List<QueryNode> clauses, CharSequence field, ProximityQueryNode.Type type, boolean inorder) {
		this(clauses, field, type, (-1), inorder);
	}

	private static void clearFields(List<QueryNode> nodes, CharSequence field) {
		if ((nodes == null) || ((nodes.size()) == 0))
			return;

		for (QueryNode clause : nodes) {
			if (clause instanceof FieldQueryNode) {
				((FieldQueryNode) (clause)).setField(field);
			}
		}
	}

	public ProximityQueryNode.Type getProximityType() {
		return this.proximityType;
	}

	@Override
	public String toString() {
		String distanceSTR = ((this.distance) == (-1)) ? "" : (" distance='" + (this.distance)) + "'";
		if (((getChildren()) == null) || ((getChildren().size()) == 0))
			return ((((((("<proximity field='" + (this.field)) + "' inorder='") + (this.inorder)) + "' type='") + (this.proximityType.toString())) + "'") + distanceSTR) + "/>";

		StringBuilder sb = new StringBuilder();
		sb.append((((((((("<proximity field='" + (this.field)) + "' inorder='") + (this.inorder)) + "' type='") + (this.proximityType.toString())) + "'") + distanceSTR) + ">"));
		for (QueryNode child : getChildren()) {
			sb.append("\n");
			sb.append(child.toString());
		}
		sb.append("\n</proximity>");
		return sb.toString();
	}

	@Override
	public CharSequence toQueryString(EscapeQuerySyntax escapeSyntaxParser) {
		String withinSTR = ((this.proximityType.toQueryString()) + ((this.distance) == (-1) ? "" : " " + (this.distance))) + (this.inorder ? " INORDER" : "");
		StringBuilder sb = new StringBuilder();
		if (((getChildren()) == null) || ((getChildren().size()) == 0)) {
		}else {
			String filler = "";
			for (QueryNode child : getChildren()) {
				sb.append(filler).append(child.toQueryString(escapeSyntaxParser));
				filler = " ";
			}
		}
		if (isDefaultField(this.field)) {
			return (("( " + (sb.toString())) + " ) ") + withinSTR;
		}else {
			return (((((this.field) + ":(( ") + (sb.toString())) + " ) ") + withinSTR) + ")";
		}
	}

	@Override
	public QueryNode cloneTree() throws CloneNotSupportedException {
		ProximityQueryNode clone = ((ProximityQueryNode) (super.cloneTree()));
		clone.proximityType = this.proximityType;
		clone.distance = this.distance;
		clone.field = this.field;
		return clone;
	}

	public int getDistance() {
		return this.distance;
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

	public boolean isInOrder() {
		return this.inorder;
	}
}

