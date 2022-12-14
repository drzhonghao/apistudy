import org.apache.cassandra.cql3.*;


import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.config.ColumnDefinition;
import org.apache.cassandra.cql3.Term.MultiColumnRaw;
import org.apache.cassandra.cql3.Term.Raw;
import org.apache.cassandra.cql3.restrictions.MultiColumnRestriction;
import org.apache.cassandra.cql3.restrictions.Restriction;
import org.apache.cassandra.cql3.restrictions.SingleColumnRestriction;
import org.apache.cassandra.cql3.statements.Bound;
import org.apache.cassandra.exceptions.InvalidRequestException;

import static org.apache.cassandra.cql3.statements.RequestValidations.checkFalse;
import static org.apache.cassandra.cql3.statements.RequestValidations.checkTrue;
import static org.apache.cassandra.cql3.statements.RequestValidations.invalidRequest;

/**
 * A relation using the tuple notation, which typically affects multiple columns.
 * Examples:
 * {@code
 *  - SELECT ... WHERE (a, b, c) > (1, 'a', 10)
 *  - SELECT ... WHERE (a, b, c) IN ((1, 2, 3), (4, 5, 6))
 *  - SELECT ... WHERE (a, b) < ?
 *  - SELECT ... WHERE (a, b) IN ?
 * }
 */
public class MultiColumnRelation extends Relation
{
    private final List<ColumnDefinition.Raw> entities;

    /** A Tuples.Literal or Tuples.Raw marker */
    private final Term.MultiColumnRaw valuesOrMarker;

    /** A list of Tuples.Literal or Tuples.Raw markers */
    private final List<? extends Term.MultiColumnRaw> inValues;

    private final Tuples.INRaw inMarker;

    private MultiColumnRelation(List<ColumnDefinition.Raw> entities, Operator relationType, Term.MultiColumnRaw valuesOrMarker, List<? extends Term.MultiColumnRaw> inValues, Tuples.INRaw inMarker)
    {
        this.entities = entities;
        this.relationType = relationType;
        this.valuesOrMarker = valuesOrMarker;

        this.inValues = inValues;
        this.inMarker = inMarker;
    }

    /**
     * Creates a multi-column EQ, LT, LTE, GT, or GTE relation.
     * {@code
     * For example: "SELECT ... WHERE (a, b) > (0, 1)"
     * }
     * @param entities the columns on the LHS of the relation
     * @param relationType the relation operator
     * @param valuesOrMarker a Tuples.Literal instance or a Tuples.Raw marker
     * @return a new <code>MultiColumnRelation</code> instance
     */
    public static MultiColumnRelation createNonInRelation(List<ColumnDefinition.Raw> entities, Operator relationType, Term.MultiColumnRaw valuesOrMarker)
    {
        assert relationType != Operator.IN;
        return new MultiColumnRelation(entities, relationType, valuesOrMarker, null, null);
    }

    /**
     * Creates a multi-column IN relation with a list of IN values or markers.
     * For example: "SELECT ... WHERE (a, b) IN ((0, 1), (2, 3))"
     * @param entities the columns on the LHS of the relation
     * @param inValues a list of Tuples.Literal instances or a Tuples.Raw markers
     * @return a new <code>MultiColumnRelation</code> instance
     */
    public static MultiColumnRelation createInRelation(List<ColumnDefinition.Raw> entities, List<? extends Term.MultiColumnRaw> inValues)
    {
        return new MultiColumnRelation(entities, Operator.IN, null, inValues, null);
    }

    /**
     * Creates a multi-column IN relation with a marker for the IN values.
     * For example: "SELECT ... WHERE (a, b) IN ?"
     * @param entities the columns on the LHS of the relation
     * @param inMarker a single IN marker
     * @return a new <code>MultiColumnRelation</code> instance
     */
    public static MultiColumnRelation createSingleMarkerInRelation(List<ColumnDefinition.Raw> entities, Tuples.INRaw inMarker)
    {
        return new MultiColumnRelation(entities, Operator.IN, null, null, inMarker);
    }

    public List<ColumnDefinition.Raw> getEntities()
    {
        return entities;
    }

    /**
     * For non-IN relations, returns the Tuples.Literal or Tuples.Raw marker for a single tuple.
     * @return a Tuples.Literal for non-IN relations or Tuples.Raw marker for a single tuple.
     */
    public Term.MultiColumnRaw getValue()
    {
        return relationType == Operator.IN ? inMarker : valuesOrMarker;
    }

    public List<? extends Term.Raw> getInValues()
    {
        assert relationType == Operator.IN;
        return inValues;
    }

    @Override
    public boolean isMultiColumn()
    {
        return true;
    }

    @Override
    protected Restriction newEQRestriction(CFMetaData cfm,
                                           VariableSpecifications boundNames) throws InvalidRequestException
    {
        List<ColumnDefinition> receivers = receivers(cfm);
        Term term = toTerm(receivers, getValue(), cfm.ksName, boundNames);
        return new MultiColumnRestriction.EQRestriction(receivers, term);
    }

    @Override
    protected Restriction newINRestriction(CFMetaData cfm,
                                           VariableSpecifications boundNames) throws InvalidRequestException
    {
        List<ColumnDefinition> receivers = receivers(cfm);
        List<Term> terms = toTerms(receivers, inValues, cfm.ksName, boundNames);
        if (terms == null)
        {
            Term term = toTerm(receivers, getValue(), cfm.ksName, boundNames);
            return new MultiColumnRestriction.InRestrictionWithMarker(receivers, (AbstractMarker) term);
        }

        if (terms.size() == 1)
            return new MultiColumnRestriction.EQRestriction(receivers, terms.get(0));

        return new MultiColumnRestriction.InRestrictionWithValues(receivers, terms);
    }

