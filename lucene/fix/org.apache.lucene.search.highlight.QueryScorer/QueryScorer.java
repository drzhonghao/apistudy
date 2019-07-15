

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.highlight.Scorer;
import org.apache.lucene.search.highlight.TextFragment;
import org.apache.lucene.search.highlight.WeightedSpanTerm;
import org.apache.lucene.search.highlight.WeightedSpanTermExtractor;
import org.apache.lucene.search.highlight.WeightedTerm;
import org.apache.lucene.util.AttributeSource;


public class QueryScorer implements Scorer {
	private float totalScore;

	private Set<String> foundTerms;

	private Map<String, WeightedSpanTerm> fieldWeightedSpanTerms;

	private float maxTermWeight;

	private int position = -1;

	private String defaultField;

	private CharTermAttribute termAtt;

	private PositionIncrementAttribute posIncAtt;

	private boolean expandMultiTermQuery = true;

	private Query query;

	private String field;

	private IndexReader reader;

	private boolean skipInitExtractor;

	private boolean wrapToCaching = true;

	private int maxCharsToAnalyze;

	private boolean usePayloads = false;

	public QueryScorer(Query query) {
		init(query, null, null, true);
	}

	public QueryScorer(Query query, String field) {
		init(query, field, null, true);
	}

	public QueryScorer(Query query, IndexReader reader, String field) {
		init(query, field, reader, true);
	}

	public QueryScorer(Query query, IndexReader reader, String field, String defaultField) {
		this.defaultField = defaultField;
		init(query, field, reader, true);
	}

	public QueryScorer(Query query, String field, String defaultField) {
		this.defaultField = defaultField;
		init(query, field, null, true);
	}

	public QueryScorer(WeightedSpanTerm[] weightedTerms) {
		this.fieldWeightedSpanTerms = new HashMap<>(weightedTerms.length);
		for (int i = 0; i < (weightedTerms.length); i++) {
		}
		skipInitExtractor = true;
	}

	@Override
	public float getFragmentScore() {
		return totalScore;
	}

	public float getMaxTermWeight() {
		return maxTermWeight;
	}

	@Override
	public float getTokenScore() {
		position += posIncAtt.getPositionIncrement();
		String termText = termAtt.toString();
		WeightedSpanTerm weightedSpanTerm;
		if ((weightedSpanTerm = fieldWeightedSpanTerms.get(termText)) == null) {
			return 0;
		}
		float score = weightedSpanTerm.getWeight();
		if (!(foundTerms.contains(termText))) {
			totalScore += score;
			foundTerms.add(termText);
		}
		return score;
	}

	@Override
	public TokenStream init(TokenStream tokenStream) throws IOException {
		position = -1;
		termAtt = tokenStream.addAttribute(CharTermAttribute.class);
		posIncAtt = tokenStream.addAttribute(PositionIncrementAttribute.class);
		if (!(skipInitExtractor)) {
			if ((fieldWeightedSpanTerms) != null) {
				fieldWeightedSpanTerms.clear();
			}
			return initExtractor(tokenStream);
		}
		return null;
	}

	public WeightedSpanTerm getWeightedSpanTerm(String token) {
		return fieldWeightedSpanTerms.get(token);
	}

	private void init(Query query, String field, IndexReader reader, boolean expandMultiTermQuery) {
		this.reader = reader;
		this.expandMultiTermQuery = expandMultiTermQuery;
		this.query = query;
		this.field = field;
	}

	private TokenStream initExtractor(TokenStream tokenStream) throws IOException {
		WeightedSpanTermExtractor qse = newTermExtractor(defaultField);
		qse.setExpandMultiTermQuery(expandMultiTermQuery);
		qse.setWrapIfNotCachingTokenFilter(wrapToCaching);
		qse.setUsePayloads(usePayloads);
		if ((reader) == null) {
			this.fieldWeightedSpanTerms = qse.getWeightedSpanTerms(query, 1.0F, tokenStream, field);
		}else {
			this.fieldWeightedSpanTerms = qse.getWeightedSpanTermsWithScores(query, 1.0F, tokenStream, field, reader);
		}
		if (qse.isCachedTokenStream()) {
			return qse.getTokenStream();
		}
		return null;
	}

	protected WeightedSpanTermExtractor newTermExtractor(String defaultField) {
		return defaultField == null ? new WeightedSpanTermExtractor() : new WeightedSpanTermExtractor(defaultField);
	}

	@Override
	public void startFragment(TextFragment newFragment) {
		foundTerms = new HashSet<>();
		totalScore = 0;
	}

	public boolean isExpandMultiTermQuery() {
		return expandMultiTermQuery;
	}

	public void setExpandMultiTermQuery(boolean expandMultiTermQuery) {
		this.expandMultiTermQuery = expandMultiTermQuery;
	}

	public boolean isUsePayloads() {
		return usePayloads;
	}

	public void setUsePayloads(boolean usePayloads) {
		this.usePayloads = usePayloads;
	}

	public void setWrapIfNotCachingTokenFilter(boolean wrap) {
		this.wrapToCaching = wrap;
	}

	public void setMaxDocCharsToAnalyze(int maxDocCharsToAnalyze) {
		this.maxCharsToAnalyze = maxDocCharsToAnalyze;
	}
}

