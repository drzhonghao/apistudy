import org.apache.cassandra.cql3.*;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.base.Joiner;

import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.config.ColumnDefinition;
import org.apache.cassandra.cql3.Term.Raw;
import org.apache.cassandra.cql3.restrictions.Restriction;
import org.apache.cassandra.cql3.restrictions.TokenRestriction;
import org.apache.cassandra.cql3.statements.Bound;
import org.apache.cassandra.exceptions.InvalidRequestException;

import static org.apache.cassandra.cql3.statements.RequestValidations.checkContainsNoDuplicates;
import static org.apache.cassandra.cql3.statements.RequestValidations.checkContainsOnly;
import static org.apache.cassandra.cql3.statements.RequestValidations.checkTrue;
import static org.apache.cassandra.cql3.statements.RequestValidations.invalidRequest;

/**
 * A relation using the token function.
 * Examples:
 * <ul>
 * <li>SELECT ... WHERE token(a) &gt; token(1)</li>
 * <li>SELECT ... WHERE token(a, b) &gt; token(1, 3)</li>
 * </ul>
 */
public final class TokenRelation extends Relation
{
    private final List<ColumnDefinition.Raw> entities;

    private final Term.Raw value;

    public TokenRelation(List<ColumnDefinition.Raw> entities, Operator type, Term.Raw value)
    {
        this.entities = entities;
        this.relationType = type;
        this.value = value;
    }

    @Override
    public boolean onToken()
    {
        return true;
    }

    public Term.Raw getValue()
    {
        return value;
    }

    public List<? extends Term.Raw> getInValues()
    {
        return null;
    }

    @Override
    protected Restriction newEQRestriction(CFMetaData cfm, VariableSpecifications boundNames) throws InvalidRequestException
    {
        List<ColumnDefinition> columnDefs = getColumnDefinitions(cfm);
        Term term = toTerm(toReceivers(cfm, columnDefs), value, cfm.ksName, boundNames);
        return new TokenRestriction.EQRestriction(cfm, columnDefs, term);
    }

    @Override
    protected Restriction newINRestriction(CFMetaData cfm, VariableSpecifications boundNames) throws InvalidRequestException
    {
        throw invalidRequest("%s cannot be used with the token function", operator());
    }

    @Override
    protected Restriction newSliceRestriction(CFMetaData cfm,
                                              VariableSpecifications boundNames,
                                              Bound bound,
                                              boolean inclusive) throws InvalidRequestException
    {
        List<ColumnDefinition> columnDefs = getColumnDefinitions(cfm);
        Term term = toTerm(toReceivers(cfm, columnDefs), value, cfm.ksName, boundNames);
        return new TokenRestriction.SliceRestriction(cfm, columnDefs, bound, inclusive, term);
    }

    @Override
    protected Restriction newContainsRestriction(CFMetaData cfm, VariableSpecifications boundNames, boolean isKey) throws InvalidRequestException
    {
        throw invalidRequest("%s cannot be used with the token function", operator());
    }

    @Override
    protected Restriction newIsNotRestriction(CFMetaData cfm, VariableSpecifications boundNames) throws InvalidRequestException
    {
        throw invalidRequest("%s cannot be used with the token function", operator());
    }

    @Override
    protected Restriction newLikeRestriction(CFMetaData cfm, VariableSpecifications boundNames, Operator operator) throws InvalidRequestException
    {
        throw invalidRequest("%s cannot be used with the token function", operator);
    }

    @Override
    protected Term toTerm(List<? extends ColumnSpecification> receivers,
                          Raw raw,
                          String keyspace,
                          VariableSpecifications boundNames) throws InvalidRequestException
    {
        Term term = raw.prepare(keyspace, receivers.get(0));
        term.collectMarkerSpecification(boundNames);
        return term;
    }

    public Relation renameIdentifier(ColumnDefinition.Raw from, ColumnDefinition.Raw to)
    {
        if (!entities.contains(from))
            return this;

        List<ColumnDefinition.Raw> newEntities = entities.stream().map(e -> e.equals(from) ? to : e).collect(Collectors.toList());
        return new TokenRelation(newEntities, operator(), value);
    }

    @Override
    public String toString()
    {
        return String.format("token%s %s %s", Tuples.tupleToString(entities), relationType, value);
    }

    /**
     * Returns the definition of the columns to which apply the token restriction.
     *
     * @param cfm the column family metadata
     * @return the definition of the columns to which apply the token restriction.
     * @throws InvalidRequestException if the entity cannot be resolved
     */
    private List<ColumnDefinition> getColumnDefinitions(CFMetaData cfm) throws InvalidRequestException
    {
        List<ColumnDefinition> columnDefs = new ArrayList<>(entities.size());
        for ( ColumnDefinition.Raw raw : entities)
            columnDefs.add(raw.prepare(cfm));
        return columnDefs;
    }

    /**
     * Returns the receivers for this relation.
     *
     * @param cfm the Column Family meta data
     * @param columnDefs the column definitions
     * @return the receivers for the specified relation.
     * @throws InvalidRequestException if the relation is invalid
     */
    private static List<? extends ColumnSpecification> toReceivers(CFMetaData cfm,
                                                                   List<ColumnDefinition> columnDefs)
                                                                   throws InvalidRequestException
    {

        if (!columnDefs.equals(cfm.partitionKeyColumns()))
        {
            checkTrue(columnDefs.containsAll(cfm.partitionKeyColumns()),
                      "The token() function must be applied to all partition key components or none of them");

            checkContainsNoDuplicates(columnDefs, "The token() function contains duplicate partition key components");

            checkContainsOnly(columnDefs, cfm.partitionKeyColumns(), "The token() function must contains only partition key components");

            throw invalidRequest("The token function arguments must be in the partition key order: %s",
                                 Joiner.on(", ").join(ColumnDefinition.toIdentifiers(cfm.partitionKeyColumns())));
        }

        ColumnDefinition firstColumn = columnDefs.get(0);
        return Collections.singletonList(new ColumnSpecification(firstColumn.ksName,
                                                                 firstColumn.cfName,
                                                                 new ColumnIdentifier("partition key token", true),
                                                                 cfm.partitioner.getTokenValidator()));
    }
}
