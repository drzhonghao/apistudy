

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.cassandra.config.ColumnDefinition;
import org.apache.cassandra.cql3.AbstractMarker;
import org.apache.cassandra.cql3.ColumnIdentifier;
import org.apache.cassandra.cql3.ColumnSpecification;
import org.apache.cassandra.cql3.Operator;
import org.apache.cassandra.cql3.QueryOptions;
import org.apache.cassandra.cql3.Term;
import org.apache.cassandra.cql3.Terms;
import org.apache.cassandra.cql3.Tuples;
import org.apache.cassandra.cql3.functions.Function;
import org.apache.cassandra.cql3.restrictions.Restriction;
import org.apache.cassandra.cql3.restrictions.SingleRestriction;
import org.apache.cassandra.cql3.restrictions.TermSlice;
import org.apache.cassandra.cql3.statements.Bound;
import org.apache.cassandra.cql3.statements.RequestValidations;
import org.apache.cassandra.db.MultiCBuilder;
import org.apache.cassandra.db.filter.RowFilter;
import org.apache.cassandra.exceptions.InvalidRequestException;
import org.apache.cassandra.index.Index;
import org.apache.cassandra.index.SecondaryIndexManager;
import org.apache.cassandra.transport.ProtocolVersion;


public abstract class MultiColumnRestriction implements SingleRestriction {
	protected final List<ColumnDefinition> columnDefs;

	public MultiColumnRestriction(List<ColumnDefinition> columnDefs) {
		this.columnDefs = columnDefs;
	}

	@Override
	public boolean isMultiColumn() {
		return true;
	}

	@Override
	public ColumnDefinition getFirstColumn() {
		return columnDefs.get(0);
	}

	@Override
	public ColumnDefinition getLastColumn() {
		return columnDefs.get(((columnDefs.size()) - 1));
	}

	@Override
	public List<ColumnDefinition> getColumnDefs() {
		return columnDefs;
	}

	@Override
	public final SingleRestriction mergeWith(SingleRestriction otherRestriction) {
		return doMergeWith(otherRestriction);
	}

	protected abstract SingleRestriction doMergeWith(SingleRestriction otherRestriction);

	protected final String getColumnsInCommons(Restriction otherRestriction) {
		Set<ColumnDefinition> commons = new HashSet<>(getColumnDefs());
		commons.retainAll(otherRestriction.getColumnDefs());
		StringBuilder builder = new StringBuilder();
		for (ColumnDefinition columnDefinition : commons) {
			if ((builder.length()) != 0)
				builder.append(" ,");

			builder.append(columnDefinition.name);
		}
		return builder.toString();
	}

	@Override
	public final boolean hasSupportingIndex(SecondaryIndexManager indexManager) {
		for (Index index : indexManager.listIndexes())
			if (isSupportedBy(index))
				return true;


		return false;
	}

	protected abstract boolean isSupportedBy(Index index);

	public static class EQRestriction extends MultiColumnRestriction {
		protected final Term value;

		public EQRestriction(List<ColumnDefinition> columnDefs, Term value) {
			super(columnDefs);
			this.value = value;
		}

		@Override
		public void addFunctionsTo(List<Function> functions) {
			value.addFunctionsTo(functions);
		}

		@Override
		public String toString() {
			return String.format("EQ(%s)", value);
		}

		@Override
		public SingleRestriction doMergeWith(SingleRestriction otherRestriction) {
			throw RequestValidations.invalidRequest("%s cannot be restricted by more than one relation if it includes an Equal", getColumnsInCommons(otherRestriction));
		}

		@Override
		protected boolean isSupportedBy(Index index) {
			for (ColumnDefinition column : columnDefs)
				if (index.supportsExpression(column, Operator.EQ))
					return true;


			return false;
		}

		@Override
		public MultiCBuilder appendTo(MultiCBuilder builder, QueryOptions options) {
			Tuples.Value t = ((Tuples.Value) (value.bind(options)));
			List<ByteBuffer> values = t.getElements();
			for (int i = 0, m = values.size(); i < m; i++) {
				builder.addElementToAll(values.get(i));
				RequestValidations.checkFalse(builder.containsNull(), "Invalid null value for column %s", columnDefs.get(i).name);
			}
			return builder;
		}

		@Override
		public final void addRowFilterTo(RowFilter filter, SecondaryIndexManager indexMananger, QueryOptions options) {
			Tuples.Value t = ((Tuples.Value) (value.bind(options)));
			List<ByteBuffer> values = t.getElements();
			for (int i = 0, m = columnDefs.size(); i < m; i++) {
				ColumnDefinition columnDef = columnDefs.get(i);
				filter.add(columnDef, Operator.EQ, values.get(i));
			}
		}
	}

	public abstract static class INRestriction extends MultiColumnRestriction {
		public INRestriction(List<ColumnDefinition> columnDefs) {
			super(columnDefs);
		}

