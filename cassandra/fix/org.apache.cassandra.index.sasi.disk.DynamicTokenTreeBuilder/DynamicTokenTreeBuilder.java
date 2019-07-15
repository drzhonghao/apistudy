

import com.carrotsearch.hppc.LongOpenHashSet;
import com.carrotsearch.hppc.LongSet;
import com.carrotsearch.hppc.cursors.LongCursor;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import org.apache.cassandra.index.sasi.disk.AbstractTokenTreeBuilder;
import org.apache.cassandra.index.sasi.disk.TokenTreeBuilder;
import org.apache.cassandra.utils.AbstractIterator;
import org.apache.cassandra.utils.Pair;


public class DynamicTokenTreeBuilder extends AbstractTokenTreeBuilder {
	private final SortedMap<Long, LongSet> tokens = new TreeMap<>();

	public DynamicTokenTreeBuilder() {
	}

	public DynamicTokenTreeBuilder(TokenTreeBuilder data) {
		add(data);
	}

	public DynamicTokenTreeBuilder(SortedMap<Long, LongSet> data) {
		add(data);
	}

	public void add(Long token, long keyPosition) {
		LongSet found = tokens.get(token);
		if (found == null)
			tokens.put(token, (found = new LongOpenHashSet(2)));

		found.add(keyPosition);
	}

	public void add(Iterator<Pair<Long, LongSet>> data) {
		while (data.hasNext()) {
			Pair<Long, LongSet> entry = data.next();
			for (LongCursor l : entry.right)
				add(entry.left, l.value);

		} 
	}

	public void add(SortedMap<Long, LongSet> data) {
		for (Map.Entry<Long, LongSet> newEntry : data.entrySet()) {
			LongSet found = tokens.get(newEntry.getKey());
			if (found == null)
				tokens.put(newEntry.getKey(), (found = new LongOpenHashSet(4)));

			for (LongCursor offset : newEntry.getValue())
				found.add(offset.value);

		}
	}

	public Iterator<Pair<Long, LongSet>> iterator() {
		final Iterator<Map.Entry<Long, LongSet>> iterator = tokens.entrySet().iterator();
		return new AbstractIterator<Pair<Long, LongSet>>() {
			protected Pair<Long, LongSet> computeNext() {
				if (!(iterator.hasNext()))
					return endOfData();

				Map.Entry<Long, LongSet> entry = iterator.next();
				return Pair.create(entry.getKey(), entry.getValue());
			}
		};
	}

	public boolean isEmpty() {
		return (tokens.size()) == 0;
	}

	protected void constructTree() {
		tokenCount = tokens.size();
		treeMinToken = tokens.firstKey();
		treeMaxToken = tokens.lastKey();
		numBlocks = 1;
		if ((tokenCount) <= (TokenTreeBuilder.TOKENS_PER_BLOCK)) {
			leftmostLeaf = new DynamicTokenTreeBuilder.DynamicLeaf(tokens);
			rightmostLeaf = leftmostLeaf;
			root = leftmostLeaf;
		}else {
			root = new AbstractTokenTreeBuilder.InteriorNode();
			rightmostParent = ((AbstractTokenTreeBuilder.InteriorNode) (root));
			int i = 0;
			AbstractTokenTreeBuilder.Leaf lastLeaf = null;
			Long firstToken = tokens.firstKey();
			Long finalToken = tokens.lastKey();
			Long lastToken;
			for (Long token : tokens.keySet()) {
				if ((i == 0) || (((i % (TokenTreeBuilder.TOKENS_PER_BLOCK)) != 0) && (i != ((tokenCount) - 1)))) {
					i++;
					continue;
				}
				lastToken = token;
				AbstractTokenTreeBuilder.Leaf leaf = ((i != ((tokenCount) - 1)) || (token.equals(finalToken))) ? new DynamicTokenTreeBuilder.DynamicLeaf(tokens.subMap(firstToken, lastToken)) : new DynamicTokenTreeBuilder.DynamicLeaf(tokens.tailMap(firstToken));
				if (i == (TokenTreeBuilder.TOKENS_PER_BLOCK))
					leftmostLeaf = leaf;
				else {
				}
				lastLeaf = leaf;
				rightmostLeaf = leaf;
				firstToken = lastToken;
				i++;
				(numBlocks)++;
				if (token.equals(finalToken)) {
					AbstractTokenTreeBuilder.Leaf finalLeaf = new DynamicTokenTreeBuilder.DynamicLeaf(tokens.tailMap(token));
					rightmostLeaf = finalLeaf;
					(numBlocks)++;
				}
			}
		}
	}

	private class DynamicLeaf extends AbstractTokenTreeBuilder.Leaf {
		private final SortedMap<Long, LongSet> tokens;

		DynamicLeaf(SortedMap<Long, LongSet> data) {
			super(data.firstKey(), data.lastKey());
			tokens = data;
		}

		public int tokenCount() {
			return tokens.size();
		}

		public boolean isSerializable() {
			return true;
		}

		protected void serializeData(ByteBuffer buf) {
			for (Map.Entry<Long, LongSet> entry : tokens.entrySet())
				createEntry(entry.getKey(), entry.getValue()).serialize(buf);

		}
	}
}

