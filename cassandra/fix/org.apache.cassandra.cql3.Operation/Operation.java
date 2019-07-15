

import java.util.List;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.config.ColumnDefinition;
import org.apache.cassandra.cql3.ColumnIdentifier;
import org.apache.cassandra.cql3.ColumnSpecification;
import org.apache.cassandra.cql3.FieldIdentifier;
import org.apache.cassandra.cql3.Maps;
import org.apache.cassandra.cql3.Term;
import org.apache.cassandra.cql3.UpdateParameters;
import org.apache.cassandra.cql3.UserTypes;
import org.apache.cassandra.cql3.VariableSpecifications;
import org.apache.cassandra.cql3.functions.Function;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.db.marshal.CollectionType;
import org.apache.cassandra.db.marshal.CounterColumnType;
import org.apache.cassandra.db.marshal.ListType;
import org.apache.cassandra.db.marshal.MapType;
import org.apache.cassandra.db.marshal.UserType;
import org.apache.cassandra.exceptions.InvalidRequestException;


public abstract class Operation {
	public final ColumnDefinition column;

	protected final Term t;

	protected Operation(ColumnDefinition column, Term t) {
		assert column != null;
		this.column = column;
		this.t = t;
	}

	public void addFunctionsTo(List<Function> functions) {
		if ((t) != null)
			t.addFunctionsTo(functions);

	}

	public boolean requiresRead() {
		return false;
	}

	public void collectMarkerSpecification(VariableSpecifications boundNames) {
		if ((t) != null)
			t.collectMarkerSpecification(boundNames);

	}

	public abstract void execute(DecoratedKey partitionKey, UpdateParameters params) throws InvalidRequestException;

	public interface RawUpdate {
		public Operation prepare(CFMetaData cfm, ColumnDefinition receiver) throws InvalidRequestException;

		public boolean isCompatibleWith(Operation.RawUpdate other);
	}

	public interface RawDeletion {
		public ColumnDefinition.Raw affectedColumn();

		public Operation prepare(String keyspace, ColumnDefinition receiver, CFMetaData cfm) throws InvalidRequestException;
	}

	public static class SetValue implements Operation.RawUpdate {
		private final Term.Raw value;

		public SetValue(Term.Raw value) {
			this.value = value;
		}

		public Operation prepare(CFMetaData cfm, ColumnDefinition receiver) throws InvalidRequestException {
			Term v = value.prepare(cfm.ksName, receiver);
			if ((receiver.type) instanceof CounterColumnType)
				throw new InvalidRequestException(String.format("Cannot set the value of counter column %s (counters can only be incremented/decremented, not set)", receiver.name));

			if (receiver.type.isCollection()) {
			}
			if (receiver.type.isUDT()) {
			}
			return null;
		}

		protected String toString(ColumnSpecification column) {
			return String.format("%s = %s", column, value);
		}

		public boolean isCompatibleWith(Operation.RawUpdate other) {
			return false;
		}

		public Term.Raw value() {
			return value;
		}
	}

	public static class SetElement implements Operation.RawUpdate {
		private final Term.Raw selector;

		private final Term.Raw value;

		public SetElement(Term.Raw selector, Term.Raw value) {
			this.selector = selector;
			this.value = value;
		}

		public Operation prepare(CFMetaData cfm, ColumnDefinition receiver) throws InvalidRequestException {
			if (!((receiver.type) instanceof CollectionType))
				throw new InvalidRequestException(String.format("Invalid operation (%s) for non collection column %s", toString(receiver), receiver.name));
			else
				if (!(receiver.type.isMultiCell()))
					throw new InvalidRequestException(String.format("Invalid operation (%s) for frozen collection column %s", toString(receiver), receiver.name));


			throw new AssertionError();
		}

		protected String toString(ColumnSpecification column) {
			return String.format("%s[%s] = %s", column.name, selector, value);
		}

		public boolean isCompatibleWith(Operation.RawUpdate other) {
			return !(other instanceof Operation.SetValue);
		}
	}

	public static class SetField implements Operation.RawUpdate {
		private final FieldIdentifier field;

		private final Term.Raw value;

		public SetField(FieldIdentifier field, Term.Raw value) {
			this.field = field;
			this.value = value;
		}

