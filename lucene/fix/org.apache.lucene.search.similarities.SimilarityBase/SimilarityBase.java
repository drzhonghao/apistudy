

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.lucene.index.FieldInvertState;
import org.apache.lucene.index.LeafMetaData;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.TermStatistics;
import org.apache.lucene.search.similarities.BasicStats;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.SmallFloat;


public abstract class SimilarityBase extends Similarity {
	private static final double LOG_2 = Math.log(2);

	protected boolean discountOverlaps = true;

	public SimilarityBase() {
	}

	public void setDiscountOverlaps(boolean v) {
		discountOverlaps = v;
	}

	public boolean getDiscountOverlaps() {
		return discountOverlaps;
	}

	@Override
	public final Similarity.SimWeight computeWeight(float boost, CollectionStatistics collectionStats, TermStatistics... termStats) {
		BasicStats[] stats = new BasicStats[termStats.length];
		for (int i = 0; i < (termStats.length); i++) {
			stats[i] = newStats(collectionStats.field(), boost);
			fillBasicStats(stats[i], collectionStats, termStats[i]);
		}
		return null;
	}

	protected BasicStats newStats(String field, float boost) {
		return new BasicStats(field, boost);
	}

	protected void fillBasicStats(BasicStats stats, CollectionStatistics collectionStats, TermStatistics termStats) {
		assert ((collectionStats.sumTotalTermFreq()) == (-1)) || ((collectionStats.sumTotalTermFreq()) >= (termStats.totalTermFreq()));
		long numberOfDocuments = ((collectionStats.docCount()) == (-1)) ? collectionStats.maxDoc() : collectionStats.docCount();
		long docFreq = termStats.docFreq();
		long totalTermFreq = termStats.totalTermFreq();
		if (totalTermFreq == (-1)) {
			totalTermFreq = docFreq;
		}
		final long numberOfFieldTokens;
		final float avgFieldLength;
		if ((collectionStats.sumTotalTermFreq()) == (-1)) {
			if ((collectionStats.sumDocFreq()) == (-1)) {
				numberOfFieldTokens = docFreq;
				avgFieldLength = 1.0F;
			}else {
				numberOfFieldTokens = collectionStats.sumDocFreq();
				avgFieldLength = ((float) ((collectionStats.sumDocFreq()) / ((double) (numberOfDocuments))));
			}
		}else {
			numberOfFieldTokens = collectionStats.sumTotalTermFreq();
			avgFieldLength = ((float) ((collectionStats.sumTotalTermFreq()) / ((double) (numberOfDocuments))));
		}
		stats.setNumberOfDocuments(numberOfDocuments);
		stats.setNumberOfFieldTokens(numberOfFieldTokens);
		stats.setAvgFieldLength(avgFieldLength);
		stats.setDocFreq(docFreq);
		stats.setTotalTermFreq(totalTermFreq);
	}

	protected abstract float score(BasicStats stats, float freq, float docLen);

	protected void explain(List<Explanation> subExpls, BasicStats stats, int doc, float freq, float docLen) {
	}

	protected Explanation explain(BasicStats stats, int doc, Explanation freq, float docLen) {
		List<Explanation> subs = new ArrayList<>();
		explain(subs, stats, doc, freq.getValue(), docLen);
		return Explanation.match(score(stats, freq.getValue(), docLen), (((((("score(" + (getClass().getSimpleName())) + ", doc=") + doc) + ", freq=") + (freq.getValue())) + "), computed from:"), subs);
	}

	@Override
	public final Similarity.SimScorer simScorer(Similarity.SimWeight stats, LeafReaderContext context) throws IOException {
		int indexCreatedVersionMajor = context.reader().getMetaData().getCreatedVersionMajor();
		return null;
	}

	@Override
	public abstract String toString();

	private static final float[] OLD_LENGTH_TABLE = new float[256];

	private static final float[] LENGTH_TABLE = new float[256];

	static {
		for (int i = 1; i < 256; i++) {
			float f = SmallFloat.byte315ToFloat(((byte) (i)));
			SimilarityBase.OLD_LENGTH_TABLE[i] = 1.0F / (f * f);
		}
		SimilarityBase.OLD_LENGTH_TABLE[0] = 1.0F / (SimilarityBase.OLD_LENGTH_TABLE[255]);
		for (int i = 0; i < 256; i++) {
			SimilarityBase.LENGTH_TABLE[i] = SmallFloat.byte4ToInt(((byte) (i)));
		}
	}

	@Override
	public final long computeNorm(FieldInvertState state) {
		final int numTerms;
		if (discountOverlaps)
			numTerms = (state.getLength()) - (state.getNumOverlap());
		else
			numTerms = state.getLength();

		int indexCreatedVersionMajor = state.getIndexCreatedVersionMajor();
		if (indexCreatedVersionMajor >= 7) {
			return SmallFloat.intToByte4(numTerms);
		}else {
			return SmallFloat.floatToByte315(((float) (1 / (Math.sqrt(numTerms)))));
		}
	}

	public static double log2(double x) {
		return (Math.log(x)) / (SimilarityBase.LOG_2);
	}

	final class BasicSimScorer extends Similarity.SimScorer {
		private final BasicStats stats;

		private final NumericDocValues norms;

		private final float[] normCache;

		BasicSimScorer(BasicStats stats, int indexCreatedVersionMajor, NumericDocValues norms) throws IOException {
			this.stats = stats;
			this.norms = norms;
			this.normCache = (indexCreatedVersionMajor >= 7) ? SimilarityBase.LENGTH_TABLE : SimilarityBase.OLD_LENGTH_TABLE;
		}

		float getLengthValue(int doc) throws IOException {
			if ((norms) == null) {
				return 1.0F;
			}
			if (norms.advanceExact(doc)) {
				return normCache[Byte.toUnsignedInt(((byte) (norms.longValue())))];
			}else {
				return 0;
			}
		}

		@Override
		public float score(int doc, float freq) throws IOException {
			return SimilarityBase.this.score(stats, freq, getLengthValue(doc));
		}

		@Override
		public Explanation explain(int doc, Explanation freq) throws IOException {
			return SimilarityBase.this.explain(stats, doc, freq, getLengthValue(doc));
		}

		@Override
		public float computeSlopFactor(int distance) {
			return 1.0F / (distance + 1);
		}

		@Override
		public float computePayloadFactor(int doc, int start, int end, BytesRef payload) {
			return 1.0F;
		}
	}
}