		@Override
		public MultiCBuilder appendTo(MultiCBuilder builder, QueryOptions options) {
			List<List<ByteBuffer>> splitInValues = splitValues(options);
			builder.addAllElementsToAll(splitInValues);
			if (builder.containsNull())
				throw RequestValidations.invalidRequest("Invalid null value in condition for columns: %s", ColumnDefinition.toIdentifiers(columnDefs));

			return builder;
		}

		@Override
		public boolean isIN() {
			return true;
		}

		@Override
		public SingleRestriction doMergeWith(SingleRestriction otherRestriction) {
			throw RequestValidations.invalidRequest("%s cannot be restricted by more than one relation if it includes a IN", getColumnsInCommons(otherRestriction));
		}

		@Override
		protected boolean isSupportedBy(Index index) {
			for (ColumnDefinition column : columnDefs)
				if (index.supportsExpression(column, Operator.IN))
					return true;


			return false;
		}

		@Override
		public final void addRowFilterTo(RowFilter filter, SecondaryIndexManager indexManager, QueryOptions options) {
			throw RequestValidations.invalidRequest("IN restrictions are not supported on indexed columns");
		}

		protected abstract List<List<ByteBuffer>> splitValues(QueryOptions options);
	}

	public static class InRestrictionWithValues extends MultiColumnRestriction.INRestriction {
		protected final List<Term> values;

		public InRestrictionWithValues(List<ColumnDefinition> columnDefs, List<Term> values) {
			super(columnDefs);
			this.values = values;
		}

		@Override
		public void addFunctionsTo(List<Function> functions) {
			Terms.addFunctions(values, functions);
		}

		@Override
		public String toString() {
			return String.format("IN(%s)", values);
		}

		@Override
		protected List<List<ByteBuffer>> splitValues(QueryOptions options) {
			List<List<ByteBuffer>> buffers = new ArrayList<>(values.size());
			for (Term value : values) {
				Term.MultiItemTerminal term = ((Term.MultiItemTerminal) (value.bind(options)));
				buffers.add(term.getElements());
			}
			return buffers;
		}
	}

	public static class InRestrictionWithMarker extends MultiColumnRestriction.INRestriction {
		protected final AbstractMarker marker;

		public InRestrictionWithMarker(List<ColumnDefinition> columnDefs, AbstractMarker marker) {
			super(columnDefs);
			this.marker = marker;
		}

		@Override
		public void addFunctionsTo(List<Function> functions) {
		}

		@Override
		public String toString() {
			return "IN ?";
		}

		@Override
		protected List<List<ByteBuffer>> splitValues(QueryOptions options) {
			Tuples.InMarker inMarker = ((Tuples.InMarker) (marker));
			Tuples.InValue inValue = inMarker.bind(options);
			RequestValidations.checkNotNull(inValue, "Invalid null value for IN restriction");
			return inValue.getSplitValues();
		}
	}

	public static class SliceRestriction extends MultiColumnRestriction {
		private final TermSlice slice;

		public SliceRestriction(List<ColumnDefinition> columnDefs, Bound bound, boolean inclusive, Term term) {
			this(columnDefs, TermSlice.newInstance(bound, inclusive, term));
		}

		SliceRestriction(List<ColumnDefinition> columnDefs, TermSlice slice) {
			super(columnDefs);
			this.slice = slice;
		}

		@Override
		public boolean isSlice() {
			return true;
		}

		@Override
		public MultiCBuilder appendTo(MultiCBuilder builder, QueryOptions options) {
			throw new UnsupportedOperationException();
		}

		@Override
		public MultiCBuilder appendBoundTo(MultiCBuilder builder, Bound bound, QueryOptions options) {
			boolean reversed = getFirstColumn().isReversedType();
			EnumMap<Bound, List<ByteBuffer>> componentBounds = new EnumMap<Bound, List<ByteBuffer>>(Bound.class);
			componentBounds.put(Bound.START, componentBounds(Bound.START, options));
			componentBounds.put(Bound.END, componentBounds(Bound.END, options));
			List<List<ByteBuffer>> toAdd = new ArrayList<>();
			List<ByteBuffer> values = new ArrayList<>();
			for (int i = 0, m = columnDefs.size(); i < m; i++) {
				ColumnDefinition column = columnDefs.get(i);
				Bound b = bound.reverseIfNeeded(column);
				if (reversed != (column.isReversedType())) {
					reversed = column.isReversedType();
					toAdd.add(values);
					if (!(hasComponent(b, i, componentBounds)))
						continue;

					if (hasComponent(b.reverse(), i, componentBounds))
						toAdd.add(values);

					values = new ArrayList<ByteBuffer>();
					List<ByteBuffer> vals = componentBounds.get(b);
					int n = Math.min(i, vals.size());
					for (int j = 0; j < n; j++) {
						ByteBuffer v = RequestValidations.checkNotNull(vals.get(j), "Invalid null value in condition for column %s", columnDefs.get(j).name);
						values.add(v);
					}
				}
				if (!(hasComponent(b, i, componentBounds)))
					continue;

				ByteBuffer v = RequestValidations.checkNotNull(componentBounds.get(b).get(i), "Invalid null value in condition for column %s", columnDefs.get(i).name);
				values.add(v);
			}
			toAdd.add(values);
			if (bound.isEnd())
				Collections.reverse(toAdd);

			return builder.addAllElementsToAll(toAdd);
		}

