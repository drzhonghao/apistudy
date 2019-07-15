

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.apache.cassandra.config.ColumnDefinition;
import org.apache.cassandra.cql3.AbstractMarker;
import org.apache.cassandra.cql3.ColumnIdentifier;
import org.apache.cassandra.cql3.ColumnSpecification;
import org.apache.cassandra.cql3.Constants;
import org.apache.cassandra.cql3.Operator;
import org.apache.cassandra.cql3.QueryOptions;
import org.apache.cassandra.cql3.Term;
import org.apache.cassandra.cql3.Terms;
import org.apache.cassandra.cql3.Tuples;
import org.apache.cassandra.cql3.functions.Function;
import org.apache.cassandra.cql3.restrictions.MultiColumnRestriction;
import org.apache.cassandra.cql3.restrictions.SingleRestriction;
import org.apache.cassandra.cql3.restrictions.TermSlice;
import org.apache.cassandra.cql3.statements.Bound;
import org.apache.cassandra.cql3.statements.RequestValidations;
import org.apache.cassandra.db.MultiCBuilder;
import org.apache.cassandra.db.filter.RowFilter;
import org.apache.cassandra.exceptions.InvalidRequestException;
import org.apache.cassandra.index.Index;
import org.apache.cassandra.index.SecondaryIndexManager;
import org.apache.cassandra.utils.ByteBufferUtil;
import org.apache.cassandra.utils.Pair;


public abstract class SingleColumnRestriction implements SingleRestriction {
	protected final ColumnDefinition columnDef;

	public SingleColumnRestriction(ColumnDefinition columnDef) {
		this.columnDef = columnDef;
	}

	@Override
	public List<ColumnDefinition> getColumnDefs() {
		return Collections.singletonList(columnDef);
	}

	@Override
	public ColumnDefinition getFirstColumn() {
		return columnDef;
	}

	@Override
	public ColumnDefinition getLastColumn() {
		return columnDef;
	}

	@Override
	public boolean hasSupportingIndex(SecondaryIndexManager indexManager) {
		for (Index index : indexManager.listIndexes())
			if (isSupportedBy(index))
				return true;


		return false;
	}

	@Override
	public final SingleRestriction mergeWith(SingleRestriction otherRestriction) {
		if ((otherRestriction.isMultiColumn()) && (canBeConvertedToMultiColumnRestriction())) {
			return toMultiColumnRestriction().mergeWith(otherRestriction);
		}
		return doMergeWith(otherRestriction);
	}

	protected abstract SingleRestriction doMergeWith(SingleRestriction otherRestriction);

	abstract MultiColumnRestriction toMultiColumnRestriction();

	boolean canBeConvertedToMultiColumnRestriction() {
		return true;
	}

	protected abstract boolean isSupportedBy(Index index);

	public static class EQRestriction extends SingleColumnRestriction {
		public final Term value;

		public EQRestriction(ColumnDefinition columnDef, Term value) {
			super(columnDef);
			this.value = value;
		}

		@Override
		public void addFunctionsTo(List<Function> functions) {
			value.addFunctionsTo(functions);
		}

		@Override
		public boolean isEQ() {
			return true;
		}

		@Override
		MultiColumnRestriction toMultiColumnRestriction() {
			return new MultiColumnRestriction.EQRestriction(Collections.singletonList(columnDef), value);
		}

		@Override
		public void addRowFilterTo(RowFilter filter, SecondaryIndexManager indexManager, QueryOptions options) {
			filter.add(columnDef, Operator.EQ, value.bindAndGet(options));
		}

		@Override
		public MultiCBuilder appendTo(MultiCBuilder builder, QueryOptions options) {
			builder.addElementToAll(value.bindAndGet(options));
			RequestValidations.checkFalse(builder.containsNull(), "Invalid null value in condition for column %s", columnDef.name);
			RequestValidations.checkFalse(builder.containsUnset(), "Invalid unset value for column %s", columnDef.name);
			return builder;
		}

		@Override
		public String toString() {
			return String.format("EQ(%s)", value);
		}

		@Override
		public SingleRestriction doMergeWith(SingleRestriction otherRestriction) {
			throw RequestValidations.invalidRequest("%s cannot be restricted by more than one relation if it includes an Equal", columnDef.name);
		}

