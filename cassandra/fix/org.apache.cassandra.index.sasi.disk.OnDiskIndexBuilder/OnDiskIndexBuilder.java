

import com.carrotsearch.hppc.LongArrayList;
import com.carrotsearch.hppc.LongSet;
import com.carrotsearch.hppc.ShortArrayList;
import com.google.common.collect.AbstractIterator;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.db.marshal.DateType;
import org.apache.cassandra.db.marshal.DoubleType;
import org.apache.cassandra.db.marshal.FloatType;
import org.apache.cassandra.db.marshal.Int32Type;
import org.apache.cassandra.db.marshal.LongType;
import org.apache.cassandra.db.marshal.TimeUUIDType;
import org.apache.cassandra.db.marshal.TimestampType;
import org.apache.cassandra.db.marshal.UUIDType;
import org.apache.cassandra.dht.Token;
import org.apache.cassandra.index.sasi.disk.Descriptor;
import org.apache.cassandra.index.sasi.disk.DynamicTokenTreeBuilder;
import org.apache.cassandra.index.sasi.disk.TokenTreeBuilder;
import org.apache.cassandra.index.sasi.plan.Expression;
import org.apache.cassandra.index.sasi.plan.Expression.Op;
import org.apache.cassandra.index.sasi.sa.IndexedTerm;
import org.apache.cassandra.index.sasi.sa.TermIterator;
import org.apache.cassandra.io.FSWriteError;
import org.apache.cassandra.io.util.BufferedDataOutputStreamPlus;
import org.apache.cassandra.io.util.DataOutputBuffer;
import org.apache.cassandra.io.util.DataOutputBufferFixed;
import org.apache.cassandra.io.util.DataOutputPlus;
import org.apache.cassandra.io.util.FileUtils;
import org.apache.cassandra.io.util.SequentialWriter;
import org.apache.cassandra.io.util.SequentialWriterOption;
import org.apache.cassandra.utils.ByteBufferUtil;
import org.apache.cassandra.utils.FBUtilities;
import org.apache.cassandra.utils.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.cassandra.index.sasi.plan.Expression.Op.EQ;
import static org.apache.cassandra.index.sasi.plan.Expression.Op.NOT_EQ;
import static org.apache.cassandra.index.sasi.plan.Expression.Op.RANGE;


public class OnDiskIndexBuilder {
	private static final Logger logger = LoggerFactory.getLogger(OnDiskIndexBuilder.class);

	public enum Mode {

		SPARSE(EnumSet.of(EQ, NOT_EQ, RANGE));
		Set<Expression.Op> supportedOps;

		Mode(Set<Expression.Op> ops) {
			supportedOps = ops;
		}

		public static OnDiskIndexBuilder.Mode mode(String mode) {
			return OnDiskIndexBuilder.Mode.valueOf(mode.toUpperCase());
		}

		public boolean supports(Expression.Op op) {
			return supportedOps.contains(op);
		}
	}

	public enum TermSize {

		INT(4),
		LONG(8),
		UUID(16),
		VARIABLE((-1));
		public final int size;

		TermSize(int size) {
			this.size = size;
		}

		public boolean isConstant() {
			return (this) != (OnDiskIndexBuilder.TermSize.VARIABLE);
		}

		public static OnDiskIndexBuilder.TermSize of(int size) {
			switch (size) {
				case -1 :
					return OnDiskIndexBuilder.TermSize.VARIABLE;
				case 4 :
					return OnDiskIndexBuilder.TermSize.INT;
				case 8 :
					return OnDiskIndexBuilder.TermSize.LONG;
				case 16 :
					return OnDiskIndexBuilder.TermSize.UUID;
				default :
					throw new IllegalStateException(("unknown state: " + size));
			}
		}

		public static OnDiskIndexBuilder.TermSize sizeOf(AbstractType<?> comparator) {
			if ((comparator instanceof Int32Type) || (comparator instanceof FloatType))
				return OnDiskIndexBuilder.TermSize.INT;

			if ((((comparator instanceof LongType) || (comparator instanceof DoubleType)) || (comparator instanceof TimestampType)) || (comparator instanceof DateType))
				return OnDiskIndexBuilder.TermSize.LONG;

			if ((comparator instanceof TimeUUIDType) || (comparator instanceof UUIDType))
				return OnDiskIndexBuilder.TermSize.UUID;

			return OnDiskIndexBuilder.TermSize.VARIABLE;
		}
	}

