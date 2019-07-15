

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStreamToAutomaton;
import org.apache.lucene.search.suggest.analyzing.AnalyzingSuggester;
import org.apache.lucene.search.suggest.analyzing.FSTUtil;
import org.apache.lucene.search.suggest.analyzing.FSTUtil.Path;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.IntsRef;
import org.apache.lucene.util.UnicodeUtil;
import org.apache.lucene.util.automaton.Automata;
import org.apache.lucene.util.automaton.Automaton;
import org.apache.lucene.util.automaton.FiniteStringsIterator;
import org.apache.lucene.util.automaton.LevenshteinAutomata;
import org.apache.lucene.util.automaton.Operations;
import org.apache.lucene.util.automaton.UTF32ToUTF8;
import org.apache.lucene.util.fst.FST;
import org.apache.lucene.util.fst.PairOutputs;
import org.apache.lucene.util.fst.PairOutputs.Pair;


public final class FuzzySuggester extends AnalyzingSuggester {
	private final int maxEdits;

	private final boolean transpositions;

	private final int nonFuzzyPrefix;

	private final int minFuzzyLength;

	private final boolean unicodeAware;

	public static final boolean DEFAULT_UNICODE_AWARE = false;

	public static final int DEFAULT_MIN_FUZZY_LENGTH = 3;

	public static final int DEFAULT_NON_FUZZY_PREFIX = 1;

	public static final int DEFAULT_MAX_EDITS = 1;

	public static final boolean DEFAULT_TRANSPOSITIONS = true;

	public FuzzySuggester(Directory tempDir, String tempFileNamePrefix, Analyzer analyzer) {
		this(tempDir, tempFileNamePrefix, analyzer, analyzer);
	}

	public FuzzySuggester(Directory tempDir, String tempFileNamePrefix, Analyzer indexAnalyzer, Analyzer queryAnalyzer) {
		this(tempDir, tempFileNamePrefix, indexAnalyzer, queryAnalyzer, ((AnalyzingSuggester.EXACT_FIRST) | (AnalyzingSuggester.PRESERVE_SEP)), 256, (-1), true, FuzzySuggester.DEFAULT_MAX_EDITS, FuzzySuggester.DEFAULT_TRANSPOSITIONS, FuzzySuggester.DEFAULT_NON_FUZZY_PREFIX, FuzzySuggester.DEFAULT_MIN_FUZZY_LENGTH, FuzzySuggester.DEFAULT_UNICODE_AWARE);
	}

	public FuzzySuggester(Directory tempDir, String tempFileNamePrefix, Analyzer indexAnalyzer, Analyzer queryAnalyzer, int options, int maxSurfaceFormsPerAnalyzedForm, int maxGraphExpansions, boolean preservePositionIncrements, int maxEdits, boolean transpositions, int nonFuzzyPrefix, int minFuzzyLength, boolean unicodeAware) {
		super(tempDir, tempFileNamePrefix, indexAnalyzer, queryAnalyzer, options, maxSurfaceFormsPerAnalyzedForm, maxGraphExpansions, preservePositionIncrements);
		if ((maxEdits < 0) || (maxEdits > (LevenshteinAutomata.MAXIMUM_SUPPORTED_DISTANCE))) {
			throw new IllegalArgumentException(("maxEdits must be between 0 and " + (LevenshteinAutomata.MAXIMUM_SUPPORTED_DISTANCE)));
		}
		if (nonFuzzyPrefix < 0) {
			throw new IllegalArgumentException((("nonFuzzyPrefix must not be >= 0 (got " + nonFuzzyPrefix) + ")"));
		}
		if (minFuzzyLength < 0) {
			throw new IllegalArgumentException((("minFuzzyLength must not be >= 0 (got " + minFuzzyLength) + ")"));
		}
		this.maxEdits = maxEdits;
		this.transpositions = transpositions;
		this.nonFuzzyPrefix = nonFuzzyPrefix;
		this.minFuzzyLength = minFuzzyLength;
		this.unicodeAware = unicodeAware;
	}

	@Override
	protected List<FSTUtil.Path<PairOutputs.Pair<Long, BytesRef>>> getFullPrefixPaths(List<FSTUtil.Path<PairOutputs.Pair<Long, BytesRef>>> prefixPaths, Automaton lookupAutomaton, FST<PairOutputs.Pair<Long, BytesRef>> fst) throws IOException {
		Automaton levA = convertAutomaton(toLevenshteinAutomata(lookupAutomaton));
		return FSTUtil.intersectPrefixPaths(levA, fst);
	}

	@Override
	protected Automaton convertAutomaton(Automaton a) {
		if (unicodeAware) {
			Automaton utf8automaton = new UTF32ToUTF8().convert(a);
			utf8automaton = Operations.determinize(utf8automaton, Operations.DEFAULT_MAX_DETERMINIZED_STATES);
			return utf8automaton;
		}else {
			return a;
		}
	}

	TokenStreamToAutomaton getTokenStreamToAutomaton() {
		return null;
	}

	Automaton toLevenshteinAutomata(Automaton automaton) {
		List<Automaton> subs = new ArrayList<>();
		FiniteStringsIterator finiteStrings = new FiniteStringsIterator(automaton);
		for (IntsRef string; (string = finiteStrings.next()) != null;) {
			if (((string.length) <= (nonFuzzyPrefix)) || ((string.length) < (minFuzzyLength))) {
				subs.add(Automata.makeString(string.ints, string.offset, string.length));
			}else {
				int[] ints = new int[(string.length) - (nonFuzzyPrefix)];
				System.arraycopy(string.ints, ((string.offset) + (nonFuzzyPrefix)), ints, 0, ints.length);
				LevenshteinAutomata lev = new LevenshteinAutomata(ints, (unicodeAware ? Character.MAX_CODE_POINT : 255), transpositions);
				subs.add(lev.toAutomaton(maxEdits, UnicodeUtil.newString(string.ints, string.offset, nonFuzzyPrefix)));
			}
		}
		if (subs.isEmpty()) {
			return Automata.makeEmpty();
		}else
			if ((subs.size()) == 1) {
				return subs.get(0);
			}else {
				Automaton a = Operations.union(subs);
				return Operations.determinize(a, Operations.DEFAULT_MAX_DETERMINIZED_STATES);
			}

	}
}

