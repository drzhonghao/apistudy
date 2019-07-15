

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.apache.lucene.search.suggest.InputIterator;
import org.apache.lucene.search.suggest.Lookup;
import org.apache.lucene.search.suggest.Lookup.LookupResult;
import org.apache.lucene.search.suggest.SortedInputIterator;
import org.apache.lucene.search.suggest.tst.TernaryTreeNode;
import org.apache.lucene.store.DataInput;
import org.apache.lucene.store.DataOutput;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefIterator;
import org.apache.lucene.util.CharsRefBuilder;
import org.apache.lucene.util.RamUsageEstimator;


public class TSTLookup extends Lookup {
	TernaryTreeNode root = new TernaryTreeNode();

	private long count = 0;

	private final Directory tempDir;

	private final String tempFileNamePrefix;

	public TSTLookup() {
		this(null, null);
	}

	public TSTLookup(Directory tempDir, String tempFileNamePrefix) {
		this.tempDir = tempDir;
		this.tempFileNamePrefix = tempFileNamePrefix;
	}

	private static final Comparator<BytesRef> utf8SortedAsUTF16SortOrder = ( a, b) -> {
		final byte[] aBytes = a.bytes;
		int aUpto = a.offset;
		final byte[] bBytes = b.bytes;
		int bUpto = b.offset;
		final int aStop = aUpto + (Math.min(a.length, b.length));
		while (aUpto < aStop) {
			int aByte = (aBytes[(aUpto++)]) & 255;
			int bByte = (bBytes[(bUpto++)]) & 255;
			if (aByte != bByte) {
				if ((aByte >= 238) && (bByte >= 238)) {
					if ((aByte & 254) == 238) {
						aByte += 14;
					}
					if ((bByte & 254) == 238) {
						bByte += 14;
					}
				}
				return aByte - bByte;
			}
		} 
		return (a.length) - (b.length);
	};

	@Override
	public void build(InputIterator iterator) throws IOException {
		if (iterator.hasPayloads()) {
			throw new IllegalArgumentException("this suggester doesn't support payloads");
		}
		if (iterator.hasContexts()) {
			throw new IllegalArgumentException("this suggester doesn't support contexts");
		}
		root = new TernaryTreeNode();
		iterator = new SortedInputIterator(tempDir, tempFileNamePrefix, iterator, TSTLookup.utf8SortedAsUTF16SortOrder);
		count = 0;
		ArrayList<String> tokens = new ArrayList<>();
		ArrayList<Number> vals = new ArrayList<>();
		BytesRef spare;
		CharsRefBuilder charsSpare = new CharsRefBuilder();
		while ((spare = iterator.next()) != null) {
			charsSpare.copyUTF8Bytes(spare);
			tokens.add(charsSpare.toString());
			vals.add(Long.valueOf(iterator.weight()));
			(count)++;
		} 
	}

	public boolean add(CharSequence key, Object value) {
		return true;
	}

	public Object get(CharSequence key) {
		return null;
	}

	private static boolean charSeqEquals(CharSequence left, CharSequence right) {
		int len = left.length();
		if (len != (right.length())) {
			return false;
		}
		for (int i = 0; i < len; i++) {
			if ((left.charAt(i)) != (right.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	@Override
	public List<Lookup.LookupResult> lookup(CharSequence key, Set<BytesRef> contexts, boolean onlyMorePopular, int num) {
		if (contexts != null) {
			throw new IllegalArgumentException("this suggester doesn't support contexts");
		}
		List<Lookup.LookupResult> res = new ArrayList<>();
		if (onlyMorePopular) {
			Lookup.LookupPriorityQueue queue = new Lookup.LookupPriorityQueue(num);
			for (Lookup.LookupResult lr : queue.getResults()) {
				res.add(lr);
			}
		}else {
		}
		return res;
	}

	private static final byte LO_KID = 1;

	private static final byte EQ_KID = 2;

	private static final byte HI_KID = 4;

	private static final byte HAS_TOKEN = 8;

	private static final byte HAS_VALUE = 16;

	private void readRecursively(DataInput in, TernaryTreeNode node) throws IOException {
		byte mask = in.readByte();
		if ((mask & (TSTLookup.HAS_TOKEN)) != 0) {
		}
		if ((mask & (TSTLookup.HAS_VALUE)) != 0) {
		}
		if ((mask & (TSTLookup.LO_KID)) != 0) {
		}
		if ((mask & (TSTLookup.EQ_KID)) != 0) {
		}
		if ((mask & (TSTLookup.HI_KID)) != 0) {
		}
	}

	private void writeRecursively(DataOutput out, TernaryTreeNode node) throws IOException {
		byte mask = 0;
		out.writeByte(mask);
	}

	@Override
	public synchronized boolean store(DataOutput output) throws IOException {
		output.writeVLong(count);
		writeRecursively(output, root);
		return true;
	}

	@Override
	public synchronized boolean load(DataInput input) throws IOException {
		count = input.readVLong();
		root = new TernaryTreeNode();
		readRecursively(input, root);
		return true;
	}

	@Override
	public long ramBytesUsed() {
		long mem = RamUsageEstimator.shallowSizeOf(this);
		if ((root) != null) {
		}
		return mem;
	}

	@Override
	public long getCount() {
		return count;
	}
}

