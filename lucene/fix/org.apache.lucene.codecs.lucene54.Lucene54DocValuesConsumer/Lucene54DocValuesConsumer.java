

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.apache.lucene.codecs.CodecUtil;
import org.apache.lucene.codecs.DocValuesConsumer;
import org.apache.lucene.codecs.DocValuesProducer;
import org.apache.lucene.codecs.LegacyDocValuesIterables;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.IndexFileNames;
import org.apache.lucene.index.SegmentInfo;
import org.apache.lucene.index.SegmentWriteState;
import org.apache.lucene.index.SortedDocValues;
import org.apache.lucene.index.SortedSetDocValues;
import org.apache.lucene.store.DataOutput;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.RAMOutputStream;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefBuilder;
import org.apache.lucene.util.IOUtils;
import org.apache.lucene.util.LongsRef;
import org.apache.lucene.util.MathUtil;
import org.apache.lucene.util.PagedBytes;
import org.apache.lucene.util.packed.DirectWriter;


final class Lucene54DocValuesConsumer extends DocValuesConsumer implements Closeable {
	enum NumberType {

		ORDINAL,
		VALUE;}

	IndexOutput data;

	IndexOutput meta;

	final int maxDoc;

	public Lucene54DocValuesConsumer(SegmentWriteState state, String dataCodec, String dataExtension, String metaCodec, String metaExtension) throws IOException {
		boolean success = false;
		try {
			String dataName = IndexFileNames.segmentFileName(state.segmentInfo.name, state.segmentSuffix, dataExtension);
			data = state.directory.createOutput(dataName, state.context);
			String metaName = IndexFileNames.segmentFileName(state.segmentInfo.name, state.segmentSuffix, metaExtension);
			meta = state.directory.createOutput(metaName, state.context);
			maxDoc = state.segmentInfo.maxDoc();
			success = true;
		} finally {
			if (!success) {
				IOUtils.closeWhileHandlingException(this);
			}
		}
	}

	@Override
	public void addNumericField(FieldInfo field, DocValuesProducer valuesProducer) throws IOException {
		addNumericField(field, LegacyDocValuesIterables.numericIterable(field, valuesProducer, maxDoc), Lucene54DocValuesConsumer.NumberType.VALUE);
	}

	void addNumericField(FieldInfo field, Iterable<Number> values, Lucene54DocValuesConsumer.NumberType numberType) throws IOException {
		long count = 0;
		long minValue = Long.MAX_VALUE;
		long maxValue = Long.MIN_VALUE;
		long gcd = 0;
		long missingCount = 0;
		long zeroCount = 0;
		HashSet<Long> uniqueValues = null;
		long missingOrdCount = 0;
		if (numberType == (Lucene54DocValuesConsumer.NumberType.VALUE)) {
			uniqueValues = new HashSet<>();
			for (Number nv : values) {
				final long v;
				if (nv == null) {
					v = 0;
					missingCount++;
					zeroCount++;
				}else {
					v = nv.longValue();
					if (v == 0) {
						zeroCount++;
					}
				}
				if (gcd != 1) {
					if ((v < ((Long.MIN_VALUE) / 2)) || (v > ((Long.MAX_VALUE) / 2))) {
						gcd = 1;
					}else
						if (count != 0) {
							gcd = MathUtil.gcd(gcd, (v - minValue));
						}

				}
				minValue = Math.min(minValue, v);
				maxValue = Math.max(maxValue, v);
				if (uniqueValues != null) {
					if (uniqueValues.add(v)) {
						if ((uniqueValues.size()) > 256) {
							uniqueValues = null;
						}
					}
				}
				++count;
			}
		}else {
			for (Number nv : values) {
				long v = nv.longValue();
				if (v == (-1L)) {
					missingOrdCount++;
				}
				minValue = Math.min(minValue, v);
				maxValue = Math.max(maxValue, v);
				++count;
			}
		}
		final long delta = maxValue - minValue;
		final int deltaBitsRequired = DirectWriter.unsignedBitsRequired(delta);
		final int tableBitsRequired = (uniqueValues == null) ? Integer.MAX_VALUE : DirectWriter.bitsRequired(((uniqueValues.size()) - 1));
		final boolean sparse;
		switch (numberType) {
			case VALUE :
				sparse = (((double) (missingCount)) / count) >= 0.99;
				break;
			case ORDINAL :
				sparse = (((double) (missingOrdCount)) / count) >= 0.99;
				break;
			default :
				throw new AssertionError();
		}
		final int format;
		if (((uniqueValues != null) && (count <= (Integer.MAX_VALUE))) && (((uniqueValues.size()) == 1) || ((((uniqueValues.size()) == 2) && (missingCount > 0)) && (zeroCount == missingCount)))) {
		}else
			if (sparse && (count >= 1024)) {
			}else
				if ((uniqueValues != null) && (tableBitsRequired < deltaBitsRequired)) {
				}else
					if ((gcd != 0) && (gcd != 1)) {
						final long gcdDelta = (maxValue - minValue) / gcd;
						final long gcdBitsRequired = DirectWriter.unsignedBitsRequired(gcdDelta);
					}else {
					}



		meta.writeVInt(field.number);
		format = 0;
		meta.writeVInt(format);
		meta.writeLong(data.getFilePointer());
		meta.writeVLong(count);
		meta.writeLong(data.getFilePointer());
	}

