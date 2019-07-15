

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.lucene.search.suggest.InputIterator;
import org.apache.lucene.search.suggest.Lookup;
import org.apache.lucene.search.suggest.Lookup.LookupResult;
import org.apache.lucene.search.suggest.jaspell.JaspellTernarySearchTrie;
import org.apache.lucene.store.DataInput;
import org.apache.lucene.store.DataOutput;
import org.apache.lucene.util.Accountable;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefIterator;
import org.apache.lucene.util.CharsRef;
import org.apache.lucene.util.CharsRefBuilder;
import org.apache.lucene.util.PriorityQueue;


@Deprecated
public class JaspellLookup extends Lookup implements Accountable {
	JaspellTernarySearchTrie trie = new JaspellTernarySearchTrie();

	private boolean usePrefix = true;

	private int editDistance = 2;

	private long count = 0;

	public JaspellLookup() {
	}

	@Override
	public void build(InputIterator iterator) throws IOException {
		if (iterator.hasPayloads()) {
			throw new IllegalArgumentException("this suggester doesn't support payloads");
		}
		if (iterator.hasContexts()) {
			throw new IllegalArgumentException("this suggester doesn't support contexts");
		}
		count = 0;
		trie = new JaspellTernarySearchTrie();
		trie.setMatchAlmostDiff(editDistance);
		BytesRef spare;
		final CharsRefBuilder charsSpare = new CharsRefBuilder();
		while ((spare = iterator.next()) != null) {
			final long weight = iterator.weight();
			if ((spare.length) == 0) {
				continue;
			}
			charsSpare.copyUTF8Bytes(spare);
			trie.put(charsSpare.toString(), Long.valueOf(weight));
			(count)++;
		} 
	}

	public boolean add(CharSequence key, Object value) {
		trie.put(key, value);
		return false;
	}

	public Object get(CharSequence key) {
		return trie.get(key);
	}

	@Override
	public List<Lookup.LookupResult> lookup(CharSequence key, Set<BytesRef> contexts, boolean onlyMorePopular, int num) {
		if (contexts != null) {
			throw new IllegalArgumentException("this suggester doesn't support contexts");
		}
		List<Lookup.LookupResult> res = new ArrayList<>();
		List<String> list;
		int count = (onlyMorePopular) ? num * 2 : num;
		if (usePrefix) {
			list = trie.matchPrefix(key, count);
		}else {
			list = trie.matchAlmost(key, count);
		}
		if ((list == null) || ((list.size()) == 0)) {
			return res;
		}
		int maxCnt = Math.min(num, list.size());
		if (onlyMorePopular) {
			Lookup.LookupPriorityQueue queue = new Lookup.LookupPriorityQueue(num);
			for (String s : list) {
				long freq = ((Number) (trie.get(s))).longValue();
				queue.insertWithOverflow(new Lookup.LookupResult(new CharsRef(s), freq));
			}
			for (Lookup.LookupResult lr : queue.getResults()) {
				res.add(lr);
			}
		}else {
			for (int i = 0; i < maxCnt; i++) {
				String s = list.get(i);
				long freq = ((Number) (trie.get(s))).longValue();
				res.add(new Lookup.LookupResult(new CharsRef(s), freq));
			}
		}
		return res;
	}

	private static final byte LO_KID = 1;

	private static final byte EQ_KID = 2;

	private static final byte HI_KID = 4;

	private static final byte HAS_VALUE = 8;

	@Override
	public boolean store(DataOutput output) throws IOException {
		output.writeVLong(count);
		return true;
	}

	@Override
	public boolean load(DataInput input) throws IOException {
		count = input.readVLong();
		return true;
	}

	@Override
	public long ramBytesUsed() {
		return trie.ramBytesUsed();
	}

	@Override
	public long getCount() {
		return count;
	}
}

