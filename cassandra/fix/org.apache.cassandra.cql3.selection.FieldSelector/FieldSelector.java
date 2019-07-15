

import java.nio.ByteBuffer;
import org.apache.cassandra.cql3.ColumnSpecification;
import org.apache.cassandra.cql3.FieldIdentifier;
import org.apache.cassandra.cql3.QueryOptions;
import org.apache.cassandra.cql3.selection.Selection;
import org.apache.cassandra.cql3.selection.SelectionColumnMapping;
import org.apache.cassandra.cql3.selection.Selector;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.db.marshal.TupleType;
import org.apache.cassandra.db.marshal.UserType;
import org.apache.cassandra.exceptions.InvalidRequestException;
import org.apache.cassandra.transport.ProtocolVersion;


final class FieldSelector extends Selector {
	private final UserType type;

	private final int field;

	private final Selector selected;

	public static Selector.Factory newFactory(final UserType type, final int field, final Selector.Factory factory) {
		return new Selector.Factory() {
			protected String getColumnName() {
				return null;
			}

			protected AbstractType<?> getReturnType() {
				return type.fieldType(field);
			}

			protected void addColumnMapping(SelectionColumnMapping mapping, ColumnSpecification resultsColumn) {
			}

			public Selector newInstance(QueryOptions options) throws InvalidRequestException {
				return new FieldSelector(type, field, factory.newInstance(options));
			}

			public boolean isAggregateSelectorFactory() {
				return factory.isAggregateSelectorFactory();
			}
		};
	}

	public void addInput(ProtocolVersion protocolVersion, Selection.ResultSetBuilder rs) throws InvalidRequestException {
		selected.addInput(protocolVersion, rs);
	}

	public ByteBuffer getOutput(ProtocolVersion protocolVersion) throws InvalidRequestException {
		ByteBuffer value = selected.getOutput(protocolVersion);
		if (value == null)
			return null;

		ByteBuffer[] buffers = type.split(value);
		return (field) < (buffers.length) ? buffers[field] : null;
	}

	public AbstractType<?> getType() {
		return type.fieldType(field);
	}

	public void reset() {
		selected.reset();
	}

	@Override
	public String toString() {
		return String.format("%s.%s", selected, type.fieldName(field));
	}

	private FieldSelector(UserType type, int field, Selector selected) {
		this.type = type;
		this.field = field;
		this.selected = selected;
	}
}

