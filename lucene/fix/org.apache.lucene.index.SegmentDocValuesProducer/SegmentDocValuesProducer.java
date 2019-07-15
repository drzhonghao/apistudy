

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.lucene.codecs.DocValuesProducer;
import org.apache.lucene.index.BinaryDocValues;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.SortedDocValues;
import org.apache.lucene.index.SortedNumericDocValues;
import org.apache.lucene.index.SortedSetDocValues;
import org.apache.lucene.util.Accountable;
import org.apache.lucene.util.Accountables;
import org.apache.lucene.util.RamUsageEstimator;


class SegmentDocValuesProducer extends DocValuesProducer {
	private static final long LONG_RAM_BYTES_USED = RamUsageEstimator.shallowSizeOfInstance(Long.class);

	private static final long BASE_RAM_BYTES_USED = RamUsageEstimator.shallowSizeOfInstance(SegmentDocValuesProducer.class);

	final Map<String, DocValuesProducer> dvProducersByField = new HashMap<>();

	final Set<DocValuesProducer> dvProducers = Collections.newSetFromMap(new IdentityHashMap<DocValuesProducer, Boolean>());

	final List<Long> dvGens = new ArrayList<>();

	@Override
	public NumericDocValues getNumeric(FieldInfo field) throws IOException {
		DocValuesProducer dvProducer = dvProducersByField.get(field.name);
		assert dvProducer != null;
		return dvProducer.getNumeric(field);
	}

	@Override
	public BinaryDocValues getBinary(FieldInfo field) throws IOException {
		DocValuesProducer dvProducer = dvProducersByField.get(field.name);
		assert dvProducer != null;
		return dvProducer.getBinary(field);
	}

	@Override
	public SortedDocValues getSorted(FieldInfo field) throws IOException {
		DocValuesProducer dvProducer = dvProducersByField.get(field.name);
		assert dvProducer != null;
		return dvProducer.getSorted(field);
	}

	@Override
	public SortedNumericDocValues getSortedNumeric(FieldInfo field) throws IOException {
		DocValuesProducer dvProducer = dvProducersByField.get(field.name);
		assert dvProducer != null;
		return dvProducer.getSortedNumeric(field);
	}

	@Override
	public SortedSetDocValues getSortedSet(FieldInfo field) throws IOException {
		DocValuesProducer dvProducer = dvProducersByField.get(field.name);
		assert dvProducer != null;
		return dvProducer.getSortedSet(field);
	}

	@Override
	public void checkIntegrity() throws IOException {
		for (DocValuesProducer producer : dvProducers) {
			producer.checkIntegrity();
		}
	}

	@Override
	public void close() throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public long ramBytesUsed() {
		long ramBytesUsed = SegmentDocValuesProducer.BASE_RAM_BYTES_USED;
		ramBytesUsed += (dvGens.size()) * (SegmentDocValuesProducer.LONG_RAM_BYTES_USED);
		ramBytesUsed += (dvProducers.size()) * (RamUsageEstimator.NUM_BYTES_OBJECT_REF);
		ramBytesUsed += ((dvProducersByField.size()) * 2) * (RamUsageEstimator.NUM_BYTES_OBJECT_REF);
		for (DocValuesProducer producer : dvProducers) {
			ramBytesUsed += producer.ramBytesUsed();
		}
		return ramBytesUsed;
	}

	@Override
	public Collection<Accountable> getChildResources() {
		final List<Accountable> resources = new ArrayList<>(dvProducers.size());
		for (Accountable producer : dvProducers) {
			resources.add(Accountables.namedAccountable("delegate", producer));
		}
		return Collections.unmodifiableList(resources);
	}

	@Override
	public String toString() {
		return (((getClass().getSimpleName()) + "(producers=") + (dvProducers.size())) + ")";
	}
}

