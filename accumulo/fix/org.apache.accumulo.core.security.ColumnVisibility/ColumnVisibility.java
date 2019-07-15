

import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import org.apache.accumulo.core.data.ArrayByteSequence;
import org.apache.accumulo.core.data.ByteSequence;
import org.apache.accumulo.core.security.VisibilityEvaluator;
import org.apache.accumulo.core.util.BadArgumentException;
import org.apache.accumulo.core.util.TextUtil;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparator;


public class ColumnVisibility {
	ColumnVisibility.Node node = null;

	private byte[] expression;

	public byte[] getExpression() {
		return expression;
	}

	public static enum NodeType {

		EMPTY,
		TERM,
		OR,
		AND;}

	private static final ColumnVisibility.Node EMPTY_NODE = new ColumnVisibility.Node(ColumnVisibility.NodeType.EMPTY, 0);

	public static class Node {
		public static final List<ColumnVisibility.Node> EMPTY = Collections.emptyList();

		ColumnVisibility.NodeType type;

		int start;

		int end;

		List<ColumnVisibility.Node> children = ColumnVisibility.Node.EMPTY;

		public Node(ColumnVisibility.NodeType type, int start) {
			this.type = type;
			this.start = start;
			this.end = start + 1;
		}

		public Node(int start, int end) {
			this.type = ColumnVisibility.NodeType.TERM;
			this.start = start;
			this.end = end;
		}

		public void add(ColumnVisibility.Node child) {
			if ((children) == (ColumnVisibility.Node.EMPTY))
				children = new ArrayList<>();

			children.add(child);
		}

		public ColumnVisibility.NodeType getType() {
			return type;
		}

		public List<ColumnVisibility.Node> getChildren() {
			return children;
		}

		public int getTermStart() {
			return start;
		}

		public int getTermEnd() {
			return end;
		}

		public ByteSequence getTerm(byte[] expression) {
			if ((type) != (ColumnVisibility.NodeType.TERM))
				throw new RuntimeException();

			if ((expression[start]) == '"') {
				int qStart = (start) + 1;
				int qEnd = (end) - 1;
				return new ArrayByteSequence(expression, qStart, (qEnd - qStart));
			}
			return new ArrayByteSequence(expression, start, ((end) - (start)));
		}
	}

	public static class NodeComparator implements Serializable , Comparator<ColumnVisibility.Node> {
		private static final long serialVersionUID = 1L;

		byte[] text;

		public NodeComparator(byte[] text) {
			this.text = text;
		}

		@Override
		public int compare(ColumnVisibility.Node a, ColumnVisibility.Node b) {
			int diff = (a.type.ordinal()) - (b.type.ordinal());
			if (diff != 0)
				return diff;

			switch (a.type) {
				case EMPTY :
					return 0;
				case TERM :
					return WritableComparator.compareBytes(text, a.start, ((a.end) - (a.start)), text, b.start, ((b.end) - (b.start)));
				case OR :
				case AND :
					diff = (a.children.size()) - (b.children.size());
					if (diff != 0)
						return diff;

					for (int i = 0; i < (a.children.size()); i++) {
						diff = compare(a.children.get(i), b.children.get(i));
						if (diff != 0)
							return diff;

					}
			}
			return 0;
		}
	}

	public static ColumnVisibility.Node normalize(ColumnVisibility.Node root, byte[] expression) {
		return ColumnVisibility.normalize(root, expression, new ColumnVisibility.NodeComparator(expression));
	}

	public static ColumnVisibility.Node normalize(ColumnVisibility.Node root, byte[] expression, ColumnVisibility.NodeComparator comparator) {
		if ((root.type) != (ColumnVisibility.NodeType.TERM)) {
			TreeSet<ColumnVisibility.Node> rolledUp = new TreeSet<>(comparator);
			Iterator<ColumnVisibility.Node> itr = root.children.iterator();
			while (itr.hasNext()) {
				ColumnVisibility.Node c = ColumnVisibility.normalize(itr.next(), expression, comparator);
				if ((c.type) == (root.type)) {
					rolledUp.addAll(c.children);
					itr.remove();
				}
			} 
			rolledUp.addAll(root.children);
			root.children.clear();
			root.children.addAll(rolledUp);
			if ((root.children.size()) == 1) {
				return root.children.get(0);
			}
		}
		return root;
	}

