

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.index.sasi.Term;
import org.apache.cassandra.index.sasi.disk.Descriptor;
import org.apache.cassandra.index.sasi.disk.OnDiskBlock;
import org.apache.cassandra.index.sasi.disk.OnDiskIndexBuilder;
import org.apache.cassandra.index.sasi.disk.Token;
import org.apache.cassandra.index.sasi.disk.TokenTree;
import org.apache.cassandra.index.sasi.plan.Expression;
import org.apache.cassandra.index.sasi.utils.AbstractIterator;
import org.apache.cassandra.index.sasi.utils.CombinedValue;
import org.apache.cassandra.index.sasi.utils.MappedBuffer;
import org.apache.cassandra.index.sasi.utils.RangeIterator;
import org.apache.cassandra.index.sasi.utils.RangeUnionIterator;
import org.apache.cassandra.io.FSReadError;
import org.apache.cassandra.io.util.ChannelProxy;
import org.apache.cassandra.io.util.FileUtils;
import org.apache.cassandra.utils.ByteBufferUtil;
import org.apache.cassandra.utils.FBUtilities;

import static org.apache.cassandra.index.sasi.disk.OnDiskBlock.BlockType.DATA;
import static org.apache.cassandra.index.sasi.disk.OnDiskBlock.BlockType.POINTER;
import static org.apache.cassandra.index.sasi.disk.OnDiskIndexBuilder.Mode.CONTAINS;
import static org.apache.cassandra.index.sasi.disk.OnDiskIndexBuilder.Mode.SPARSE;
import static org.apache.cassandra.index.sasi.disk.OnDiskIndexBuilder.TermSize.of;
import static org.apache.cassandra.index.sasi.plan.Expression.Op.EQ;
import static org.apache.cassandra.index.sasi.plan.Expression.Op.NOT_EQ;
import static org.apache.cassandra.index.sasi.plan.Expression.Op.PREFIX;
import static org.apache.cassandra.index.sasi.plan.Expression.Op.RANGE;


public class OnDiskIndex implements Closeable , Iterable<OnDiskIndex.DataTerm> {
	public enum IteratorOrder {

		DESC(1),
		ASC((-1));
		public final int step;

		IteratorOrder(int step) {
			this.step = step;
		}

		public int startAt(OnDiskBlock<OnDiskIndex.DataTerm> block, Expression e) {
			return 0;
		}

		public int startAt(OnDiskBlock.SearchResult<OnDiskIndex.DataTerm> found, boolean inclusive) {
			switch (this) {
				case DESC :
					if ((found.cmp) < 0)
						return (found.index) + 1;

					return inclusive || ((found.cmp) != 0) ? found.index : (found.index) + 1;
				case ASC :
					if ((found.cmp) < 0)
						return found.index;

					return inclusive && (((found.cmp) == 0) || ((found.cmp) < 0)) ? found.index : (found.index) - 1;
				default :
					throw new IllegalArgumentException(("Unknown order: " + (this)));
			}
		}
	}

	public final Descriptor descriptor;

	protected final OnDiskIndexBuilder.Mode mode;

	protected final OnDiskIndexBuilder.TermSize termSize;

	protected final AbstractType<?> comparator;

	protected final MappedBuffer indexFile;

	protected final long indexSize;

	protected final boolean hasMarkedPartials;

	protected final Function<Long, DecoratedKey> keyFetcher;

	protected final String indexPath;

	protected final OnDiskIndex.PointerLevel[] levels;

	protected final OnDiskIndex.DataLevel dataLevel;

	protected final ByteBuffer minTerm;

	protected final ByteBuffer maxTerm;

	protected final ByteBuffer minKey;

	protected final ByteBuffer maxKey;

