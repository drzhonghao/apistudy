

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.AnalyzerWrapper;
import org.apache.lucene.analysis.TokenStreamToAutomaton;
import org.apache.lucene.analysis.miscellaneous.ConcatenateGraphFilter;


public final class CompletionAnalyzer extends AnalyzerWrapper {
	static final int HOLE_CHARACTER = TokenStreamToAutomaton.HOLE;

	private final Analyzer analyzer;

	private final boolean preserveSep;

	private final boolean preservePositionIncrements;

	private final int maxGraphExpansions;

	public CompletionAnalyzer(Analyzer analyzer, boolean preserveSep, boolean preservePositionIncrements, int maxGraphExpansions) {
		super(Analyzer.PER_FIELD_REUSE_STRATEGY);
		this.analyzer = analyzer;
		this.preserveSep = preserveSep;
		this.preservePositionIncrements = preservePositionIncrements;
		this.maxGraphExpansions = maxGraphExpansions;
	}

	public CompletionAnalyzer(Analyzer analyzer) {
		this(analyzer, ConcatenateGraphFilter.DEFAULT_PRESERVE_SEP, ConcatenateGraphFilter.DEFAULT_PRESERVE_POSITION_INCREMENTS, ConcatenateGraphFilter.DEFAULT_MAX_GRAPH_EXPANSIONS);
	}

	public CompletionAnalyzer(Analyzer analyzer, boolean preserveSep, boolean preservePositionIncrements) {
		this(analyzer, preserveSep, preservePositionIncrements, ConcatenateGraphFilter.DEFAULT_MAX_GRAPH_EXPANSIONS);
	}

	public CompletionAnalyzer(Analyzer analyzer, int maxGraphExpansions) {
		this(analyzer, ConcatenateGraphFilter.DEFAULT_PRESERVE_SEP, ConcatenateGraphFilter.DEFAULT_PRESERVE_POSITION_INCREMENTS, maxGraphExpansions);
	}

	public boolean preserveSep() {
		return preserveSep;
	}

	public boolean preservePositionIncrements() {
		return preservePositionIncrements;
	}

	@Override
	protected Analyzer getWrappedAnalyzer(String fieldName) {
		return analyzer;
	}

	@Override
	protected Analyzer.TokenStreamComponents wrapComponents(String fieldName, Analyzer.TokenStreamComponents components) {
		return null;
	}
}

