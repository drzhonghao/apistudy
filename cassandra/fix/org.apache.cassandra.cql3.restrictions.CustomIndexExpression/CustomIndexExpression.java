

import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.cql3.ColumnIdentifier;
import org.apache.cassandra.cql3.ColumnSpecification;
import org.apache.cassandra.cql3.IndexName;
import org.apache.cassandra.cql3.QueryOptions;
import org.apache.cassandra.cql3.Term;
import org.apache.cassandra.cql3.VariableSpecifications;
import org.apache.cassandra.db.filter.RowFilter;
import org.apache.cassandra.db.marshal.AbstractType;


public class CustomIndexExpression {
	private final ColumnIdentifier valueColId = new ColumnIdentifier("custom index expression", false);

	public final IndexName targetIndex;

	public final Term.Raw valueRaw;

	private Term value;

	public CustomIndexExpression(IndexName targetIndex, Term.Raw value) {
		this.targetIndex = targetIndex;
		this.valueRaw = value;
	}

	public void prepareValue(CFMetaData cfm, AbstractType<?> expressionType, VariableSpecifications boundNames) {
		ColumnSpecification spec = new ColumnSpecification(cfm.ksName, cfm.ksName, valueColId, expressionType);
		value = valueRaw.prepare(cfm.ksName, spec);
		value.collectMarkerSpecification(boundNames);
	}

	public void addToRowFilter(RowFilter filter, CFMetaData cfm, QueryOptions options) {
	}
}

