

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.apache.lucene.codecs.FieldsConsumer;
import org.apache.lucene.codecs.FieldsProducer;
import org.apache.lucene.codecs.PostingsFormat;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.FieldInfos;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.FilterLeafReader;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.MergeState;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.SegmentInfo;
import org.apache.lucene.index.SegmentReadState;
import org.apache.lucene.index.SegmentWriteState;
import org.apache.lucene.index.Terms;
import org.apache.lucene.util.Accountable;
import org.apache.lucene.util.Accountables;
import org.apache.lucene.util.IOUtils;
import org.apache.lucene.util.RamUsageEstimator;


public abstract class PerFieldPostingsFormat extends PostingsFormat {
	public static final String PER_FIELD_NAME = "PerField40";

	public static final String PER_FIELD_FORMAT_KEY = (PerFieldPostingsFormat.class.getSimpleName()) + ".format";

	public static final String PER_FIELD_SUFFIX_KEY = (PerFieldPostingsFormat.class.getSimpleName()) + ".suffix";

	public PerFieldPostingsFormat() {
		super(PerFieldPostingsFormat.PER_FIELD_NAME);
	}

	static class FieldsGroup {
		final Set<String> fields = new TreeSet<>();

		int suffix;

		SegmentWriteState state;
	}

	static String getSuffix(String formatName, String suffix) {
		return (formatName + "_") + suffix;
	}

	static String getFullSegmentSuffix(String fieldName, String outerSegmentSuffix, String segmentSuffix) {
		if ((outerSegmentSuffix.length()) == 0) {
			return segmentSuffix;
		}else {
			throw new IllegalStateException((("cannot embed PerFieldPostingsFormat inside itself (field \"" + fieldName) + "\" returned PerFieldPostingsFormat)"));
		}
	}

	private class FieldsWriter extends FieldsConsumer {
		final SegmentWriteState writeState;

		final List<Closeable> toClose = new ArrayList<Closeable>();

		public FieldsWriter(SegmentWriteState writeState) {
			this.writeState = writeState;
		}

		@Override
		public void write(Fields fields) throws IOException {
			Map<PostingsFormat, PerFieldPostingsFormat.FieldsGroup> formatToGroups = buildFieldsGroupMapping(fields);
			boolean success = false;
			try {
				for (Map.Entry<PostingsFormat, PerFieldPostingsFormat.FieldsGroup> ent : formatToGroups.entrySet()) {
					PostingsFormat format = ent.getKey();
					final PerFieldPostingsFormat.FieldsGroup group = ent.getValue();
					Fields maskedFields = new FilterLeafReader.FilterFields(fields) {
						@Override
						public Iterator<String> iterator() {
							return group.fields.iterator();
						}
					};
					FieldsConsumer consumer = format.fieldsConsumer(group.state);
					toClose.add(consumer);
					consumer.write(maskedFields);
				}
				success = true;
			} finally {
				if (!success) {
					IOUtils.closeWhileHandlingException(toClose);
				}
			}
		}

		@Override
		public void merge(MergeState mergeState) throws IOException {
			Map<PostingsFormat, PerFieldPostingsFormat.FieldsGroup> formatToGroups = buildFieldsGroupMapping(new MultiFields(mergeState.fieldsProducers, null));
			boolean success = false;
			try {
				for (Map.Entry<PostingsFormat, PerFieldPostingsFormat.FieldsGroup> ent : formatToGroups.entrySet()) {
					PostingsFormat format = ent.getKey();
					final PerFieldPostingsFormat.FieldsGroup group = ent.getValue();
					FieldsConsumer consumer = format.fieldsConsumer(group.state);
					toClose.add(consumer);
				}
				success = true;
			} finally {
				if (!success) {
					IOUtils.closeWhileHandlingException(toClose);
				}
			}
		}

		private Map<PostingsFormat, PerFieldPostingsFormat.FieldsGroup> buildFieldsGroupMapping(Fields fields) {
			Map<PostingsFormat, PerFieldPostingsFormat.FieldsGroup> formatToGroups = new HashMap<>();
			Map<String, Integer> suffixes = new HashMap<>();
			for (String field : fields) {
				FieldInfo fieldInfo = writeState.fieldInfos.fieldInfo(field);
				final PostingsFormat format = getPostingsFormatForField(field);
				if (format == null) {
					throw new IllegalStateException((("invalid null PostingsFormat for field=\"" + field) + "\""));
				}
				String formatName = format.getName();
				PerFieldPostingsFormat.FieldsGroup group = formatToGroups.get(format);
				if (group == null) {
					Integer suffix = suffixes.get(formatName);
					if (suffix == null) {
						suffix = 0;
					}else {
						suffix = suffix + 1;
					}
					suffixes.put(formatName, suffix);
					String segmentSuffix = PerFieldPostingsFormat.getFullSegmentSuffix(field, writeState.segmentSuffix, PerFieldPostingsFormat.getSuffix(formatName, Integer.toString(suffix)));
					group = new PerFieldPostingsFormat.FieldsGroup();
					group.state = new SegmentWriteState(writeState, segmentSuffix);
					group.suffix = suffix;
					formatToGroups.put(format, group);
				}else {
					if (!(suffixes.containsKey(formatName))) {
						throw new IllegalStateException(((("no suffix for format name: " + formatName) + ", expected: ") + (group.suffix)));
					}
				}
				group.fields.add(field);
				String previousValue = fieldInfo.putAttribute(PerFieldPostingsFormat.PER_FIELD_FORMAT_KEY, formatName);
				if (previousValue != null) {
					throw new IllegalStateException(((((((("found existing value for " + (PerFieldPostingsFormat.PER_FIELD_FORMAT_KEY)) + ", field=") + (fieldInfo.name)) + ", old=") + previousValue) + ", new=") + formatName));
				}
				previousValue = fieldInfo.putAttribute(PerFieldPostingsFormat.PER_FIELD_SUFFIX_KEY, Integer.toString(group.suffix));
				if (previousValue != null) {
					throw new IllegalStateException(((((((("found existing value for " + (PerFieldPostingsFormat.PER_FIELD_SUFFIX_KEY)) + ", field=") + (fieldInfo.name)) + ", old=") + previousValue) + ", new=") + (group.suffix)));
				}
			}
			return formatToGroups;
		}

