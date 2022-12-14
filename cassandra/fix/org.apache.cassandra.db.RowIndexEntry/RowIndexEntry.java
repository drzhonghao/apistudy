

import com.codahale.metrics.Histogram;
import java.io.Closeable;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.List;
import org.apache.cassandra.cache.IMeasurableMemory;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.db.DeletionTime;
import org.apache.cassandra.db.SerializationHeader;
import org.apache.cassandra.db.TypeSizes;
import org.apache.cassandra.io.ISerializer;
import org.apache.cassandra.io.sstable.IndexInfo;
import org.apache.cassandra.io.sstable.format.Version;
import org.apache.cassandra.io.util.BufferedDataOutputStreamPlus;
import org.apache.cassandra.io.util.DataInputPlus;
import org.apache.cassandra.io.util.DataOutputBuffer;
import org.apache.cassandra.io.util.DataOutputPlus;
import org.apache.cassandra.io.util.FileDataInput;
import org.apache.cassandra.io.util.FileHandle;
import org.apache.cassandra.io.util.RandomAccessReader;
import org.apache.cassandra.io.util.TrackedDataInputPlus;
import org.apache.cassandra.metrics.CassandraMetricsRegistry;
import org.apache.cassandra.metrics.DefaultNameFactory;
import org.apache.cassandra.metrics.MetricNameFactory;
import org.apache.cassandra.utils.ObjectSizes;
import org.apache.cassandra.utils.vint.VIntCoding;
import org.github.jamm.Unmetered;


public class RowIndexEntry<T> implements IMeasurableMemory {
	private static final long EMPTY_SIZE = ObjectSizes.measure(new RowIndexEntry(0));

	static final int CACHE_NOT_INDEXED = 0;

	static final int CACHE_INDEXED = 1;

	static final int CACHE_INDEXED_SHALLOW = 2;

	static final Histogram indexEntrySizeHistogram;

	static final Histogram indexInfoCountHistogram;

	static final Histogram indexInfoGetsHistogram;

	static {
		MetricNameFactory factory = new DefaultNameFactory("Index", "RowIndexEntry");
		indexEntrySizeHistogram = CassandraMetricsRegistry.Metrics.histogram(factory.createMetricName("IndexedEntrySize"), false);
		indexInfoCountHistogram = CassandraMetricsRegistry.Metrics.histogram(factory.createMetricName("IndexInfoCount"), false);
		indexInfoGetsHistogram = CassandraMetricsRegistry.Metrics.histogram(factory.createMetricName("IndexInfoGets"), false);
	}

	public final long position;

	public RowIndexEntry(long position) {
		this.position = position;
	}

	public boolean isIndexed() {
		return (columnsIndexCount()) > 1;
	}

	public boolean indexOnHeap() {
		return false;
	}

	public DeletionTime deletionTime() {
		throw new UnsupportedOperationException();
	}

	public long headerLength() {
		throw new UnsupportedOperationException();
	}

	public int columnsIndexCount() {
		return 0;
	}

	public long unsharedHeapSize() {
		return RowIndexEntry.EMPTY_SIZE;
	}

	public static RowIndexEntry<IndexInfo> create(long dataFilePosition, long indexFilePosition, DeletionTime deletionTime, long headerLength, int columnIndexCount, int indexedPartSize, List<IndexInfo> indexSamples, int[] offsets, ISerializer<IndexInfo> idxInfoSerializer) {
		if ((indexSamples != null) && ((indexSamples.size()) > 1))
			return new RowIndexEntry.IndexedEntry(dataFilePosition, deletionTime, headerLength, indexSamples.toArray(new IndexInfo[indexSamples.size()]), offsets, indexedPartSize, idxInfoSerializer);

		if (columnIndexCount > 1)
			return new RowIndexEntry.ShallowIndexedEntry(dataFilePosition, indexFilePosition, deletionTime, headerLength, columnIndexCount, indexedPartSize, idxInfoSerializer);

		return new RowIndexEntry<>(dataFilePosition);
	}

	public RowIndexEntry.IndexInfoRetriever openWithIndex(FileHandle indexFile) {
		return null;
	}

