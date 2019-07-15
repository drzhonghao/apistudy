

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.apache.cassandra.cql3.AssignmentTestable;
import org.apache.cassandra.cql3.CQL3Type;
import org.apache.cassandra.cql3.ColumnSpecification;
import org.apache.cassandra.cql3.Term;
import org.apache.cassandra.db.TypeSizes;
import org.apache.cassandra.db.marshal.TypeParser;
import org.apache.cassandra.exceptions.SyntaxException;
import org.apache.cassandra.io.util.DataInputPlus;
import org.apache.cassandra.io.util.DataOutputPlus;
import org.apache.cassandra.serializers.MarshalException;
import org.apache.cassandra.serializers.TypeSerializer;
import org.apache.cassandra.transport.ProtocolVersion;
import org.apache.cassandra.utils.ByteBufferUtil;
import org.apache.cassandra.utils.FastByteOperations;
import org.github.jamm.Unmetered;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.cassandra.cql3.AssignmentTestable.TestResult.NOT_ASSIGNABLE;


@Unmetered
public abstract class AbstractType<T> implements Comparator<ByteBuffer> , AssignmentTestable {
	private static final Logger logger = LoggerFactory.getLogger(AbstractType.class);

	public final Comparator<ByteBuffer> reverseComparator;

	public enum ComparisonType {

		NOT_COMPARABLE,
		BYTE_ORDER,
		CUSTOM;}

	public final AbstractType.ComparisonType comparisonType;

	public final boolean isByteOrderComparable;

	protected AbstractType(AbstractType.ComparisonType comparisonType) {
		this.comparisonType = comparisonType;
		this.isByteOrderComparable = comparisonType == (AbstractType.ComparisonType.BYTE_ORDER);
		reverseComparator = ( o1, o2) -> this.compare(o2, o1);
		try {
			Method custom = getClass().getMethod("compareCustom", ByteBuffer.class, ByteBuffer.class);
		} catch (NoSuchMethodException e) {
			throw new IllegalStateException();
		}
	}

	public static List<String> asCQLTypeStringList(List<AbstractType<?>> abstractTypes) {
		List<String> r = new ArrayList<>(abstractTypes.size());
		for (AbstractType<?> abstractType : abstractTypes)
			r.add(abstractType.asCQL3Type().toString());

		return r;
	}

	public T compose(ByteBuffer bytes) {
		return getSerializer().deserialize(bytes);
	}

	public ByteBuffer decompose(T value) {
		return getSerializer().serialize(value);
	}

	public String getString(ByteBuffer bytes) {
		if (bytes == null)
			return "null";

		TypeSerializer<T> serializer = getSerializer();
		serializer.validate(bytes);
		return serializer.toString(serializer.deserialize(bytes));
	}

	public abstract ByteBuffer fromString(String source) throws MarshalException;

	public abstract Term fromJSONObject(Object parsed) throws MarshalException;

	public String toJSONString(ByteBuffer buffer, ProtocolVersion protocolVersion) {
		return ('"' + (getSerializer().deserialize(buffer).toString())) + '"';
	}

	public void validate(ByteBuffer bytes) throws MarshalException {
		getSerializer().validate(bytes);
	}

	public final int compare(ByteBuffer left, ByteBuffer right) {
		return isByteOrderComparable ? FastByteOperations.compareUnsigned(left, right) : compareCustom(left, right);
	}

	public int compareCustom(ByteBuffer left, ByteBuffer right) {
		throw new UnsupportedOperationException();
	}

	public void validateCellValue(ByteBuffer cellValue) throws MarshalException {
		validate(cellValue);
	}

	public CQL3Type asCQL3Type() {
		return null;
	}

	public int compareForCQL(ByteBuffer v1, ByteBuffer v2) {
		return compare(v1, v2);
	}

	public abstract TypeSerializer<T> getSerializer();

	public String getString(Collection<ByteBuffer> names) {
		StringBuilder builder = new StringBuilder();
		for (ByteBuffer name : names) {
			builder.append(getString(name)).append(",");
		}
		return builder.toString();
	}

	public boolean isCounter() {
		return false;
	}

	public boolean isFrozenCollection() {
		return (isCollection()) && (!(isMultiCell()));
	}

