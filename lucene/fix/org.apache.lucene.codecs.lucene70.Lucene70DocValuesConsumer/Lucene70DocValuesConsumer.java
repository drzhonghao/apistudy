

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.lucene.codecs.CodecUtil;
import org.apache.lucene.codecs.DocValuesConsumer;
import org.apache.lucene.codecs.DocValuesProducer;
import org.apache.lucene.index.BinaryDocValues;
import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.EmptyDocValuesProducer;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.IndexFileNames;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.SegmentInfo;
import org.apache.lucene.index.SegmentWriteState;
import org.apache.lucene.index.SortedDocValues;
import org.apache.lucene.index.SortedNumericDocValues;
import org.apache.lucene.index.SortedSetDocValues;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.SortedSetSelector;
import org.apache.lucene.store.DataOutput;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.GrowableByteArrayDataOutput;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.RAMOutputStream;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefBuilder;
import org.apache.lucene.util.BytesRefIterator;
import org.apache.lucene.util.IOUtils;
import org.apache.lucene.util.MathUtil;
import org.apache.lucene.util.packed.DirectWriter;

import static org.apache.lucene.search.SortedSetSelector.Type.MIN;


final class Lucene70DocValuesConsumer extends DocValuesConsumer implements Closeable {
	IndexOutput data;

	IndexOutput meta;

	final int maxDoc;