	public interface IndexSerializer<T> {
		void serialize(RowIndexEntry<T> rie, DataOutputPlus out, ByteBuffer indexInfo) throws IOException;

		RowIndexEntry<T> deserialize(DataInputPlus in, long indexFilePosition) throws IOException;

		void serializeForCache(RowIndexEntry<T> rie, DataOutputPlus out) throws IOException;

		RowIndexEntry<T> deserializeForCache(DataInputPlus in) throws IOException;

		long deserializePositionAndSkip(DataInputPlus in) throws IOException;

		ISerializer<T> indexInfoSerializer();
	}

	public static final class Serializer implements RowIndexEntry.IndexSerializer<IndexInfo> {
		private final IndexInfo.Serializer idxInfoSerializer;

		private final Version version;

		public Serializer(CFMetaData metadata, Version version, SerializationHeader header) {
			this.version = version;
			idxInfoSerializer = null;
		}

		public IndexInfo.Serializer indexInfoSerializer() {
			return idxInfoSerializer;
		}

		public void serialize(RowIndexEntry<IndexInfo> rie, DataOutputPlus out, ByteBuffer indexInfo) throws IOException {
			assert version.storeRows() : "We read old index files but we should never write them";
			rie.serialize(out, idxInfoSerializer, indexInfo);
		}

		public void serializeForCache(RowIndexEntry<IndexInfo> rie, DataOutputPlus out) throws IOException {
			assert version.storeRows();
			rie.serializeForCache(out);
		}

		public RowIndexEntry<IndexInfo> deserializeForCache(DataInputPlus in) throws IOException {
			assert version.storeRows();
			long position = in.readUnsignedVInt();
			switch (in.readByte()) {
				case RowIndexEntry.CACHE_NOT_INDEXED :
					return new RowIndexEntry<>(position);
				case RowIndexEntry.CACHE_INDEXED :
					return new RowIndexEntry.IndexedEntry(position, in, idxInfoSerializer, version);
				case RowIndexEntry.CACHE_INDEXED_SHALLOW :
					return new RowIndexEntry.ShallowIndexedEntry(position, in, idxInfoSerializer);
				default :
					throw new AssertionError();
			}
		}

		public static void skipForCache(DataInputPlus in, Version version) throws IOException {
			assert version.storeRows();
			in.readUnsignedVInt();
			switch (in.readByte()) {
				case RowIndexEntry.CACHE_NOT_INDEXED :
					break;
				case RowIndexEntry.CACHE_INDEXED :
					RowIndexEntry.IndexedEntry.skipForCache(in);
					break;
				case RowIndexEntry.CACHE_INDEXED_SHALLOW :
					RowIndexEntry.ShallowIndexedEntry.skipForCache(in);
					break;
				default :
					assert false;
			}
		}

		public RowIndexEntry<IndexInfo> deserialize(DataInputPlus in, long indexFilePosition) throws IOException {
			if (!(version.storeRows()))
				return RowIndexEntry.LegacyShallowIndexedEntry.deserialize(in, indexFilePosition, idxInfoSerializer);

			long position = in.readUnsignedVInt();
			int size = ((int) (in.readUnsignedVInt()));
			if (size == 0) {
				return new RowIndexEntry<>(position);
			}else {
				long headerLength = in.readUnsignedVInt();
				DeletionTime deletionTime = DeletionTime.serializer.deserialize(in);
				int columnsIndexCount = ((int) (in.readUnsignedVInt()));
				int indexedPartSize = size - (RowIndexEntry.serializedSize(deletionTime, headerLength, columnsIndexCount));
				if (size <= (DatabaseDescriptor.getColumnIndexCacheSize())) {
					return new RowIndexEntry.IndexedEntry(position, in, deletionTime, headerLength, columnsIndexCount, idxInfoSerializer, version, indexedPartSize);
				}else {
					in.skipBytes(indexedPartSize);
					return new RowIndexEntry.ShallowIndexedEntry(position, indexFilePosition, deletionTime, headerLength, columnsIndexCount, indexedPartSize, idxInfoSerializer);
				}
			}
		}

