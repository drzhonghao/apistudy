

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.AbstractCollection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.config.ColumnDefinition;
import org.apache.cassandra.cql3.ColumnIdentifier;
import org.apache.cassandra.cql3.ColumnSpecification;
import org.apache.cassandra.db.Clustering;
import org.apache.cassandra.db.ClusteringComparator;
import org.apache.cassandra.db.ClusteringPrefix;
import org.apache.cassandra.db.DeletionTime;
import org.apache.cassandra.db.LivenessInfo;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.db.marshal.CollectionType;
import org.apache.cassandra.db.marshal.ShortType;
import org.apache.cassandra.db.marshal.UserType;
import org.apache.cassandra.db.rows.Cell;
import org.apache.cassandra.db.rows.CellPath;
import org.apache.cassandra.db.rows.ColumnData;
import org.apache.cassandra.db.rows.ComplexColumnData;
import org.apache.cassandra.db.rows.Row;
import org.apache.cassandra.db.rows.Unfiltered;
import org.apache.cassandra.serializers.MarshalException;
import org.apache.cassandra.serializers.TypeSerializer;
import org.apache.cassandra.utils.FBUtilities;

import static org.apache.cassandra.db.rows.Unfiltered.Kind.ROW;


public abstract class AbstractRow extends AbstractCollection<ColumnData> implements Row {
	public Unfiltered.Kind kind() {
		return ROW;
	}

	@Override
	public boolean hasLiveData(int nowInSec, boolean enforceStrictLiveness) {
		if (primaryKeyLivenessInfo().isLive(nowInSec))
			return true;
		else
			if (enforceStrictLiveness)
				return false;


		return Iterables.any(cells(), ( cell) -> cell.isLive(nowInSec));
	}

	public boolean isStatic() {
		return (clustering()) == (Clustering.STATIC_CLUSTERING);
	}

	public void digest(MessageDigest digest) {
		digest(digest, Collections.emptySet());
	}

	public void digest(MessageDigest digest, Set<ByteBuffer> columnsToExclude) {
		FBUtilities.updateWithByte(digest, kind().ordinal());
		clustering().digest(digest);
		deletion().digest(digest);
		primaryKeyLivenessInfo().digest(digest);
		for (ColumnData cd : this) {
		}
	}

	public void validateData(CFMetaData metadata) {
		Clustering clustering = clustering();
		for (int i = 0; i < (clustering.size()); i++) {
			ByteBuffer value = clustering.get(i);
			if (value != null)
				metadata.comparator.subtype(i).validate(value);

		}
		primaryKeyLivenessInfo().validate();
		if ((deletion().time().localDeletionTime()) < 0)
			throw new MarshalException("A local deletion time should not be negative");

		for (ColumnData cd : this)
			cd.validate();

	}

	public String toString(CFMetaData metadata) {
		return toString(metadata, false);
	}

	public String toString(CFMetaData metadata, boolean fullDetails) {
		return toString(metadata, true, fullDetails);
	}

	public String toString(CFMetaData metadata, boolean includeClusterKeys, boolean fullDetails) {
		StringBuilder sb = new StringBuilder();
		sb.append("Row");
		if (fullDetails) {
			sb.append("[info=").append(primaryKeyLivenessInfo());
			if (!(deletion().isLive()))
				sb.append(" del=").append(deletion());

			sb.append(" ]");
		}
		sb.append(": ");
		if (includeClusterKeys)
			sb.append(clustering().toString(metadata));
		else
			sb.append(clustering().toCQLString(metadata));

		sb.append(" | ");
		boolean isFirst = true;
		for (ColumnData cd : this) {
			if (isFirst)
				isFirst = false;
			else
				sb.append(", ");

			if (fullDetails) {
				if (cd.column().isSimple()) {
					sb.append(cd);
				}else {
					ComplexColumnData complexData = ((ComplexColumnData) (cd));
					if (!(complexData.complexDeletion().isLive()))
						sb.append("del(").append(cd.column().name).append(")=").append(complexData.complexDeletion());

					for (Cell cell : complexData)
						sb.append(", ").append(cell);

				}
			}else {
				if (cd.column().isSimple()) {
					Cell cell = ((Cell) (cd));
					sb.append(cell.column().name).append('=');
					if (cell.isTombstone())
						sb.append("<tombstone>");
					else
						sb.append(cell.column().type.getString(cell.value()));

				}else {
					sb.append(cd.column().name).append('=');
					ComplexColumnData complexData = ((ComplexColumnData) (cd));
					Function<Cell, String> transform = null;
					if (cd.column().type.isCollection()) {
						CollectionType ct = ((CollectionType) (cd.column().type));
						transform = ( cell) -> String.format("%s -> %s", ct.nameComparator().getString(cell.path().get(0)), ct.valueComparator().getString(cell.value()));
					}else
						if (cd.column().type.isUDT()) {
							UserType ut = ((UserType) (cd.column().type));
							transform = ( cell) -> {
								Short fId = ut.nameComparator().getSerializer().deserialize(cell.path().get(0));
								return String.format("%s -> %s", ut.fieldNameAsString(fId), ut.fieldType(fId).getString(cell.value()));
							};
						}

				}
			}
		}
		return sb.toString();
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof Row))
			return false;

		Row that = ((Row) (other));
		if (((!(this.clustering().equals(that.clustering()))) || (!(this.primaryKeyLivenessInfo().equals(that.primaryKeyLivenessInfo())))) || (!(this.deletion().equals(that.deletion()))))
			return false;

		return Iterables.elementsEqual(this, that);
	}

	@Override
	public int hashCode() {
		int hash = Objects.hash(clustering(), primaryKeyLivenessInfo(), deletion());
		for (ColumnData cd : this)
			hash += 31 * (cd.hashCode());

		return hash;
	}
}