	@SuppressWarnings("resource")
	public OnDiskIndex(File index, AbstractType<?> cmp, Function<Long, DecoratedKey> keyReader) {
		keyFetcher = keyReader;
		comparator = cmp;
		indexPath = index.getAbsolutePath();
		RandomAccessFile backingFile = null;
		try {
			backingFile = new RandomAccessFile(index, "r");
			descriptor = new Descriptor(backingFile.readUTF());
			termSize = of(backingFile.readShort());
			minTerm = ByteBufferUtil.readWithShortLength(backingFile);
			maxTerm = ByteBufferUtil.readWithShortLength(backingFile);
			minKey = ByteBufferUtil.readWithShortLength(backingFile);
			maxKey = ByteBufferUtil.readWithShortLength(backingFile);
			mode = OnDiskIndexBuilder.Mode.mode(backingFile.readUTF());
			hasMarkedPartials = backingFile.readBoolean();
			indexSize = backingFile.length();
			indexFile = new MappedBuffer(new ChannelProxy(indexPath, backingFile.getChannel()));
			indexFile.position(indexFile.getLong(((indexSize) - 8)));
			int numLevels = indexFile.getInt();
			levels = new OnDiskIndex.PointerLevel[numLevels];
			for (int i = 0; i < (levels.length); i++) {
				int blockCount = indexFile.getInt();
				levels[i] = new OnDiskIndex.PointerLevel(indexFile.position(), blockCount);
				indexFile.position(((indexFile.position()) + (blockCount * 8)));
			}
			int blockCount = indexFile.getInt();
			dataLevel = new OnDiskIndex.DataLevel(indexFile.position(), blockCount);
		} catch (IOException e) {
			throw new FSReadError(e, index);
		} finally {
			FileUtils.closeQuietly(backingFile);
		}
	}

	public boolean hasMarkedPartials() {
		return hasMarkedPartials;
	}

	public OnDiskIndexBuilder.Mode mode() {
		return mode;
	}

	public ByteBuffer minTerm() {
		return minTerm;
	}

	public ByteBuffer maxTerm() {
		return maxTerm;
	}

	public ByteBuffer minKey() {
		return minKey;
	}

	public ByteBuffer maxKey() {
		return maxKey;
	}

	public OnDiskIndex.DataTerm min() {
		return null;
	}

	public OnDiskIndex.DataTerm max() {
		OnDiskIndex.DataBlock block = dataLevel.getBlock(((dataLevel.blockCount) - 1));
		return null;
	}

	public RangeIterator<Long, Token> search(Expression exp) {
		assert mode.supports(exp.getOp());
		if ((((exp.getOp()) == (PREFIX)) && ((mode) == (CONTAINS))) && (!(hasMarkedPartials)))
			throw new UnsupportedOperationException("prefix queries in CONTAINS mode are not supported by this index");

		if ((exp.getOp()) == (EQ)) {
			OnDiskIndex.DataTerm term = getTerm(exp.lower.value);
			return term == null ? null : term.getTokens();
		}
		final Expression expression = ((exp.getOp()) != (NOT_EQ)) ? exp : new Expression(exp).setOp(RANGE).setLower(new Expression.Bound(minTerm, true)).setUpper(new Expression.Bound(maxTerm, true)).addExclusion(exp.lower.value);
		List<ByteBuffer> exclusions = new ArrayList<>(expression.exclusions.size());
		Iterables.addAll(exclusions, expression.exclusions.stream().filter(( exclusion) -> {
			return (!(((expression.lower) != null) && ((comparator.compare(exclusion, expression.lower.value)) < 0))) && (!(((expression.upper) != null) && ((comparator.compare(exclusion, expression.upper.value)) > 0)));
		}).collect(Collectors.toList()));
		Collections.sort(exclusions, comparator);
		if ((exclusions.size()) == 0)
			return searchRange(expression);

		List<Expression> ranges = new ArrayList<>(exclusions.size());
		Iterator<ByteBuffer> exclusionsIterator = exclusions.iterator();
		Expression.Bound min = expression.lower;
		Expression.Bound max = null;
		while (exclusionsIterator.hasNext()) {
			max = new Expression.Bound(exclusionsIterator.next(), false);
			ranges.add(new Expression(expression).setOp(RANGE).setLower(min).setUpper(max));
			min = max;
		} 
		assert max != null;
		ranges.add(new Expression(expression).setOp(RANGE).setLower(max).setUpper(expression.upper));
		RangeUnionIterator.Builder<Long, Token> builder = RangeUnionIterator.builder();
		for (Expression e : ranges) {
			@SuppressWarnings("resource")
			RangeIterator<Long, Token> range = searchRange(e);
			if (range != null)
				builder.add(range);

		}
		return builder.build();
	}

