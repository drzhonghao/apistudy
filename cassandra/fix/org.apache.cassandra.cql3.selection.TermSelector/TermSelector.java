

import java.nio.ByteBuffer;
import org.apache.cassandra.cql3.ColumnSpecification;
import org.apache.cassandra.cql3.QueryOptions;
import org.apache.cassandra.cql3.Term;
import org.apache.cassandra.cql3.selection.Selection;
import org.apache.cassandra.cql3.selection.SelectionColumnMapping;
import org.apache.cassandra.cql3.selection.Selector;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.exceptions.InvalidRequestException;
import org.apache.cassandra.transport.ProtocolVersion;


public class TermSelector extends Selector {
	private final ByteBuffer value;

	private final AbstractType<?> type;

	public static Selector.Factory newFactory(final String name, final Term term, final AbstractType<?> type) {
		return new Selector.Factory() {
			protected String getColumnName() {
				return name;
			}

			protected AbstractType<?> getReturnType() {
				return type;
			}

			protected void addColumnMapping(SelectionColumnMapping mapping, ColumnSpecification resultColumn) {
			}

			public Selector newInstance(QueryOptions options) {
				return new TermSelector(term.bindAndGet(options), type);
			}
		};
	}

	private TermSelector(ByteBuffer value, AbstractType<?> type) {
		this.value = value;
		this.type = type;
	}

	public void addInput(ProtocolVersion protocolVersion, Selection.ResultSetBuilder rs) throws InvalidRequestException {
	}

	public ByteBuffer getOutput(ProtocolVersion protocolVersion) throws InvalidRequestException {
		return value;
	}

	public AbstractType<?> getType() {
		return type;
	}

	public void reset() {
	}
}

