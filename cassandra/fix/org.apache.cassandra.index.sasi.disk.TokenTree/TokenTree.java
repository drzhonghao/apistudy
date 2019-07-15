

import com.carrotsearch.hppc.LongOpenHashSet;
import com.carrotsearch.hppc.LongSet;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.Iterators;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.index.sasi.disk.Descriptor;
import org.apache.cassandra.index.sasi.disk.Token;
import org.apache.cassandra.index.sasi.disk.TokenTreeBuilder;
import org.apache.cassandra.index.sasi.utils.AbstractIterator;
import org.apache.cassandra.index.sasi.utils.CombinedValue;
import org.apache.cassandra.index.sasi.utils.MappedBuffer;
import org.apache.cassandra.index.sasi.utils.RangeIterator;
import org.apache.cassandra.utils.MergeIterator;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import static org.apache.cassandra.index.sasi.disk.TokenTreeBuilder.EntryType.FACTORED;
import static org.apache.cassandra.index.sasi.disk.TokenTreeBuilder.EntryType.OVERFLOW;
import static org.apache.cassandra.index.sasi.disk.TokenTreeBuilder.EntryType.PACKED;
import static org.apache.cassandra.index.sasi.disk.TokenTreeBuilder.EntryType.SIMPLE;
import static org.apache.cassandra.index.sasi.disk.TokenTreeBuilder.EntryType.of;


public class TokenTree {
	private static final int LONG_BYTES = (Long.SIZE) / 8;

	private static final int SHORT_BYTES = (Short.SIZE) / 8;

	private final Descriptor descriptor;

	private final MappedBuffer file;

	private final long startPos;

	private final long treeMinToken;

	private final long treeMaxToken;

	private final long tokenCount;

	@VisibleForTesting
	protected TokenTree(MappedBuffer tokenTree) {
		this(Descriptor.CURRENT, tokenTree);
	}

	public TokenTree(Descriptor d, MappedBuffer tokenTree) {
		descriptor = d;
		file = tokenTree;
		startPos = file.position();
		file.position(((startPos) + (TokenTreeBuilder.SHARED_HEADER_BYTES)));
		if (!(validateMagic()))
			throw new IllegalArgumentException("invalid token tree");

		tokenCount = file.getLong();
		treeMinToken = file.getLong();
		treeMaxToken = file.getLong();
	}

	public long getCount() {
		return tokenCount;
	}

	public RangeIterator<Long, Token> iterator(Function<Long, DecoratedKey> keyFetcher) {
		return new TokenTree.TokenTreeIterator(file.duplicate(), keyFetcher);
	}

	public TokenTree.OnDiskToken get(final long searchToken, Function<Long, DecoratedKey> keyFetcher) {
		seekToLeaf(searchToken, file);
		long leafStart = file.position();
		short leafSize = file.getShort((leafStart + 1));
		file.position((leafStart + (TokenTreeBuilder.BLOCK_HEADER_BYTES)));
		short tokenIndex = searchLeaf(searchToken, leafSize);
		file.position((leafStart + (TokenTreeBuilder.BLOCK_HEADER_BYTES)));
		TokenTree.OnDiskToken token = TokenTree.OnDiskToken.getTokenAt(file, tokenIndex, leafSize, keyFetcher);
		return token.get().equals(searchToken) ? token : null;
	}

	private boolean validateMagic() {
		switch (descriptor.version.toString()) {
			case Descriptor.VERSION_AA :
				return true;
			case Descriptor.VERSION_AB :
				return (TokenTreeBuilder.AB_MAGIC) == (file.getShort());
			default :
				return false;
		}
	}

	private void seekToLeaf(long token, MappedBuffer file) {
		long blockStart = startPos;
		while (true) {
			file.position(blockStart);
			byte info = file.get();
			boolean isLeaf = (info & 1) == 1;
			if (isLeaf) {
				file.position(blockStart);
				break;
			}
			short tokenCount = file.getShort();
			long minToken = file.getLong();
			long maxToken = file.getLong();
			long seekBase = blockStart + (TokenTreeBuilder.BLOCK_HEADER_BYTES);
			if (minToken > token) {
				file.position((seekBase + (tokenCount * (TokenTree.LONG_BYTES))));
				blockStart = (startPos) + ((int) (file.getLong()));
			}else
				if (maxToken < token) {
					file.position((seekBase + ((2 * tokenCount) * (TokenTree.LONG_BYTES))));
					blockStart = (startPos) + ((int) (file.getLong()));
				}else {
					file.position(seekBase);
					short offsetIndex = searchBlock(token, tokenCount, file);
					if (offsetIndex == tokenCount)
						file.position(((file.position()) + (offsetIndex * (TokenTree.LONG_BYTES))));
					else
						file.position(((file.position()) + ((((tokenCount - offsetIndex) - 1) + offsetIndex) * (TokenTree.LONG_BYTES))));

					blockStart = (startPos) + ((int) (file.getLong()));
				}

		} 
	}

