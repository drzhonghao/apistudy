

import java.nio.ByteBuffer;
import org.apache.cassandra.config.ColumnDefinition;
import org.apache.cassandra.cql3.ColumnIdentifier;
import org.apache.cassandra.cql3.ColumnSpecification;
import org.apache.cassandra.cql3.QueryOptions;
import org.apache.cassandra.cql3.selection.Selection;
import org.apache.cassandra.cql3.selection.SelectionColumnMapping;
import org.apache.cassandra.cql3.selection.Selector;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.db.marshal.Int32Type;
import org.apache.cassandra.db.marshal.LongType;
import org.apache.cassandra.transport.ProtocolVersion;


final class WritetimeOrTTLSelector extends Selector {
	private final String columnName;

	private final int idx;

	private final boolean isWritetime;

	private ByteBuffer current;

	private boolean isSet;

	public static Selector.Factory newFactory(final ColumnDefinition def, final int idx, final boolean isWritetime) {
		return new Selector.Factory() {
			protected String getColumnName() {
				return String.format("%s(%s)", (isWritetime ? "writetime" : "ttl"), def.name.toString());
			}

			protected AbstractType<?> getReturnType() {
				return isWritetime ? LongType.instance : Int32Type.instance;
			}

			protected void addColumnMapping(SelectionColumnMapping mapping, ColumnSpecification resultsColumn) {
			}

			public Selector newInstance(QueryOptions options) {
				return new WritetimeOrTTLSelector(def.name.toString(), idx, isWritetime);
			}

			public boolean isWritetimeSelectorFactory() {
				return isWritetime;
			}

			public boolean isTTLSelectorFactory() {
				return !isWritetime;
			}
		};
	}

	public void addInput(ProtocolVersion protocolVersion, Selection.ResultSetBuilder rs) {
		if (isSet)
			return;

		isSet = true;
		if (isWritetime) {
		}else {
		}
	}

	public ByteBuffer getOutput(ProtocolVersion protocolVersion) {
		return current;
	}

	public void reset() {
		isSet = false;
		current = null;
	}

	public AbstractType<?> getType() {
		return isWritetime ? LongType.instance : Int32Type.instance;
	}

	@Override
	public String toString() {
		return columnName;
	}

	private WritetimeOrTTLSelector(String columnName, int idx, boolean isWritetime) {
		this.columnName = columnName;
		this.idx = idx;
		this.isWritetime = isWritetime;
	}
}

