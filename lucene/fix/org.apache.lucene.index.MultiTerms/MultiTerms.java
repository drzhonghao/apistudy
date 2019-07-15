

import java.io.IOException;
import org.apache.lucene.index.ReaderSlice;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.automaton.CompiledAutomaton;


public final class MultiTerms extends Terms {
	private final Terms[] subs;

	private final ReaderSlice[] subSlices;

	private final boolean hasFreqs;

	private final boolean hasOffsets;

	private final boolean hasPositions;

	private final boolean hasPayloads;

	public MultiTerms(Terms[] subs, ReaderSlice[] subSlices) throws IOException {
		this.subs = subs;
		this.subSlices = subSlices;
		assert (subs.length) > 0 : "inefficient: don't use MultiTerms over one sub";
		boolean _hasFreqs = true;
		boolean _hasOffsets = true;
		boolean _hasPositions = true;
		boolean _hasPayloads = false;
		for (int i = 0; i < (subs.length); i++) {
			_hasFreqs &= subs[i].hasFreqs();
			_hasOffsets &= subs[i].hasOffsets();
			_hasPositions &= subs[i].hasPositions();
			_hasPayloads |= subs[i].hasPayloads();
		}
		hasFreqs = _hasFreqs;
		hasOffsets = _hasOffsets;
		hasPositions = _hasPositions;
		hasPayloads = (hasPositions) && _hasPayloads;
	}

	public Terms[] getSubTerms() {
		return subs;
	}

	public ReaderSlice[] getSubSlices() {
		return subSlices;
	}

	@Override
	public TermsEnum intersect(CompiledAutomaton compiled, BytesRef startTerm) throws IOException {
		for (int i = 0; i < (subs.length); i++) {
			final TermsEnum termsEnum = subs[i].intersect(compiled, startTerm);
			if (termsEnum != null) {
			}
		}
		return null;
	}

	@Override
	public BytesRef getMin() throws IOException {
		BytesRef minTerm = null;
		for (Terms terms : subs) {
			BytesRef term = terms.getMin();
			if ((minTerm == null) || ((term.compareTo(minTerm)) < 0)) {
				minTerm = term;
			}
		}
		return minTerm;
	}

	@Override
	public BytesRef getMax() throws IOException {
		BytesRef maxTerm = null;
		for (Terms terms : subs) {
			BytesRef term = terms.getMax();
			if ((maxTerm == null) || ((term.compareTo(maxTerm)) > 0)) {
				maxTerm = term;
			}
		}
		return maxTerm;
	}

	@Override
	public TermsEnum iterator() throws IOException {
		for (int i = 0; i < (subs.length); i++) {
			final TermsEnum termsEnum = subs[i].iterator();
			if (termsEnum != null) {
			}
		}
		return null;
	}

	@Override
	public long size() {
		return -1;
	}

	@Override
	public long getSumTotalTermFreq() throws IOException {
		long sum = 0;
		for (Terms terms : subs) {
			final long v = terms.getSumTotalTermFreq();
			if (v == (-1)) {
				return -1;
			}
			sum += v;
		}
		return sum;
	}

	@Override
	public long getSumDocFreq() throws IOException {
		long sum = 0;
		for (Terms terms : subs) {
			final long v = terms.getSumDocFreq();
			if (v == (-1)) {
				return -1;
			}
			sum += v;
		}
		return sum;
	}

	@Override
	public int getDocCount() throws IOException {
		int sum = 0;
		for (Terms terms : subs) {
			final int v = terms.getDocCount();
			if (v == (-1)) {
				return -1;
			}
			sum += v;
		}
		return sum;
	}

	@Override
	public boolean hasFreqs() {
		return hasFreqs;
	}

	@Override
	public boolean hasOffsets() {
		return hasOffsets;
	}

	@Override
	public boolean hasPositions() {
		return hasPositions;
	}

	@Override
	public boolean hasPayloads() {
		return hasPayloads;
	}
}

