import org.apache.cassandra.cql3.*;


import java.util.List;

import com.google.common.collect.ImmutableList;

import org.apache.cassandra.cql3.restrictions.CustomIndexExpression;

public final class WhereClause
{

    private static final WhereClause EMPTY = new WhereClause(new Builder());

    public final List<Relation> relations;
    public final List<CustomIndexExpression> expressions;

    private WhereClause(Builder builder)
    {
        this(builder.relations.build(), builder.expressions.build());
    }

    private WhereClause(List<Relation> relations, List<CustomIndexExpression> expressions)
    {
        this.relations = relations;
        this.expressions = expressions;
    }

    public static WhereClause empty()
    {
        return EMPTY;
    }

    public WhereClause copy(List<Relation> newRelations)
    {
        return new WhereClause(newRelations, expressions);
    }

    public boolean containsCustomExpressions()
    {
        return !expressions.isEmpty();
    }

    public static final class Builder
    {
        ImmutableList.Builder<Relation> relations = new ImmutableList.Builder<>();
        ImmutableList.Builder<CustomIndexExpression> expressions = new ImmutableList.Builder<>();

        public Builder add(Relation relation)
        {
            relations.add(relation);
            return this;
        }

        public Builder add(CustomIndexExpression expression)
        {
            expressions.add(expression);
            return this;
        }

        public WhereClause build()
        {
            return new WhereClause(this);
        }
    }
}