		@Override
		protected boolean isSupportedBy(Index index) {
			return index.supportsExpression(columnDef, Operator.EQ);
		}
	}

	public static abstract class INRestriction extends SingleColumnRestriction {
		public INRestriction(ColumnDefinition columnDef) {
			super(columnDef);
		}

		@Override
		public final boolean isIN() {
			return true;
		}

		@Override
		public final SingleRestriction doMergeWith(SingleRestriction otherRestriction) {
			throw RequestValidations.invalidRequest("%s cannot be restricted by more than one relation if it includes a IN", columnDef.name);
		}

		@Override
		public MultiCBuilder appendTo(MultiCBuilder builder, QueryOptions options) {
			builder.addEachElementToAll(getValues(options));
			RequestValidations.checkFalse(builder.containsNull(), "Invalid null value in condition for column %s", columnDef.name);
			RequestValidations.checkFalse(builder.containsUnset(), "Invalid unset value for column %s", columnDef.name);
			return builder;
		}

		@Override
		public void addRowFilterTo(RowFilter filter, SecondaryIndexManager indexManager, QueryOptions options) {
			throw RequestValidations.invalidRequest("IN restrictions are not supported on indexed columns");
		}

		@Override
		protected final boolean isSupportedBy(Index index) {
			return index.supportsExpression(columnDef, Operator.IN);
		}

		protected abstract List<ByteBuffer> getValues(QueryOptions options);
	}

	public static class InRestrictionWithValues extends SingleColumnRestriction.INRestriction {
		protected final List<Term> values;

		public InRestrictionWithValues(ColumnDefinition columnDef, List<Term> values) {
			super(columnDef);
			this.values = values;
		}

		@Override
		MultiColumnRestriction toMultiColumnRestriction() {
			return new MultiColumnRestriction.InRestrictionWithValues(Collections.singletonList(columnDef), values);
		}

		@Override
		public void addFunctionsTo(List<Function> functions) {
			Terms.addFunctions(values, functions);
		}

		@Override
		protected List<ByteBuffer> getValues(QueryOptions options) {
			List<ByteBuffer> buffers = new ArrayList<>(values.size());
			for (Term value : values)
				buffers.add(value.bindAndGet(options));

			return buffers;
		}

		@Override
		public String toString() {
			return String.format("IN(%s)", values);
		}
	}

	public static class InRestrictionWithMarker extends SingleColumnRestriction.INRestriction {
		protected final AbstractMarker marker;

		public InRestrictionWithMarker(ColumnDefinition columnDef, AbstractMarker marker) {
			super(columnDef);
			this.marker = marker;
		}

		@Override
		public void addFunctionsTo(List<Function> functions) {
		}

		@Override
		MultiColumnRestriction toMultiColumnRestriction() {
			return new MultiColumnRestriction.InRestrictionWithMarker(Collections.singletonList(columnDef), marker);
		}

		@Override
		protected List<ByteBuffer> getValues(QueryOptions options) {
			Term.Terminal term = marker.bind(options);
			RequestValidations.checkNotNull(term, "Invalid null value for column %s", columnDef.name);
			RequestValidations.checkFalse((term == (Constants.UNSET_VALUE)), "Invalid unset value for column %s", columnDef.name);
			Term.MultiItemTerminal lval = ((Term.MultiItemTerminal) (term));
			return lval.getElements();
		}

		@Override
		public String toString() {
			return "IN ?";
		}
	}

	public static class SliceRestriction extends SingleColumnRestriction {
		public final TermSlice slice;

		public SliceRestriction(ColumnDefinition columnDef, Bound bound, boolean inclusive, Term term) {
			super(columnDef);
			slice = TermSlice.newInstance(bound, inclusive, term);
		}

		@Override
		public void addFunctionsTo(List<Function> functions) {
			slice.addFunctionsTo(functions);
		}