	public static final int BLOCK_SIZE = 4096;

	public static final int MAX_TERM_SIZE = 1024;

	public static final int SUPER_BLOCK_SIZE = 64;

	public static final int IS_PARTIAL_BIT = 15;

	private static final SequentialWriterOption WRITER_OPTION = SequentialWriterOption.newBuilder().bufferSize(OnDiskIndexBuilder.BLOCK_SIZE).build();

	private final List<OnDiskIndexBuilder.MutableLevel<OnDiskIndexBuilder.InMemoryPointerTerm>> levels = new ArrayList<>();

	private OnDiskIndexBuilder.MutableLevel<OnDiskIndexBuilder.InMemoryDataTerm> dataLevel;

	private final OnDiskIndexBuilder.TermSize termSize;

	private final AbstractType<?> keyComparator;

	private final AbstractType<?> termComparator;

	private final Map<ByteBuffer, TokenTreeBuilder> terms;

	private final OnDiskIndexBuilder.Mode mode;

	private final boolean marksPartials;

	private ByteBuffer minKey;

	private ByteBuffer maxKey;

	private long estimatedBytes;

	public OnDiskIndexBuilder(AbstractType<?> keyComparator, AbstractType<?> comparator, OnDiskIndexBuilder.Mode mode) {
		this(keyComparator, comparator, mode, true);
	}

	public OnDiskIndexBuilder(AbstractType<?> keyComparator, AbstractType<?> comparator, OnDiskIndexBuilder.Mode mode, boolean marksPartials) {
		this.keyComparator = keyComparator;
		this.termComparator = comparator;
		this.terms = new HashMap<>();
		this.termSize = OnDiskIndexBuilder.TermSize.sizeOf(comparator);
		this.mode = mode;
		this.marksPartials = marksPartials;
	}

	public OnDiskIndexBuilder add(ByteBuffer term, DecoratedKey key, long keyPosition) {
		if ((term.remaining()) >= (OnDiskIndexBuilder.MAX_TERM_SIZE)) {
			OnDiskIndexBuilder.logger.error("Rejecting value (value size {}, maximum size {}).", FBUtilities.prettyPrintMemory(term.remaining()), FBUtilities.prettyPrintMemory(Short.MAX_VALUE));
			return this;
		}
		TokenTreeBuilder tokens = terms.get(term);
		if (tokens == null) {
			terms.put(term, (tokens = new DynamicTokenTreeBuilder()));
			estimatedBytes += (64 + 48) + (term.remaining());
		}
		tokens.add(((Long) (key.getToken().getTokenValue())), keyPosition);
		minKey = (((minKey) == null) || ((keyComparator.compare(minKey, key.getKey())) > 0)) ? key.getKey() : minKey;
		maxKey = (((maxKey) == null) || ((keyComparator.compare(maxKey, key.getKey())) < 0)) ? key.getKey() : maxKey;
		estimatedBytes += (60 + 40) + 8;
		return this;
	}

	public long estimatedMemoryUse() {
		return estimatedBytes;
	}

	private void addTerm(OnDiskIndexBuilder.InMemoryDataTerm term, SequentialWriter out) throws IOException {
		OnDiskIndexBuilder.InMemoryPointerTerm ptr = dataLevel.add(term);
		if (ptr == null)
			return;

		int levelIdx = 0;
		for (; ;) {
			OnDiskIndexBuilder.MutableLevel<OnDiskIndexBuilder.InMemoryPointerTerm> level = getIndexLevel((levelIdx++), out);
			if ((ptr = level.add(ptr)) == null)
				break;

		}
	}

	public boolean isEmpty() {
		return terms.isEmpty();
	}

	public void finish(Pair<ByteBuffer, ByteBuffer> range, File file, TermIterator terms) {
		finish(Descriptor.CURRENT, range, file, terms);
	}

	public boolean finish(File indexFile) throws FSWriteError {
		return finish(Descriptor.CURRENT, indexFile);
	}