	private RangeIterator<Long, Token> searchRange(Expression range) {
		Expression.Bound lower = range.lower;
		Expression.Bound upper = range.upper;
		int lowerBlock = (lower == null) ? 0 : getDataBlock(lower.value);
		int upperBlock = (upper == null) ? (dataLevel.blockCount) - 1 : (lower != null) && ((comparator.compare(lower.value, upper.value)) == 0) ? lowerBlock : getDataBlock(upper.value);
		return (((mode) != (SPARSE)) || (lowerBlock == upperBlock)) || ((upperBlock - lowerBlock) <= 1) ? searchPoint(lowerBlock, range) : searchRange(lowerBlock, lower, upperBlock, upper);
	}

	private RangeIterator<Long, Token> searchRange(int lowerBlock, Expression.Bound lower, int upperBlock, Expression.Bound upper) {
		OnDiskBlock.SearchResult<OnDiskIndex.DataTerm> lowerPosition = (lower == null) ? null : searchIndex(lower.value, lowerBlock);
		OnDiskBlock.SearchResult<OnDiskIndex.DataTerm> upperPosition = (upper == null) ? null : searchIndex(upper.value, upperBlock);
		RangeUnionIterator.Builder<Long, Token> builder = RangeUnionIterator.builder();
		int firstFullBlockIdx = lowerBlock;
		int lastFullBlockIdx = upperBlock;
		if ((lowerPosition != null) && (((lowerPosition.index) > 0) || (!(lower.inclusive)))) {
			OnDiskIndex.DataBlock block = dataLevel.getBlock(lowerBlock);
			int start = ((lower.inclusive) || ((lowerPosition.cmp) != 0)) ? lowerPosition.index : (lowerPosition.index) + 1;
			firstFullBlockIdx = lowerBlock + 1;
		}
		if (upperPosition != null) {
			OnDiskIndex.DataBlock block = dataLevel.getBlock(upperBlock);
		}
		int totalSuperBlocks = (lastFullBlockIdx - firstFullBlockIdx) / (OnDiskIndexBuilder.SUPER_BLOCK_SIZE);
		if (totalSuperBlocks == 0) {
			for (int i = firstFullBlockIdx; i <= lastFullBlockIdx; i++)
				builder.add(dataLevel.getBlock(i).getBlockIndex().iterator(keyFetcher));

			return builder.build();
		}
		int superBlockAlignedStart = (firstFullBlockIdx == 0) ? 0 : ((int) (FBUtilities.align(firstFullBlockIdx, OnDiskIndexBuilder.SUPER_BLOCK_SIZE)));
		for (int blockIdx = firstFullBlockIdx; blockIdx < (Math.min(superBlockAlignedStart, lastFullBlockIdx)); blockIdx++)
			builder.add(getBlockIterator(blockIdx));

		int superBlockIdx = superBlockAlignedStart / (OnDiskIndexBuilder.SUPER_BLOCK_SIZE);
		for (int offset = 0; offset < (totalSuperBlocks - 1); offset++)
			builder.add(dataLevel.getSuperBlock((superBlockIdx++)).iterator());

		int lastCoveredBlock = superBlockIdx * (OnDiskIndexBuilder.SUPER_BLOCK_SIZE);
		for (int offset = 0; offset <= (lastFullBlockIdx - lastCoveredBlock); offset++)
			builder.add(getBlockIterator((lastCoveredBlock + offset)));

		return builder.build();
	}