	void writeMissingBitset(Iterable<?> values) throws IOException {
		byte bits = 0;
		int count = 0;
		for (Object v : values) {
			if (count == 8) {
				data.writeByte(bits);
				count = 0;
				bits = 0;
			}
			if (v != null) {
				bits |= 1 << (count & 7);
			}
			count++;
		}
		if (count > 0) {
			data.writeByte(bits);
		}
	}

	long writeSparseMissingBitset(Iterable<Number> values, Lucene54DocValuesConsumer.NumberType numberType, long numDocsWithValue) throws IOException {
		meta.writeVLong(numDocsWithValue);
		long docID = 0;
		for (Number nv : values) {
			switch (numberType) {
				case VALUE :
					if (nv != null) {
					}
					break;
				case ORDINAL :
					if ((nv.longValue()) != (-1L)) {
					}
					break;
				default :
					throw new AssertionError();
			}
			docID++;
		}
		return docID;
	}

	@Override
	public void addBinaryField(FieldInfo field, DocValuesProducer valuesProducer) throws IOException {
		addBinaryField(field, LegacyDocValuesIterables.binaryIterable(field, valuesProducer, maxDoc));
	}

	private void addBinaryField(FieldInfo field, Iterable<BytesRef> values) throws IOException {
		meta.writeVInt(field.number);
		int minLength = Integer.MAX_VALUE;
		int maxLength = Integer.MIN_VALUE;
		final long startFP = data.getFilePointer();
		long count = 0;
		long missingCount = 0;
		for (BytesRef v : values) {
			final int length;
			if (v == null) {
				length = 0;
				missingCount++;
			}else {
				length = v.length;
			}
			minLength = Math.min(minLength, length);
			maxLength = Math.max(maxLength, length);
			if (v != null) {
				data.writeBytes(v.bytes, v.offset, v.length);
			}
			count++;
		}
		if (missingCount == 0) {
		}else
			if (missingCount == count) {
			}else {
				meta.writeLong(data.getFilePointer());
				writeMissingBitset(values);
			}

		meta.writeVInt(minLength);
		meta.writeVInt(maxLength);
		meta.writeVLong(count);
		meta.writeLong(startFP);
		if (minLength != maxLength) {
			meta.writeLong(data.getFilePointer());
			long addr = 0;
			for (BytesRef v : values) {
				if (v != null) {
					addr += v.length;
				}
			}
			meta.writeLong(data.getFilePointer());
		}
	}

	private void addTermsDict(FieldInfo field, final Iterable<BytesRef> values) throws IOException {
		int minLength = Integer.MAX_VALUE;
		int maxLength = Integer.MIN_VALUE;
		long numValues = 0;
		BytesRefBuilder previousValue = new BytesRefBuilder();
		long prefixSum = 0;
		for (BytesRef v : values) {
			minLength = Math.min(minLength, v.length);
			maxLength = Math.max(maxLength, v.length);
			if (minLength == maxLength) {
			}
			numValues++;
		}
	}