		public long deserializePositionAndSkip(DataInputPlus in) throws IOException {
			if (!(version.storeRows()))
				return RowIndexEntry.LegacyShallowIndexedEntry.deserializePositionAndSkip(in);

			return RowIndexEntry.ShallowIndexedEntry.deserializePositionAndSkip(in);
		}

		public static long readPosition(DataInputPlus in, Version version) throws IOException {
			return version.storeRows() ? in.readUnsignedVInt() : in.readLong();
		}

		public static void skip(DataInputPlus in, Version version) throws IOException {
			RowIndexEntry.Serializer.readPosition(in, version);
			RowIndexEntry.Serializer.skipPromotedIndex(in, version);
		}

		private static void skipPromotedIndex(DataInputPlus in, Version version) throws IOException {
			int size = (version.storeRows()) ? ((int) (in.readUnsignedVInt())) : in.readInt();
			if (size <= 0)
				return;

			in.skipBytesFully(size);
		}

		public static void serializeOffsets(DataOutputBuffer out, int[] indexOffsets, int columnIndexCount) throws IOException {
			for (int i = 0; i < columnIndexCount; i++)
				out.writeInt(indexOffsets[i]);

		}
	}

	private static int serializedSize(DeletionTime deletionTime, long headerLength, int columnIndexCount) {
		return ((TypeSizes.sizeofUnsignedVInt(headerLength)) + ((int) (DeletionTime.serializer.serializedSize(deletionTime)))) + (TypeSizes.sizeofUnsignedVInt(columnIndexCount));
	}

	public void serialize(DataOutputPlus out, IndexInfo.Serializer idxInfoSerializer, ByteBuffer indexInfo) throws IOException {
		out.writeUnsignedVInt(position);
		out.writeUnsignedVInt(0);
	}

	public void serializeForCache(DataOutputPlus out) throws IOException {
		out.writeUnsignedVInt(position);
		out.writeByte(RowIndexEntry.CACHE_NOT_INDEXED);
	}

	private static final class LegacyShallowIndexedEntry extends RowIndexEntry<IndexInfo> {
		private static final long BASE_SIZE;

		static {
			BASE_SIZE = ObjectSizes.measure(new RowIndexEntry.LegacyShallowIndexedEntry(0, 0, DeletionTime.LIVE, 0, new int[0], null, 0));
		}

		private final long indexFilePosition;

		private final int[] offsets;

		@Unmetered
		private final IndexInfo.Serializer idxInfoSerializer;

		private final DeletionTime deletionTime;

		private final long headerLength;

		private final int serializedSize;

		private LegacyShallowIndexedEntry(long dataFilePosition, long indexFilePosition, DeletionTime deletionTime, long headerLength, int[] offsets, IndexInfo.Serializer idxInfoSerializer, int serializedSize) {
			super(dataFilePosition);
			this.deletionTime = deletionTime;
			this.headerLength = headerLength;
			this.indexFilePosition = indexFilePosition;
			this.offsets = offsets;
			this.idxInfoSerializer = idxInfoSerializer;
			this.serializedSize = serializedSize;
		}

		@Override
		public DeletionTime deletionTime() {
			return deletionTime;
		}

		@Override
		public long headerLength() {
			return headerLength;
		}

		@Override
		public long unsharedHeapSize() {
			return (RowIndexEntry.LegacyShallowIndexedEntry.BASE_SIZE) + ((offsets.length) * (TypeSizes.sizeof(0)));
		}

		@Override
		public int columnsIndexCount() {
			return offsets.length;
		}

		@Override
		public void serialize(DataOutputPlus out, IndexInfo.Serializer idxInfoSerializer, ByteBuffer indexInfo) {
			throw new UnsupportedOperationException("serializing legacy index entries is not supported");
		}

		@Override
		public void serializeForCache(DataOutputPlus out) {
			throw new UnsupportedOperationException("serializing legacy index entries is not supported");
		}