		@Override
		public void close() throws IOException {
			IOUtils.close(toClose);
		}
	}

	private static class FieldsReader extends FieldsProducer {
		private static final long BASE_RAM_BYTES_USED = RamUsageEstimator.shallowSizeOfInstance(PerFieldPostingsFormat.FieldsReader.class);

		private final Map<String, FieldsProducer> fields = new TreeMap<>();

		private final Map<String, FieldsProducer> formats = new HashMap<>();

		private final String segment;

		FieldsReader(PerFieldPostingsFormat.FieldsReader other) throws IOException {
			Map<FieldsProducer, FieldsProducer> oldToNew = new IdentityHashMap<>();
			for (Map.Entry<String, FieldsProducer> ent : other.formats.entrySet()) {
				FieldsProducer values = ent.getValue().getMergeInstance();
				formats.put(ent.getKey(), values);
				oldToNew.put(ent.getValue(), values);
			}
			for (Map.Entry<String, FieldsProducer> ent : other.fields.entrySet()) {
				FieldsProducer producer = oldToNew.get(ent.getValue());
				assert producer != null;
				fields.put(ent.getKey(), producer);
			}
			segment = other.segment;
		}

		public FieldsReader(final SegmentReadState readState) throws IOException {
			boolean success = false;
			try {
				for (FieldInfo fi : readState.fieldInfos) {
					if ((fi.getIndexOptions()) != (IndexOptions.NONE)) {
						final String fieldName = fi.name;
						final String formatName = fi.getAttribute(PerFieldPostingsFormat.PER_FIELD_FORMAT_KEY);
						if (formatName != null) {
							final String suffix = fi.getAttribute(PerFieldPostingsFormat.PER_FIELD_SUFFIX_KEY);
							if (suffix == null) {
								throw new IllegalStateException(((("missing attribute: " + (PerFieldPostingsFormat.PER_FIELD_SUFFIX_KEY)) + " for field: ") + fieldName));
							}
							PostingsFormat format = PostingsFormat.forName(formatName);
							String segmentSuffix = PerFieldPostingsFormat.getSuffix(formatName, suffix);
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
			this.segment = readState.segmentInfo.name;
		}

		@Override
		public Iterator<String> iterator() {
			return Collections.unmodifiableSet(fields.keySet()).iterator();
		}

		@Override
		public Terms terms(String field) throws IOException {
			FieldsProducer fieldsProducer = fields.get(field);
			return fieldsProducer == null ? null : fieldsProducer.terms(field);
		}

		@Override
		public int size() {
			return fields.size();
		}

		@Override
		public void close() throws IOException {
			IOUtils.close(formats.values());
		}

		@Override
		public long ramBytesUsed() {
			long ramBytesUsed = PerFieldPostingsFormat.FieldsReader.BASE_RAM_BYTES_USED;
			ramBytesUsed += ((fields.size()) * 2L) * (RamUsageEstimator.NUM_BYTES_OBJECT_REF);
			ramBytesUsed += ((formats.size()) * 2L) * (RamUsageEstimator.NUM_BYTES_OBJECT_REF);
			for (Map.Entry<String, FieldsProducer> entry : formats.entrySet()) {
				ramBytesUsed += entry.getValue().ramBytesUsed();
			}
			return ramBytesUsed;
		}

		@Override
		public Collection<Accountable> getChildResources() {
			return Accountables.namedAccountables("format", formats);
		}

		@Override
		public void checkIntegrity() throws IOException {
			for (FieldsProducer producer : formats.values()) {
				producer.checkIntegrity();
			}
		}

		@Override
		public FieldsProducer getMergeInstance() throws IOException {
			return new PerFieldPostingsFormat.FieldsReader(this);
		}

		@Override
		public String toString() {
			return ((("PerFieldPostings(segment=" + (segment)) + " formats=") + (formats.size())) + ")";
		}
	}

	@Override
	public final FieldsConsumer fieldsConsumer(SegmentWriteState state) throws IOException {
		return new PerFieldPostingsFormat.FieldsWriter(state);
	}

	@Override
	public final FieldsProducer fieldsProducer(SegmentReadState state) throws IOException {
		return new PerFieldPostingsFormat.FieldsReader(state);
	}

	public abstract PostingsFormat getPostingsFormatForField(String field);
}