		@Override
		MultiColumnRestriction toMultiColumnRestriction() {
			return null;
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
		public boolean hasBound(Bound b) {
			return slice.hasBound(b);
		}

		@Override
		public MultiCBuilder appendBoundTo(MultiCBuilder builder, Bound bound, QueryOptions options) {
			Bound b = bound.reverseIfNeeded(getFirstColumn());
			if (!(hasBound(b)))
				return builder;

			ByteBuffer value = slice.bound(b).bindAndGet(options);
			RequestValidations.checkBindValueSet(value, "Invalid unset value for column %s", columnDef.name);
			return builder.addElementToAll(value);
		}

		@Override
		public boolean isInclusive(Bound b) {
			return slice.isInclusive(b);
		}

		@Override
		public SingleRestriction doMergeWith(SingleRestriction otherRestriction) {
			RequestValidations.checkTrue(otherRestriction.isSlice(), "Column \"%s\" cannot be restricted by both an equality and an inequality relation", columnDef.name);
			SingleColumnRestriction.SliceRestriction otherSlice = ((SingleColumnRestriction.SliceRestriction) (otherRestriction));
			RequestValidations.checkFalse(((hasBound(Bound.START)) && (otherSlice.hasBound(Bound.START))), "More than one restriction was found for the start bound on %s", columnDef.name);
			RequestValidations.checkFalse(((hasBound(Bound.END)) && (otherSlice.hasBound(Bound.END))), "More than one restriction was found for the end bound on %s", columnDef.name);
			return new SingleColumnRestriction.SliceRestriction(columnDef, slice.merge(otherSlice.slice));
		}

		@Override
		public void addRowFilterTo(RowFilter filter, SecondaryIndexManager indexManager, QueryOptions options) {
			for (Bound b : Bound.values())
				if (hasBound(b))
					filter.add(columnDef, slice.getIndexOperator(b), slice.bound(b).bindAndGet(options));


		}

		@Override
		protected boolean isSupportedBy(Index index) {
			return slice.isSupportedBy(columnDef, index);
		}

		@Override
		public String toString() {
			return String.format("SLICE%s", slice);
		}

		SliceRestriction(ColumnDefinition columnDef, TermSlice slice) {
			super(columnDef);
			this.slice = slice;
		}
	}

	public static final class ContainsRestriction extends SingleColumnRestriction {
		private List<Term> values = new ArrayList<>();

		private List<Term> keys = new ArrayList<>();

		private List<Term> entryKeys = new ArrayList<>();

		private List<Term> entryValues = new ArrayList<>();

		public ContainsRestriction(ColumnDefinition columnDef, Term t, boolean isKey) {
			super(columnDef);
			if (isKey)
				keys.add(t);
			else
				values.add(t);

		}

		public ContainsRestriction(ColumnDefinition columnDef, Term mapKey, Term mapValue) {
			super(columnDef);
			entryKeys.add(mapKey);
			entryValues.add(mapValue);
		}

		@Override
		MultiColumnRestriction toMultiColumnRestriction() {
			throw new UnsupportedOperationException();
		}

		@Override
		boolean canBeConvertedToMultiColumnRestriction() {
			return false;
		}

		@Override
		public MultiCBuilder appendTo(MultiCBuilder builder, QueryOptions options) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean isContains() {
			return true;
		}

		@Override
		public SingleRestriction doMergeWith(SingleRestriction otherRestriction) {
			RequestValidations.checkTrue(otherRestriction.isContains(), "Collection column %s can only be restricted by CONTAINS, CONTAINS KEY, or map-entry equality", columnDef.name);
			SingleColumnRestriction.ContainsRestriction newContains = new SingleColumnRestriction.ContainsRestriction(columnDef);
			SingleColumnRestriction.ContainsRestriction.copyKeysAndValues(this, newContains);
			SingleColumnRestriction.ContainsRestriction.copyKeysAndValues(((SingleColumnRestriction.ContainsRestriction) (otherRestriction)), newContains);
			return newContains;
		}

		@Override
		public void addRowFilterTo(RowFilter filter, SecondaryIndexManager indexManager, QueryOptions options) {
			for (ByteBuffer value : SingleColumnRestriction.ContainsRestriction.bindAndGet(values, options))
				filter.add(columnDef, Operator.CONTAINS, value);

			for (ByteBuffer key : SingleColumnRestriction.ContainsRestriction.bindAndGet(keys, options))
				filter.add(columnDef, Operator.CONTAINS_KEY, key);

			List<ByteBuffer> eks = SingleColumnRestriction.ContainsRestriction.bindAndGet(entryKeys, options);
			List<ByteBuffer> evs = SingleColumnRestriction.ContainsRestriction.bindAndGet(entryValues, options);
			assert (eks.size()) == (evs.size());
			for (int i = 0; i < (eks.size()); i++)
				filter.addMapEquality(columnDef, eks.get(i), Operator.EQ, evs.get(i));

		}