		@Override
		public RowIndexEntry.IndexInfoRetriever openWithIndex(FileHandle indexFile) {
			int fieldsSize = ((int) (DeletionTime.serializer.serializedSize(deletionTime))) + (TypeSizes.sizeof(0));
			RowIndexEntry.indexEntrySizeHistogram.update(serializedSize);
			RowIndexEntry.indexInfoCountHistogram.update(offsets.length);
			return new RowIndexEntry.LegacyIndexInfoRetriever(((((indexFilePosition) + (TypeSizes.sizeof(0L))) + (TypeSizes.sizeof(0))) + fieldsSize), offsets, indexFile.createReader(), idxInfoSerializer);
		}

		public static RowIndexEntry<IndexInfo> deserialize(DataInputPlus in, long indexFilePosition, IndexInfo.Serializer idxInfoSerializer) throws IOException {
			long dataFilePosition = in.readLong();
			int size = in.readInt();
			if (size == 0) {
				return new RowIndexEntry<>(dataFilePosition);
			}else
				if (size <= (DatabaseDescriptor.getColumnIndexCacheSize())) {
					return new RowIndexEntry.IndexedEntry(dataFilePosition, in, idxInfoSerializer);
				}else {
					DeletionTime deletionTime = DeletionTime.serializer.deserialize(in);
					int entries = in.readInt();
					int[] offsets = new int[entries];
					TrackedDataInputPlus tracked = new TrackedDataInputPlus(in);
					long start = tracked.getBytesRead();
					long headerLength = 0L;
					for (int i = 0; i < entries; i++) {
						offsets[i] = ((int) ((tracked.getBytesRead()) - start));
						if (i == 0) {
							IndexInfo info = idxInfoSerializer.deserialize(tracked);
							headerLength = info.offset;
						}else
							idxInfoSerializer.skip(tracked);

					}
					return new RowIndexEntry.LegacyShallowIndexedEntry(dataFilePosition, indexFilePosition, deletionTime, headerLength, offsets, idxInfoSerializer, size);
				}

		}

		static long deserializePositionAndSkip(DataInputPlus in) throws IOException {
			long position = in.readLong();
			int size = in.readInt();
			if (size > 0)
				in.skipBytesFully(size);

			return position;
		}
	}

	private static final class LegacyIndexInfoRetriever extends RowIndexEntry.FileIndexInfoRetriever {
		private final int[] offsets;

		private LegacyIndexInfoRetriever(long indexFilePosition, int[] offsets, FileDataInput reader, IndexInfo.Serializer idxInfoSerializer) {
			super(indexFilePosition, reader, idxInfoSerializer);
			this.offsets = offsets;
		}

		IndexInfo fetchIndex(int index) throws IOException {
			(retrievals)++;
			indexReader.seek(((indexInfoFilePosition) + (offsets[index])));
			return idxInfoSerializer.deserialize(indexReader);
		}
	}

	private static final class IndexedEntry extends RowIndexEntry<IndexInfo> {
		private static final long BASE_SIZE;

		static {
			BASE_SIZE = ObjectSizes.measure(new RowIndexEntry.IndexedEntry(0, DeletionTime.LIVE, 0, null, null, 0, null));
		}

		private final DeletionTime deletionTime;

		private final long headerLength;

		private final IndexInfo[] columnsIndex;

		private final int[] offsets;

		private final int indexedPartSize;

		@Unmetered
		private final ISerializer<IndexInfo> idxInfoSerializer;

		private IndexedEntry(long dataFilePosition, DeletionTime deletionTime, long headerLength, IndexInfo[] columnsIndex, int[] offsets, int indexedPartSize, ISerializer<IndexInfo> idxInfoSerializer) {
			super(dataFilePosition);
			this.headerLength = headerLength;
			this.deletionTime = deletionTime;
			this.columnsIndex = columnsIndex;
			this.offsets = offsets;
			this.indexedPartSize = indexedPartSize;
			this.idxInfoSerializer = idxInfoSerializer;
		}

