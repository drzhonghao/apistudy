

import java.io.IOException;
import java.util.PriorityQueue;
import org.apache.lucene.analysis.miscellaneous.ConcatenateGraphFilter;
import org.apache.lucene.store.DataOutput;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefBuilder;
import org.apache.lucene.util.IntsRef;
import org.apache.lucene.util.IntsRefBuilder;
import org.apache.lucene.util.fst.Builder;
import org.apache.lucene.util.fst.ByteSequenceOutputs;
import org.apache.lucene.util.fst.FST;
import org.apache.lucene.util.fst.PairOutputs;
import org.apache.lucene.util.fst.PairOutputs.Pair;
import org.apache.lucene.util.fst.PositiveIntOutputs;
import org.apache.lucene.util.fst.Util;

import static org.apache.lucene.util.fst.FST.INPUT_TYPE.BYTE1;


final class NRTSuggesterBuilder {
	public static final int PAYLOAD_SEP = ConcatenateGraphFilter.SEP_LABEL;

	public static final int END_BYTE = 0;

	private final PairOutputs<Long, BytesRef> outputs;

	private final Builder<PairOutputs.Pair<Long, BytesRef>> builder;

	private final IntsRefBuilder scratchInts = new IntsRefBuilder();

	private final BytesRefBuilder analyzed = new BytesRefBuilder();

	private final PriorityQueue<NRTSuggesterBuilder.Entry> entries;

	private final int payloadSep;

	private final int endByte;

	private int maxAnalyzedPathsPerOutput = 0;

	public NRTSuggesterBuilder() {
		this.payloadSep = NRTSuggesterBuilder.PAYLOAD_SEP;
		this.endByte = NRTSuggesterBuilder.END_BYTE;
		this.outputs = new PairOutputs<>(PositiveIntOutputs.getSingleton(), ByteSequenceOutputs.getSingleton());
		this.entries = new PriorityQueue<>();
		this.builder = new Builder<>(BYTE1, outputs);
	}

	public void startTerm(BytesRef analyzed) {
		this.analyzed.copyBytes(analyzed);
		this.analyzed.append(((byte) (endByte)));
	}

	public void finishTerm() throws IOException {
		int numArcs = 0;
		int numDedupBytes = 1;
		analyzed.grow(((analyzed.length()) + 1));
		analyzed.setLength(((analyzed.length()) + 1));
		for (NRTSuggesterBuilder.Entry entry : entries) {
			if (numArcs == (NRTSuggesterBuilder.maxNumArcsForDedupByte(numDedupBytes))) {
				analyzed.setByteAt(((analyzed.length()) - 1), ((byte) (numArcs)));
				analyzed.grow(((analyzed.length()) + 1));
				analyzed.setLength(((analyzed.length()) + 1));
				numArcs = 0;
				numDedupBytes++;
			}
			analyzed.setByteAt(((analyzed.length()) - 1), ((byte) (numArcs++)));
			Util.toIntsRef(analyzed.get(), scratchInts);
			builder.add(scratchInts.get(), outputs.newPair(entry.weight, entry.payload));
		}
		maxAnalyzedPathsPerOutput = Math.max(maxAnalyzedPathsPerOutput, entries.size());
		entries.clear();
	}

	public boolean store(DataOutput output) throws IOException {
		final FST<PairOutputs.Pair<Long, BytesRef>> build = builder.finish();
		if (build == null) {
			return false;
		}
		build.save(output);
		assert (maxAnalyzedPathsPerOutput) > 0;
		output.writeVInt(maxAnalyzedPathsPerOutput);
		output.writeVInt(NRTSuggesterBuilder.END_BYTE);
		output.writeVInt(NRTSuggesterBuilder.PAYLOAD_SEP);
		return true;
	}

	private static int maxNumArcsForDedupByte(int currentNumDedupBytes) {
		int maxArcs = 1 + (2 * currentNumDedupBytes);
		if (currentNumDedupBytes > 5) {
			maxArcs *= currentNumDedupBytes;
		}
		return Math.min(maxArcs, 255);
	}

	private static final class Entry implements Comparable<NRTSuggesterBuilder.Entry> {
		final BytesRef payload;

		final long weight;

		public Entry(BytesRef payload, long weight) {
			this.payload = payload;
			this.weight = weight;
		}

		@Override
		public int compareTo(NRTSuggesterBuilder.Entry o) {
			return Long.compare(weight, o.weight);
		}
	}
}