		@Override
		protected boolean isSupportedBy(Index index) {
			boolean supported = false;
			if ((numberOfValues()) > 0)
				supported |= index.supportsExpression(columnDef, Operator.CONTAINS);

			if ((numberOfKeys()) > 0)
				supported |= index.supportsExpression(columnDef, Operator.CONTAINS_KEY);

			if ((numberOfEntries()) > 0)
				supported |= index.supportsExpression(columnDef, Operator.EQ);

			return supported;
		}

		public int numberOfValues() {
			return values.size();
		}

		public int numberOfKeys() {
			return keys.size();
		}

		public int numberOfEntries() {
			return entryKeys.size();
		}

		@Override
		public void addFunctionsTo(List<Function> functions) {
			Terms.addFunctions(values, functions);
			Terms.addFunctions(keys, functions);
			Terms.addFunctions(entryKeys, functions);
			Terms.addFunctions(entryValues, functions);
		}

		@Override
		public String toString() {
			return String.format("CONTAINS(values=%s, keys=%s, entryKeys=%s, entryValues=%s)", values, keys, entryKeys, entryValues);
		}

		@Override
		public boolean hasBound(Bound b) {
			throw new UnsupportedOperationException();
		}

		@Override
		public MultiCBuilder appendBoundTo(MultiCBuilder builder, Bound bound, QueryOptions options) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean isInclusive(Bound b) {
			throw new UnsupportedOperationException();
		}

		private static List<ByteBuffer> bindAndGet(List<Term> terms, QueryOptions options) {
			List<ByteBuffer> buffers = new ArrayList<>(terms.size());
			for (Term value : terms)
				buffers.add(value.bindAndGet(options));

			return buffers;
		}

		private static void copyKeysAndValues(SingleColumnRestriction.ContainsRestriction from, SingleColumnRestriction.ContainsRestriction to) {
			to.values.addAll(from.values);
			to.keys.addAll(from.keys);
			to.entryKeys.addAll(from.entryKeys);
			to.entryValues.addAll(from.entryValues);
		}

		private ContainsRestriction(ColumnDefinition columnDef) {
			super(columnDef);
		}
	}

	public static final class IsNotNullRestriction extends SingleColumnRestriction {
		public IsNotNullRestriction(ColumnDefinition columnDef) {
			super(columnDef);
		}

		@Override
		public void addFunctionsTo(List<Function> functions) {
		}

		@Override
		public boolean isNotNull() {
			return true;
		}

		@Override
		MultiColumnRestriction toMultiColumnRestriction() {
			return new MultiColumnRestriction.NotNullRestriction(Collections.singletonList(columnDef));
		}

		@Override
		public void addRowFilterTo(RowFilter filter, SecondaryIndexManager indexManager, QueryOptions options) {
			throw new UnsupportedOperationException("Secondary indexes do not support IS NOT NULL restrictions");
		}

		@Override
		public MultiCBuilder appendTo(MultiCBuilder builder, QueryOptions options) {
			throw new UnsupportedOperationException("Cannot use IS NOT NULL restriction for slicing");
		}

		@Override
		public String toString() {
			return "IS NOT NULL";
		}

		@Override
		public SingleRestriction doMergeWith(SingleRestriction otherRestriction) {
			throw RequestValidations.invalidRequest("%s cannot be restricted by a relation if it includes an IS NOT NULL", columnDef.name);
		}

		@Override
		protected boolean isSupportedBy(Index index) {
			return index.supportsExpression(columnDef, Operator.IS_NOT);
		}
	}

	public static final class LikeRestriction extends SingleColumnRestriction {
		private static final ByteBuffer LIKE_WILDCARD = ByteBufferUtil.bytes("%");

		private final Operator operator;

		private final Term value;

		public LikeRestriction(ColumnDefinition columnDef, Operator operator, Term value) {
			super(columnDef);
			this.operator = operator;
			this.value = value;
		}