	private short searchBlock(long searchToken, short tokenCount, MappedBuffer file) {
		short offsetIndex = 0;
		for (int i = 0; i < tokenCount; i++) {
			long readToken = file.getLong();
			if (searchToken < readToken)
				break;

			offsetIndex++;
		}
		return offsetIndex;
	}

	private short searchLeaf(long searchToken, short tokenCount) {
		long base = file.position();
		int start = 0;
		int end = tokenCount;
		int middle = 0;
		while (start <= end) {
			middle = start + ((end - start) >> 1);
			long token = file.getLong((base + ((middle * (2 * (TokenTree.LONG_BYTES))) + 4)));
			if (token == searchToken)
				break;

			if (token < searchToken)
				start = middle + 1;
			else
				end = middle - 1;

		} 
		return ((short) (middle));
	}

	public class TokenTreeIterator extends RangeIterator<Long, Token> {
		private final Function<Long, DecoratedKey> keyFetcher;

		private final MappedBuffer file;

		private long currentLeafStart;

		private int currentTokenIndex;

		private long leafMinToken;

		private long leafMaxToken;

		private short leafSize;

		protected boolean firstIteration = true;

		private boolean lastLeaf;

		TokenTreeIterator(MappedBuffer file, Function<Long, DecoratedKey> keyFetcher) {
			super(treeMinToken, treeMaxToken, tokenCount);
			this.file = file;
			this.keyFetcher = keyFetcher;
		}

		protected Token computeNext() {
			maybeFirstIteration();
			if (((currentTokenIndex) >= (leafSize)) && (lastLeaf))
				return endOfData();

			if ((currentTokenIndex) < (leafSize)) {
				return getTokenAt(((currentTokenIndex)++));
			}else {
				assert !(lastLeaf);
				seekToNextLeaf();
				setupBlock();
				return computeNext();
			}
		}

		protected void performSkipTo(Long nextToken) {
			maybeFirstIteration();
			if (nextToken <= (leafMaxToken)) {
				searchLeaf(nextToken);
			}else {
				seekToLeaf(nextToken, file);
				setupBlock();
				findNearest(nextToken);
			}
		}

		private void setupBlock() {
			currentLeafStart = file.position();
			currentTokenIndex = 0;
			lastLeaf = ((file.get()) & (1 << (TokenTreeBuilder.LAST_LEAF_SHIFT))) > 0;
			leafSize = file.getShort();
			leafMinToken = file.getLong();
			leafMaxToken = file.getLong();
			file.position(((currentLeafStart) + (TokenTreeBuilder.BLOCK_HEADER_BYTES)));
		}

		private void findNearest(Long next) {
			if ((next > (leafMaxToken)) && (!(lastLeaf))) {
				seekToNextLeaf();
				setupBlock();
				findNearest(next);
			}else
				if (next > (leafMinToken))
					searchLeaf(next);


		}

		private void searchLeaf(long next) {
			for (int i = currentTokenIndex; i < (leafSize); i++) {
				if ((compareTokenAt(currentTokenIndex, next)) >= 0)
					break;

				(currentTokenIndex)++;
			}
		}

		private int compareTokenAt(int idx, long toToken) {
			return Long.compare(file.getLong(getTokenPosition(idx)), toToken);
		}

		private Token getTokenAt(int idx) {
			return TokenTree.OnDiskToken.getTokenAt(file, idx, leafSize, keyFetcher);
		}

		private long getTokenPosition(int idx) {
			return (TokenTree.OnDiskToken.getEntryPosition(idx, file)) + (2 * (TokenTree.SHORT_BYTES));
		}

		private void seekToNextLeaf() {
			file.position(((currentLeafStart) + (TokenTreeBuilder.BLOCK_BYTES)));
		}

		public void close() throws IOException {
		}

		private void maybeFirstIteration() {
			if (!(firstIteration))
				return;

			seekToLeaf(treeMinToken, file);
			setupBlock();
			firstIteration = false;
		}
	}

	public static class OnDiskToken extends Token {
		private final Set<TokenTree.TokenInfo> info = new HashSet<>(2);

		private final Set<DecoratedKey> loadedKeys = new TreeSet<>(DecoratedKey.comparator);

		public OnDiskToken(MappedBuffer buffer, long position, short leafSize, Function<Long, DecoratedKey> keyFetcher) {
			super(buffer.getLong((position + (2 * (TokenTree.SHORT_BYTES)))));
			info.add(new TokenTree.TokenInfo(buffer, position, leafSize, keyFetcher));
		}