	public static void stringify(ColumnVisibility.Node root, byte[] expression, StringBuilder out) {
		if ((root.type) == (ColumnVisibility.NodeType.TERM)) {
			out.append(new String(expression, root.start, ((root.end) - (root.start)), StandardCharsets.UTF_8));
		}else {
			String sep = "";
			for (ColumnVisibility.Node c : root.children) {
				out.append(sep);
				boolean parens = ((c.type) != (ColumnVisibility.NodeType.TERM)) && ((root.type) != (c.type));
				if (parens)
					out.append("(");

				ColumnVisibility.stringify(c, expression, out);
				if (parens)
					out.append(")");

				sep = ((root.type) == (ColumnVisibility.NodeType.AND)) ? "&" : "|";
			}
		}
	}

	public byte[] flatten() {
		ColumnVisibility.Node normRoot = ColumnVisibility.normalize(node, expression);
		StringBuilder builder = new StringBuilder(expression.length);
		ColumnVisibility.stringify(normRoot, expression, builder);
		return builder.toString().getBytes(StandardCharsets.UTF_8);
	}

	private static class ColumnVisibilityParser {
		private int index = 0;

		private int parens = 0;

		public ColumnVisibilityParser() {
		}

		ColumnVisibility.Node parse(byte[] expression) {
			if ((expression.length) > 0) {
				ColumnVisibility.Node node = parse_(expression);
				if (node == null) {
					throw new BadArgumentException("operator or missing parens", new String(expression, StandardCharsets.UTF_8), ((index) - 1));
				}
				if ((parens) != 0) {
					throw new BadArgumentException("parenthesis mis-match", new String(expression, StandardCharsets.UTF_8), ((index) - 1));
				}
				return node;
			}
			return null;
		}

		ColumnVisibility.Node processTerm(int start, int end, ColumnVisibility.Node expr, byte[] expression) {
			if (start != end) {
				if (expr != null)
					throw new BadArgumentException("expression needs | or &", new String(expression, StandardCharsets.UTF_8), start);

				return new ColumnVisibility.Node(start, end);
			}
			if (expr == null)
				throw new BadArgumentException("empty term", new String(expression, StandardCharsets.UTF_8), start);

			return expr;
		}