		public Operation prepare(CFMetaData cfm, ColumnDefinition receiver) throws InvalidRequestException {
			if (!(receiver.type.isUDT()))
				throw new InvalidRequestException(String.format("Invalid operation (%s) for non-UDT column %s", toString(receiver), receiver.name));
			else
				if (!(receiver.type.isMultiCell()))
					throw new InvalidRequestException(String.format("Invalid operation (%s) for frozen UDT column %s", toString(receiver), receiver.name));


			int fieldPosition = ((UserType) (receiver.type)).fieldPosition(field);
			if (fieldPosition == (-1))
				throw new InvalidRequestException(String.format("UDT column %s does not have a field named %s", receiver.name, field));

			Term val = value.prepare(cfm.ksName, UserTypes.fieldSpecOf(receiver, fieldPosition));
			return null;
		}

		protected String toString(ColumnSpecification column) {
			return String.format("%s.%s = %s", column.name, field, value);
		}

		public boolean isCompatibleWith(Operation.RawUpdate other) {
			if (other instanceof Operation.SetField)
				return !(((Operation.SetField) (other)).field.equals(field));
			else
				return !(other instanceof Operation.SetValue);

		}
	}

	public static class ElementAddition implements Operation.RawUpdate {
		private final Term.Raw selector;

		private final Term.Raw value;

		public ElementAddition(Term.Raw selector, Term.Raw value) {
			this.selector = selector;
			this.value = value;
		}

		public Operation prepare(CFMetaData cfm, ColumnDefinition receiver) throws InvalidRequestException {
			assert (receiver.type) instanceof MapType;
			Term k = selector.prepare(cfm.ksName, Maps.keySpecOf(receiver));
			Term v = value.prepare(cfm.ksName, Maps.valueSpecOf(receiver));
			return null;
		}

		protected String toString(ColumnSpecification column) {
			return String.format("%s = %s + %s", column.name, column.name, value);
		}

		public boolean isCompatibleWith(Operation.RawUpdate other) {
			return !(other instanceof Operation.SetValue);
		}
	}

	public static class ElementSubtraction implements Operation.RawUpdate {
		private final Term.Raw selector;

		private final Term.Raw value;

		public ElementSubtraction(Term.Raw selector, Term.Raw value) {
			this.selector = selector;
			this.value = value;
		}

		public Operation prepare(CFMetaData cfm, ColumnDefinition receiver) throws InvalidRequestException {
			assert (receiver.type) instanceof MapType;
			Term k = selector.prepare(cfm.ksName, Maps.keySpecOf(receiver));
			Term v = value.prepare(cfm.ksName, Maps.valueSpecOf(receiver));
			return null;
		}

		protected String toString(ColumnSpecification column) {
			return String.format("%s = %s + %s", column.name, column.name, value);
		}

		public boolean isCompatibleWith(Operation.RawUpdate other) {
			return !(other instanceof Operation.SetValue);
		}
	}

	public static class Addition implements Operation.RawUpdate {
		private final Term.Raw value;

		public Addition(Term.Raw value) {
			this.value = value;
		}

		public Operation prepare(CFMetaData cfm, ColumnDefinition receiver) throws InvalidRequestException {
			Term v = value.prepare(cfm.ksName, receiver);
			if (!((receiver.type) instanceof CollectionType)) {
				if (!((receiver.type) instanceof CounterColumnType))
					throw new InvalidRequestException(String.format("Invalid operation (%s) for non counter column %s", toString(receiver), receiver.name));

			}else
				if (!(receiver.type.isMultiCell()))
					throw new InvalidRequestException(String.format("Invalid operation (%s) for frozen collection column %s", toString(receiver), receiver.name));


			throw new AssertionError();
		}

		protected String toString(ColumnSpecification column) {
			return String.format("%s = %s + %s", column.name, column.name, value);
		}

		public boolean isCompatibleWith(Operation.RawUpdate other) {
			return !(other instanceof Operation.SetValue);
		}

		public Term.Raw value() {
			return value;
		}
	}

	public static class Substraction implements Operation.RawUpdate {
		private final Term.Raw value;

