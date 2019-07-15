

import java.util.List;
import org.apache.cassandra.cql3.AssignmentTestable;
import org.apache.cassandra.cql3.ColumnIdentifier;
import org.apache.cassandra.cql3.ColumnSpecification;
import org.apache.cassandra.cql3.Constants;
import org.apache.cassandra.cql3.Term;
import org.apache.cassandra.cql3.VariableSpecifications;
import org.apache.cassandra.cql3.functions.Function;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.db.marshal.ListType;
import org.apache.cassandra.exceptions.InvalidRequestException;

import static org.apache.cassandra.cql3.AssignmentTestable.TestResult.WEAKLY_ASSIGNABLE;


public abstract class AbstractMarker extends Term.NonTerminal {
	protected final int bindIndex;

	protected final ColumnSpecification receiver;

	protected AbstractMarker(int bindIndex, ColumnSpecification receiver) {
		this.bindIndex = bindIndex;
		this.receiver = receiver;
	}

	public void collectMarkerSpecification(VariableSpecifications boundNames) {
		boundNames.add(bindIndex, receiver);
	}

	public boolean containsBindMarker() {
		return true;
	}

	public void addFunctionsTo(List<Function> functions) {
	}

	public static class Raw extends Term.Raw {
		private final int bindIndex;

		public Raw(int bindIndex) {
			this.bindIndex = bindIndex;
		}

		public Term.NonTerminal prepare(String keyspace, ColumnSpecification receiver) throws InvalidRequestException {
			if (receiver.type.isCollection()) {
			}else
				if (receiver.type.isUDT()) {
				}

			return new Constants.Marker(bindIndex, receiver);
		}

		@Override
		public AssignmentTestable.TestResult testAssignment(String keyspace, ColumnSpecification receiver) {
			return WEAKLY_ASSIGNABLE;
		}

		public AbstractType<?> getExactTypeIfKnown(String keyspace) {
			return null;
		}

		@Override
		public String getText() {
			return "?";
		}

		public int bindIndex() {
			return bindIndex;
		}
	}

	public static abstract class MultiColumnRaw extends Term.MultiColumnRaw {
		protected final int bindIndex;

		public MultiColumnRaw(int bindIndex) {
			this.bindIndex = bindIndex;
		}

		public Term.NonTerminal prepare(String keyspace, ColumnSpecification receiver) throws InvalidRequestException {
			throw new AssertionError("MultiColumnRaw..prepare() requires a list of receivers");
		}

		public AssignmentTestable.TestResult testAssignment(String keyspace, ColumnSpecification receiver) {
			return WEAKLY_ASSIGNABLE;
		}

		@Override
		public String getText() {
			return "?";
		}
	}

	public static class INRaw extends AbstractMarker.Raw {
		public INRaw(int bindIndex) {
			super(bindIndex);
		}

		private static ColumnSpecification makeInReceiver(ColumnSpecification receiver) {
			ColumnIdentifier inName = new ColumnIdentifier((("in(" + (receiver.name)) + ")"), true);
			return new ColumnSpecification(receiver.ksName, receiver.cfName, inName, ListType.getInstance(receiver.type, false));
		}

		@Override
		public AbstractMarker prepare(String keyspace, ColumnSpecification receiver) throws InvalidRequestException {
			return null;
		}
	}
}

