

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.apache.lucene.search.suggest.document.CompletionScorer;
import org.apache.lucene.search.suggest.document.TopSuggestDocsCollector;
import org.apache.lucene.store.ByteArrayDataOutput;
import org.apache.lucene.store.DataInput;
import org.apache.lucene.store.DataOutput;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.util.Accountable;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.CharsRefBuilder;
import org.apache.lucene.util.IntsRef;
import org.apache.lucene.util.IntsRefBuilder;
import org.apache.lucene.util.fst.ByteSequenceOutputs;
import org.apache.lucene.util.fst.FST;
import org.apache.lucene.util.fst.PairOutputs;
import org.apache.lucene.util.fst.PairOutputs.Pair;
import org.apache.lucene.util.fst.PositiveIntOutputs;
import org.apache.lucene.util.fst.Util;


public final class NRTSuggester implements Accountable {
	private final FST<PairOutputs.Pair<Long, BytesRef>> fst;

	private final int maxAnalyzedPathsPerOutput;

	private final int payloadSep;

	private static final long MAX_TOP_N_QUEUE_SIZE = 5000;

	private NRTSuggester(FST<PairOutputs.Pair<Long, BytesRef>> fst, int maxAnalyzedPathsPerOutput, int payloadSep) {
		this.fst = fst;
		this.maxAnalyzedPathsPerOutput = maxAnalyzedPathsPerOutput;
		this.payloadSep = payloadSep;
	}

	@Override
	public long ramBytesUsed() {
		return (fst) == null ? 0 : fst.ramBytesUsed();
	}

	@Override
	public Collection<Accountable> getChildResources() {
		return Collections.emptyList();
	}

	public void lookup(final CompletionScorer scorer, Bits acceptDocs, final TopSuggestDocsCollector collector) throws IOException {
		final CharsRefBuilder spare = new CharsRefBuilder();
		Comparator<PairOutputs.Pair<Long, BytesRef>> comparator = NRTSuggester.getComparator();
	}

	private static class ScoringPathComparator implements Comparator<Util.FSTPath<PairOutputs.Pair<Long, BytesRef>>> {
		private final CompletionScorer scorer;

		public ScoringPathComparator(CompletionScorer scorer) {
			this.scorer = scorer;
		}

		@Override
		public int compare(Util.FSTPath<PairOutputs.Pair<Long, BytesRef>> first, Util.FSTPath<PairOutputs.Pair<Long, BytesRef>> second) {
			int cmp = Float.compare(scorer.score(NRTSuggester.decode(second.output.output1), second.boost), scorer.score(NRTSuggester.decode(first.output.output1), first.boost));
			return cmp != 0 ? cmp : first.input.get().compareTo(second.input.get());
		}
	}

	private static Comparator<PairOutputs.Pair<Long, BytesRef>> getComparator() {
		return new Comparator<PairOutputs.Pair<Long, BytesRef>>() {
			@Override
			public int compare(PairOutputs.Pair<Long, BytesRef> o1, PairOutputs.Pair<Long, BytesRef> o2) {
				return Long.compare(o1.output1, o2.output1);
			}
		};
	}

	private int getMaxTopNSearcherQueueSize(int topN, int numDocs, double liveDocsRatio, boolean filterEnabled) {
		long maxQueueSize = topN * (maxAnalyzedPathsPerOutput);
		assert liveDocsRatio <= 1.0;
		maxQueueSize = ((long) (maxQueueSize / liveDocsRatio));
		if (filterEnabled) {
			maxQueueSize = maxQueueSize + (numDocs / 2);
		}
		return ((int) (Math.min(NRTSuggester.MAX_TOP_N_QUEUE_SIZE, maxQueueSize)));
	}

	private static double calculateLiveDocRatio(int numDocs, int maxDocs) {
		return numDocs > 0 ? ((double) (numDocs)) / maxDocs : -1;
	}

	public static NRTSuggester load(IndexInput input) throws IOException {
		final FST<PairOutputs.Pair<Long, BytesRef>> fst = new FST<>(input, new PairOutputs<>(PositiveIntOutputs.getSingleton(), ByteSequenceOutputs.getSingleton()));
		int maxAnalyzedPathsPerOutput = input.readVInt();
		int endByte = input.readVInt();
		int payloadSep = input.readVInt();
		return new NRTSuggester(fst, maxAnalyzedPathsPerOutput, payloadSep);
	}

	static long encode(long input) {
		if ((input < 0) || (input > (Integer.MAX_VALUE))) {
			throw new UnsupportedOperationException(("cannot encode value: " + input));
		}
		return (Integer.MAX_VALUE) - input;
	}

	static long decode(long output) {
		assert (output >= 0) && (output <= (Integer.MAX_VALUE)) : ("decoded output: " + output) + " is not within 0 and Integer.MAX_VALUE";
		return (Integer.MAX_VALUE) - output;
	}

	static final class PayLoadProcessor {
		private static final int MAX_DOC_ID_LEN_WITH_SEP = 6;

		static int parseSurfaceForm(final BytesRef output, int payloadSep, CharsRefBuilder spare) {
			int surfaceFormLen = -1;
			for (int i = 0; i < (output.length); i++) {
				if ((output.bytes[((output.offset) + i)]) == payloadSep) {
					surfaceFormLen = i;
					break;
				}
			}
			assert surfaceFormLen != (-1) : "no payloadSep found, unable to determine surface form";
			spare.copyUTF8Bytes(output.bytes, output.offset, surfaceFormLen);
			return surfaceFormLen;
		}

		static BytesRef make(final BytesRef surface, int docID, int payloadSep) throws IOException {
			int len = (surface.length) + (NRTSuggester.PayLoadProcessor.MAX_DOC_ID_LEN_WITH_SEP);
			byte[] buffer = new byte[len];
			ByteArrayDataOutput output = new ByteArrayDataOutput(buffer);
			output.writeBytes(surface.bytes, ((surface.length) - (surface.offset)));
			output.writeByte(((byte) (payloadSep)));
			output.writeVInt(docID);
			return new BytesRef(buffer, 0, output.getPosition());
		}
	}
}

