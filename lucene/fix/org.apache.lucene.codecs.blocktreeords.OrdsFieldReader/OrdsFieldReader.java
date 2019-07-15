

import java.io.IOException;
import java.util.Collection;
import org.apache.lucene.codecs.blocktreeords.OrdsBlockTreeTermsReader;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.Accountable;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.automaton.CompiledAutomaton;

import static org.apache.lucene.util.automaton.CompiledAutomaton.AUTOMATON_TYPE.NORMAL;


final class OrdsFieldReader extends Terms implements Accountable {
	final long numTerms = 0L;

	final FieldInfo fieldInfo = null;

	final long sumTotalTermFreq = 0L;

	final long sumDocFreq = 0L;

	final int docCount = 0;

	final long indexStartFP = 0L;

	final long rootBlockFP = 0L;

	final BytesRef minTerm = null;

	final BytesRef maxTerm = null;

	final int longsSize = 0;

	final OrdsBlockTreeTermsReader parent = null;

	@Override
	public BytesRef getMin() throws IOException {
		if ((minTerm) == null) {
			return super.getMin();
		}else {
			return minTerm;
		}
	}

	@Override
	public BytesRef getMax() throws IOException {
		if ((maxTerm) == null) {
			return super.getMax();
		}else {
			return maxTerm;
		}
	}

	@Override
	public boolean hasFreqs() {
		return (fieldInfo.getIndexOptions().compareTo(IndexOptions.DOCS_AND_FREQS)) >= 0;
	}

	@Override
	public boolean hasOffsets() {
		return (fieldInfo.getIndexOptions().compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS)) >= 0;
	}

	@Override
	public boolean hasPositions() {
		return (fieldInfo.getIndexOptions().compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS)) >= 0;
	}

	@Override
	public boolean hasPayloads() {
		return fieldInfo.hasPayloads();
	}

	@Override
	public TermsEnum iterator() throws IOException {
		return null;
	}

	@Override
	public long size() {
		return numTerms;
	}

	@Override
	public long getSumTotalTermFreq() {
		return sumTotalTermFreq;
	}

	@Override
	public long getSumDocFreq() {
		return sumDocFreq;
	}

	@Override
	public int getDocCount() {
		return docCount;
	}

	@Override
	public TermsEnum intersect(CompiledAutomaton compiled, BytesRef startTerm) throws IOException {
		if ((compiled.type) != (NORMAL)) {
			throw new IllegalArgumentException("please use CompiledAutomaton.getTermsEnum instead");
		}
		return null;
	}

	@Override
	public long ramBytesUsed() {
		return 0l;
	}

	@Override
	public Collection<Accountable> getChildResources() {
		return null;
	}

	@Override
	public String toString() {
		return ((((((("OrdsBlockTreeTerms(terms=" + (numTerms)) + ",postings=") + (sumDocFreq)) + ",positions=") + (sumTotalTermFreq)) + ",docs=") + (docCount)) + ")";
	}
}