		@Override
		public void addFunctionsTo(List<Function> functions) {
			value.addFunctionsTo(functions);
		}

		@Override
		public boolean isEQ() {
			return false;
		}

		@Override
		public boolean isLIKE() {
			return true;
		}

		@Override
		public boolean canBeConvertedToMultiColumnRestriction() {
			return false;
		}

		@Override
		MultiColumnRestriction toMultiColumnRestriction() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void addRowFilterTo(RowFilter filter, SecondaryIndexManager indexManager, QueryOptions options) {
			Pair<Operator, ByteBuffer> operation = SingleColumnRestriction.LikeRestriction.makeSpecific(value.bindAndGet(options));
			RowFilter.SimpleExpression expression = filter.add(columnDef, operation.left, operation.right);
			indexManager.getBestIndexFor(expression).orElseThrow(() -> RequestValidations.invalidRequest("%s is only supported on properly indexed columns", expression));
		}

		@Override
		public MultiCBuilder appendTo(MultiCBuilder builder, QueryOptions options) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String toString() {
			return operator.toString();
		}

		@Override
		public SingleRestriction doMergeWith(SingleRestriction otherRestriction) {
			throw RequestValidations.invalidRequest("%s cannot be restricted by more than one relation if it includes a %s", columnDef.name, operator);
		}

		@Override
		protected boolean isSupportedBy(Index index) {
			return index.supportsExpression(columnDef, operator);
		}

		private static Pair<Operator, ByteBuffer> makeSpecific(ByteBuffer value) {
			Operator operator;
			int beginIndex = value.position();
			int endIndex = (value.limit()) - 1;
			if (ByteBufferUtil.endsWith(value, SingleColumnRestriction.LikeRestriction.LIKE_WILDCARD)) {
				if (ByteBufferUtil.startsWith(value, SingleColumnRestriction.LikeRestriction.LIKE_WILDCARD)) {
					operator = Operator.LIKE_CONTAINS;
					beginIndex = +1;
				}else {
					operator = Operator.LIKE_PREFIX;
				}
			}else
				if (ByteBufferUtil.startsWith(value, SingleColumnRestriction.LikeRestriction.LIKE_WILDCARD)) {
					operator = Operator.LIKE_SUFFIX;
					beginIndex += 1;
					endIndex += 1;
				}else {
					operator = Operator.LIKE_MATCHES;
					endIndex += 1;
				}

			if ((endIndex == 0) || (beginIndex == endIndex))
				throw RequestValidations.invalidRequest("LIKE value can't be empty.");

			ByteBuffer newValue = value.duplicate();
			newValue.position(beginIndex);
			newValue.limit(endIndex);
			return Pair.create(operator, newValue);
		}
	}

	public static class SuperColumnMultiEQRestriction extends SingleColumnRestriction.EQRestriction {
		public ByteBuffer firstValue;

		public ByteBuffer secondValue;

		public SuperColumnMultiEQRestriction(ColumnDefinition columnDef, Term value) {
			super(columnDef, value);
		}

		@Override
		public MultiCBuilder appendTo(MultiCBuilder builder, QueryOptions options) {
			Term term = value.bind(options);
			assert term instanceof Tuples.Value;
			firstValue = ((Tuples.Value) (term)).getElements().get(0);
			secondValue = ((Tuples.Value) (term)).getElements().get(1);
			builder.addElementToAll(firstValue);
			RequestValidations.checkFalse(builder.containsNull(), "Invalid null value in condition for column %s", columnDef.name);
			RequestValidations.checkFalse(builder.containsUnset(), "Invalid unset value for column %s", columnDef.name);
			return builder;
		}
	}

	public static class SuperColumnMultiSliceRestriction extends SingleColumnRestriction.SliceRestriction {
		public ByteBuffer firstValue;

		public ByteBuffer secondValue;

		public final Bound bound;

		public final boolean trueInclusive;

		public SuperColumnMultiSliceRestriction(ColumnDefinition columnDef, Bound bound, boolean inclusive, Term term) {
			super(columnDef, bound, true, term);
			this.bound = bound;
			this.trueInclusive = inclusive;
		}

