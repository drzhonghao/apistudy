

import java.io.IOException;
import org.apache.lucene.util.fst.Builder;
import org.apache.lucene.util.fst.FST;
import org.apache.lucene.util.packed.PackedInts;
import org.apache.lucene.util.packed.PagedGrowableWriter;


final class NodeHash<T> {
	private PagedGrowableWriter table;

	private long count;

	private long mask;

	private final FST<T> fst;

	private final FST.Arc<T> scratchArc = new FST.Arc<>();

	private final FST.BytesReader in;

	public NodeHash(FST<T> fst, FST.BytesReader in) {
		table = new PagedGrowableWriter(16, (1 << 27), 8, PackedInts.COMPACT);
		mask = 15;
		this.fst = fst;
		this.in = in;
	}

	private boolean nodesEqual(Builder.UnCompiledNode<T> node, long address) throws IOException {
		fst.readFirstRealTargetArc(address, scratchArc, in);
		if (((scratchArc.bytesPerArc) != 0) && ((node.numArcs) != (scratchArc.numArcs))) {
			return false;
		}
		for (int arcUpto = 0; arcUpto < (node.numArcs); arcUpto++) {
			final Builder.Arc<T> arc = node.arcs[arcUpto];
			if (scratchArc.isLast()) {
				if (arcUpto == ((node.numArcs) - 1)) {
					return true;
				}else {
					return false;
				}
			}
			fst.readNextRealArc(scratchArc, in);
		}
		return false;
	}

	private long hash(Builder.UnCompiledNode<T> node) {
		final int PRIME = 31;
		long h = 0;
		for (int arcIdx = 0; arcIdx < (node.numArcs); arcIdx++) {
			final Builder.Arc<T> arc = node.arcs[arcIdx];
			h = (PRIME * h) + (arc.label);
			h = (PRIME * h) + (arc.output.hashCode());
			h = (PRIME * h) + (arc.nextFinalOutput.hashCode());
			if (arc.isFinal) {
				h += 17;
			}
		}
		return h & (Long.MAX_VALUE);
	}

	private long hash(long node) throws IOException {
		final int PRIME = 31;
		long h = 0;
		fst.readFirstRealTargetArc(node, scratchArc, in);
		while (true) {
			h = (PRIME * h) + (scratchArc.label);
			h = (PRIME * h) + ((int) ((scratchArc.target) ^ ((scratchArc.target) >> 32)));
			h = (PRIME * h) + (scratchArc.output.hashCode());
			h = (PRIME * h) + (scratchArc.nextFinalOutput.hashCode());
			if (scratchArc.isFinal()) {
				h += 17;
			}
			if (scratchArc.isLast()) {
				break;
			}
			fst.readNextRealArc(scratchArc, in);
		} 
		return h & (Long.MAX_VALUE);
	}

	public long add(Builder<T> builder, Builder.UnCompiledNode<T> nodeIn) throws IOException {
		final long h = hash(nodeIn);
		long pos = h & (mask);
		int c = 0;
		while (true) {
			final long v = table.get(pos);
			if (v == 0) {
				(count)++;
				if ((count) > ((2 * (table.size())) / 3)) {
					rehash();
				}
			}else
				if (nodesEqual(nodeIn, v)) {
					return v;
				}

			pos = (pos + (++c)) & (mask);
		} 
	}

	private void addNew(long address) throws IOException {
		long pos = (hash(address)) & (mask);
		int c = 0;
		while (true) {
			if ((table.get(pos)) == 0) {
				table.set(pos, address);
				break;
			}
			pos = (pos + (++c)) & (mask);
		} 
	}

	private void rehash() throws IOException {
		final PagedGrowableWriter oldTable = table;
		table = new PagedGrowableWriter((2 * (oldTable.size())), (1 << 30), PackedInts.bitsRequired(count), PackedInts.COMPACT);
		mask = (table.size()) - 1;
		for (long idx = 0; idx < (oldTable.size()); idx++) {
			final long address = oldTable.get(idx);
			if (address != 0) {
				addNew(address);
			}
		}
	}
}

