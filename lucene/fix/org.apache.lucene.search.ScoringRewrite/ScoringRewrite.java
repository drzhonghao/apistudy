

import java.io.IOException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.index.TermState;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostAttribute;
import org.apache.lucene.search.MultiTermQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.AttributeSource;
import org.apache.lucene.util.ByteBlockPool;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefHash;
import org.apache.lucene.util.RamUsageEstimator;


public abstract class ScoringRewrite<B> {
	public static final ScoringRewrite<BooleanQuery.Builder> SCORING_BOOLEAN_REWRITE = null;

	public static final MultiTermQuery.RewriteMethod CONSTANT_SCORE_BOOLEAN_REWRITE = null;

	protected abstract void checkMaxClauseCount(int count) throws IOException;

	public final Query rewrite(final IndexReader reader, final MultiTermQuery query) throws IOException {
		final ScoringRewrite<B>.ParallelArraysTermCollector col = new ParallelArraysTermCollector();
		final int size = col.terms.size();
		if (size > 0) {
			final int[] sort = col.terms.sort();
			final float[] boost = col.array.boost;
			final TermContext[] termStates = col.array.termState;
			for (int i = 0; i < size; i++) {
				final int pos = sort[i];
				final Term term = new Term(query.getField(), col.terms.get(pos, new BytesRef()));
				assert (reader.docFreq(term)) == (termStates[pos].docFreq());
			}
		}
		return null;
	}

	final class ParallelArraysTermCollector {
		final ScoringRewrite.TermFreqBoostByteStart array = new ScoringRewrite.TermFreqBoostByteStart(16);

		final BytesRefHash terms = new BytesRefHash(new ByteBlockPool(new ByteBlockPool.DirectAllocator()), 16, array);

		TermsEnum termsEnum;

		private BoostAttribute boostAtt;

		public void setNextEnum(TermsEnum termsEnum) {
			this.termsEnum = termsEnum;
			this.boostAtt = termsEnum.attributes().addAttribute(BoostAttribute.class);
		}

		public boolean collect(BytesRef bytes) throws IOException {
			final int e = terms.add(bytes);
			final TermState state = termsEnum.termState();
			assert state != null;
			if (e < 0) {
				final int pos = (-e) - 1;
				assert (array.boost[pos]) == (boostAtt.getBoost()) : "boost should be equal in all segment TermsEnums";
			}else {
				array.boost[e] = boostAtt.getBoost();
				ScoringRewrite.this.checkMaxClauseCount(terms.size());
			}
			return true;
		}
	}

	static final class TermFreqBoostByteStart extends BytesRefHash.DirectBytesStartArray {
		float[] boost;

		TermContext[] termState;

		public TermFreqBoostByteStart(int initSize) {
			super(initSize);
		}

		@Override
		public int[] init() {
			final int[] ord = super.init();
			boost = new float[ArrayUtil.oversize(ord.length, Float.BYTES)];
			termState = new TermContext[ArrayUtil.oversize(ord.length, RamUsageEstimator.NUM_BYTES_OBJECT_REF)];
			assert ((termState.length) >= (ord.length)) && ((boost.length) >= (ord.length));
			return ord;
		}

		@Override
		public int[] grow() {
			final int[] ord = super.grow();
			boost = ArrayUtil.grow(boost, ord.length);
			if ((termState.length) < (ord.length)) {
				TermContext[] tmpTermState = new TermContext[ArrayUtil.oversize(ord.length, RamUsageEstimator.NUM_BYTES_OBJECT_REF)];
				System.arraycopy(termState, 0, tmpTermState, 0, termState.length);
				termState = tmpTermState;
			}
			assert ((termState.length) >= (ord.length)) && ((boost.length) >= (ord.length));
			return ord;
		}

		@Override
		public int[] clear() {
			boost = null;
			termState = null;
			return super.clear();
		}
	}
}

