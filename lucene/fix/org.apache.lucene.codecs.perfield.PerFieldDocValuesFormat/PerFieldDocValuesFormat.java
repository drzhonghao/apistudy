

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import org.apache.lucene.codecs.DocValuesConsumer;
import org.apache.lucene.codecs.DocValuesFormat;
import org.apache.lucene.codecs.DocValuesProducer;
import org.apache.lucene.index.BinaryDocValues;
import org.apache.lucene.index.DocValuesType;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.FieldInfos;
import org.apache.lucene.index.MergeState;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.SegmentReadState;
import org.apache.lucene.index.SegmentWriteState;
import org.apache.lucene.index.SortedDocValues;
import org.apache.lucene.index.SortedNumericDocValues;
import org.apache.lucene.index.SortedSetDocValues;
import org.apache.lucene.util.Accountable;
import org.apache.lucene.util.Accountables;
import org.apache.lucene.util.IOUtils;


public abstract class PerFieldDocValuesFormat extends DocValuesFormat {
	public static final String PER_FIELD_NAME = "PerFieldDV40";

	public static final String PER_FIELD_FORMAT_KEY = (PerFieldDocValuesFormat.class.getSimpleName()) + ".format";

	public static final String PER_FIELD_SUFFIX_KEY = (PerFieldDocValuesFormat.class.getSimpleName()) + ".suffix";

	public PerFieldDocValuesFormat() {
		super(PerFieldDocValuesFormat.PER_FIELD_NAME);
	}

	@Override
	public final DocValuesConsumer fieldsConsumer(SegmentWriteState state) throws IOException {
		return new PerFieldDocValuesFormat.FieldsWriter(state);
	}

	static class ConsumerAndSuffix implements Closeable {
		DocValuesConsumer consumer;

		int suffix;

		@Override
		public void close() throws IOException {
			consumer.close();
		}
	}

	private class FieldsWriter extends DocValuesConsumer {
		private final Map<DocValuesFormat, PerFieldDocValuesFormat.ConsumerAndSuffix> formats = new HashMap<>();

		private final Map<String, Integer> suffixes = new HashMap<>();

		private final SegmentWriteState segmentWriteState;

		public FieldsWriter(SegmentWriteState state) {
			segmentWriteState = state;
		}

		@Override
		public void addNumericField(FieldInfo field, DocValuesProducer valuesProducer) throws IOException {
			getInstance(field).addNumericField(field, valuesProducer);
		}

		@Override
		public void addBinaryField(FieldInfo field, DocValuesProducer valuesProducer) throws IOException {
			getInstance(field).addBinaryField(field, valuesProducer);
		}

		@Override
		public void addSortedField(FieldInfo field, DocValuesProducer valuesProducer) throws IOException {
			getInstance(field).addSortedField(field, valuesProducer);
		}

		@Override
		public void addSortedNumericField(FieldInfo field, DocValuesProducer valuesProducer) throws IOException {
			getInstance(field).addSortedNumericField(field, valuesProducer);
		}

		@Override
		public void addSortedSetField(FieldInfo field, DocValuesProducer valuesProducer) throws IOException {
			getInstance(field).addSortedSetField(field, valuesProducer);
		}

		@Override
		public void merge(MergeState mergeState) throws IOException {
			Map<DocValuesConsumer, Collection<String>> consumersToField = new IdentityHashMap<>();
			for (FieldInfo fi : mergeState.mergeFieldInfos) {
				DocValuesConsumer consumer = getInstance(fi);
				Collection<String> fieldsForConsumer = consumersToField.get(consumer);
				if (fieldsForConsumer == null) {
					fieldsForConsumer = new ArrayList<>();
					consumersToField.put(consumer, fieldsForConsumer);
				}
				fieldsForConsumer.add(fi.name);
			}
			try {
				for (Map.Entry<DocValuesConsumer, Collection<String>> e : consumersToField.entrySet()) {
				}
			} finally {
			}
		}