	private RangeIterator<Long, Token> searchPoint(int lowerBlock, Expression expression) {
		Iterator<OnDiskIndex.DataTerm> terms = new OnDiskIndex.TermIterator(lowerBlock, expression, OnDiskIndex.IteratorOrder.DESC);
		RangeUnionIterator.Builder<Long, Token> builder = RangeUnionIterator.builder();
		while (terms.hasNext()) {
			try {
				builder.add(terms.next().getTokens());
			} finally {
				expression.checkpoint();
			}
		} 
		return builder.build();
	}

	private RangeIterator<Long, Token> getBlockIterator(int blockIdx) {
		OnDiskIndex.DataBlock block = dataLevel.getBlock(blockIdx);
		return null;
	}

	public Iterator<OnDiskIndex.DataTerm> iteratorAt(ByteBuffer query, OnDiskIndex.IteratorOrder order, boolean inclusive) {
		Expression e = new Expression("", comparator);
		Expression.Bound bound = new Expression.Bound(query, inclusive);
		switch (order) {
			case DESC :
				e.setLower(bound);
				break;
			case ASC :
				e.setUpper(bound);
				break;
			default :
				throw new IllegalArgumentException(("Unknown order: " + order));
		}
		return new OnDiskIndex.TermIterator(((levels.length) == 0 ? 0 : getBlockIdx(findPointer(query), query)), e, order);
	}

	private int getDataBlock(ByteBuffer query) {
		return (levels.length) == 0 ? 0 : getBlockIdx(findPointer(query), query);
	}

	public Iterator<OnDiskIndex.DataTerm> iterator() {
		return new OnDiskIndex.TermIterator(0, new Expression("", comparator), OnDiskIndex.IteratorOrder.DESC);
	}

	public void close() throws IOException {
		FileUtils.closeQuietly(indexFile);
	}

	private OnDiskIndex.PointerTerm findPointer(ByteBuffer query) {
		OnDiskIndex.PointerTerm ptr = null;
		for (OnDiskIndex.PointerLevel level : levels) {
			if ((ptr = level.getPointer(ptr, query)) == null)
				return null;

		}
		return ptr;
	}

	private OnDiskIndex.DataTerm getTerm(ByteBuffer query) {
		OnDiskBlock.SearchResult<OnDiskIndex.DataTerm> term = searchIndex(query, getDataBlock(query));
		return (term.cmp) == 0 ? term.result : null;
	}

	private OnDiskBlock.SearchResult<OnDiskIndex.DataTerm> searchIndex(ByteBuffer query, int blockIdx) {
		return dataLevel.getBlock(blockIdx).search(comparator, query);
	}

	private int getBlockIdx(OnDiskIndex.PointerTerm ptr, ByteBuffer query) {
		int blockIdx = 0;
		if (ptr != null) {
			int cmp = ptr.compareTo(comparator, query);
			blockIdx = ((cmp == 0) || (cmp > 0)) ? ptr.getBlock() : (ptr.getBlock()) + 1;
		}
		return blockIdx;
	}

	protected class PointerLevel extends OnDiskIndex.Level<OnDiskIndex.PointerBlock> {
		public PointerLevel(long offset, int count) {
			super(offset, count);
		}

		public OnDiskIndex.PointerTerm getPointer(OnDiskIndex.PointerTerm parent, ByteBuffer query) {
			return getBlock(getBlockIdx(parent, query)).search(comparator, query).result;
		}

		protected OnDiskIndex.PointerBlock cast(MappedBuffer block) {
			return new OnDiskIndex.PointerBlock(block);
		}
	}