		public void merge(CombinedValue<Long> other) {
			if (!(other instanceof Token))
				return;

			Token o = ((Token) (other));
			if (o instanceof TokenTree.OnDiskToken) {
				info.addAll(((TokenTree.OnDiskToken) (other)).info);
			}else {
				Iterators.addAll(loadedKeys, o.iterator());
			}
		}

		public Iterator<DecoratedKey> iterator() {
			List<Iterator<DecoratedKey>> keys = new ArrayList<>(info.size());
			for (TokenTree.TokenInfo i : info)
				keys.add(i.iterator());

			if (!(loadedKeys.isEmpty()))
				keys.add(loadedKeys.iterator());

			return MergeIterator.get(keys, DecoratedKey.comparator, new MergeIterator.Reducer<DecoratedKey, DecoratedKey>() {
				DecoratedKey reduced = null;

				public boolean trivialReduceIsTrivial() {
					return true;
				}

				public void reduce(int idx, DecoratedKey current) {
					reduced = current;
				}

				protected DecoratedKey getReduced() {
					return reduced;
				}
			});
		}

		public LongSet getOffsets() {
			LongSet offsets = new LongOpenHashSet(4);
			for (TokenTree.TokenInfo i : info) {
				for (long offset : i.fetchOffsets())
					offsets.add(offset);

			}
			return offsets;
		}

		public static TokenTree.OnDiskToken getTokenAt(MappedBuffer buffer, int idx, short leafSize, Function<Long, DecoratedKey> keyFetcher) {
			return new TokenTree.OnDiskToken(buffer, TokenTree.OnDiskToken.getEntryPosition(idx, buffer), leafSize, keyFetcher);
		}

		private static long getEntryPosition(int idx, MappedBuffer file) {
			return (file.position()) + (idx * (2 * (TokenTree.LONG_BYTES)));
		}
	}

	private static class TokenInfo {
		private final MappedBuffer buffer;

		private final Function<Long, DecoratedKey> keyFetcher;

		private final long position;

		private final short leafSize;

		public TokenInfo(MappedBuffer buffer, long position, short leafSize, Function<Long, DecoratedKey> keyFetcher) {
			this.keyFetcher = keyFetcher;
			this.buffer = buffer;
			this.position = position;
			this.leafSize = leafSize;
		}

		public Iterator<DecoratedKey> iterator() {
			return new TokenTree.KeyIterator(keyFetcher, fetchOffsets());
		}

		public int hashCode() {
			return new HashCodeBuilder().append(keyFetcher).append(position).append(leafSize).build();
		}

		public boolean equals(Object other) {
			if (!(other instanceof TokenTree.TokenInfo))
				return false;

			TokenTree.TokenInfo o = ((TokenTree.TokenInfo) (other));
			return ((keyFetcher) == (o.keyFetcher)) && ((position) == (o.position));
		}

		private long[] fetchOffsets() {
			short info = buffer.getShort(position);
			int offsetExtra = (buffer.getShort(((position) + (TokenTree.SHORT_BYTES)))) & 65535;
			int offsetData = buffer.getInt((((position) + (2 * (TokenTree.SHORT_BYTES))) + (TokenTree.LONG_BYTES)));
			TokenTreeBuilder.EntryType type = of((info & (TokenTreeBuilder.ENTRY_TYPE_MASK)));
			switch (type) {
				case SIMPLE :
					return new long[]{ offsetData };
				case OVERFLOW :
					long[] offsets = new long[offsetExtra];
					long offsetPos = ((buffer.position()) + (2 * ((leafSize) * (TokenTree.LONG_BYTES)))) + (offsetData * (TokenTree.LONG_BYTES));
					for (int i = 0; i < offsetExtra; i++)
						offsets[i] = buffer.getLong((offsetPos + (i * (TokenTree.LONG_BYTES))));

					return offsets;
				case FACTORED :
					return new long[]{ (((long) (offsetData)) << (Short.SIZE)) + offsetExtra };
				case PACKED :
					return new long[]{ offsetExtra, offsetData };
				default :
					throw new IllegalStateException(("Unknown entry type: " + type));
			}
		}
	}

	private static class KeyIterator extends AbstractIterator<DecoratedKey> {
		private final Function<Long, DecoratedKey> keyFetcher;

		private final long[] offsets;

		private int index = 0;

		public KeyIterator(Function<Long, DecoratedKey> keyFetcher, long[] offsets) {
			this.keyFetcher = keyFetcher;
			this.offsets = offsets;
		}

		public DecoratedKey computeNext() {
			return (index) < (offsets.length) ? keyFetcher.apply(offsets[((index)++)]) : endOfData();
		}
	}
}

