import org.apache.cassandra.cql3.Term;
import org.apache.cassandra.cql3.CQL3Type;
import org.apache.cassandra.cql3.*;


import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.exceptions.InvalidRequestException;

public class TypeCast extends Term.Raw
{
    private final CQL3Type.Raw type;
    private final Term.Raw term;

    public TypeCast(CQL3Type.Raw type, Term.Raw term)
    {
        this.type = type;
        this.term = term;
    }

    public Term prepare(String keyspace, ColumnSpecification receiver) throws InvalidRequestException
    {
        if (!term.testAssignment(keyspace, castedSpecOf(keyspace, receiver)).isAssignable())
            throw new InvalidRequestException(String.format("Cannot cast value %s to type %s", term, type));

        if (!testAssignment(keyspace, receiver).isAssignable())
            throw new InvalidRequestException(String.format("Cannot assign value %s to %s of type %s", this, receiver.name, receiver.type.asCQL3Type()));

        return term.prepare(keyspace, receiver);
    }

    private ColumnSpecification castedSpecOf(String keyspace, ColumnSpecification receiver) throws InvalidRequestException
    {
        return new ColumnSpecification(receiver.ksName, receiver.cfName, new ColumnIdentifier(toString(), true), type.prepare(keyspace).getType());
    }

    public AssignmentTestable.TestResult testAssignment(String keyspace, ColumnSpecification receiver)
    {
        AbstractType<?> castedType = type.prepare(keyspace).getType();
        if (receiver.type.equals(castedType))
            return AssignmentTestable.TestResult.EXACT_MATCH;
        else if (receiver.type.isValueCompatibleWith(castedType))
            return AssignmentTestable.TestResult.WEAKLY_ASSIGNABLE;
        else
            return AssignmentTestable.TestResult.NOT_ASSIGNABLE;
    }

    public AbstractType<?> getExactTypeIfKnown(String keyspace)
    {
        return type.prepare(keyspace).getType();
    }

    public String getText()
    {
        return "(" + type + ")" + term;
    }
}