		@Override
		public MultiCBuilder appendBoundTo(MultiCBuilder builder, Bound bound, QueryOptions options) {
			Bound b = bound.reverseIfNeeded(getFirstColumn());
			if (!(hasBound(b)))
				return builder;

			Term term = slice.bound(b);
			assert term instanceof Tuples.Value;
			firstValue = ((Tuples.Value) (term)).getElements().get(0);
			secondValue = ((Tuples.Value) (term)).getElements().get(1);
			RequestValidations.checkBindValueSet(firstValue, "Invalid unset value for column %s", columnDef.name);
			RequestValidations.checkBindValueSet(secondValue, "Invalid unset value for column %s", columnDef.name);
			return builder.addElementToAll(firstValue);
		}
	}

	public static final class SuperColumnKeyEQRestriction extends SingleColumnRestriction.EQRestriction {
		public SuperColumnKeyEQRestriction(ColumnDefinition columnDef, Term value) {
			super(columnDef, value);
		}

		public ByteBuffer bindValue(QueryOptions options) {
			return value.bindAndGet(options);
		}

		@Override
		public MultiCBuilder appendBoundTo(MultiCBuilder builder, Bound bound, QueryOptions options) {
			return builder;
		}

		@Override
		public void addRowFilterTo(RowFilter filter, SecondaryIndexManager indexManager, QueryOptions options) throws InvalidRequestException {
		}
	}

	public static abstract class SuperColumnKeyINRestriction extends SingleColumnRestriction.INRestriction {
		public SuperColumnKeyINRestriction(ColumnDefinition columnDef) {
			super(columnDef);
		}

		@Override
		public MultiCBuilder appendTo(MultiCBuilder builder, QueryOptions options) {
			return builder;
		}

		@Override
		public void addRowFilterTo(RowFilter filter, SecondaryIndexManager indexManager, QueryOptions options) throws InvalidRequestException {
		}

		public void addFunctionsTo(List<Function> functions) {
		}

		MultiColumnRestriction toMultiColumnRestriction() {
			return null;
		}

		public abstract List<ByteBuffer> getValues(QueryOptions options) throws InvalidRequestException;
	}

	public static class SuperColumnKeyINRestrictionWithMarkers extends SingleColumnRestriction.SuperColumnKeyINRestriction {
		protected final AbstractMarker marker;

		public SuperColumnKeyINRestrictionWithMarkers(ColumnDefinition columnDef, AbstractMarker marker) {
			super(columnDef);
			this.marker = marker;
		}

		public List<ByteBuffer> getValues(QueryOptions options) throws InvalidRequestException {
			Term.Terminal term = marker.bind(options);
			RequestValidations.checkNotNull(term, "Invalid null value for column %s", columnDef.name);
			RequestValidations.checkFalse((term == (Constants.UNSET_VALUE)), "Invalid unset value for column %s", columnDef.name);
			Term.MultiItemTerminal lval = ((Term.MultiItemTerminal) (term));
			return lval.getElements();
		}
	}

	public static class SuperColumnKeyINRestrictionWithValues extends SingleColumnRestriction.SuperColumnKeyINRestriction {
		private final List<Term> values;

		public SuperColumnKeyINRestrictionWithValues(ColumnDefinition columnDef, List<Term> values) {
			super(columnDef);
			this.values = values;
		}

		public List<ByteBuffer> getValues(QueryOptions options) throws InvalidRequestException {
			List<ByteBuffer> buffers = new ArrayList<>(values.size());
			for (Term value : values)
				buffers.add(value.bindAndGet(options));

			return buffers;
		}
	}

	public static class SuperColumnKeySliceRestriction extends SingleColumnRestriction.SliceRestriction {
		private Term term;

		public SuperColumnKeySliceRestriction(ColumnDefinition columnDef, Bound bound, boolean inclusive, Term term) {
			super(columnDef, bound, inclusive, term);
			this.term = term;
		}

		public ByteBuffer bindValue(QueryOptions options) {
			return term.bindAndGet(options);
		}

		@Override
		public MultiCBuilder appendBoundTo(MultiCBuilder builder, Bound bound, QueryOptions options) {
			return builder;
		}

		@Override
		public void addRowFilterTo(RowFilter filter, SecondaryIndexManager indexManager, QueryOptions options) throws InvalidRequestException {
		}
	}
}