		private DocValuesConsumer getInstance(FieldInfo field) throws IOException {
			DocValuesFormat format = null;
			if ((field.getDocValuesGen()) != (-1)) {
				final String formatName = field.getAttribute(PerFieldDocValuesFormat.PER_FIELD_FORMAT_KEY);
				if (formatName != null) {
					format = DocValuesFormat.forName(formatName);
				}
			}
			if (format == null) {
				format = getDocValuesFormatForField(field.name);
			}
			if (format == null) {
				throw new IllegalStateException((("invalid null DocValuesFormat for field=\"" + (field.name)) + "\""));
			}
			final String formatName = format.getName();
			String previousValue = field.putAttribute(PerFieldDocValuesFormat.PER_FIELD_FORMAT_KEY, formatName);
			if (((field.getDocValuesGen()) == (-1)) && (previousValue != null)) {
				throw new IllegalStateException(((((((("found existing value for " + (PerFieldDocValuesFormat.PER_FIELD_FORMAT_KEY)) + ", field=") + (field.name)) + ", old=") + previousValue) + ", new=") + formatName));
			}
			Integer suffix = null;
			PerFieldDocValuesFormat.ConsumerAndSuffix consumer = formats.get(format);
			if (consumer == null) {
				if ((field.getDocValuesGen()) != (-1)) {
					final String suffixAtt = field.getAttribute(PerFieldDocValuesFormat.PER_FIELD_SUFFIX_KEY);
					if (suffixAtt != null) {
						suffix = Integer.valueOf(suffixAtt);
					}
				}
				if (suffix == null) {
					suffix = suffixes.get(formatName);
					if (suffix == null) {
						suffix = 0;
					}else {
						suffix = suffix + 1;
					}
				}
				suffixes.put(formatName, suffix);
				final String segmentSuffix = PerFieldDocValuesFormat.getFullSegmentSuffix(segmentWriteState.segmentSuffix, PerFieldDocValuesFormat.getSuffix(formatName, Integer.toString(suffix)));
				consumer = new PerFieldDocValuesFormat.ConsumerAndSuffix();
				consumer.consumer = format.fieldsConsumer(new SegmentWriteState(segmentWriteState, segmentSuffix));
				consumer.suffix = suffix;
				formats.put(format, consumer);
			}else {
				assert suffixes.containsKey(formatName);
				suffix = consumer.suffix;
			}
			previousValue = field.putAttribute(PerFieldDocValuesFormat.PER_FIELD_SUFFIX_KEY, Integer.toString(suffix));
			if (((field.getDocValuesGen()) == (-1)) && (previousValue != null)) {
				throw new IllegalStateException(((((((("found existing value for " + (PerFieldDocValuesFormat.PER_FIELD_SUFFIX_KEY)) + ", field=") + (field.name)) + ", old=") + previousValue) + ", new=") + suffix));
			}
			return consumer.consumer;
		}

		@Override
		public void close() throws IOException {
			IOUtils.close(formats.values());
		}
	}

	static String getSuffix(String formatName, String suffix) {
		return (formatName + "_") + suffix;
	}

	static String getFullSegmentSuffix(String outerSegmentSuffix, String segmentSuffix) {
		if ((outerSegmentSuffix.length()) == 0) {
			return segmentSuffix;
		}else {
			return (outerSegmentSuffix + "_") + segmentSuffix;
		}
	}

	private class FieldsReader extends DocValuesProducer {
		private final Map<String, DocValuesProducer> fields = new TreeMap<>();

		private final Map<String, DocValuesProducer> formats = new HashMap<>();

		FieldsReader(PerFieldDocValuesFormat.FieldsReader other) throws IOException {
			Map<DocValuesProducer, DocValuesProducer> oldToNew = new IdentityHashMap<>();
			for (Map.Entry<String, DocValuesProducer> ent : other.formats.entrySet()) {
				DocValuesProducer values = ent.getValue().getMergeInstance();
				formats.put(ent.getKey(), values);
				oldToNew.put(ent.getValue(), values);
			}
			for (Map.Entry<String, DocValuesProducer> ent : other.fields.entrySet()) {
				DocValuesProducer producer = oldToNew.get(ent.getValue());
				assert producer != null;
				fields.put(ent.getKey(), producer);
			}
		}

