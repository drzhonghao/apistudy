

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;
import org.apache.cassandra.cql3.CQL3Type;
import org.apache.cassandra.cql3.ColumnSpecification;
import org.apache.cassandra.cql3.Lists;
import org.apache.cassandra.cql3.Maps;
import org.apache.cassandra.cql3.Sets;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.db.marshal.BytesType;
import org.apache.cassandra.db.rows.Cell;
import org.apache.cassandra.db.rows.CellPath;
import org.apache.cassandra.io.util.DataInputPlus;
import org.apache.cassandra.io.util.DataOutputPlus;
import org.apache.cassandra.serializers.CollectionSerializer;
import org.apache.cassandra.serializers.MarshalException;
import org.apache.cassandra.transport.ProtocolVersion;
import org.apache.cassandra.utils.ByteBufferUtil;


public abstract class CollectionType<T> extends AbstractType<T> {
	public static CellPath.Serializer cellPathSerializer = new CollectionType.CollectionPathSerializer();

	public enum Kind {

		MAP() {
			public ColumnSpecification makeCollectionReceiver(ColumnSpecification collection, boolean isKey) {
				return isKey ? Maps.keySpecOf(collection) : Maps.valueSpecOf(collection);
			}
		},
		SET() {
			public ColumnSpecification makeCollectionReceiver(ColumnSpecification collection, boolean isKey) {
				return Sets.valueSpecOf(collection);
			}
		},
		LIST() {
			public ColumnSpecification makeCollectionReceiver(ColumnSpecification collection, boolean isKey) {
				return Lists.valueSpecOf(collection);
			}
		};
		public abstract ColumnSpecification makeCollectionReceiver(ColumnSpecification collection, boolean isKey);
	}

	public final CollectionType.Kind kind;

	protected CollectionType(AbstractType.ComparisonType comparisonType, CollectionType.Kind kind) {
		super(comparisonType);
		this.kind = kind;
	}

	public abstract AbstractType<?> nameComparator();

	public abstract AbstractType<?> valueComparator();

	protected abstract List<ByteBuffer> serializedValues(Iterator<Cell> cells);

	@Override
	public abstract CollectionSerializer<T> getSerializer();

	public ColumnSpecification makeCollectionReceiver(ColumnSpecification collection, boolean isKey) {
		return kind.makeCollectionReceiver(collection, isKey);
	}

	public String getString(ByteBuffer bytes) {
		return BytesType.instance.getString(bytes);
	}

	public ByteBuffer fromString(String source) {
		try {
			return ByteBufferUtil.hexToBytes(source);
		} catch (NumberFormatException e) {
			throw new MarshalException(String.format("cannot parse '%s' as hex bytes", source), e);
		}
	}

	public boolean isCollection() {
		return true;
	}

	@Override
	public void validateCellValue(ByteBuffer cellValue) throws MarshalException {
		if (isMultiCell())
			valueComparator().validateCellValue(cellValue);
		else
			super.validateCellValue(cellValue);

	}

	public boolean isMap() {
		return (kind) == (CollectionType.Kind.MAP);
	}

	@Override
	public boolean isFreezable() {
		return true;
	}

	protected int collectionSize(List<ByteBuffer> values) {
		return values.size();
	}

	public ByteBuffer serializeForNativeProtocol(Iterator<Cell> cells, ProtocolVersion version) {
		assert isMultiCell();
		List<ByteBuffer> values = serializedValues(cells);
		int size = collectionSize(values);
		return CollectionSerializer.pack(values, size, version);
	}

	@Override
	public boolean isCompatibleWith(AbstractType<?> previous) {
		if ((this) == previous)
			return true;

		if (!(getClass().equals(previous.getClass())))
			return false;

		CollectionType tprev = ((CollectionType) (previous));
		if ((this.isMultiCell()) != (tprev.isMultiCell()))
			return false;

		if (!(this.isMultiCell()))
			return isCompatibleWithFrozen(tprev);

		if (!(this.nameComparator().isCompatibleWith(tprev.nameComparator())))
			return false;

		return this.valueComparator().isValueCompatibleWith(tprev.valueComparator());
	}

	@Override
	public boolean isValueCompatibleWithInternal(AbstractType<?> previous) {
		if (this.isMultiCell())
			return isCompatibleWith(previous);

		if ((this) == previous)
			return true;

		if (!(getClass().equals(previous.getClass())))
			return false;

		CollectionType tprev = ((CollectionType) (previous));
		if ((this.isMultiCell()) != (tprev.isMultiCell()))
			return false;

		return isValueCompatibleWithFrozen(tprev);
	}

	protected abstract boolean isCompatibleWithFrozen(CollectionType<?> previous);

	protected abstract boolean isValueCompatibleWithFrozen(CollectionType<?> previous);

	public CQL3Type asCQL3Type() {
		return null;
	}

	@Override
	public boolean equals(Object o, boolean ignoreFreezing) {
		if ((this) == o)
			return true;

		if (!(o instanceof CollectionType))
			return false;

		CollectionType other = ((CollectionType) (o));
		if ((kind) != (other.kind))
			return false;

		if ((!ignoreFreezing) && ((isMultiCell()) != (other.isMultiCell())))
			return false;

		return (nameComparator().equals(other.nameComparator(), ignoreFreezing)) && (valueComparator().equals(other.valueComparator(), ignoreFreezing));
	}

	@Override
	public String toString() {
		return this.toString(false);
	}

	private static class CollectionPathSerializer implements CellPath.Serializer {
		public void serialize(CellPath path, DataOutputPlus out) throws IOException {
			ByteBufferUtil.writeWithVIntLength(path.get(0), out);
		}

		public CellPath deserialize(DataInputPlus in) throws IOException {
			return CellPath.create(ByteBufferUtil.readWithVIntLength(in));
		}

		public long serializedSize(CellPath path) {
			return ByteBufferUtil.serializedSizeWithVIntLength(path.get(0));
		}

		public void skip(DataInputPlus in) throws IOException {
			ByteBufferUtil.skipWithVIntLength(in);
		}
	}
}