	private void flushTermsDictBlock(RAMOutputStream headerBuffer, RAMOutputStream bytesBuffer, int[] suffixDeltas) throws IOException {
		boolean twoByte = false;
		for (int i = 1; i < (suffixDeltas.length); i++) {
			if ((suffixDeltas[i]) > 254) {
				twoByte = true;
			}
		}
		if (twoByte) {
			headerBuffer.writeByte(((byte) (255)));
			for (int i = 1; i < (suffixDeltas.length); i++) {
				headerBuffer.writeShort(((short) (suffixDeltas[i])));
			}
		}else {
			for (int i = 1; i < (suffixDeltas.length); i++) {
				headerBuffer.writeByte(((byte) (suffixDeltas[i])));
			}
		}
		headerBuffer.writeTo(data);
		headerBuffer.reset();
		bytesBuffer.writeTo(data);
		bytesBuffer.reset();
	}

	private void addReverseTermIndex(FieldInfo field, final Iterable<BytesRef> values, int maxLength) throws IOException {
		long count = 0;
		BytesRefBuilder priorTerm = new BytesRefBuilder();
		priorTerm.grow(maxLength);
		BytesRef indexTerm = new BytesRef();
		long startFP = data.getFilePointer();
		PagedBytes pagedBytes = new PagedBytes(15);
		for (BytesRef b : values) {
			count++;
		}
		long numBytes = pagedBytes.getPointer();
		pagedBytes.freeze(true);
		PagedBytes.PagedBytesDataInput in = pagedBytes.getDataInput();
		meta.writeLong(startFP);
		data.writeVLong(numBytes);
		data.copyBytes(in, numBytes);
	}

	@Override
	public void addSortedField(FieldInfo field, DocValuesProducer valuesProducer) throws IOException {
		meta.writeVInt(field.number);
		addTermsDict(field, LegacyDocValuesIterables.valuesIterable(valuesProducer.getSorted(field)));
		addNumericField(field, LegacyDocValuesIterables.sortedOrdIterable(valuesProducer, field, maxDoc), Lucene54DocValuesConsumer.NumberType.ORDINAL);
	}

	private void addSortedField(FieldInfo field, Iterable<BytesRef> values, Iterable<Number> ords) throws IOException {
		meta.writeVInt(field.number);
		addTermsDict(field, values);
		addNumericField(field, ords, Lucene54DocValuesConsumer.NumberType.ORDINAL);
	}

	@Override
	public void addSortedNumericField(FieldInfo field, final DocValuesProducer valuesProducer) throws IOException {
		final Iterable<Number> docToValueCount = LegacyDocValuesIterables.sortedNumericToDocCount(valuesProducer, field, maxDoc);
		final Iterable<Number> values = LegacyDocValuesIterables.sortedNumericToValues(valuesProducer, field);
		meta.writeVInt(field.number);
		if (DocValuesConsumer.isSingleValued(docToValueCount)) {
			addNumericField(field, DocValuesConsumer.singletonView(docToValueCount, values, null), Lucene54DocValuesConsumer.NumberType.VALUE);
		}else {
			final SortedSet<LongsRef> uniqueValueSets = uniqueValueSets(docToValueCount, values);
			if (uniqueValueSets != null) {
				writeDictionary(uniqueValueSets);
				addNumericField(field, docToSetId(uniqueValueSets, docToValueCount, values), Lucene54DocValuesConsumer.NumberType.ORDINAL);
			}else {
				addNumericField(field, values, Lucene54DocValuesConsumer.NumberType.VALUE);
				addOrdIndex(field, docToValueCount);
			}
		}
	}

	@Override
	public void addSortedSetField(FieldInfo field, DocValuesProducer valuesProducer) throws IOException {
		Iterable<BytesRef> values = LegacyDocValuesIterables.valuesIterable(valuesProducer.getSortedSet(field));
		Iterable<Number> docToOrdCount = LegacyDocValuesIterables.sortedSetOrdCountIterable(valuesProducer, field, maxDoc);
		Iterable<Number> ords = LegacyDocValuesIterables.sortedSetOrdsIterable(valuesProducer, field);
		meta.writeVInt(field.number);
		if (DocValuesConsumer.isSingleValued(docToOrdCount)) {
			addSortedField(field, values, DocValuesConsumer.singletonView(docToOrdCount, ords, (-1L)));
		}else {
			final SortedSet<LongsRef> uniqueValueSets = uniqueValueSets(docToOrdCount, ords);
			if (uniqueValueSets != null) {
				writeDictionary(uniqueValueSets);
				addTermsDict(field, values);
				addNumericField(field, docToSetId(uniqueValueSets, docToOrdCount, ords), Lucene54DocValuesConsumer.NumberType.ORDINAL);
			}else {
				addTermsDict(field, values);
				addNumericField(field, ords, Lucene54DocValuesConsumer.NumberType.ORDINAL);
				addOrdIndex(field, docToOrdCount);
			}
		}
	}