	public Lucene70DocValuesConsumer(SegmentWriteState state, String dataCodec, String dataExtension, String metaCodec, String metaExtension) throws IOException {
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
	public void close() throws IOException {
		boolean success = false;
		try {
			if ((meta) != null) {
				meta.writeInt((-1));
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

	@Override
	public void addNumericField(FieldInfo field, DocValuesProducer valuesProducer) throws IOException {
		meta.writeInt(field.number);
		writeValues(field, new EmptyDocValuesProducer() {
			@Override
			public SortedNumericDocValues getSortedNumeric(FieldInfo field) throws IOException {
				return DocValues.singleton(valuesProducer.getNumeric(field));
			}
		});
	}

	private static class MinMaxTracker {
		long min;

		long max;

		long numValues;

		long spaceInBits;

		MinMaxTracker() {
			reset();
			spaceInBits = 0;
		}

		private void reset() {
			min = Long.MAX_VALUE;
			max = Long.MIN_VALUE;
			numValues = 0;
		}

		void update(long v) {
			min = Math.min(min, v);
			max = Math.max(max, v);
			++(numValues);
		}

		void finish() {
			if ((max) > (min)) {
				spaceInBits += (DirectWriter.unsignedBitsRequired(((max) - (min)))) * (numValues);
			}
		}

		void nextBlock() {
			finish();
			reset();
		}
	}

	private long[] writeValues(FieldInfo field, DocValuesProducer valuesProducer) throws IOException {
		SortedNumericDocValues values = valuesProducer.getSortedNumeric(field);
		int numDocsWithValue = 0;
		Lucene70DocValuesConsumer.MinMaxTracker minMax = new Lucene70DocValuesConsumer.MinMaxTracker();
		Lucene70DocValuesConsumer.MinMaxTracker blockMinMax = new Lucene70DocValuesConsumer.MinMaxTracker();
		long gcd = 0;
		Set<Long> uniqueValues = new HashSet<>();
		for (int doc = values.nextDoc(); doc != (DocIdSetIterator.NO_MORE_DOCS); doc = values.nextDoc()) {
			for (int i = 0, count = values.docValueCount(); i < count; ++i) {
				long v = values.nextValue();
				if (gcd != 1) {
					if ((v < ((Long.MIN_VALUE) / 2)) || (v > ((Long.MAX_VALUE) / 2))) {
						gcd = 1;
					}else
						if ((minMax.numValues) != 0) {
							gcd = MathUtil.gcd(gcd, (v - (minMax.min)));
						}

				}
				minMax.update(v);
				blockMinMax.update(v);
				if (((uniqueValues != null) && (uniqueValues.add(v))) && ((uniqueValues.size()) > 256)) {
					uniqueValues = null;
				}
			}
			numDocsWithValue++;
		}
		minMax.finish();
		blockMinMax.finish();
		final long numValues = minMax.numValues;
		long min = minMax.min;
		final long max = minMax.max;
		assert (blockMinMax.spaceInBits) <= (minMax.spaceInBits);
		if (numDocsWithValue == 0) {
			meta.writeLong((-2));
			meta.writeLong(0L);
		}else
			if (numDocsWithValue == (maxDoc)) {
				meta.writeLong((-1));
				meta.writeLong(0L);
			}else {
				long offset = data.getFilePointer();
				meta.writeLong(offset);
				values = valuesProducer.getSortedNumeric(field);
				meta.writeLong(((data.getFilePointer()) - offset));
			}

		meta.writeLong(numValues);
		final int numBitsPerValue;
		boolean doBlocks = false;
		Map<Long, Integer> encode = null;
		if (min >= max) {
			numBitsPerValue = 0;
			meta.writeInt((-1));
		}else {
			if (((uniqueValues != null) && ((uniqueValues.size()) > 1)) && ((DirectWriter.unsignedBitsRequired(((uniqueValues.size()) - 1))) < (DirectWriter.unsignedBitsRequired(((max - min) / gcd))))) {
				numBitsPerValue = DirectWriter.unsignedBitsRequired(((uniqueValues.size()) - 1));
				final Long[] sortedUniqueValues = uniqueValues.toArray(new Long[0]);
				Arrays.sort(sortedUniqueValues);
				meta.writeInt(sortedUniqueValues.length);
				for (Long v : sortedUniqueValues) {
					meta.writeLong(v);
				}
				encode = new HashMap<>();
				for (int i = 0; i < (sortedUniqueValues.length); ++i) {
					encode.put(sortedUniqueValues[i], i);
				}
				min = 0;
				gcd = 1;
			}else {
				uniqueValues = null;
				doBlocks = ((minMax.spaceInBits) > 0) && ((((double) (blockMinMax.spaceInBits)) / (minMax.spaceInBits)) <= 0.9);
				if (doBlocks) {
					numBitsPerValue = 255;
				}else {
					numBitsPerValue = DirectWriter.unsignedBitsRequired(((max - min) / gcd));
					if (((gcd == 1) && (min > 0)) && ((DirectWriter.unsignedBitsRequired(max)) == (DirectWriter.unsignedBitsRequired((max - min))))) {
						min = 0;
					}
					meta.writeInt((-1));
				}
			}
		}
		meta.writeByte(((byte) (numBitsPerValue)));
		meta.writeLong(min);
		meta.writeLong(gcd);
		long startOffset = data.getFilePointer();
		meta.writeLong(startOffset);
		if (doBlocks) {
			writeValuesMultipleBlocks(valuesProducer.getSortedNumeric(field), gcd);
		}else
			if (numBitsPerValue != 0) {
				writeValuesSingleBlock(valuesProducer.getSortedNumeric(field), numValues, numBitsPerValue, min, gcd, encode);
			}

		meta.writeLong(((data.getFilePointer()) - startOffset));
		return new long[]{ numDocsWithValue, numValues };
	}

	private void writeValuesSingleBlock(SortedNumericDocValues values, long numValues, int numBitsPerValue, long min, long gcd, Map<Long, Integer> encode) throws IOException {
		DirectWriter writer = DirectWriter.getInstance(data, numValues, numBitsPerValue);
		for (int doc = values.nextDoc(); doc != (DocIdSetIterator.NO_MORE_DOCS); doc = values.nextDoc()) {
			for (int i = 0, count = values.docValueCount(); i < count; ++i) {
				long v = values.nextValue();
				if (encode == null) {
					writer.add(((v - min) / gcd));
				}else {
					writer.add(encode.get(v));
				}
			}
		}
		writer.finish();
	}

	private void writeValuesMultipleBlocks(SortedNumericDocValues values, long gcd) throws IOException {
		int upTo = 0;
		for (int doc = values.nextDoc(); doc != (DocIdSetIterator.NO_MORE_DOCS); doc = values.nextDoc()) {
			for (int i = 0, count = values.docValueCount(); i < count; ++i) {
			}
		}
		if (upTo > 0) {
		}
	}

	private void writeBlock(long[] values, int length, long gcd, GrowableByteArrayDataOutput buffer) throws IOException {
		assert length > 0;
		long min = values[0];
		long max = values[0];
		for (int i = 1; i < length; ++i) {
			final long v = values[i];
			assert (Math.floorMod(((values[i]) - min), gcd)) == 0;
			min = Math.min(min, v);
			max = Math.max(max, v);
		}
		if (min == max) {
			data.writeByte(((byte) (0)));
			data.writeLong(min);
		}else {
			final int bitsPerValue = DirectWriter.unsignedBitsRequired((max - min));
			buffer.reset();
			assert (buffer.getPosition()) == 0;
			final DirectWriter w = DirectWriter.getInstance(buffer, length, bitsPerValue);
			for (int i = 0; i < length; ++i) {
				w.add((((values[i]) - min) / gcd));
			}
			w.finish();
			data.writeByte(((byte) (bitsPerValue)));
			data.writeLong(min);
			data.writeInt(buffer.getPosition());
			data.writeBytes(buffer.getBytes(), buffer.getPosition());
		}
	}

	@Override
	public void addBinaryField(FieldInfo field, DocValuesProducer valuesProducer) throws IOException {
		meta.writeInt(field.number);
		BinaryDocValues values = valuesProducer.getBinary(field);
		long start = data.getFilePointer();
		meta.writeLong(start);
		int numDocsWithField = 0;
		int minLength = Integer.MAX_VALUE;
		int maxLength = 0;
		for (int doc = values.nextDoc(); doc != (DocIdSetIterator.NO_MORE_DOCS); doc = values.nextDoc()) {
			numDocsWithField++;
			BytesRef v = values.binaryValue();
			int length = v.length;
			data.writeBytes(v.bytes, v.offset, v.length);
			minLength = Math.min(length, minLength);
			maxLength = Math.max(length, maxLength);
		}
		assert numDocsWithField <= (maxDoc);
		meta.writeLong(((data.getFilePointer()) - start));
		if (numDocsWithField == 0) {
			meta.writeLong((-2));
			meta.writeLong(0L);
		}else
			if (numDocsWithField == (maxDoc)) {
				meta.writeLong((-1));
				meta.writeLong(0L);
			}else {
				long offset = data.getFilePointer();
				meta.writeLong(offset);
				values = valuesProducer.getBinary(field);
				meta.writeLong(((data.getFilePointer()) - offset));
			}

		meta.writeInt(numDocsWithField);
		meta.writeInt(minLength);
		meta.writeInt(maxLength);
		if (maxLength > minLength) {
			start = data.getFilePointer();
			meta.writeLong(start);
			long addr = 0;
			values = valuesProducer.getBinary(field);
			for (int doc = values.nextDoc(); doc != (DocIdSetIterator.NO_MORE_DOCS); doc = values.nextDoc()) {
				addr += values.binaryValue().length;
			}
			meta.writeLong(((data.getFilePointer()) - start));
		}
	}

	@Override
	public void addSortedField(FieldInfo field, DocValuesProducer valuesProducer) throws IOException {
		meta.writeInt(field.number);
		doAddSortedField(field, valuesProducer);
	}

	private void doAddSortedField(FieldInfo field, DocValuesProducer valuesProducer) throws IOException {
		SortedDocValues values = valuesProducer.getSorted(field);
		int numDocsWithField = 0;
		for (int doc = values.nextDoc(); doc != (DocIdSetIterator.NO_MORE_DOCS); doc = values.nextDoc()) {
			numDocsWithField++;
		}
		if (numDocsWithField == 0) {
			meta.writeLong((-2));
			meta.writeLong(0L);
		}else
			if (numDocsWithField == (maxDoc)) {
				meta.writeLong((-1));
				meta.writeLong(0L);
			}else {
				long offset = data.getFilePointer();
				meta.writeLong(offset);
				values = valuesProducer.getSorted(field);
				meta.writeLong(((data.getFilePointer()) - offset));
			}

		meta.writeInt(numDocsWithField);
		if ((values.getValueCount()) <= 1) {
			meta.writeByte(((byte) (0)));
			meta.writeLong(0L);
			meta.writeLong(0L);
		}else {
			int numberOfBitsPerOrd = DirectWriter.unsignedBitsRequired(((values.getValueCount()) - 1));
			meta.writeByte(((byte) (numberOfBitsPerOrd)));
			long start = data.getFilePointer();
			meta.writeLong(start);
			DirectWriter writer = DirectWriter.getInstance(data, numDocsWithField, numberOfBitsPerOrd);
			values = valuesProducer.getSorted(field);
			for (int doc = values.nextDoc(); doc != (DocIdSetIterator.NO_MORE_DOCS); doc = values.nextDoc()) {
				writer.add(values.ordValue());
			}
			writer.finish();
			meta.writeLong(((data.getFilePointer()) - start));
		}
		addTermsDict(DocValues.singleton(valuesProducer.getSorted(field)));
	}

	private void addTermsDict(SortedSetDocValues values) throws IOException {
		final long size = values.getValueCount();
		meta.writeVLong(size);
		RAMOutputStream addressBuffer = new RAMOutputStream();
		BytesRefBuilder previous = new BytesRefBuilder();
		long ord = 0;
		long start = data.getFilePointer();
		int maxLength = 0;
		TermsEnum iterator = values.termsEnum();
		for (BytesRef term = iterator.next(); term != null; term = iterator.next()) {
			maxLength = Math.max(maxLength, term.length);
			previous.copyBytes(term);
			++ord;
		}
		meta.writeInt(maxLength);
		meta.writeLong(start);
		meta.writeLong(((data.getFilePointer()) - start));
		start = data.getFilePointer();
		addressBuffer.writeTo(data);
		meta.writeLong(start);
		meta.writeLong(((data.getFilePointer()) - start));
		writeTermsIndex(values);
	}

	private void writeTermsIndex(SortedSetDocValues values) throws IOException {
		final long size = values.getValueCount();
		long start = data.getFilePointer();
		RAMOutputStream addressBuffer = new RAMOutputStream();
		TermsEnum iterator = values.termsEnum();
		BytesRefBuilder previous = new BytesRefBuilder();
		long offset = 0;
		long ord = 0;
		for (BytesRef term = iterator.next(); term != null; term = iterator.next()) {
			++ord;
		}
		meta.writeLong(start);
		meta.writeLong(((data.getFilePointer()) - start));
		start = data.getFilePointer();
		addressBuffer.writeTo(data);
		meta.writeLong(start);
		meta.writeLong(((data.getFilePointer()) - start));
	}

	@Override
	public void addSortedNumericField(FieldInfo field, DocValuesProducer valuesProducer) throws IOException {
		meta.writeInt(field.number);
		long[] stats = writeValues(field, valuesProducer);
		int numDocsWithField = Math.toIntExact(stats[0]);
		long numValues = stats[1];
		assert numValues >= numDocsWithField;
		meta.writeInt(numDocsWithField);
		if (numValues > numDocsWithField) {
			long start = data.getFilePointer();
			meta.writeLong(start);
			long addr = 0;
			SortedNumericDocValues values = valuesProducer.getSortedNumeric(field);
			for (int doc = values.nextDoc(); doc != (DocIdSetIterator.NO_MORE_DOCS); doc = values.nextDoc()) {
				addr += values.docValueCount();
			}
			meta.writeLong(((data.getFilePointer()) - start));
		}
	}

	@Override
	public void addSortedSetField(FieldInfo field, DocValuesProducer valuesProducer) throws IOException {
		meta.writeInt(field.number);
		SortedSetDocValues values = valuesProducer.getSortedSet(field);
		int numDocsWithField = 0;
		long numOrds = 0;
		for (int doc = values.nextDoc(); doc != (DocIdSetIterator.NO_MORE_DOCS); doc = values.nextDoc()) {
			numDocsWithField++;
			for (long ord = values.nextOrd(); ord != (SortedSetDocValues.NO_MORE_ORDS); ord = values.nextOrd()) {
				numOrds++;
			}
		}
		if (numDocsWithField == numOrds) {
			meta.writeByte(((byte) (0)));
			doAddSortedField(field, new EmptyDocValuesProducer() {
				@Override
				public SortedDocValues getSorted(FieldInfo field) throws IOException {
					return SortedSetSelector.wrap(valuesProducer.getSortedSet(field), MIN);
				}
			});
			return;
		}
		meta.writeByte(((byte) (1)));
		assert numDocsWithField != 0;
		if (numDocsWithField == (maxDoc)) {
			meta.writeLong((-1));
			meta.writeLong(0L);
		}else {
			long offset = data.getFilePointer();
			meta.writeLong(offset);
			values = valuesProducer.getSortedSet(field);
			meta.writeLong(((data.getFilePointer()) - offset));
		}
		int numberOfBitsPerOrd = DirectWriter.unsignedBitsRequired(((values.getValueCount()) - 1));
		meta.writeByte(((byte) (numberOfBitsPerOrd)));
		long start = data.getFilePointer();
		meta.writeLong(start);
		DirectWriter writer = DirectWriter.getInstance(data, numOrds, numberOfBitsPerOrd);
		values = valuesProducer.getSortedSet(field);
		for (int doc = values.nextDoc(); doc != (DocIdSetIterator.NO_MORE_DOCS); doc = values.nextDoc()) {
			for (long ord = values.nextOrd(); ord != (SortedSetDocValues.NO_MORE_ORDS); ord = values.nextOrd()) {
				writer.add(ord);
			}
		}
		writer.finish();
		meta.writeLong(((data.getFilePointer()) - start));
		meta.writeInt(numDocsWithField);
		start = data.getFilePointer();
		meta.writeLong(start);
		long addr = 0;
		values = valuesProducer.getSortedSet(field);
		for (int doc = values.nextDoc(); doc != (DocIdSetIterator.NO_MORE_DOCS); doc = values.nextDoc()) {
			values.nextOrd();
			addr++;
			while ((values.nextOrd()) != (SortedSetDocValues.NO_MORE_ORDS)) {
				addr++;
			} 
		}
		meta.writeLong(((data.getFilePointer()) - start));
		addTermsDict(values);
	}
}

