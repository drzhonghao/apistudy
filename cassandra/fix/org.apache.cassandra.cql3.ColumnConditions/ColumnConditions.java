

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.cassandra.config.ColumnDefinition;
import org.apache.cassandra.cql3.ColumnCondition;
import org.apache.cassandra.cql3.QueryOptions;
import org.apache.cassandra.cql3.statements.CQL3CasRequest;
import org.apache.cassandra.db.Clustering;


public final class ColumnConditions {
	private final List<ColumnCondition> columnConditions;

	private final List<ColumnCondition> staticConditions;

	private ColumnConditions(ColumnConditions.Builder builder) {
		this.columnConditions = builder.columnConditions;
		this.staticConditions = builder.staticConditions;
	}

	public boolean appliesToStaticColumns() {
		return !(staticConditions.isEmpty());
	}

	public boolean appliesToRegularColumns() {
		return !(columnConditions.isEmpty());
	}

	public Collection<ColumnDefinition> getColumns() {
		return Stream.concat(columnConditions.stream(), staticConditions.stream()).map(( e) -> e.column).collect(Collectors.toList());
	}

	public boolean isEmpty() {
		return (columnConditions.isEmpty()) && (staticConditions.isEmpty());
	}

	public void addConditionsTo(CQL3CasRequest request, Clustering clustering, QueryOptions options) {
		if (!(columnConditions.isEmpty()))
			request.addConditions(clustering, columnConditions, options);

		if (!(staticConditions.isEmpty()))
			request.addConditions(Clustering.STATIC_CLUSTERING, staticConditions, options);

	}

	public void addFunctionsTo(List<org.apache.cassandra.cql3.functions.Function> functions) {
		columnConditions.forEach(( p) -> p.addFunctionsTo(functions));
		staticConditions.forEach(( p) -> p.addFunctionsTo(functions));
	}

	public Collection<ColumnCondition> columnConditions() {
		return this.columnConditions;
	}

	public static ColumnConditions.Builder newBuilder() {
		return new ColumnConditions.Builder();
	}

	public static final class Builder {
		private List<ColumnCondition> columnConditions = Collections.emptyList();

		private List<ColumnCondition> staticConditions = Collections.emptyList();

		public ColumnConditions.Builder add(ColumnCondition condition) {
			List<ColumnCondition> conds = null;
			if (condition.column.isStatic()) {
				if (staticConditions.isEmpty())
					staticConditions = new ArrayList<>();

				conds = staticConditions;
			}else {
				if (columnConditions.isEmpty())
					columnConditions = new ArrayList<>();

				conds = columnConditions;
			}
			conds.add(condition);
			return this;
		}

		public ColumnConditions build() {
			return new ColumnConditions(this);
		}

		private Builder() {
		}
	}
}

