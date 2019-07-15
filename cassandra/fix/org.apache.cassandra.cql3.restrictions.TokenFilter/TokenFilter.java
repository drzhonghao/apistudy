

import com.google.common.collect.BoundType;
import com.google.common.collect.ImmutableRangeSet;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.config.ColumnDefinition;
import org.apache.cassandra.cql3.QueryOptions;
import org.apache.cassandra.cql3.functions.Function;
import org.apache.cassandra.cql3.restrictions.Restriction;
import org.apache.cassandra.cql3.restrictions.TokenRestriction;
import org.apache.cassandra.cql3.statements.Bound;
import org.apache.cassandra.db.filter.RowFilter;
import org.apache.cassandra.dht.IPartitioner;
import org.apache.cassandra.dht.Token;
import org.apache.cassandra.exceptions.InvalidRequestException;
import org.apache.cassandra.index.SecondaryIndexManager;


final class TokenFilter {
	private final TokenRestriction tokenRestriction = null;

	private final IPartitioner partitioner = null;

	public boolean hasIN() {
		return false;
	}

	public boolean hasContains() {
		return false;
	}

	public boolean hasOnlyEqualityRestrictions() {
		return false;
	}

	public Set<Restriction> getRestrictions(ColumnDefinition columnDef) {
		Set<Restriction> set = new HashSet<>();
		set.addAll(tokenRestriction.getRestrictions(columnDef));
		return set;
	}

	public boolean isOnToken() {
		return false;
	}

	public List<ByteBuffer> values(QueryOptions options) throws InvalidRequestException {
		return null;
	}

	public boolean isInclusive(Bound bound) {
		return false;
	}

	public boolean hasBound(Bound bound) {
		return false;
	}

	public List<ByteBuffer> bounds(Bound bound, QueryOptions options) throws InvalidRequestException {
		return null;
	}

	private List<ByteBuffer> filter(List<ByteBuffer> values, QueryOptions options) throws InvalidRequestException {
		RangeSet<Token> rangeSet = (tokenRestriction.hasSlice()) ? toRangeSet(tokenRestriction, options) : toRangeSet(tokenRestriction.values(options));
		return filterWithRangeSet(rangeSet, values);
	}

	private List<ByteBuffer> filterWithRangeSet(RangeSet<Token> tokens, List<ByteBuffer> values) {
		List<ByteBuffer> remaining = new ArrayList<>();
		for (ByteBuffer value : values) {
			Token token = partitioner.getToken(value);
			if (!(tokens.contains(token)))
				continue;

			remaining.add(value);
		}
		return remaining;
	}

	private RangeSet<Token> toRangeSet(List<ByteBuffer> buffers) {
		ImmutableRangeSet.Builder<Token> builder = ImmutableRangeSet.builder();
		for (ByteBuffer buffer : buffers)
			builder.add(Range.singleton(deserializeToken(buffer)));

		return builder.build();
	}

	private RangeSet<Token> toRangeSet(TokenRestriction slice, QueryOptions options) throws InvalidRequestException {
		if (slice.hasBound(Bound.START)) {
			Token start = deserializeToken(slice.bounds(Bound.START, options).get(0));
			BoundType startBoundType = TokenFilter.toBoundType(slice.isInclusive(Bound.START));
			if (slice.hasBound(Bound.END)) {
				BoundType endBoundType = TokenFilter.toBoundType(slice.isInclusive(Bound.END));
				Token end = deserializeToken(slice.bounds(Bound.END, options).get(0));
				if ((start.equals(end)) && (((BoundType.OPEN) == startBoundType) || ((BoundType.OPEN) == endBoundType)))
					return ImmutableRangeSet.of();

				if ((start.compareTo(end)) <= 0)
					return ImmutableRangeSet.of(Range.range(start, startBoundType, end, endBoundType));

				return ImmutableRangeSet.<Token>builder().add(Range.upTo(end, endBoundType)).add(Range.downTo(start, startBoundType)).build();
			}
			return ImmutableRangeSet.of(Range.downTo(start, startBoundType));
		}
		Token end = deserializeToken(slice.bounds(Bound.END, options).get(0));
		return ImmutableRangeSet.of(Range.upTo(end, TokenFilter.toBoundType(slice.isInclusive(Bound.END))));
	}

	private Token deserializeToken(ByteBuffer buffer) {
		return partitioner.getTokenFactory().fromByteArray(buffer);
	}

	private static BoundType toBoundType(boolean inclusive) {
		return inclusive ? BoundType.CLOSED : BoundType.OPEN;
	}

	public ColumnDefinition getFirstColumn() {
		return null;
	}

	public ColumnDefinition getLastColumn() {
		return null;
	}

	public List<ColumnDefinition> getColumnDefs() {
		return null;
	}

	public void addFunctionsTo(List<Function> functions) {
	}

	public boolean hasSupportingIndex(SecondaryIndexManager indexManager) {
		return false;
	}

	public void addRowFilterTo(RowFilter filter, SecondaryIndexManager indexManager, QueryOptions options) {
	}

	public boolean isEmpty() {
		return false;
	}

	public int size() {
		return 0;
	}

	public boolean needFiltering(CFMetaData cfm) {
		return false;
	}

	public boolean hasUnrestrictedPartitionKeyComponents(CFMetaData cfm) {
		return false;
	}

	public boolean hasSlice() {
		return false;
	}
}