		public FieldsReader(final SegmentReadState readState) throws IOException {
			boolean success = false;
			try {
				for (FieldInfo fi : readState.fieldInfos) {
					if ((fi.getDocValuesType()) != (DocValuesType.NONE)) {
						final String fieldName = fi.name;
						final String formatName = fi.getAttribute(PerFieldDocValuesFormat.PER_FIELD_FORMAT_KEY);
						if (formatName != null) {
							final String suffix = fi.getAttribute(PerFieldDocValuesFormat.PER_FIELD_SUFFIX_KEY);
							if (suffix == null) {
								throw new IllegalStateException(((("missing attribute: " + (PerFieldDocValuesFormat.PER_FIELD_SUFFIX_KEY)) + " for field: ") + fieldName));
							}
							DocValuesFormat format = DocValuesFormat.forName(formatName);
							String segmentSuffix = PerFieldDocValuesFormat.getFullSegmentSuffix(readState.segmentSuffix, PerFieldDocValuesFormat.getSuffix(formatName, suffix));
							if (!(formats.containsKey(segmentSuffix))) {
								formats.put(segmentSuffix, format.fieldsProducer(new SegmentReadState(readState, segmentSuffix)));
							}
							fields.put(fieldName, formats.get(segmentSuffix));
						}
					}
				}
				success = true;
			} finally {
				if (!success) {
					IOUtils.closeWhileHandlingException(formats.values());
				}
			}
		}

		@Override
		public NumericDocValues getNumeric(FieldInfo field) throws IOException {
			DocValuesProducer producer = fields.get(field.name);
			return producer == null ? null : producer.getNumeric(field);
		}

		@Override
		public BinaryDocValues getBinary(FieldInfo field) throws IOException {
			DocValuesProducer producer = fields.get(field.name);
			return producer == null ? null : producer.getBinary(field);
		}

		@Override
		public SortedDocValues getSorted(FieldInfo field) throws IOException {
			DocValuesProducer producer = fields.get(field.name);
			return producer == null ? null : producer.getSorted(field);
		}

		@Override
		public SortedNumericDocValues getSortedNumeric(FieldInfo field) throws IOException {
			DocValuesProducer producer = fields.get(field.name);
			return producer == null ? null : producer.getSortedNumeric(field);
		}

		@Override
		public SortedSetDocValues getSortedSet(FieldInfo field) throws IOException {
			DocValuesProducer producer = fields.get(field.name);
			return producer == null ? null : producer.getSortedSet(field);
		}

		@Override
		public void close() throws IOException {
			IOUtils.close(formats.values());
		}

		@Override
		public long ramBytesUsed() {
			long size = 0;
			for (Map.Entry<String, DocValuesProducer> entry : formats.entrySet()) {
				size += ((entry.getKey().length()) * (Character.BYTES)) + (entry.getValue().ramBytesUsed());
			}
			return size;
		}

		@Override
		public Collection<Accountable> getChildResources() {
			return Accountables.namedAccountables("format", formats);
		}

		@Override
		public void checkIntegrity() throws IOException {
			for (DocValuesProducer format : formats.values()) {
				format.checkIntegrity();
			}
		}

		@Override
		public DocValuesProducer getMergeInstance() throws IOException {
			return new PerFieldDocValuesFormat.FieldsReader(this);
		}

		@Override
		public String toString() {
			return ("PerFieldDocValues(formats=" + (formats.size())) + ")";
		}
	}

	@Override
	public final DocValuesProducer fieldsProducer(SegmentReadState state) throws IOException {
		return new PerFieldDocValuesFormat.FieldsReader(state);
	}

	public abstract DocValuesFormat getDocValuesFormatForField(String field);
}

