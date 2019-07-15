

import java.util.HashMap;
import java.util.Map;
import org.apache.lucene.analysis.ko.dict.ConnectionCosts;


public class GraphvizFormatter {
	private static final String BOS_LABEL = "BOS";

	private static final String EOS_LABEL = "EOS";

	private static final String FONT_NAME = "Helvetica";

	private final ConnectionCosts costs;

	private final Map<String, String> bestPathMap;

	private final StringBuilder sb = new StringBuilder();

	public GraphvizFormatter(ConnectionCosts costs) {
		this.costs = costs;
		this.bestPathMap = new HashMap<>();
		sb.append(formatHeader());
		sb.append("  init [style=invis]\n");
		sb.append((("  init -> 0.0 [label=\"" + (GraphvizFormatter.BOS_LABEL)) + "\"]\n"));
	}

	public String finish() {
		sb.append(formatTrailer());
		return sb.toString();
	}

	private String formatHeader() {
		StringBuilder sb = new StringBuilder();
		sb.append("digraph viterbi {\n");
		sb.append("  graph [ fontsize=30 labelloc=\"t\" label=\"\" splines=true overlap=false rankdir = \"LR\"];\n");
		sb.append((("  edge [ fontname=\"" + (GraphvizFormatter.FONT_NAME)) + "\" fontcolor=\"red\" color=\"#606060\" ]\n"));
		sb.append((("  node [ style=\"filled\" fillcolor=\"#e8e8f0\" shape=\"Mrecord\" fontname=\"" + (GraphvizFormatter.FONT_NAME)) + "\" ]\n"));
		return sb.toString();
	}

	private String formatTrailer() {
		return "}";
	}

	private String getNodeID(int pos, int idx) {
		return (pos + ".") + idx;
	}
}