    @Override
    protected Restriction newSliceRestriction(CFMetaData cfm,
                                              VariableSpecifications boundNames,
                                              Bound bound,
                                              boolean inclusive) throws InvalidRequestException
    {
        List<ColumnDefinition> receivers = receivers(cfm);
        Term term = toTerm(receivers, getValue(), cfm.ksName, boundNames);
        return new MultiColumnRestriction.SliceRestriction(receivers, bound, inclusive, term);
    }

    @Override
    protected Restriction newContainsRestriction(CFMetaData cfm,
                                                 VariableSpecifications boundNames,
                                                 boolean isKey) throws InvalidRequestException
    {
        throw invalidRequest("%s cannot be used for multi-column relations", operator());
    }

    @Override
    protected Restriction newIsNotRestriction(CFMetaData cfm,
                                              VariableSpecifications boundNames) throws InvalidRequestException
    {
        // this is currently disallowed by the grammar
        throw new AssertionError(String.format("%s cannot be used for multi-column relations", operator()));
    }

    @Override
    protected Restriction newLikeRestriction(CFMetaData cfm, VariableSpecifications boundNames, Operator operator) throws InvalidRequestException
    {
        throw invalidRequest("%s cannot be used for multi-column relations", operator());
    }

    @Override
    protected Term toTerm(List<? extends ColumnSpecification> receivers,
                          Raw raw,
                          String keyspace,
                          VariableSpecifications boundNames) throws InvalidRequestException
    {
        Term term = ((MultiColumnRaw) raw).prepare(keyspace, receivers);
        term.collectMarkerSpecification(boundNames);
        return term;
    }

    protected List<ColumnDefinition> receivers(CFMetaData cfm) throws InvalidRequestException
    {
        List<ColumnDefinition> names = new ArrayList<>(getEntities().size());
        int previousPosition = -1;
        for (ColumnDefinition.Raw raw : getEntities())
        {
            ColumnDefinition def = raw.prepare(cfm);
            checkTrue(def.isClusteringColumn(), "Multi-column relations can only be applied to clustering columns but was applied to: %s", def.name);
            checkFalse(names.contains(def), "Column \"%s\" appeared twice in a relation: %s", def.name, this);

            // check that no clustering columns were skipped
            checkFalse(previousPosition != -1 && def.position() != previousPosition + 1,
                       "Clustering columns must appear in the PRIMARY KEY order in multi-column relations: %s", this);

            names.add(def);
            previousPosition = def.position();
        }
        return names;
    }

    public Relation renameIdentifier(ColumnDefinition.Raw from, ColumnDefinition.Raw to)
    {
        if (!entities.contains(from))
            return this;

        List<ColumnDefinition.Raw> newEntities = entities.stream().map(e -> e.equals(from) ? to : e).collect(Collectors.toList());
        return new MultiColumnRelation(newEntities, operator(), valuesOrMarker, inValues, inMarker);
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder(Tuples.tupleToString(entities));
        if (isIN())
        {
            return builder.append(" IN ")
                          .append(inMarker != null ? '?' : Tuples.tupleToString(inValues))
                          .toString();
        }
        return builder.append(" ")
                      .append(relationType)
                      .append(" ")
                      .append(valuesOrMarker)
                      .toString();
    }

    @Override
    public Relation toSuperColumnAdapter()
    {
        return new SuperColumnMultiColumnRelation(entities, relationType, valuesOrMarker, inValues, inMarker);
    }

    /**
     * Required for SuperColumn compatibility, in order to map the SuperColumn key restrictions from the regular
     * column to the collection key one.
     */
    private class SuperColumnMultiColumnRelation extends MultiColumnRelation
    {
        private SuperColumnMultiColumnRelation(List<ColumnDefinition.Raw> entities, Operator relationType, MultiColumnRaw valuesOrMarker, List<? extends MultiColumnRaw> inValues, Tuples.INRaw inMarker)
        {
            super(entities, relationType, valuesOrMarker, inValues, inMarker);
        }

        @Override
        protected Restriction newSliceRestriction(CFMetaData cfm,
                                                  VariableSpecifications boundNames,
                                                  Bound bound,
                                                  boolean inclusive) throws InvalidRequestException
        {
            assert cfm.isSuper() && cfm.isDense();
            List<ColumnDefinition> receivers = receivers(cfm);
            Term term = toTerm(receivers, getValue(), cfm.ksName, boundNames);
            return new SingleColumnRestriction.SuperColumnMultiSliceRestriction(receivers.get(0), bound, inclusive, term);
        }

        @Override
        protected Restriction newEQRestriction(CFMetaData cfm,
                                               VariableSpecifications boundNames) throws InvalidRequestException
        {
            assert cfm.isSuper() && cfm.isDense();
            List<ColumnDefinition> receivers = receivers(cfm);
            Term term = toTerm(receivers, getValue(), cfm.ksName, boundNames);
            return new SingleColumnRestriction.SuperColumnMultiEQRestriction(receivers.get(0), term);
        }

        @Override
        protected List<ColumnDefinition> receivers(CFMetaData cfm) throws InvalidRequestException
        {
            assert cfm.isSuper() && cfm.isDense();
            List<ColumnDefinition> names = new ArrayList<>(getEntities().size());

            for (ColumnDefinition.Raw raw : getEntities())
            {
                ColumnDefinition def = raw.prepare(cfm);

                checkTrue(def.isClusteringColumn() ||
                          cfm.isSuperColumnKeyColumn(def),
                          "Multi-column relations can only be applied to clustering columns but was applied to: %s", def.name);

                checkFalse(names.contains(def), "Column \"%s\" appeared twice in a relation: %s", def.name, this);

                names.add(def);
            }
            return names;
        }
    }
}