		@Override
		protected boolean isSupportedBy(Index index) {
			for (ColumnDefinition def : columnDefs)
				if (slice.isSupportedBy(def, index))
					return true;


			return false;
		}

		@Override
		public boolean hasBound(Bound bound) {
			return slice.hasBound(bound);
		}

		@Override
		public void addFunctionsTo(List<Function> functions) {
			slice.addFunctionsTo(functions);
		}

		@Override
		public boolean isInclusive(Bound bound) {
			return slice.isInclusive(bound);
		}

		@Override
		public SingleRestriction doMergeWith(SingleRestriction otherRestriction) {
			RequestValidations.checkTrue(otherRestriction.isSlice(), "Column \"%s\" cannot be restricted by both an equality and an inequality relation", getColumnsInCommons(otherRestriction));
			if (!(getFirstColumn().equals(otherRestriction.getFirstColumn()))) {
				ColumnDefinition column = ((getFirstColumn().position()) > (otherRestriction.getFirstColumn().position())) ? getFirstColumn() : otherRestriction.getFirstColumn();
				throw RequestValidations.invalidRequest("Column \"%s\" cannot be restricted by two inequalities not starting with the same column", column.name);
			}
			RequestValidations.checkFalse(((hasBound(Bound.START)) && (otherRestriction.hasBound(Bound.START))), "More than one restriction was found for the start bound on %s", getColumnsInCommons(otherRestriction));
			RequestValidations.checkFalse(((hasBound(Bound.END)) && (otherRestriction.hasBound(Bound.END))), "More than one restriction was found for the end bound on %s", getColumnsInCommons(otherRestriction));
			MultiColumnRestriction.SliceRestriction otherSlice = ((MultiColumnRestriction.SliceRestriction) (otherRestriction));
			List<ColumnDefinition> newColumnDefs = ((columnDefs.size()) >= (otherSlice.columnDefs.size())) ? columnDefs : otherSlice.columnDefs;
			return new MultiColumnRestriction.SliceRestriction(newColumnDefs, slice.merge(otherSlice.slice));
		}

		@Override
		public final void addRowFilterTo(RowFilter filter, SecondaryIndexManager indexManager, QueryOptions options) {
			throw RequestValidations.invalidRequest("Multi-column slice restrictions cannot be used for filtering.");
		}

		@Override
		public String toString() {
			return "SLICE" + (slice);
		}

		private List<ByteBuffer> componentBounds(Bound b, QueryOptions options) {
			if (!(slice.hasBound(b)))
				return Collections.emptyList();

			Term.Terminal terminal = slice.bound(b).bind(options);
			if (terminal instanceof Tuples.Value) {
				return ((Tuples.Value) (terminal)).getElements();
			}
			return Collections.singletonList(terminal.get(options.getProtocolVersion()));
		}

		private boolean hasComponent(Bound b, int index, EnumMap<Bound, List<ByteBuffer>> componentBounds) {
			return (componentBounds.get(b).size()) > index;
		}
	}

	public static class NotNullRestriction extends MultiColumnRestriction {
		public NotNullRestriction(List<ColumnDefinition> columnDefs) {
			super(columnDefs);
			assert (columnDefs.size()) == 1;
		}

		@Override
		public void addFunctionsTo(List<Function> functions) {
		}

		@Override
		public boolean isNotNull() {
			return true;
		}

		@Override
		public String toString() {
			return "IS NOT NULL";
		}

		@Override
		public SingleRestriction doMergeWith(SingleRestriction otherRestriction) {
			throw RequestValidations.invalidRequest("%s cannot be restricted by a relation if it includes an IS NOT NULL clause", getColumnsInCommons(otherRestriction));
		}

		@Override
		protected boolean isSupportedBy(Index index) {
			for (ColumnDefinition column : columnDefs)
				if (index.supportsExpression(column, Operator.IS_NOT))
					return true;


			return false;
		}

		@Override
		public MultiCBuilder appendTo(MultiCBuilder builder, QueryOptions options) {
			throw new UnsupportedOperationException("Cannot use IS NOT NULL restriction for slicing");
		}

		@Override
		public final void addRowFilterTo(RowFilter filter, SecondaryIndexManager indexMananger, QueryOptions options) {
			throw new UnsupportedOperationException("Secondary indexes do not support IS NOT NULL restrictions");
		}
	}
}