		private IndexedEntry(long dataFilePosition, DataInputPlus in, DeletionTime deletionTime, long headerLength, int columnIndexCount, IndexInfo.Serializer idxInfoSerializer, Version version, int indexedPartSize) throws IOException {
			super(dataFilePosition);
			this.headerLength = headerLength;
			this.deletionTime = deletionTime;
			int columnsIndexCount = columnIndexCount;
			this.columnsIndex = new IndexInfo[columnsIndexCount];
			for (int i = 0; i < columnsIndexCount; i++)
				this.columnsIndex[i] = idxInfoSerializer.deserialize(in);

			int[] offsets = null;
			if (version.storeRows()) {
				offsets = new int[this.columnsIndex.length];
				for (int i = 0; i < (offsets.length); i++)
					offsets[i] = in.readInt();

			}
			this.offsets = offsets;
			this.indexedPartSize = indexedPartSize;
			this.idxInfoSerializer = idxInfoSerializer;
		}

		private IndexedEntry(long dataFilePosition, DataInputPlus in, IndexInfo.Serializer idxInfoSerializer, Version version) throws IOException {
			super(dataFilePosition);
			this.headerLength = in.readUnsignedVInt();
			this.deletionTime = DeletionTime.serializer.deserialize(in);
			int columnsIndexCount = ((int) (in.readUnsignedVInt()));
			TrackedDataInputPlus trackedIn = new TrackedDataInputPlus(in);
			this.columnsIndex = new IndexInfo[columnsIndexCount];
			for (int i = 0; i < columnsIndexCount; i++)
				this.columnsIndex[i] = idxInfoSerializer.deserialize(trackedIn);

			this.offsets = null;
			this.indexedPartSize = ((int) (trackedIn.getBytesRead()));
			this.idxInfoSerializer = idxInfoSerializer;
		}

		private IndexedEntry(long dataFilePosition, DataInputPlus in, IndexInfo.Serializer idxInfoSerializer) throws IOException {
			super(dataFilePosition);
			long headerLength = 0;
			this.deletionTime = DeletionTime.serializer.deserialize(in);
			int columnsIndexCount = in.readInt();
			TrackedDataInputPlus trackedIn = new TrackedDataInputPlus(in);
			this.columnsIndex = new IndexInfo[columnsIndexCount];
			for (int i = 0; i < columnsIndexCount; i++) {
				this.columnsIndex[i] = idxInfoSerializer.deserialize(trackedIn);
				if (i == 0)
					headerLength = this.columnsIndex[i].offset;

			}
			this.headerLength = headerLength;
			this.offsets = null;
			this.indexedPartSize = ((int) (trackedIn.getBytesRead()));
			this.idxInfoSerializer = idxInfoSerializer;
		}

		@Override
		public boolean indexOnHeap() {
			return true;
		}

		@Override
		public int columnsIndexCount() {
			return columnsIndex.length;
		}

		@Override
		public DeletionTime deletionTime() {
			return deletionTime;
		}

		@Override
		public long headerLength() {
			return headerLength;
		}

		@Override
		public RowIndexEntry.IndexInfoRetriever openWithIndex(FileHandle indexFile) {
			RowIndexEntry.indexEntrySizeHistogram.update(((RowIndexEntry.serializedSize(deletionTime, headerLength, columnsIndex.length)) + (indexedPartSize)));
			RowIndexEntry.indexInfoCountHistogram.update(columnsIndex.length);
			return new RowIndexEntry.IndexInfoRetriever() {
				private int retrievals;

				@Override
				public IndexInfo columnsIndex(int index) {
					(retrievals)++;
					return columnsIndex[index];
				}

				public void close() {
					RowIndexEntry.indexInfoGetsHistogram.update(retrievals);
				}
			};
		}

		@Override
		public long unsharedHeapSize() {
			long entrySize = 0;
			for (IndexInfo idx : columnsIndex)
				entrySize += idx.unsharedHeapSize();

			return ((RowIndexEntry.IndexedEntry.BASE_SIZE) + entrySize) + (ObjectSizes.sizeOfReferenceArray(columnsIndex.length));
		}