	private SortedSet<LongsRef> uniqueValueSets(Iterable<Number> docToValueCount, Iterable<Number> values) {
		Set<LongsRef> uniqueValueSet = new HashSet<>();
		LongsRef docValues = new LongsRef(256);
		Iterator<Number> valueCountIterator = docToValueCount.iterator();
		Iterator<Number> valueIterator = values.iterator();
		int totalDictSize = 0;
		while (valueCountIterator.hasNext()) {
			docValues.length = valueCountIterator.next().intValue();
			if ((docValues.length) > 256) {
				return null;
			}
			for (int i = 0; i < (docValues.length); ++i) {
				docValues.longs[i] = valueIterator.next().longValue();
			}
			if (uniqueValueSet.contains(docValues)) {
				continue;
			}
			totalDictSize += docValues.length;
			if (totalDictSize > 256) {
				return null;
			}
			uniqueValueSet.add(new LongsRef(Arrays.copyOf(docValues.longs, docValues.length), 0, docValues.length));
		} 
		assert (valueIterator.hasNext()) == false;
		return new TreeSet<>(uniqueValueSet);
	}

	private void writeDictionary(SortedSet<LongsRef> uniqueValueSets) throws IOException {
		int lengthSum = 0;
		for (LongsRef longs : uniqueValueSets) {
			lengthSum += longs.length;
		}
		meta.writeInt(lengthSum);
		for (LongsRef valueSet : uniqueValueSets) {
			for (int i = 0; i < (valueSet.length); ++i) {
				meta.writeLong(valueSet.longs[((valueSet.offset) + i)]);
			}
		}
		meta.writeInt(uniqueValueSets.size());
		for (LongsRef valueSet : uniqueValueSets) {
			meta.writeInt(valueSet.length);
		}
	}

	private Iterable<Number> docToSetId(SortedSet<LongsRef> uniqueValueSets, Iterable<Number> docToValueCount, Iterable<Number> values) {
		final Map<LongsRef, Integer> setIds = new HashMap<>();
		int i = 0;
		for (LongsRef set : uniqueValueSets) {
			setIds.put(set, (i++));
		}
		assert i == (uniqueValueSets.size());
		return new Iterable<Number>() {
			@Override
			public Iterator<Number> iterator() {
				final Iterator<Number> valueCountIterator = docToValueCount.iterator();
				final Iterator<Number> valueIterator = values.iterator();
				final LongsRef docValues = new LongsRef(256);
				return new Iterator<Number>() {
					@Override
					public boolean hasNext() {
						return valueCountIterator.hasNext();
					}

					@Override
					public Number next() {
						docValues.length = valueCountIterator.next().intValue();
						for (int i = 0; i < (docValues.length); ++i) {
							docValues.longs[i] = valueIterator.next().longValue();
						}
						final Integer id = setIds.get(docValues);
						assert id != null;
						return id;
					}
				};
			}
		};
	}

	private void addOrdIndex(FieldInfo field, Iterable<Number> values) throws IOException {
		meta.writeVInt(field.number);
		meta.writeLong((-1L));
		meta.writeLong(data.getFilePointer());
		meta.writeVLong(maxDoc);
		long addr = 0;
		for (Number v : values) {
			addr += v.longValue();
		}
		meta.writeLong(data.getFilePointer());
	}

	@Override
	public void close() throws IOException {
		boolean success = false;
		try {
			if ((meta) != null) {
				meta.writeVInt((-1));
				CodecUtil.writeFooter(meta);
			}
			if ((data) != null) {
				CodecUtil.writeFooter(data);
			}
			success = true;
		} finally {
			if (success) {
				IOUtils.close(data, meta);
			}else {
				IOUtils.closeWhileHandlingException(data, meta);
			}
			meta = data = null;
		}
	}
}