	protected class DataLevel extends OnDiskIndex.Level<OnDiskIndex.DataBlock> {
		protected final int superBlockCnt;

		protected final long superBlocksOffset;

		public DataLevel(long offset, int count) {
			super(offset, count);
			long baseOffset = (blockOffsets) + ((blockCount) * 8);
			superBlockCnt = indexFile.getInt(baseOffset);
			superBlocksOffset = baseOffset + 4;
		}

		protected OnDiskIndex.DataBlock cast(MappedBuffer block) {
			return new OnDiskIndex.DataBlock(block);
		}

		public OnDiskIndex.OnDiskSuperBlock getSuperBlock(int idx) {
			assert idx < (superBlockCnt) : String.format("requested index %d is greater than super block count %d", idx, superBlockCnt);
			long blockOffset = indexFile.getLong(((superBlocksOffset) + (idx * 8)));
			return new OnDiskIndex.OnDiskSuperBlock(indexFile.duplicate().position(blockOffset));
		}
	}

	protected class OnDiskSuperBlock {
		private final TokenTree tokenTree;

		public OnDiskSuperBlock(MappedBuffer buffer) {
			tokenTree = new TokenTree(descriptor, buffer);
		}

		public RangeIterator<Long, Token> iterator() {
			return tokenTree.iterator(keyFetcher);
		}
	}

	protected abstract class Level<T extends OnDiskBlock> {
		protected final long blockOffsets;

		protected final int blockCount;

		public Level(long offsets, int count) {
			this.blockOffsets = offsets;
			this.blockCount = count;
		}

		public T getBlock(int idx) throws FSReadError {
			assert (idx >= 0) && (idx < (blockCount));
			long blockOffset = indexFile.getLong(((blockOffsets) + (idx * 8)));
			return cast(indexFile.duplicate().position(blockOffset));
		}

		protected abstract T cast(MappedBuffer block);
	}

	protected class DataBlock extends OnDiskBlock<OnDiskIndex.DataTerm> {
		public DataBlock(MappedBuffer data) {
			super(descriptor, data, DATA);
		}

		protected OnDiskIndex.DataTerm cast(MappedBuffer data) {
			return new OnDiskIndex.DataTerm(data, termSize, getBlockIndex());
		}

		public RangeIterator<Long, Token> getRange(int start, int end) {
			RangeUnionIterator.Builder<Long, Token> builder = RangeUnionIterator.builder();
			NavigableMap<Long, Token> sparse = new TreeMap<>();
			for (int i = start; i < end; i++) {
				OnDiskIndex.DataTerm term = getTerm(i);
				if (term.isSparse()) {
					NavigableMap<Long, Token> tokens = term.getSparseTokens();
					for (Map.Entry<Long, Token> t : tokens.entrySet()) {
						Token token = sparse.get(t.getKey());
						if (token == null)
							sparse.put(t.getKey(), t.getValue());
						else
							token.merge(t.getValue());

					}
				}else {
					builder.add(term.getTokens());
				}
			}
			OnDiskIndex.PrefetchedTokensIterator prefetched = (sparse.isEmpty()) ? null : new OnDiskIndex.PrefetchedTokensIterator(sparse);
			if ((builder.rangeCount()) == 0)
				return prefetched;

			builder.add(prefetched);
			return builder.build();
		}
	}

	protected class PointerBlock extends OnDiskBlock<OnDiskIndex.PointerTerm> {
		public PointerBlock(MappedBuffer block) {
			super(descriptor, block, POINTER);
		}

		protected OnDiskIndex.PointerTerm cast(MappedBuffer data) {
			return new OnDiskIndex.PointerTerm(data, termSize, hasMarkedPartials);
		}
	}

	public class DataTerm extends Term implements Comparable<OnDiskIndex.DataTerm> {
		private final TokenTree perBlockIndex;

