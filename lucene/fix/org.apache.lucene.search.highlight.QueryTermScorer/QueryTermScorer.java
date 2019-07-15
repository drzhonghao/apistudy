

import java.util.HashMap;
import java.util.HashSet;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.highlight.QueryTermExtractor;
import org.apache.lucene.search.highlight.Scorer;
import org.apache.lucene.search.highlight.TextFragment;
import org.apache.lucene.search.highlight.WeightedTerm;
import org.apache.lucene.util.AttributeSource;


public class QueryTermScorer implements Scorer {
	TextFragment currentTextFragment = null;

	HashSet<String> uniqueTermsInFragment;

	float totalScore = 0;

	float maxTermWeight = 0;

	private HashMap<String, WeightedTerm> termsToFind;

	private CharTermAttribute termAtt;

	public QueryTermScorer(Query query) {
		this(QueryTermExtractor.getTerms(query));
	}

	public QueryTermScorer(Query query, String fieldName) {
		this(QueryTermExtractor.getTerms(query, false, fieldName));
	}

	public QueryTermScorer(Query query, IndexReader reader, String fieldName) {
		this(QueryTermExtractor.getIdfWeightedTerms(query, reader, fieldName));
	}

	public QueryTermScorer(WeightedTerm[] weightedTerms) {
		termsToFind = new HashMap<>();
		for (int i = 0; i < (weightedTerms.length); i++) {
		}
	}

	@Override
	public TokenStream init(TokenStream tokenStream) {
		termAtt = tokenStream.addAttribute(CharTermAttribute.class);
		return null;
	}

	@Override
	public void startFragment(TextFragment newFragment) {
		uniqueTermsInFragment = new HashSet<>();
		currentTextFragment = newFragment;
		totalScore = 0;
	}

	@Override
	public float getTokenScore() {
		String termText = termAtt.toString();
		WeightedTerm queryTerm = termsToFind.get(termText);
		if (queryTerm == null) {
			return 0;
		}
		if (!(uniqueTermsInFragment.contains(termText))) {
			totalScore += queryTerm.getWeight();
			uniqueTermsInFragment.add(termText);
		}
		return queryTerm.getWeight();
	}

	@Override
	public float getFragmentScore() {
		return totalScore;
	}

	public void allFragmentsProcessed() {
	}

	public float getMaxTermWeight() {
		return maxTermWeight;
	}
}

