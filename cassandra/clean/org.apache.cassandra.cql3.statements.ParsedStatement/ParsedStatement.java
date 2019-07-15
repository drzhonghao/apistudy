import org.apache.cassandra.cql3.statements.*;


import java.util.Collections;
import java.util.List;

import org.apache.cassandra.cql3.*;
import org.apache.cassandra.cql3.functions.Function;
import org.apache.cassandra.exceptions.RequestValidationException;
import org.apache.cassandra.service.ClientState;

public abstract class ParsedStatement
{
    private VariableSpecifications variables;

    public VariableSpecifications getBoundVariables()
    {
        return variables;
    }

    // Used by the parser and preparable statement
    public void setBoundVariables(List<ColumnIdentifier> boundNames)
    {
        this.variables = new VariableSpecifications(boundNames);
    }

    public void setBoundVariables(VariableSpecifications variables)
    {
        this.variables = variables;
    }

    public abstract Prepared prepare(ClientState clientState) throws RequestValidationException;

    public static class Prepared
    {
        /**
         * Contains the CQL statement source if the statement has been "regularly" perpared via
         * {@link org.apache.cassandra.cql3.QueryProcessor#prepare(java.lang.String, org.apache.cassandra.service.ClientState, boolean)} /
         * {@link QueryHandler#prepare(java.lang.String, org.apache.cassandra.service.QueryState, java.util.Map)}.
         * Other usages of this class may or may not contain the CQL statement source.
         */
        public String rawCQLStatement;

        public final CQLStatement statement;
        public final List<ColumnSpecification> boundNames;
        public final short[] partitionKeyBindIndexes;

        protected Prepared(CQLStatement statement, List<ColumnSpecification> boundNames, short[] partitionKeyBindIndexes)
        {
            this.statement = statement;
            this.boundNames = boundNames;
            this.partitionKeyBindIndexes = partitionKeyBindIndexes;
            this.rawCQLStatement = "";
        }

        public Prepared(CQLStatement statement, VariableSpecifications names, short[] partitionKeyBindIndexes)
        {
            this(statement, names.getSpecifications(), partitionKeyBindIndexes);
        }

        public Prepared(CQLStatement statement)
        {
            this(statement, Collections.<ColumnSpecification>emptyList(), null);
        }
    }

    public Iterable<Function> getFunctions()
    {
        return Collections.emptyList();
    }
}