		ColumnVisibility.Node parse_(byte[] expression) {
			ColumnVisibility.Node result = null;
			ColumnVisibility.Node expr = null;
			int wholeTermStart = index;
			int subtermStart = index;
			boolean subtermComplete = false;
			while ((index) < (expression.length)) {
				switch (expression[((index)++)]) {
					case '&' :
						{
							expr = processTerm(subtermStart, ((index) - 1), expr, expression);
							if (result != null) {
								if (!(result.type.equals(ColumnVisibility.NodeType.AND)))
									throw new BadArgumentException("cannot mix & and |", new String(expression, StandardCharsets.UTF_8), ((index) - 1));

							}else {
								result = new ColumnVisibility.Node(ColumnVisibility.NodeType.AND, wholeTermStart);
							}
							result.add(expr);
							expr = null;
							subtermStart = index;
							subtermComplete = false;
							break;
						}
					case '|' :
						{
							expr = processTerm(subtermStart, ((index) - 1), expr, expression);
							if (result != null) {
								if (!(result.type.equals(ColumnVisibility.NodeType.OR)))
									throw new BadArgumentException("cannot mix | and &", new String(expression, StandardCharsets.UTF_8), ((index) - 1));

							}else {
								result = new ColumnVisibility.Node(ColumnVisibility.NodeType.OR, wholeTermStart);
							}
							result.add(expr);
							expr = null;
							subtermStart = index;
							subtermComplete = false;
							break;
						}
					case '(' :
						{
							(parens)++;
							if ((subtermStart != ((index) - 1)) || (expr != null))
								throw new BadArgumentException("expression needs & or |", new String(expression, StandardCharsets.UTF_8), ((index) - 1));

							expr = parse_(expression);
							subtermStart = index;
							subtermComplete = false;
							break;
						}
					case ')' :
						{
							(parens)--;
							ColumnVisibility.Node child = processTerm(subtermStart, ((index) - 1), expr, expression);
							if ((child == null) && (result == null))
								throw new BadArgumentException("empty expression not allowed", new String(expression, StandardCharsets.UTF_8), index);

							if (result == null)
								return child;

							if ((result.type) == (child.type))
								for (ColumnVisibility.Node c : child.children)
									result.add(c);

							else
								result.add(child);

							result.end = (index) - 1;
							return result;
						}
					case '"' :
						{
							if (subtermStart != ((index) - 1))
								throw new BadArgumentException("expression needs & or |", new String(expression, StandardCharsets.UTF_8), ((index) - 1));

							while (((index) < (expression.length)) && ((expression[index]) != '"')) {
								if ((expression[index]) == '\\') {
									(index)++;
									if (((expression[index]) != '\\') && ((expression[index]) != '"'))
										throw new BadArgumentException("invalid escaping within quotes", new String(expression, StandardCharsets.UTF_8), ((index) - 1));

								}
								(index)++;
							} 
							if ((index) == (expression.length))
								throw new BadArgumentException("unclosed quote", new String(expression, StandardCharsets.UTF_8), subtermStart);

							if ((subtermStart + 1) == (index))
								throw new BadArgumentException("empty term", new String(expression, StandardCharsets.UTF_8), subtermStart);

							(index)++;
							subtermComplete = true;
							break;
						}
					default :
						{
							if (subtermComplete)
								throw new BadArgumentException("expression needs & or |", new String(expression, StandardCharsets.UTF_8), ((index) - 1));

							byte c = expression[((index) - 1)];
						}
				}
			} 
			ColumnVisibility.Node child = processTerm(subtermStart, index, expr, expression);
			if (result != null) {
				result.add(child);
				result.end = index;
			}else
				result = child;

			if ((result.type) != (ColumnVisibility.NodeType.TERM))
				if ((result.children.size()) < 2)
					throw new BadArgumentException("missing term", new String(expression, StandardCharsets.UTF_8), index);


			return result;
		}
	}

	private void validate(byte[] expression) {
		if ((expression != null) && ((expression.length) > 0)) {
			ColumnVisibility.ColumnVisibilityParser p = new ColumnVisibility.ColumnVisibilityParser();
			node = p.parse(expression);
		}else {
			node = ColumnVisibility.EMPTY_NODE;
		}
		this.expression = expression;
	}

	public ColumnVisibility() {
		this(new byte[]{  });
	}

	public ColumnVisibility(String expression) {
		this(expression.getBytes(StandardCharsets.UTF_8));
	}

	public ColumnVisibility(Text expression) {
		this(TextUtil.getBytes(expression));
	}

	public ColumnVisibility(byte[] expression) {
		validate(expression);
	}

	@Override
	public String toString() {
		return ("[" + (new String(expression, StandardCharsets.UTF_8))) + "]";
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ColumnVisibility)
			return equals(((ColumnVisibility) (obj)));

		return false;
	}

	public boolean equals(ColumnVisibility otherLe) {
		return Arrays.equals(expression, otherLe.expression);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(expression);
	}

	public ColumnVisibility.Node getParseTree() {
		return node;
	}

	public static String quote(String term) {
		return new String(ColumnVisibility.quote(term.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
	}

	public static byte[] quote(byte[] term) {
		boolean needsQuote = false;
		for (int i = 0; i < (term.length); i++) {
		}
		if (!needsQuote)
			return term;

		return VisibilityEvaluator.escape(term, true);
	}
}

