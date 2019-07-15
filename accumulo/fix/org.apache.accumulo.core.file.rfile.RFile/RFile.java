

import com.google.common.base.Preconditions;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.accumulo.core.client.SampleNotPresentException;
import org.apache.accumulo.core.client.sample.Sampler;
import org.apache.accumulo.core.client.sample.SamplerConfiguration;
import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.conf.DefaultConfiguration;
import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.core.data.ArrayByteSequence;
import org.apache.accumulo.core.data.ByteSequence;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.file.FileSKVIterator;
import org.apache.accumulo.core.file.FileSKVWriter;
import org.apache.accumulo.core.file.NoSuchMetaStoreException;
import org.apache.accumulo.core.file.blockfile.ABlockReader;
import org.apache.accumulo.core.file.blockfile.ABlockWriter;
import org.apache.accumulo.core.file.blockfile.BlockFileReader;
import org.apache.accumulo.core.file.blockfile.BlockFileWriter;
import org.apache.accumulo.core.file.rfile.BlockIndex;
import org.apache.accumulo.core.file.rfile.KeyShortener;
import org.apache.accumulo.core.file.rfile.MetricsGatherer;
import org.apache.accumulo.core.file.rfile.MultiLevelIndex;
import org.apache.accumulo.core.file.rfile.MultiLevelIndex.IndexEntry;
import org.apache.accumulo.core.file.rfile.RelativeKey;
import org.apache.accumulo.core.file.rfile.bcfile.MetaBlockDoesNotExist;
import org.apache.accumulo.core.iterators.IterationInterruptedException;
import org.apache.accumulo.core.iterators.IteratorEnvironment;
import org.apache.accumulo.core.iterators.SortedKeyValueIterator;
import org.apache.accumulo.core.iterators.system.HeapIterator;
import org.apache.accumulo.core.iterators.system.InterruptibleIterator;
import org.apache.accumulo.core.iterators.system.LocalityGroupIterator;
import org.apache.accumulo.core.sample.impl.SamplerConfigurationImpl;
import org.apache.accumulo.core.util.MutableByteSequence;
import org.apache.commons.lang.mutable.MutableLong;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class RFile {
	public static final String EXTENSION = "rf";

	private static final Logger log = LoggerFactory.getLogger(RFile.class);

	private RFile() {
	}

	private static final int RINDEX_MAGIC = 543388788;

	static final int RINDEX_VER_8 = 8;

	static final int RINDEX_VER_7 = 7;

	static final int RINDEX_VER_6 = 6;

	static final int RINDEX_VER_4 = 4;

	static final int RINDEX_VER_3 = 3;

	private static int sampleBufferSize = 10000000;

	@com.google.common.annotations.VisibleForTesting
	public static void setSampleBufferSize(int bufferSize) {
		RFile.sampleBufferSize = bufferSize;
	}

	private static class LocalityGroupMetadata implements Writable {
		private int startBlock = -1;

		private Key firstKey;

		private Map<ByteSequence, MutableLong> columnFamilies;

		private boolean isDefaultLG = false;

		private String name;

		private Set<ByteSequence> previousColumnFamilies;

		private MultiLevelIndex.BufferedWriter indexWriter;

		private MultiLevelIndex.Reader indexReader;

		private int version;

		public LocalityGroupMetadata(int version, BlockFileReader br) {
			columnFamilies = new HashMap<>();
			indexReader = new MultiLevelIndex.Reader(br, version);
			this.version = version;
		}

		public LocalityGroupMetadata(Set<ByteSequence> pcf, int indexBlockSize, BlockFileWriter bfw) {
			isDefaultLG = true;
			columnFamilies = new HashMap<>();
			previousColumnFamilies = pcf;
		}

		public LocalityGroupMetadata(String name, Set<ByteSequence> cfset, int indexBlockSize, BlockFileWriter bfw) {
			this.name = name;
			isDefaultLG = false;
			columnFamilies = new HashMap<>();
			for (ByteSequence cf : cfset) {
				columnFamilies.put(cf, new MutableLong(0));
			}
		}

		private Key getFirstKey() {
			return firstKey;
		}

		private void setFirstKey(Key key) {
			if ((firstKey) != null)
				throw new IllegalStateException();

			this.firstKey = new Key(key);
		}

		public void updateColumnCount(Key key) {
			if ((isDefaultLG) && ((columnFamilies) == null)) {
				if ((previousColumnFamilies.size()) > 0) {
					ByteSequence cf = key.getColumnFamilyData();
					if (previousColumnFamilies.contains(cf)) {
						throw new IllegalArgumentException((("Added column family \"" + cf) + "\" to default locality group that was in previous locality group"));
					}
				}
				return;
			}
			ByteSequence cf = key.getColumnFamilyData();
			MutableLong count = columnFamilies.get(cf);
			if (count == null) {
				if (!(isDefaultLG)) {
					throw new IllegalArgumentException(("invalid column family : " + cf));
				}
				if (previousColumnFamilies.contains(cf)) {
					throw new IllegalArgumentException((("Added column family \"" + cf) + "\" to default locality group that was in previous locality group"));
				}
				if ((columnFamilies.size()) > (RFile.Writer.MAX_CF_IN_DLG)) {
					columnFamilies = null;
					return;
				}
				count = new MutableLong(0);
				columnFamilies.put(new ArrayByteSequence(cf.getBackingArray(), cf.offset(), cf.length()), count);
			}
		}

		@Override
		public void readFields(DataInput in) throws IOException {
			isDefaultLG = in.readBoolean();
			if (!(isDefaultLG)) {
				name = in.readUTF();
			}
			if (((((version) == (RFile.RINDEX_VER_3)) || ((version) == (RFile.RINDEX_VER_4))) || ((version) == (RFile.RINDEX_VER_6))) || ((version) == (RFile.RINDEX_VER_7))) {
				startBlock = in.readInt();
			}
			int size = in.readInt();
			if (size == (-1)) {
				if (!(isDefaultLG))
					throw new IllegalStateException((("Non default LG " + (name)) + " does not have column families"));

				columnFamilies = null;
			}else {
				if ((columnFamilies) == null)
					columnFamilies = new HashMap<>();
				else
					columnFamilies.clear();

				for (int i = 0; i < size; i++) {
					int len = in.readInt();
					byte[] cf = new byte[len];
					in.readFully(cf);
					long count = in.readLong();
					columnFamilies.put(new ArrayByteSequence(cf), new MutableLong(count));
				}
			}
			if (in.readBoolean()) {
				firstKey = new Key();
				firstKey.readFields(in);
			}else {
				firstKey = null;
			}
			indexReader.readFields(in);
		}

		@Override
		public void write(DataOutput out) throws IOException {
			out.writeBoolean(isDefaultLG);
			if (!(isDefaultLG)) {
				out.writeUTF(name);
			}
			if ((isDefaultLG) && ((columnFamilies) == null)) {
				out.writeInt((-1));
			}else {
				out.writeInt(columnFamilies.size());
				for (Map.Entry<ByteSequence, MutableLong> entry : columnFamilies.entrySet()) {
					out.writeInt(entry.getKey().length());
					out.write(entry.getKey().getBackingArray(), entry.getKey().offset(), entry.getKey().length());
					out.writeLong(entry.getValue().longValue());
				}
			}
			out.writeBoolean(((firstKey) != null));
			if ((firstKey) != null)
				firstKey.write(out);

			indexWriter.close(out);
		}

		public void printInfo(boolean isSample, boolean includeIndexDetails) throws IOException {
			PrintStream out = System.out;
			out.printf("%-24s : %s\n", ((isSample ? "Sample " : "") + "Locality group "), (isDefaultLG ? "<DEFAULT>" : name));
			if (((((version) == (RFile.RINDEX_VER_3)) || ((version) == (RFile.RINDEX_VER_4))) || ((version) == (RFile.RINDEX_VER_6))) || ((version) == (RFile.RINDEX_VER_7))) {
				out.printf("\t%-22s : %d\n", "Start block", startBlock);
			}
			out.printf("\t%-22s : %,d\n", "Num   blocks", indexReader.size());
			TreeMap<Integer, Long> sizesByLevel = new TreeMap<>();
			TreeMap<Integer, Long> countsByLevel = new TreeMap<>();
			indexReader.getIndexInfo(sizesByLevel, countsByLevel);
			for (Map.Entry<Integer, Long> entry : sizesByLevel.descendingMap().entrySet()) {
				out.printf("\t%-22s : %,d bytes  %,d blocks\n", ("Index level " + (entry.getKey())), entry.getValue(), countsByLevel.get(entry.getKey()));
			}
			out.printf("\t%-22s : %s\n", "First key", firstKey);
			Key lastKey = null;
			if ((indexReader.size()) > 0) {
				lastKey = indexReader.getLastKey();
			}
			out.printf("\t%-22s : %s\n", "Last key", lastKey);
			long numKeys = 0;
			MultiLevelIndex.Reader.IndexIterator countIter = indexReader.lookup(new Key());
			while (countIter.hasNext()) {
				MultiLevelIndex.IndexEntry indexEntry = countIter.next();
				numKeys += indexEntry.getNumEntries();
			} 
			out.printf("\t%-22s : %,d\n", "Num entries", numKeys);
			out.printf("\t%-22s : %s\n", "Column families", ((isDefaultLG) && ((columnFamilies) == null) ? "<UNKNOWN>" : columnFamilies.keySet()));
			if (includeIndexDetails) {
				out.printf("\t%-22s :\nIndex Entries", lastKey);
				String prefix = String.format("\t   ");
				indexReader.printIndex(prefix, out);
			}
		}
	}

	private static class SampleEntry {
		Key key;

		Value val;

		SampleEntry(Key key, Value val) {
			this.key = new Key(key);
			this.val = new Value(val);
		}
	}

	private static class SampleLocalityGroupWriter {
		private Sampler sampler;

		private List<RFile.SampleEntry> entries = new ArrayList<>();

		private long dataSize = 0;

		private RFile.LocalityGroupWriter lgr;

		public SampleLocalityGroupWriter(RFile.LocalityGroupWriter lgr, Sampler sampler) {
			this.lgr = lgr;
			this.sampler = sampler;
		}

		public void append(Key key, Value value) throws IOException {
			if (sampler.accept(key)) {
				entries.add(new RFile.SampleEntry(key, value));
				dataSize += (key.getSize()) + (value.getSize());
			}
		}

		public void close() throws IOException {
			for (RFile.SampleEntry se : entries) {
				lgr.append(se.key, se.val);
			}
			lgr.close();
		}

		public void flushIfNeeded() throws IOException {
			if ((dataSize) > (RFile.sampleBufferSize)) {
				List<RFile.SampleEntry> subList = entries.subList(0, ((entries.size()) - 1));
				if ((subList.size()) > 0) {
					for (RFile.SampleEntry se : subList) {
						lgr.append(se.key, se.val);
					}
					lgr.closeBlock(subList.get(((subList.size()) - 1)).key, false);
					subList.clear();
					dataSize = 0;
				}
			}
		}
	}

	private static class LocalityGroupWriter {
		private BlockFileWriter fileWriter;

		private ABlockWriter blockWriter;

		private final long blockSize;

		private final long maxBlockSize;

		private int entries = 0;

		private RFile.LocalityGroupMetadata currentLocalityGroup = null;

		private Key lastKeyInBlock = null;

		private Key prevKey = new Key();

		private RFile.SampleLocalityGroupWriter sample;

		private double avergageKeySize = 0;

		LocalityGroupWriter(BlockFileWriter fileWriter, long blockSize, long maxBlockSize, RFile.LocalityGroupMetadata currentLocalityGroup, RFile.SampleLocalityGroupWriter sample) {
			this.fileWriter = fileWriter;
			this.blockSize = blockSize;
			this.maxBlockSize = maxBlockSize;
			this.currentLocalityGroup = currentLocalityGroup;
			this.sample = sample;
		}

		private boolean isGiantKey(Key k) {
			return false;
		}

		public void append(Key key, Value value) throws IOException {
			if ((key.compareTo(prevKey)) < 0) {
				throw new IllegalArgumentException(((("Keys appended out-of-order.  New key " + key) + ", previous key ") + (prevKey)));
			}
			currentLocalityGroup.updateColumnCount(key);
			if ((currentLocalityGroup.getFirstKey()) == null) {
				currentLocalityGroup.setFirstKey(key);
			}
			if ((sample) != null) {
				sample.append(key, value);
			}
			if ((blockWriter) == null) {
				blockWriter = fileWriter.prepareDataBlock();
			}else
				if ((blockWriter.getRawSize()) > (blockSize)) {
					if ((avergageKeySize) == 0) {
					}
					Key closeKey = KeyShortener.shorten(prevKey, key);
					if ((((closeKey.getSize()) <= (avergageKeySize)) || ((blockWriter.getRawSize()) > (maxBlockSize))) && (!(isGiantKey(closeKey)))) {
						closeBlock(closeKey, false);
						blockWriter = fileWriter.prepareDataBlock();
						avergageKeySize = 0;
					}
				}

			RelativeKey rk = new RelativeKey(lastKeyInBlock, key);
			rk.write(blockWriter);
			value.write(blockWriter);
			(entries)++;
			prevKey = new Key(key);
			lastKeyInBlock = prevKey;
		}

		private void closeBlock(Key key, boolean lastBlock) throws IOException {
			blockWriter.close();
			if (lastBlock)
				currentLocalityGroup.indexWriter.addLast(key, entries, blockWriter.getStartPos(), blockWriter.getCompressedSize(), blockWriter.getRawSize());
			else
				currentLocalityGroup.indexWriter.add(key, entries, blockWriter.getStartPos(), blockWriter.getCompressedSize(), blockWriter.getRawSize());

			if ((sample) != null)
				sample.flushIfNeeded();

			blockWriter = null;
			lastKeyInBlock = null;
			entries = 0;
		}

		public void close() throws IOException {
			if ((blockWriter) != null) {
				closeBlock(lastKeyInBlock, true);
			}
			if ((sample) != null) {
				sample.close();
			}
		}
	}

	public static class Writer implements FileSKVWriter {
		public static final int MAX_CF_IN_DLG = 1000;

		private static final double MAX_BLOCK_MULTIPLIER = 1.1;

		private BlockFileWriter fileWriter;

		private final long blockSize;

		private final long maxBlockSize;

		private final int indexBlockSize;

		private ArrayList<RFile.LocalityGroupMetadata> localityGroups = new ArrayList<>();

		private ArrayList<RFile.LocalityGroupMetadata> sampleGroups = new ArrayList<>();

		private RFile.LocalityGroupMetadata currentLocalityGroup = null;

		private RFile.LocalityGroupMetadata sampleLocalityGroup = null;

		private boolean dataClosed = false;

		private boolean closed = false;

		private boolean startedDefaultLocalityGroup = false;

		private HashSet<ByteSequence> previousColumnFamilies;

		private long length = -1;

		private RFile.LocalityGroupWriter lgWriter;

		private SamplerConfigurationImpl samplerConfig;

		private Sampler sampler;

		public Writer(BlockFileWriter bfw, int blockSize) throws IOException {
			this(bfw, blockSize, ((int) (AccumuloConfiguration.getDefaultConfiguration().getMemoryInBytes(Property.TABLE_FILE_COMPRESSED_BLOCK_SIZE_INDEX))), null, null);
		}

		public Writer(BlockFileWriter bfw, int blockSize, int indexBlockSize, SamplerConfigurationImpl samplerConfig, Sampler sampler) throws IOException {
			this.blockSize = blockSize;
			this.maxBlockSize = ((long) (blockSize * (RFile.Writer.MAX_BLOCK_MULTIPLIER)));
			this.indexBlockSize = indexBlockSize;
			this.fileWriter = bfw;
			previousColumnFamilies = new HashSet<>();
			this.samplerConfig = samplerConfig;
			this.sampler = sampler;
		}

		@Override
		public synchronized void close() throws IOException {
			if (closed) {
				return;
			}
			closeData();
			ABlockWriter mba = fileWriter.prepareMetaBlock("RFile.index");
			mba.writeInt(RFile.RINDEX_MAGIC);
			mba.writeInt(RFile.RINDEX_VER_8);
			if ((currentLocalityGroup) != null) {
				localityGroups.add(currentLocalityGroup);
				sampleGroups.add(sampleLocalityGroup);
			}
			mba.writeInt(localityGroups.size());
			for (RFile.LocalityGroupMetadata lc : localityGroups) {
				lc.write(mba);
			}
			if ((samplerConfig) == null) {
				mba.writeBoolean(false);
			}else {
				mba.writeBoolean(true);
				for (RFile.LocalityGroupMetadata lc : sampleGroups) {
					lc.write(mba);
				}
				samplerConfig.write(mba);
			}
			mba.close();
			fileWriter.close();
			length = fileWriter.getLength();
			closed = true;
		}

		private void closeData() throws IOException {
			if (dataClosed) {
				return;
			}
			dataClosed = true;
			if ((lgWriter) != null) {
				lgWriter.close();
			}
		}

		@Override
		public void append(Key key, Value value) throws IOException {
			if (dataClosed) {
				throw new IllegalStateException("Cannont append, data closed");
			}
			lgWriter.append(key, value);
		}

		@Override
		public DataOutputStream createMetaStore(String name) throws IOException {
			closeData();
			return ((DataOutputStream) (fileWriter.prepareMetaBlock(name)));
		}

		private void _startNewLocalityGroup(String name, Set<ByteSequence> columnFamilies) throws IOException {
			if (dataClosed) {
				throw new IllegalStateException("data closed");
			}
			if (startedDefaultLocalityGroup) {
				throw new IllegalStateException("Can not start anymore new locality groups after default locality group started");
			}
			if ((lgWriter) != null) {
				lgWriter.close();
			}
			if ((currentLocalityGroup) != null) {
				localityGroups.add(currentLocalityGroup);
				sampleGroups.add(sampleLocalityGroup);
			}
			if (columnFamilies == null) {
				startedDefaultLocalityGroup = true;
				currentLocalityGroup = new RFile.LocalityGroupMetadata(previousColumnFamilies, indexBlockSize, fileWriter);
				sampleLocalityGroup = new RFile.LocalityGroupMetadata(previousColumnFamilies, indexBlockSize, fileWriter);
			}else {
				if (!(Collections.disjoint(columnFamilies, previousColumnFamilies))) {
					HashSet<ByteSequence> overlap = new HashSet<>(columnFamilies);
					overlap.retainAll(previousColumnFamilies);
					throw new IllegalArgumentException(("Column families over lap with previous locality group : " + overlap));
				}
				currentLocalityGroup = new RFile.LocalityGroupMetadata(name, columnFamilies, indexBlockSize, fileWriter);
				sampleLocalityGroup = new RFile.LocalityGroupMetadata(name, columnFamilies, indexBlockSize, fileWriter);
				previousColumnFamilies.addAll(columnFamilies);
			}
			RFile.SampleLocalityGroupWriter sampleWriter = null;
			if ((sampler) != null) {
				sampleWriter = new RFile.SampleLocalityGroupWriter(new RFile.LocalityGroupWriter(fileWriter, blockSize, maxBlockSize, sampleLocalityGroup, null), sampler);
			}
			lgWriter = new RFile.LocalityGroupWriter(fileWriter, blockSize, maxBlockSize, currentLocalityGroup, sampleWriter);
		}

		@Override
		public void startNewLocalityGroup(String name, Set<ByteSequence> columnFamilies) throws IOException {
			if (columnFamilies == null)
				throw new NullPointerException();

			_startNewLocalityGroup(name, columnFamilies);
		}

		@Override
		public void startDefaultLocalityGroup() throws IOException {
			_startNewLocalityGroup(null, null);
		}

		@Override
		public boolean supportsLocalityGroups() {
			return true;
		}

		@Override
		public long getLength() throws IOException {
			if (!(closed)) {
				return fileWriter.getLength();
			}
			return length;
		}
	}

	private static class LocalityGroupReader extends LocalityGroupIterator.LocalityGroup implements FileSKVIterator {
		private BlockFileReader reader;

		private MultiLevelIndex.Reader index;

		private int blockCount;

		private Key firstKey;

		private int startBlock;

		private boolean closed = false;

		private int version;

		private boolean checkRange = true;

		private LocalityGroupReader(BlockFileReader reader, RFile.LocalityGroupMetadata lgm, int version) throws IOException {
			super(lgm.columnFamilies, lgm.isDefaultLG);
			this.firstKey = lgm.firstKey;
			this.index = lgm.indexReader;
			this.startBlock = lgm.startBlock;
			blockCount = index.size();
			this.version = version;
			this.reader = reader;
		}

		public LocalityGroupReader(RFile.LocalityGroupReader lgr) {
			super(lgr.columnFamilies, lgr.isDefaultLocalityGroup);
			this.firstKey = lgr.firstKey;
			this.index = lgr.index;
			this.startBlock = lgr.startBlock;
			this.blockCount = lgr.blockCount;
			this.reader = lgr.reader;
			this.version = lgr.version;
		}

		Iterator<MultiLevelIndex.IndexEntry> getIndex() throws IOException {
			return index.lookup(new Key());
		}

		@Override
		public void close() throws IOException {
			closed = true;
			hasTop = false;
			if ((currBlock) != null)
				currBlock.close();

		}

		private MultiLevelIndex.Reader.IndexIterator iiter;

		private int entriesLeft;

		private ABlockReader currBlock;

		private RelativeKey rk;

		private Value val;

		private Key prevKey = null;

		private Range range = null;

		private boolean hasTop = false;

		private AtomicBoolean interruptFlag;

		@Override
		public Key getTopKey() {
			return rk.getKey();
		}

		@Override
		public Value getTopValue() {
			return val;
		}

		@Override
		public boolean hasTop() {
			return hasTop;
		}

		@Override
		public void next() throws IOException {
			try {
				_next();
			} catch (IOException ioe) {
				reset();
				throw ioe;
			}
		}

		private void _next() throws IOException {
			if (!(hasTop))
				throw new IllegalStateException();

			if ((entriesLeft) == 0) {
				currBlock.close();
				if ((metricsGatherer) != null)
					metricsGatherer.startBlock();

				if (iiter.hasNext()) {
					MultiLevelIndex.IndexEntry indexEntry = iiter.next();
					entriesLeft = indexEntry.getNumEntries();
					currBlock = getDataBlock(indexEntry);
					checkRange = range.afterEndKey(indexEntry.getKey());
					if (!(checkRange))
						hasTop = true;

				}else {
					rk = null;
					val = null;
					hasTop = false;
					return;
				}
			}
			prevKey = rk.getKey();
			rk.readFields(currBlock);
			val.readFields(currBlock);
			if ((metricsGatherer) != null)
				metricsGatherer.addMetric(rk.getKey(), val);

			(entriesLeft)--;
			if (checkRange)
				hasTop = !(range.afterEndKey(rk.getKey()));

		}

		private ABlockReader getDataBlock(MultiLevelIndex.IndexEntry indexEntry) throws IOException {
			if (((interruptFlag) != null) && (interruptFlag.get()))
				throw new IterationInterruptedException();

			if (((version) == (RFile.RINDEX_VER_3)) || ((version) == (RFile.RINDEX_VER_4)))
				return reader.getDataBlock(((startBlock) + (iiter.previousIndex())));
			else
				return reader.getDataBlock(indexEntry.getOffset(), indexEntry.getCompressedSize(), indexEntry.getRawSize());

		}

		@Override
		public void seek(Range range, Collection<ByteSequence> columnFamilies, boolean inclusive) throws IOException {
			if (closed)
				throw new IllegalStateException("Locality group reader closed");

			if (((columnFamilies.size()) != 0) || inclusive)
				throw new IllegalArgumentException("I do not know how to filter column families");

			if (((interruptFlag) != null) && (interruptFlag.get())) {
				throw new IterationInterruptedException();
			}
			try {
				_seek(range);
			} catch (IOException ioe) {
				reset();
				throw ioe;
			}
		}

		private void reset() {
			rk = null;
			hasTop = false;
			if ((currBlock) != null) {
				try {
					try {
						currBlock.close();
					} catch (IOException e) {
						RFile.log.warn("Failed to close block reader", e);
					}
				} finally {
					currBlock = null;
				}
			}
		}

		private void _seek(Range range) throws IOException {
			this.range = range;
			this.checkRange = true;
			if ((blockCount) == 0) {
				rk = null;
				return;
			}
			Key startKey = range.getStartKey();
			if (startKey == null)
				startKey = new Key();

			boolean reseek = true;
			if (range.afterEndKey(firstKey)) {
				reset();
				reseek = false;
			}
			if ((rk) != null) {
				if ((range.beforeStartKey(prevKey)) && (range.afterEndKey(getTopKey()))) {
					reseek = false;
				}
				if (((startKey.compareTo(getTopKey())) <= 0) && ((startKey.compareTo(prevKey)) > 0)) {
					reseek = false;
				}
				if ((((entriesLeft) > 0) && ((startKey.compareTo(getTopKey())) >= 0)) && ((startKey.compareTo(iiter.peekPrevious().getKey())) <= 0)) {
					MutableByteSequence valbs = new MutableByteSequence(new byte[64], 0, 0);
					RelativeKey.SkippR skippr = RelativeKey.fastSkip(currBlock, startKey, valbs, prevKey, getTopKey(), entriesLeft);
					reseek = false;
				}
				if ((((entriesLeft) == 0) && ((startKey.compareTo(getTopKey())) > 0)) && ((startKey.compareTo(iiter.peekPrevious().getKey())) <= 0)) {
					reseek = false;
				}
				if ((((iiter.previousIndex()) == 0) && (getTopKey().equals(firstKey))) && ((startKey.compareTo(firstKey)) <= 0)) {
					reseek = false;
				}
			}
			if (reseek) {
				iiter = index.lookup(startKey);
				reset();
				if (!(iiter.hasNext())) {
				}else {
					while ((iiter.hasPrevious()) && (iiter.peekPrevious().getKey().equals(iiter.peek().getKey()))) {
						iiter.previous();
					} 
					if (iiter.hasPrevious())
						prevKey = new Key(iiter.peekPrevious().getKey());
					else
						prevKey = new Key();

					MultiLevelIndex.IndexEntry indexEntry = iiter.next();
					entriesLeft = indexEntry.getNumEntries();
					currBlock = getDataBlock(indexEntry);
					checkRange = range.afterEndKey(indexEntry.getKey());
					if (!(checkRange))
						hasTop = true;

					MutableByteSequence valbs = new MutableByteSequence(new byte[64], 0, 0);
					Key currKey = null;
					if (currBlock.isIndexable()) {
						BlockIndex blockIndex = BlockIndex.getIndex(currBlock, indexEntry);
						if (blockIndex != null) {
							BlockIndex.BlockIndexEntry bie = blockIndex.seekBlock(startKey, currBlock);
							if (bie != null) {
								RelativeKey tmpRk = new RelativeKey();
								tmpRk.setPrevKey(bie.getPrevKey());
								tmpRk.readFields(currBlock);
								val = new Value();
								val.readFields(currBlock);
								valbs = new MutableByteSequence(val.get(), 0, val.getSize());
								entriesLeft = (bie.getEntriesLeft()) - 1;
								prevKey = new Key(bie.getPrevKey());
								currKey = tmpRk.getKey();
							}
						}
					}
					RelativeKey.SkippR skippr = RelativeKey.fastSkip(currBlock, startKey, valbs, prevKey, currKey, entriesLeft);
					val = new Value(valbs.toArray());
				}
			}
			hasTop = ((rk) != null) && (!(range.afterEndKey(rk.getKey())));
			while ((hasTop()) && (range.beforeStartKey(getTopKey()))) {
				next();
			} 
			if ((metricsGatherer) != null) {
				metricsGatherer.startLocalityGroup(rk.getKey().getColumnFamily());
				metricsGatherer.addMetric(rk.getKey(), val);
			}
		}

		@Override
		public Key getFirstKey() throws IOException {
			return firstKey;
		}

		@Override
		public Key getLastKey() throws IOException {
			if ((index.size()) == 0)
				return null;

			return index.getLastKey();
		}

		@Override
		public SortedKeyValueIterator<Key, Value> deepCopy(IteratorEnvironment env) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void closeDeepCopies() throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void init(SortedKeyValueIterator<Key, Value> source, Map<String, String> options, IteratorEnvironment env) throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public DataInputStream getMetaStore(String name) throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setInterruptFlag(AtomicBoolean flag) {
			this.interruptFlag = flag;
		}

		@Override
		public InterruptibleIterator getIterator() {
			return this;
		}

		private MetricsGatherer<?> metricsGatherer;

		public void registerMetrics(MetricsGatherer<?> vmg) {
			metricsGatherer = vmg;
		}

		@Override
		public FileSKVIterator getSample(SamplerConfigurationImpl sampleConfig) {
			throw new UnsupportedOperationException();
		}
	}

	public static class Reader extends HeapIterator implements FileSKVIterator {
		private final BlockFileReader reader;

		private final ArrayList<RFile.LocalityGroupMetadata> localityGroups = new ArrayList<>();

		private final ArrayList<RFile.LocalityGroupMetadata> sampleGroups = new ArrayList<>();

		private final RFile.LocalityGroupReader[] currentReaders;

		private final RFile.LocalityGroupReader[] readers;

		private final RFile.LocalityGroupReader[] sampleReaders;

		private final LocalityGroupIterator.LocalityGroupContext lgContext;

		private LocalityGroupIterator.LocalityGroupSeekCache lgCache;

		private List<RFile.Reader> deepCopies;

		private boolean deepCopy = false;

		private AtomicBoolean interruptFlag;

		private SamplerConfigurationImpl samplerConfig = null;

		private int rfileVersion;

		public Reader(BlockFileReader rdr) throws IOException {
			this.reader = rdr;
			ABlockReader mb = reader.getMetaBlock("RFile.index");
			try {
				int magic = mb.readInt();
				int ver = mb.readInt();
				rfileVersion = ver;
				if (magic != (RFile.RINDEX_MAGIC))
					throw new IOException(("Did not see expected magic number, saw " + magic));

				if (((((ver != (RFile.RINDEX_VER_8)) && (ver != (RFile.RINDEX_VER_7))) && (ver != (RFile.RINDEX_VER_6))) && (ver != (RFile.RINDEX_VER_4))) && (ver != (RFile.RINDEX_VER_3)))
					throw new IOException(("Did not see expected version, saw " + ver));

				int size = mb.readInt();
				currentReaders = new RFile.LocalityGroupReader[size];
				deepCopies = new LinkedList<>();
				for (int i = 0; i < size; i++) {
					RFile.LocalityGroupMetadata lgm = new RFile.LocalityGroupMetadata(ver, rdr);
					lgm.readFields(mb);
					localityGroups.add(lgm);
					currentReaders[i] = new RFile.LocalityGroupReader(reader, lgm, ver);
				}
				readers = currentReaders;
				if ((ver == (RFile.RINDEX_VER_8)) && (mb.readBoolean())) {
					sampleReaders = new RFile.LocalityGroupReader[size];
					for (int i = 0; i < size; i++) {
						RFile.LocalityGroupMetadata lgm = new RFile.LocalityGroupMetadata(ver, rdr);
						lgm.readFields(mb);
						sampleGroups.add(lgm);
						sampleReaders[i] = new RFile.LocalityGroupReader(reader, lgm, ver);
					}
					samplerConfig = new SamplerConfigurationImpl(mb);
				}else {
					sampleReaders = null;
					samplerConfig = null;
				}
			} finally {
				mb.close();
			}
			lgContext = new LocalityGroupIterator.LocalityGroupContext(currentReaders);
			createHeap(currentReaders.length);
		}

		private Reader(RFile.Reader r, RFile.LocalityGroupReader[] sampleReaders) {
			super(sampleReaders.length);
			this.reader = r.reader;
			this.currentReaders = new RFile.LocalityGroupReader[sampleReaders.length];
			this.deepCopies = r.deepCopies;
			this.deepCopy = false;
			this.readers = r.readers;
			this.sampleReaders = r.sampleReaders;
			this.samplerConfig = r.samplerConfig;
			this.rfileVersion = r.rfileVersion;
			for (int i = 0; i < (sampleReaders.length); i++) {
				this.currentReaders[i] = sampleReaders[i];
				this.currentReaders[i].setInterruptFlag(r.interruptFlag);
			}
			this.lgContext = new LocalityGroupIterator.LocalityGroupContext(currentReaders);
		}

		private Reader(RFile.Reader r, boolean useSample) {
			super(r.currentReaders.length);
			this.reader = r.reader;
			this.currentReaders = new RFile.LocalityGroupReader[r.currentReaders.length];
			this.deepCopies = r.deepCopies;
			this.deepCopy = true;
			this.samplerConfig = r.samplerConfig;
			this.rfileVersion = r.rfileVersion;
			this.readers = r.readers;
			this.sampleReaders = r.sampleReaders;
			for (int i = 0; i < (r.readers.length); i++) {
				if (useSample) {
					this.currentReaders[i] = new RFile.LocalityGroupReader(r.sampleReaders[i]);
					this.currentReaders[i].setInterruptFlag(r.interruptFlag);
				}else {
					this.currentReaders[i] = new RFile.LocalityGroupReader(r.readers[i]);
					this.currentReaders[i].setInterruptFlag(r.interruptFlag);
				}
			}
			this.lgContext = new LocalityGroupIterator.LocalityGroupContext(currentReaders);
		}

		private void closeLocalityGroupReaders() {
			for (RFile.LocalityGroupReader lgr : currentReaders) {
				try {
					lgr.close();
				} catch (IOException e) {
					RFile.log.warn("Errored out attempting to close LocalityGroupReader.", e);
				}
			}
		}

		@Override
		public void closeDeepCopies() {
			if (deepCopy)
				throw new RuntimeException("Calling closeDeepCopies on a deep copy is not supported");

			for (RFile.Reader deepCopy : deepCopies)
				deepCopy.closeLocalityGroupReaders();

			deepCopies.clear();
		}

		@Override
		public void close() throws IOException {
			if (deepCopy)
				throw new RuntimeException("Calling close on a deep copy is not supported");

			closeDeepCopies();
			closeLocalityGroupReaders();
			if ((sampleReaders) != null) {
				for (RFile.LocalityGroupReader lgr : sampleReaders) {
					try {
						lgr.close();
					} catch (IOException e) {
						RFile.log.warn("Errored out attempting to close LocalityGroupReader.", e);
					}
				}
			}
			try {
				reader.close();
			} finally {
			}
		}

		@Override
		public Key getFirstKey() throws IOException {
			if ((currentReaders.length) == 0) {
				return null;
			}
			Key minKey = null;
			for (int i = 0; i < (currentReaders.length); i++) {
				if (minKey == null) {
					minKey = currentReaders[i].getFirstKey();
				}else {
					Key firstKey = currentReaders[i].getFirstKey();
					if ((firstKey != null) && ((firstKey.compareTo(minKey)) < 0))
						minKey = firstKey;

				}
			}
			return minKey;
		}

		@Override
		public Key getLastKey() throws IOException {
			if ((currentReaders.length) == 0) {
				return null;
			}
			Key maxKey = null;
			for (int i = 0; i < (currentReaders.length); i++) {
				if (maxKey == null) {
					maxKey = currentReaders[i].getLastKey();
				}else {
					Key lastKey = currentReaders[i].getLastKey();
					if ((lastKey != null) && ((lastKey.compareTo(maxKey)) > 0))
						maxKey = lastKey;

				}
			}
			return maxKey;
		}

		@Override
		public DataInputStream getMetaStore(String name) throws IOException, NoSuchMetaStoreException {
			try {
				return this.reader.getMetaBlock(name).getStream();
			} catch (MetaBlockDoesNotExist e) {
				throw new NoSuchMetaStoreException(("name = " + name), e);
			}
		}

		@Override
		public SortedKeyValueIterator<Key, Value> deepCopy(IteratorEnvironment env) {
			if ((env != null) && (env.isSamplingEnabled())) {
				SamplerConfiguration sc = env.getSamplerConfiguration();
				if (sc == null) {
					throw new SampleNotPresentException();
				}
				if (((this.samplerConfig) != null) && (this.samplerConfig.equals(new SamplerConfigurationImpl(sc)))) {
					RFile.Reader copy = new RFile.Reader(this, true);
					copy.setInterruptFlagInternal(interruptFlag);
					deepCopies.add(copy);
					return copy;
				}else {
					throw new SampleNotPresentException();
				}
			}else {
				RFile.Reader copy = new RFile.Reader(this, false);
				copy.setInterruptFlagInternal(interruptFlag);
				deepCopies.add(copy);
				return copy;
			}
		}

		@Override
		public void init(SortedKeyValueIterator<Key, Value> source, Map<String, String> options, IteratorEnvironment env) throws IOException {
			throw new UnsupportedOperationException();
		}

		public Map<String, ArrayList<ByteSequence>> getLocalityGroupCF() {
			Map<String, ArrayList<ByteSequence>> cf = new HashMap<>();
			for (RFile.LocalityGroupMetadata lcg : localityGroups) {
				ArrayList<ByteSequence> setCF;
				if ((lcg.columnFamilies) == null) {
					Preconditions.checkState(lcg.isDefaultLG, ("Group %s has null families. " + "Only expect default locality group to have null families."), lcg.name);
					setCF = new ArrayList<>();
				}else {
					setCF = new ArrayList<>(lcg.columnFamilies.keySet());
				}
				cf.put(lcg.name, setCF);
			}
			return cf;
		}

		public void registerMetrics(MetricsGatherer<?> vmg) {
			vmg.init(getLocalityGroupCF());
			for (RFile.LocalityGroupReader lgr : currentReaders) {
				lgr.registerMetrics(vmg);
			}
			if ((sampleReaders) != null) {
				for (RFile.LocalityGroupReader lgr : sampleReaders) {
					lgr.registerMetrics(vmg);
				}
			}
		}

		@Override
		public void seek(Range range, Collection<ByteSequence> columnFamilies, boolean inclusive) throws IOException {
			lgCache = LocalityGroupIterator.seek(this, lgContext, range, columnFamilies, inclusive, lgCache);
		}

		int getNumLocalityGroupsSeeked() {
			return (lgCache) == null ? 0 : lgCache.getNumLGSeeked();
		}

		public FileSKVIterator getIndex() throws IOException {
			ArrayList<Iterator<MultiLevelIndex.IndexEntry>> indexes = new ArrayList<>();
			for (RFile.LocalityGroupReader lgr : currentReaders) {
				indexes.add(lgr.getIndex());
			}
			return null;
		}

		@Override
		public FileSKVIterator getSample(SamplerConfigurationImpl sampleConfig) {
			Objects.requireNonNull(sampleConfig);
			if (((this.samplerConfig) != null) && (this.samplerConfig.equals(sampleConfig))) {
				RFile.Reader copy = new RFile.Reader(this, sampleReaders);
				copy.setInterruptFlagInternal(interruptFlag);
				return copy;
			}
			return null;
		}

		FileSKVIterator getSample() {
			if ((samplerConfig) == null)
				return null;

			return getSample(this.samplerConfig);
		}

		public void printInfo() throws IOException {
			printInfo(false);
		}

		public void printInfo(boolean includeIndexDetails) throws IOException {
			System.out.printf("%-24s : %d\n", "RFile Version", rfileVersion);
			System.out.println();
			for (RFile.LocalityGroupMetadata lgm : localityGroups) {
				lgm.printInfo(false, includeIndexDetails);
			}
			if ((sampleGroups.size()) > 0) {
				System.out.println();
				System.out.printf("%-24s :\n", "Sample Configuration");
				System.out.printf("\t%-22s : %s\n", "Sampler class ", samplerConfig.getClassName());
				System.out.printf("\t%-22s : %s\n", "Sampler options ", samplerConfig.getOptions());
				System.out.println();
				for (RFile.LocalityGroupMetadata lgm : sampleGroups) {
					lgm.printInfo(true, includeIndexDetails);
				}
			}
		}

		@Override
		public void setInterruptFlag(AtomicBoolean flag) {
			if (deepCopy)
				throw new RuntimeException("Calling setInterruptFlag on a deep copy is not supported");

			if ((deepCopies.size()) != 0)
				throw new RuntimeException("Setting interrupt flag after calling deep copy not supported");

			setInterruptFlagInternal(flag);
		}

		private void setInterruptFlagInternal(AtomicBoolean flag) {
			this.interruptFlag = flag;
			for (RFile.LocalityGroupReader lgr : currentReaders) {
				lgr.setInterruptFlag(interruptFlag);
			}
		}
	}
}

