

import com.google.common.base.Joiner;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.config.ColumnDefinition;
import org.apache.cassandra.cql3.ColumnIdentifier;
import org.apache.cassandra.cql3.QueryOptions;
import org.apache.cassandra.cql3.Term;
import org.apache.cassandra.cql3.functions.Function;
import org.apache.cassandra.cql3.restrictions.Restriction;
import org.apache.cassandra.cql3.restrictions.TermSlice;
import org.apache.cassandra.cql3.statements.Bound;
import org.apache.cassandra.db.filter.RowFilter;
import org.apache.cassandra.exceptions.InvalidRequestException;
import org.apache.cassandra.index.SecondaryIndexManager;


public abstract class TokenRestriction {
	protected final List<ColumnDefinition> columnDefs;

	protected final CFMetaData metadata;

	public TokenRestriction(CFMetaData metadata, List<ColumnDefinition> columnDefs) {
		this.columnDefs = columnDefs;
		this.metadata = metadata;
	}

	public boolean hasIN() {
		return false;
	}

	public boolean hasOnlyEqualityRestrictions() {
		return false;
	}

	public Set<Restriction> getRestrictions(ColumnDefinition columnDef) {
		return null;
	}

	public final boolean isOnToken() {
		return true;
	}

	public boolean needFiltering(CFMetaData cfm) {
		return false;
	}

	public boolean hasSlice() {
		return false;
	}

	public boolean hasUnrestrictedPartitionKeyComponents(CFMetaData cfm) {
		return false;
	}

	public List<ColumnDefinition> getColumnDefs() {
		return columnDefs;
	}

	public ColumnDefinition getFirstColumn() {
		return columnDefs.get(0);
	}

	public ColumnDefinition getLastColumn() {
		return columnDefs.get(((columnDefs.size()) - 1));
	}

	public boolean hasSupportingIndex(SecondaryIndexManager secondaryIndexManager) {
		return false;
	}

	public void addRowFilterTo(RowFilter filter, SecondaryIndexManager indexManager, QueryOptions options) {
		throw new UnsupportedOperationException("Index expression cannot be created for token restriction");
	}

	public final boolean isEmpty() {
		return getColumnDefs().isEmpty();
	}

	public final int size() {
		return getColumnDefs().size();
	}

	protected final String getColumnNamesAsString() {
		return Joiner.on(", ").join(ColumnDefinition.toIdentifiers(columnDefs));
	}

	public static final class EQRestriction extends TokenRestriction {
		private final Term value;

		public EQRestriction(CFMetaData cfm, List<ColumnDefinition> columnDefs, Term value) {
			super(cfm, columnDefs);
			this.value = value;
		}

		public void addFunctionsTo(List<Function> functions) {
			value.addFunctionsTo(functions);
		}

		public List<ByteBuffer> bounds(Bound b, QueryOptions options) throws InvalidRequestException {
			return values(options);
		}

		public boolean hasBound(Bound b) {
			return true;
		}

		public boolean isInclusive(Bound b) {
			return true;
		}

		public List<ByteBuffer> values(QueryOptions options) throws InvalidRequestException {
			return Collections.singletonList(value.bindAndGet(options));
		}

		public boolean hasContains() {
			return false;
		}
	}

	public static class SliceRestriction extends TokenRestriction {
		private final TermSlice slice;

		public SliceRestriction(CFMetaData cfm, List<ColumnDefinition> columnDefs, Bound bound, boolean inclusive, Term term) {
			super(cfm, columnDefs);
			slice = TermSlice.newInstance(bound, inclusive, term);
		}

		public boolean hasContains() {
			return false;
		}

		@Override
		public boolean hasSlice() {
			return true;
		}

		public List<ByteBuffer> values(QueryOptions options) throws InvalidRequestException {
			throw new UnsupportedOperationException();
		}

		public boolean hasBound(Bound b) {
			return slice.hasBound(b);
		}

		public List<ByteBuffer> bounds(Bound b, QueryOptions options) throws InvalidRequestException {
			return Collections.singletonList(slice.bound(b).bindAndGet(options));
		}

		public void addFunctionsTo(List<Function> functions) {
			slice.addFunctionsTo(functions);
		}

		public boolean isInclusive(Bound b) {
			return slice.isInclusive(b);
		}

		@Override
		public String toString() {
			return String.format("SLICE%s", slice);
		}

		private SliceRestriction(CFMetaData cfm, List<ColumnDefinition> columnDefs, TermSlice slice) {
			super(cfm, columnDefs);
			this.slice = slice;
		}
	}
}