	@com.google.common.annotations.VisibleForTesting
	protected boolean finish(Descriptor descriptor, File file) throws FSWriteError {
		if (terms.isEmpty()) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				throw new FSWriteError(e, file);
			}
			return false;
		}
		for (Map.Entry<ByteBuffer, TokenTreeBuilder> term : terms.entrySet()) {
		}
		return true;
	}

	@SuppressWarnings("resource")
	protected void finish(Descriptor descriptor, Pair<ByteBuffer, ByteBuffer> range, File file, TermIterator terms) {
		SequentialWriter out = null;
		try {
			out = new SequentialWriter(file, OnDiskIndexBuilder.WRITER_OPTION);
			out.writeUTF(descriptor.version.toString());
			out.writeShort(termSize.size);
			ByteBufferUtil.writeWithShortLength(terms.minTerm(), out);
			ByteBufferUtil.writeWithShortLength(terms.maxTerm(), out);
			ByteBufferUtil.writeWithShortLength(range.left, out);
			ByteBufferUtil.writeWithShortLength(range.right, out);
			out.writeUTF(mode.toString());
			out.writeBoolean(marksPartials);
			out.skipBytes(((int) ((OnDiskIndexBuilder.BLOCK_SIZE) - (out.position()))));
			dataLevel = ((mode) == (OnDiskIndexBuilder.Mode.SPARSE)) ? new OnDiskIndexBuilder.DataBuilderLevel(out, new OnDiskIndexBuilder.MutableDataBlock(termComparator, mode)) : new OnDiskIndexBuilder.MutableLevel<>(out, new OnDiskIndexBuilder.MutableDataBlock(termComparator, mode));
			while (terms.hasNext()) {
				Pair<IndexedTerm, TokenTreeBuilder> term = terms.next();
				addTerm(new OnDiskIndexBuilder.InMemoryDataTerm(term.left, term.right), out);
			} 
			dataLevel.finalFlush();
			for (OnDiskIndexBuilder.MutableLevel l : levels)
				l.flush();

			final long levelIndexPosition = out.position();
			out.writeInt(levels.size());
			for (int i = (levels.size()) - 1; i >= 0; i--)
				levels.get(i).flushMetadata();

			dataLevel.flushMetadata();
			out.writeLong(levelIndexPosition);
			out.sync();
		} catch (IOException e) {
			throw new FSWriteError(e, file);
		} finally {
			FileUtils.closeQuietly(out);
		}
	}

	private OnDiskIndexBuilder.MutableLevel<OnDiskIndexBuilder.InMemoryPointerTerm> getIndexLevel(int idx, SequentialWriter out) {
		if ((levels.size()) == 0)
			levels.add(new OnDiskIndexBuilder.MutableLevel<>(out, new OnDiskIndexBuilder.MutableBlock<>()));

		if (((levels.size()) - 1) < idx) {
			int toAdd = idx - ((levels.size()) - 1);
			for (int i = 0; i < toAdd; i++)
				levels.add(new OnDiskIndexBuilder.MutableLevel<>(out, new OnDiskIndexBuilder.MutableBlock<>()));

		}
		return levels.get(idx);
	}

	protected static void alignToBlock(SequentialWriter out) throws IOException {
		long endOfBlock = out.position();
		if ((endOfBlock & ((OnDiskIndexBuilder.BLOCK_SIZE) - 1)) != 0)
			out.skipBytes(((int) ((FBUtilities.align(endOfBlock, OnDiskIndexBuilder.BLOCK_SIZE)) - endOfBlock)));

	}

	private class InMemoryTerm {
		protected final IndexedTerm term;

		public InMemoryTerm(IndexedTerm term) {
			this.term = term;
		}

		public int serializedSize() {
			return (termSize.isConstant() ? 0 : 2) + (term.getBytes().remaining());
		}

		public void serialize(DataOutputPlus out) throws IOException {
			if (termSize.isConstant()) {
				out.write(term.getBytes());
			}else {
				out.writeShort(((term.getBytes().remaining()) | (((marksPartials) && (term.isPartial()) ? 1 : 0) << (OnDiskIndexBuilder.IS_PARTIAL_BIT))));
				out.write(term.getBytes());
			}
		}
	}

	private class InMemoryPointerTerm extends OnDiskIndexBuilder.InMemoryTerm {
		protected final int blockCnt;

		public InMemoryPointerTerm(IndexedTerm term, int blockCnt) {
			super(term);
			this.blockCnt = blockCnt;
		}

		public int serializedSize() {
			return (super.serializedSize()) + 4;
		}

		public void serialize(DataOutputPlus out) throws IOException {
			super.serialize(out);
			out.writeInt(blockCnt);
		}
	}

	private class InMemoryDataTerm extends OnDiskIndexBuilder.InMemoryTerm {
		private final TokenTreeBuilder keys;

		public InMemoryDataTerm(IndexedTerm term, TokenTreeBuilder keys) {
			super(term);
			this.keys = keys;
		}
	}

	private class MutableLevel<T extends OnDiskIndexBuilder.InMemoryTerm> {
		private final LongArrayList blockOffsets = new LongArrayList();

		protected final SequentialWriter out;

		private final OnDiskIndexBuilder.MutableBlock<T> inProcessBlock;

		private OnDiskIndexBuilder.InMemoryPointerTerm lastTerm;

		public MutableLevel(SequentialWriter out, OnDiskIndexBuilder.MutableBlock<T> block) {
			this.out = out;
			this.inProcessBlock = block;
		}

		public OnDiskIndexBuilder.InMemoryPointerTerm add(T term) throws IOException {
			OnDiskIndexBuilder.InMemoryPointerTerm toPromote = null;
			if (!(inProcessBlock.hasSpaceFor(term))) {
				flush();
				toPromote = lastTerm;
			}
			inProcessBlock.add(term);
			lastTerm = new OnDiskIndexBuilder.InMemoryPointerTerm(term.term, blockOffsets.size());
			return toPromote;
		}

		public void flush() throws IOException {
			blockOffsets.add(out.position());
			inProcessBlock.flushAndClear(out);
		}

		public void finalFlush() throws IOException {
			flush();
		}

		public void flushMetadata() throws IOException {
			flushMetadata(blockOffsets);
		}

		protected void flushMetadata(LongArrayList longArrayList) throws IOException {
			out.writeInt(longArrayList.size());
			for (int i = 0; i < (longArrayList.size()); i++)
				out.writeLong(longArrayList.get(i));

		}
	}

	private class DataBuilderLevel extends OnDiskIndexBuilder.MutableLevel<OnDiskIndexBuilder.InMemoryDataTerm> {
		private final LongArrayList superBlockOffsets = new LongArrayList();

		private int dataBlocksCnt;

		private TokenTreeBuilder superBlockTree;

		public DataBuilderLevel(SequentialWriter out, OnDiskIndexBuilder.MutableBlock<OnDiskIndexBuilder.InMemoryDataTerm> block) {
			super(out, block);
			superBlockTree = new DynamicTokenTreeBuilder();
		}

		public OnDiskIndexBuilder.InMemoryPointerTerm add(OnDiskIndexBuilder.InMemoryDataTerm term) throws IOException {
			OnDiskIndexBuilder.InMemoryPointerTerm ptr = super.add(term);
			if (ptr != null) {
				(dataBlocksCnt)++;
				flushSuperBlock(false);
			}
			superBlockTree.add(term.keys);
			return ptr;
		}

		public void flushSuperBlock(boolean force) throws IOException {
			if (((dataBlocksCnt) == (OnDiskIndexBuilder.SUPER_BLOCK_SIZE)) || (force && (!(superBlockTree.isEmpty())))) {
				superBlockOffsets.add(out.position());
				superBlockTree.finish().write(out);
				OnDiskIndexBuilder.alignToBlock(out);
				dataBlocksCnt = 0;
				superBlockTree = new DynamicTokenTreeBuilder();
			}
		}

		public void finalFlush() throws IOException {
			super.flush();
			flushSuperBlock(true);
		}

		public void flushMetadata() throws IOException {
			super.flushMetadata();
			flushMetadata(superBlockOffsets);
		}
	}

	private static class MutableBlock<T extends OnDiskIndexBuilder.InMemoryTerm> {
		protected final DataOutputBufferFixed buffer;

		protected final ShortArrayList offsets;

		public MutableBlock() {
			buffer = new DataOutputBufferFixed(OnDiskIndexBuilder.BLOCK_SIZE);
			offsets = new ShortArrayList();
		}

		public final void add(T term) throws IOException {
			offsets.add(((short) (buffer.position())));
			addInternal(term);
		}

		protected void addInternal(T term) throws IOException {
			term.serialize(buffer);
		}

		public boolean hasSpaceFor(T element) {
			return (sizeAfter(element)) < (OnDiskIndexBuilder.BLOCK_SIZE);
		}

		protected int sizeAfter(T element) {
			return ((getWatermark()) + 4) + (element.serializedSize());
		}

		protected int getWatermark() {
			return (4 + ((offsets.size()) * 2)) + ((int) (buffer.position()));
		}

		public void flushAndClear(SequentialWriter out) throws IOException {
			out.writeInt(offsets.size());
			for (int i = 0; i < (offsets.size()); i++)
				out.writeShort(offsets.get(i));

			out.write(buffer.buffer());
			OnDiskIndexBuilder.alignToBlock(out);
			offsets.clear();
			buffer.clear();
		}
	}

	private static class MutableDataBlock extends OnDiskIndexBuilder.MutableBlock<OnDiskIndexBuilder.InMemoryDataTerm> {
		private static final int MAX_KEYS_SPARSE = 5;

		private final AbstractType<?> comparator;

		private final OnDiskIndexBuilder.Mode mode;

		private int offset = 0;

		private final List<TokenTreeBuilder> containers = new ArrayList<>();

		private TokenTreeBuilder combinedIndex;

		public MutableDataBlock(AbstractType<?> comparator, OnDiskIndexBuilder.Mode mode) {
			this.comparator = comparator;
			this.mode = mode;
			this.combinedIndex = initCombinedIndex();
		}

		protected void addInternal(OnDiskIndexBuilder.InMemoryDataTerm term) throws IOException {
			TokenTreeBuilder keys = term.keys;
			if ((mode) == (OnDiskIndexBuilder.Mode.SPARSE)) {
				if ((keys.getTokenCount()) > (OnDiskIndexBuilder.MutableDataBlock.MAX_KEYS_SPARSE))
					throw new IOException(String.format("Term - '%s' belongs to more than %d keys in %s mode, which is not allowed.", comparator.getString(term.term.getBytes()), OnDiskIndexBuilder.MutableDataBlock.MAX_KEYS_SPARSE, mode.name()));

				writeTerm(term, keys);
			}else {
				writeTerm(term, offset);
				offset += keys.serializedSize();
				containers.add(keys);
			}
			if ((mode) == (OnDiskIndexBuilder.Mode.SPARSE))
				combinedIndex.add(keys);

		}

		protected int sizeAfter(OnDiskIndexBuilder.InMemoryDataTerm element) {
			return (super.sizeAfter(element)) + (ptrLength(element));
		}

		public void flushAndClear(SequentialWriter out) throws IOException {
			super.flushAndClear(out);
			out.writeInt(((mode) == (OnDiskIndexBuilder.Mode.SPARSE) ? offset : -1));
			if ((containers.size()) > 0) {
				for (TokenTreeBuilder tokens : containers)
					tokens.write(out);

			}
			if (((mode) == (OnDiskIndexBuilder.Mode.SPARSE)) && ((combinedIndex) != null))
				combinedIndex.finish().write(out);

			OnDiskIndexBuilder.alignToBlock(out);
			containers.clear();
			combinedIndex = initCombinedIndex();
			offset = 0;
		}

		private int ptrLength(OnDiskIndexBuilder.InMemoryDataTerm term) {
			return (term.keys.getTokenCount()) > 5 ? 5 : 1 + (8 * ((int) (term.keys.getTokenCount())));
		}

		private void writeTerm(OnDiskIndexBuilder.InMemoryTerm term, TokenTreeBuilder keys) throws IOException {
			term.serialize(buffer);
			buffer.writeByte(((byte) (keys.getTokenCount())));
			for (Pair<Long, LongSet> key : keys)
				buffer.writeLong(key.left);

		}

		private void writeTerm(OnDiskIndexBuilder.InMemoryTerm term, int offset) throws IOException {
			term.serialize(buffer);
			buffer.writeByte(0);
			buffer.writeInt(offset);
		}

		private TokenTreeBuilder initCombinedIndex() {
			return (mode) == (OnDiskIndexBuilder.Mode.SPARSE) ? new DynamicTokenTreeBuilder() : null;
		}
	}
}