	public boolean isReversed() {
		return false;
	}

	public static AbstractType<?> parseDefaultParameters(AbstractType<?> baseType, TypeParser parser) throws SyntaxException {
		Map<String, String> parameters = parser.getKeyValueParameters();
		String reversed = parameters.get("reversed");
		if ((reversed != null) && ((reversed.isEmpty()) || (reversed.equals("true")))) {
		}else {
			return baseType;
		}
		return null;
	}

	public boolean isCompatibleWith(AbstractType<?> previous) {
		return this.equals(previous);
	}

	public boolean isValueCompatibleWith(AbstractType<?> otherType) {
		return false;
	}

	protected boolean isValueCompatibleWithInternal(AbstractType<?> otherType) {
		return isCompatibleWith(otherType);
	}

	public int compareCollectionMembers(ByteBuffer v1, ByteBuffer v2, ByteBuffer collectionName) {
		return compare(v1, v2);
	}

	public void validateCollectionMember(ByteBuffer bytes, ByteBuffer collectionName) throws MarshalException {
		validate(bytes);
	}

	public boolean isCollection() {
		return false;
	}

	public boolean isUDT() {
		return false;
	}

	public boolean isTuple() {
		return false;
	}

	public boolean isMultiCell() {
		return false;
	}

	public boolean isFreezable() {
		return false;
	}

	public AbstractType<?> freeze() {
		return this;
	}

	public AbstractType<?> freezeNestedMulticellTypes() {
		return this;
	}

	public boolean isEmptyValueMeaningless() {
		return false;
	}

	public String toString(boolean ignoreFreezing) {
		return this.toString();
	}

	public int componentsCount() {
		return 1;
	}

	public List<AbstractType<?>> getComponents() {
		return Collections.<AbstractType<?>>singletonList(this);
	}

	protected int valueLengthIfFixed() {
		return -1;
	}

	public void writeValue(ByteBuffer value, DataOutputPlus out) throws IOException {
		assert value.hasRemaining();
		if ((valueLengthIfFixed()) >= 0)
			out.write(value);
		else
			ByteBufferUtil.writeWithVIntLength(value, out);

	}

	public long writtenLength(ByteBuffer value) {
		assert value.hasRemaining();
		return (valueLengthIfFixed()) >= 0 ? value.remaining() : TypeSizes.sizeofWithVIntLength(value);
	}

	public ByteBuffer readValue(DataInputPlus in) throws IOException {
		return readValue(in, Integer.MAX_VALUE);
	}

	public ByteBuffer readValue(DataInputPlus in, int maxValueSize) throws IOException {
		int length = valueLengthIfFixed();
		if (length >= 0)
			return ByteBufferUtil.read(in, length);
		else {
			int l = ((int) (in.readUnsignedVInt()));
			if (l < 0)
				throw new IOException("Corrupt (negative) value length encountered");

			if (l > maxValueSize)
				throw new IOException(String.format(("Corrupt value length %d encountered, as it exceeds the maximum of %d, " + "which is set via max_value_size_in_mb in cassandra.yaml"), l, maxValueSize));

			return ByteBufferUtil.read(in, l);
		}
	}

	public void skipValue(DataInputPlus in) throws IOException {
		int length = valueLengthIfFixed();
		if (length >= 0)
			in.skipBytesFully(length);
		else
			ByteBufferUtil.skipWithVIntLength(in);

	}

	public boolean referencesUserType(String userTypeName) {
		return false;
	}

	public boolean referencesDuration() {
		return false;
	}

	@Override
	public String toString() {
		return getClass().getName();
	}

	public boolean equals(Object other, boolean ignoreFreezing) {
		return this.equals(other);
	}

	public void checkComparable() {
		switch (comparisonType) {
			case NOT_COMPARABLE :
				throw new IllegalArgumentException(((this) + " cannot be used in comparisons, so cannot be used as a clustering column"));
		}
	}

	public final AssignmentTestable.TestResult testAssignment(String keyspace, ColumnSpecification receiver) {
		if ((isFreezable()) && (!(isMultiCell()))) {
		}
		if (isReversed()) {
		}
		return NOT_ASSIGNABLE;
	}
}

