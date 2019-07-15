

import java.io.IOException;
import java.util.AbstractCollection;
import java.util.HashSet;
import java.util.Iterator;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Weight;
import org.apache.lucene.search.highlight.WeightedTerm;

import static org.apache.lucene.search.BooleanClause.Occur.MUST_NOT;


public final class QueryTermExtractor {
	private static final IndexSearcher EMPTY_INDEXSEARCHER;

	static {
		try {
			IndexReader emptyReader = new MultiReader();
			EMPTY_INDEXSEARCHER = new IndexSearcher(emptyReader);
			QueryTermExtractor.EMPTY_INDEXSEARCHER.setQueryCache(null);
		} catch (IOException bogus) {
			throw new RuntimeException(bogus);
		}
	}

	public static final WeightedTerm[] getTerms(Query query) {
		return QueryTermExtractor.getTerms(query, false);
	}

	public static final WeightedTerm[] getIdfWeightedTerms(Query query, IndexReader reader, String fieldName) {
		WeightedTerm[] terms = QueryTermExtractor.getTerms(query, false, fieldName);
		int totalNumDocs = reader.maxDoc();
		for (int i = 0; i < (terms.length); i++) {
		}
		return terms;
	}

	public static final WeightedTerm[] getTerms(Query query, boolean prohibited, String fieldName) {
		HashSet<WeightedTerm> terms = new HashSet<>();
		QueryTermExtractor.getTerms(query, 1.0F, terms, prohibited, fieldName);
		return terms.toArray(new WeightedTerm[0]);
	}

	public static final WeightedTerm[] getTerms(Query query, boolean prohibited) {
		return QueryTermExtractor.getTerms(query, prohibited, null);
	}

	private static final void getTerms(Query query, float boost, HashSet<WeightedTerm> terms, boolean prohibited, String fieldName) {
		try {
			if (query instanceof BoostQuery) {
				BoostQuery boostQuery = ((BoostQuery) (query));
				QueryTermExtractor.getTerms(boostQuery.getQuery(), (boost * (boostQuery.getBoost())), terms, prohibited, fieldName);
			}else
				if (query instanceof BooleanQuery)
					QueryTermExtractor.getTermsFromBooleanQuery(((BooleanQuery) (query)), boost, terms, prohibited, fieldName);
				else {
					HashSet<Term> nonWeightedTerms = new HashSet<>();
					try {
						QueryTermExtractor.EMPTY_INDEXSEARCHER.createWeight(QueryTermExtractor.EMPTY_INDEXSEARCHER.rewrite(query), false, 1).extractTerms(nonWeightedTerms);
					} catch (IOException bogus) {
						throw new RuntimeException("Should not happen on an empty index", bogus);
					}
					for (Iterator<Term> iter = nonWeightedTerms.iterator(); iter.hasNext();) {
						Term term = iter.next();
						if ((fieldName == null) || (term.field().equals(fieldName))) {
							terms.add(new WeightedTerm(boost, term.text()));
						}
					}
				}

		} catch (UnsupportedOperationException ignore) {
		}
	}

	private static final void getTermsFromBooleanQuery(BooleanQuery query, float boost, HashSet<WeightedTerm> terms, boolean prohibited, String fieldName) {
		for (BooleanClause clause : query) {
			if (prohibited || ((clause.getOccur()) != (MUST_NOT)))
				QueryTermExtractor.getTerms(clause.getQuery(), boost, terms, prohibited, fieldName);

		}
	}
}