		protected DataTerm(MappedBuffer content, OnDiskIndexBuilder.TermSize size, TokenTree perBlockIndex) {
			super(content, size, OnDiskIndex.this.hasMarkedPartials);
			this.perBlockIndex = perBlockIndex;
		}

		public RangeIterator<Long, Token> getTokens() {
			final long blockEnd = FBUtilities.align(content.position(), OnDiskIndexBuilder.BLOCK_SIZE);
			if (isSparse())
				return new OnDiskIndex.PrefetchedTokensIterator(getSparseTokens());

			long offset = (blockEnd + 4) + (content.getInt(((getDataOffset()) + 1)));
			return new TokenTree(descriptor, indexFile.duplicate().position(offset)).iterator(keyFetcher);
		}

		public boolean isSparse() {
			return (content.get(getDataOffset())) > 0;
		}

		public NavigableMap<Long, Token> getSparseTokens() {
			long ptrOffset = getDataOffset();
			byte size = content.get(ptrOffset);
			assert size > 0;
			NavigableMap<Long, Token> individualTokens = new TreeMap<>();
			for (int i = 0; i < size; i++) {
				Token token = perBlockIndex.get(content.getLong(((ptrOffset + 1) + (8 * i))), keyFetcher);
				assert token != null;
				individualTokens.put(token.get(), token);
			}
			return individualTokens;
		}

		public int compareTo(OnDiskIndex.DataTerm other) {
			return other == null ? 1 : compareTo(comparator, other.getTerm());
		}
	}

	protected static class PointerTerm extends Term {
		public PointerTerm(MappedBuffer content, OnDiskIndexBuilder.TermSize size, boolean hasMarkedPartials) {
			super(content, size, hasMarkedPartials);
		}

		public int getBlock() {
			return content.getInt(getDataOffset());
		}
	}

	private static class PrefetchedTokensIterator extends RangeIterator<Long, Token> {
		private final NavigableMap<Long, Token> tokens;

		private PeekingIterator<Token> currentIterator;

		public PrefetchedTokensIterator(NavigableMap<Long, Token> tokens) {
			super(tokens.firstKey(), tokens.lastKey(), tokens.size());
			this.tokens = tokens;
			this.currentIterator = Iterators.peekingIterator(tokens.values().iterator());
		}

		protected Token computeNext() {
			return ((currentIterator) != null) && (currentIterator.hasNext()) ? currentIterator.next() : endOfData();
		}

		protected void performSkipTo(Long nextToken) {
			currentIterator = Iterators.peekingIterator(tokens.tailMap(nextToken, true).values().iterator());
		}

		public void close() throws IOException {
			endOfData();
		}
	}

	public AbstractType<?> getComparator() {
		return comparator;
	}

	public String getIndexPath() {
		return indexPath;
	}

	private class TermIterator extends AbstractIterator<OnDiskIndex.DataTerm> {
		private final Expression e;

		private final OnDiskIndex.IteratorOrder order;

		protected OnDiskBlock<OnDiskIndex.DataTerm> currentBlock;

		protected int blockIndex;

		protected int offset;

		private boolean checkLower = true;

		private boolean checkUpper = true;

		public TermIterator(int startBlock, Expression expression, OnDiskIndex.IteratorOrder order) {
			this.e = expression;
			this.order = order;
			this.blockIndex = startBlock;
			nextBlock();
		}

		protected OnDiskIndex.DataTerm computeNext() {
			for (; ;) {
				if ((currentBlock) == null)
					return endOfData();

				nextBlock();
			}
		}

		protected void nextBlock() {
			currentBlock = null;
			if (((blockIndex) < 0) || ((blockIndex) >= (dataLevel.blockCount)))
				return;

			currentBlock = dataLevel.getBlock(nextBlockIndex());
		}

		protected int nextBlockIndex() {
			int current = blockIndex;
			blockIndex += order.step;
			return current;
		}

		protected int nextOffset() {
			int current = offset;
			offset += order.step;
			return current;
		}
	}
}