		@Override
		public void serialize(DataOutputPlus out, IndexInfo.Serializer idxInfoSerializer, ByteBuffer indexInfo) throws IOException {
			assert (indexedPartSize) != (Integer.MIN_VALUE);
			out.writeUnsignedVInt(position);
			out.writeUnsignedVInt(((RowIndexEntry.serializedSize(deletionTime, headerLength, columnsIndex.length)) + (indexedPartSize)));
			out.writeUnsignedVInt(headerLength);
			DeletionTime.serializer.serialize(deletionTime, out);
			out.writeUnsignedVInt(columnsIndex.length);
			for (IndexInfo info : columnsIndex)
				idxInfoSerializer.serialize(info, out);

			for (int offset : offsets)
				out.writeInt(offset);

		}

		@Override
		public void serializeForCache(DataOutputPlus out) throws IOException {
			out.writeUnsignedVInt(position);
			out.writeByte(RowIndexEntry.CACHE_INDEXED);
			out.writeUnsignedVInt(headerLength);
			DeletionTime.serializer.serialize(deletionTime, out);
			out.writeUnsignedVInt(columnsIndexCount());
			for (IndexInfo indexInfo : columnsIndex)
				idxInfoSerializer.serialize(indexInfo, out);

		}

		static void skipForCache(DataInputPlus in) throws IOException {
			in.readUnsignedVInt();
			DeletionTime.serializer.skip(in);
			in.readUnsignedVInt();
			in.readUnsignedVInt();
		}
	}

	private static final class ShallowIndexedEntry extends RowIndexEntry<IndexInfo> {
		private static final long BASE_SIZE;

		static {
			BASE_SIZE = ObjectSizes.measure(new RowIndexEntry.ShallowIndexedEntry(0, 0, DeletionTime.LIVE, 0, 10, 0, null));
		}

		private final long indexFilePosition;

		private final DeletionTime deletionTime;

		private final long headerLength;

		private final int columnsIndexCount;

		private final int indexedPartSize;

		private final int offsetsOffset;

		@Unmetered
		private final ISerializer<IndexInfo> idxInfoSerializer;

		private final int fieldsSerializedSize;

		private ShallowIndexedEntry(long dataFilePosition, long indexFilePosition, DeletionTime deletionTime, long headerLength, int columnIndexCount, int indexedPartSize, ISerializer<IndexInfo> idxInfoSerializer) {
			super(dataFilePosition);
			assert columnIndexCount > 1;
			this.indexFilePosition = indexFilePosition;
			this.headerLength = headerLength;
			this.deletionTime = deletionTime;
			this.columnsIndexCount = columnIndexCount;
			this.indexedPartSize = indexedPartSize;
			this.idxInfoSerializer = idxInfoSerializer;
			this.fieldsSerializedSize = RowIndexEntry.serializedSize(deletionTime, headerLength, columnIndexCount);
			this.offsetsOffset = (indexedPartSize + (fieldsSerializedSize)) - ((columnsIndexCount) * (TypeSizes.sizeof(0)));
		}

		private ShallowIndexedEntry(long dataFilePosition, DataInputPlus in, IndexInfo.Serializer idxInfoSerializer) throws IOException {
			super(dataFilePosition);
			this.indexFilePosition = in.readUnsignedVInt();
			this.headerLength = in.readUnsignedVInt();
			this.deletionTime = DeletionTime.serializer.deserialize(in);
			this.columnsIndexCount = ((int) (in.readUnsignedVInt()));
			this.indexedPartSize = ((int) (in.readUnsignedVInt()));
			this.idxInfoSerializer = idxInfoSerializer;
			this.fieldsSerializedSize = RowIndexEntry.serializedSize(deletionTime, headerLength, columnsIndexCount);
			this.offsetsOffset = ((indexedPartSize) + (fieldsSerializedSize)) - ((columnsIndexCount) * (TypeSizes.sizeof(0)));
		}

		@Override
		public int columnsIndexCount() {
			return columnsIndexCount;
		}

		@Override
		public DeletionTime deletionTime() {
			return deletionTime;
		}

		@Override
		public long headerLength() {
			return headerLength;
		}

