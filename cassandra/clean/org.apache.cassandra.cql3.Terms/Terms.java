import org.apache.cassandra.cql3.CQLFragmentParser;
import org.apache.cassandra.cql3.*;


import java.nio.ByteBuffer;
import java.util.List;

import org.apache.cassandra.cql3.functions.Function;
import org.apache.cassandra.db.marshal.AbstractType;

public class Terms
{
    public static void addFunctions(Iterable<Term> terms, List<Function> functions)
    {
        if (terms != null)
            terms.forEach(t -> t.addFunctionsTo(functions));
    }

    public static ByteBuffer asBytes(String keyspace, String term, AbstractType type)
    {
        ColumnSpecification receiver = new ColumnSpecification(keyspace, "--dummy--", new ColumnIdentifier("(dummy)", true), type);
        Term.Raw rawTerm = CQLFragmentParser.parseAny(CqlParser::term, term, "CQL term");
        return rawTerm.prepare(keyspace, receiver).bindAndGet(QueryOptions.DEFAULT);
    }
}