		public Substraction(Term.Raw value) {
			this.value = value;
		}

		public Operation prepare(CFMetaData cfm, ColumnDefinition receiver) throws InvalidRequestException {
			if (!((receiver.type) instanceof CollectionType)) {
				if (!((receiver.type) instanceof CounterColumnType))
					throw new InvalidRequestException(String.format("Invalid operation (%s) for non counter column %s", toString(receiver), receiver.name));

			}else
				if (!(receiver.type.isMultiCell()))
					throw new InvalidRequestException(String.format("Invalid operation (%s) for frozen collection column %s", toString(receiver), receiver.name));


			throw new AssertionError();
		}

		protected String toString(ColumnSpecification column) {
			return String.format("%s = %s - %s", column.name, column.name, value);
		}

		public boolean isCompatibleWith(Operation.RawUpdate other) {
			return !(other instanceof Operation.SetValue);
		}

		public Term.Raw value() {
			return value;
		}
	}

	public static class Prepend implements Operation.RawUpdate {
		private final Term.Raw value;

		public Prepend(Term.Raw value) {
			this.value = value;
		}

		public Operation prepare(CFMetaData cfm, ColumnDefinition receiver) throws InvalidRequestException {
			Term v = value.prepare(cfm.ksName, receiver);
			if (!((receiver.type) instanceof ListType))
				throw new InvalidRequestException(String.format("Invalid operation (%s) for non list column %s", toString(receiver), receiver.name));
			else
				if (!(receiver.type.isMultiCell()))
					throw new InvalidRequestException(String.format("Invalid operation (%s) for frozen list column %s", toString(receiver), receiver.name));


			return null;
		}

		protected String toString(ColumnSpecification column) {
			return String.format("%s = %s - %s", column.name, value, column.name);
		}

		public boolean isCompatibleWith(Operation.RawUpdate other) {
			return !(other instanceof Operation.SetValue);
		}
	}

	public static class ColumnDeletion implements Operation.RawDeletion {
		private final ColumnDefinition.Raw id;

		public ColumnDeletion(ColumnDefinition.Raw id) {
			this.id = id;
		}

		public ColumnDefinition.Raw affectedColumn() {
			return id;
		}

		public Operation prepare(String keyspace, ColumnDefinition receiver, CFMetaData cfm) throws InvalidRequestException {
			return null;
		}
	}

	public static class ElementDeletion implements Operation.RawDeletion {
		private final ColumnDefinition.Raw id;

		private final Term.Raw element;

		public ElementDeletion(ColumnDefinition.Raw id, Term.Raw element) {
			this.id = id;
			this.element = element;
		}

		public ColumnDefinition.Raw affectedColumn() {
			return id;
		}

		public Operation prepare(String keyspace, ColumnDefinition receiver, CFMetaData cfm) throws InvalidRequestException {
			if (!(receiver.type.isCollection()))
				throw new InvalidRequestException(String.format("Invalid deletion operation for non collection column %s", receiver.name));
			else
				if (!(receiver.type.isMultiCell()))
					throw new InvalidRequestException(String.format("Invalid deletion operation for frozen collection column %s", receiver.name));


			throw new AssertionError();
		}
	}

	public static class FieldDeletion implements Operation.RawDeletion {
		private final ColumnDefinition.Raw id;

		private final FieldIdentifier field;

		public FieldDeletion(ColumnDefinition.Raw id, FieldIdentifier field) {
			this.id = id;
			this.field = field;
		}

		public ColumnDefinition.Raw affectedColumn() {
			return id;
		}

		public Operation prepare(String keyspace, ColumnDefinition receiver, CFMetaData cfm) throws InvalidRequestException {
			if (!(receiver.type.isUDT()))
				throw new InvalidRequestException(String.format("Invalid field deletion operation for non-UDT column %s", receiver.name));
			else
				if (!(receiver.type.isMultiCell()))
					throw new InvalidRequestException(String.format("Frozen UDT column %s does not support field deletions", receiver.name));


			if ((((UserType) (receiver.type)).fieldPosition(field)) == (-1))
				throw new InvalidRequestException(String.format("UDT column %s does not have a field named %s", receiver.name, field));

			return null;
		}
	}
}