		@Override
		public RowIndexEntry.IndexInfoRetriever openWithIndex(FileHandle indexFile) {
			RowIndexEntry.indexEntrySizeHistogram.update(((indexedPartSize) + (fieldsSerializedSize)));
			RowIndexEntry.indexInfoCountHistogram.update(columnsIndexCount);
			return new RowIndexEntry.ShallowInfoRetriever(((((indexFilePosition) + (VIntCoding.computeUnsignedVIntSize(position))) + (VIntCoding.computeUnsignedVIntSize(((indexedPartSize) + (fieldsSerializedSize))))) + (fieldsSerializedSize)), ((offsetsOffset) - (fieldsSerializedSize)), indexFile.createReader(), idxInfoSerializer);
		}

		@Override
		public long unsharedHeapSize() {
			return RowIndexEntry.ShallowIndexedEntry.BASE_SIZE;
		}

		@Override
		public void serialize(DataOutputPlus out, IndexInfo.Serializer idxInfoSerializer, ByteBuffer indexInfo) throws IOException {
			out.writeUnsignedVInt(position);
			out.writeUnsignedVInt(((fieldsSerializedSize) + (indexInfo.limit())));
			out.writeUnsignedVInt(headerLength);
			DeletionTime.serializer.serialize(deletionTime, out);
			out.writeUnsignedVInt(columnsIndexCount);
			out.write(indexInfo);
		}

		static long deserializePositionAndSkip(DataInputPlus in) throws IOException {
			long position = in.readUnsignedVInt();
			int size = ((int) (in.readUnsignedVInt()));
			if (size > 0)
				in.skipBytesFully(size);

			return position;
		}

		@Override
		public void serializeForCache(DataOutputPlus out) throws IOException {
			out.writeUnsignedVInt(position);
			out.writeByte(RowIndexEntry.CACHE_INDEXED_SHALLOW);
			out.writeUnsignedVInt(indexFilePosition);
			out.writeUnsignedVInt(headerLength);
			DeletionTime.serializer.serialize(deletionTime, out);
			out.writeUnsignedVInt(columnsIndexCount);
			out.writeUnsignedVInt(indexedPartSize);
		}

		static void skipForCache(DataInputPlus in) throws IOException {
			in.readUnsignedVInt();
			in.readUnsignedVInt();
			DeletionTime.serializer.skip(in);
			in.readUnsignedVInt();
			in.readUnsignedVInt();
		}
	}

	private static final class ShallowInfoRetriever extends RowIndexEntry.FileIndexInfoRetriever {
		private final int offsetsOffset;

		private ShallowInfoRetriever(long indexInfoFilePosition, int offsetsOffset, FileDataInput indexReader, ISerializer<IndexInfo> idxInfoSerializer) {
			super(indexInfoFilePosition, indexReader, idxInfoSerializer);
			this.offsetsOffset = offsetsOffset;
		}

		IndexInfo fetchIndex(int index) throws IOException {
			(retrievals)++;
			indexReader.seek((((indexInfoFilePosition) + (offsetsOffset)) + (index * (TypeSizes.sizeof(0)))));
			int indexInfoPos = indexReader.readInt();
			indexReader.seek(((indexInfoFilePosition) + indexInfoPos));
			return idxInfoSerializer.deserialize(indexReader);
		}
	}

	public interface IndexInfoRetriever extends AutoCloseable {
		IndexInfo columnsIndex(int index) throws IOException;

		void close() throws IOException;
	}

	private abstract static class FileIndexInfoRetriever implements RowIndexEntry.IndexInfoRetriever {
		final long indexInfoFilePosition;

		final ISerializer<IndexInfo> idxInfoSerializer;

		final FileDataInput indexReader;

		int retrievals;

		FileIndexInfoRetriever(long indexInfoFilePosition, FileDataInput indexReader, ISerializer<IndexInfo> idxInfoSerializer) {
			this.indexInfoFilePosition = indexInfoFilePosition;
			this.idxInfoSerializer = idxInfoSerializer;
			this.indexReader = indexReader;
		}

		public final IndexInfo columnsIndex(int index) throws IOException {
			return fetchIndex(index);
		}

		abstract IndexInfo fetchIndex(int index) throws IOException;

		public void close() throws IOException {
			indexReader.close();
			RowIndexEntry.indexInfoGetsHistogram.update(retrievals);
		}
	}
}

