import org.apache.cassandra.cql3.CQL3Type;
import org.apache.cassandra.schema.*;


import com.google.common.collect.ImmutableSet;

import org.apache.cassandra.cql3.*;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.db.marshal.UserType;

import static org.apache.cassandra.utils.ByteBufferUtil.bytes;

public final class CQLTypeParser
{
    private static final ImmutableSet<String> PRIMITIVE_TYPES;

    static
    {
        ImmutableSet.Builder<String> builder = ImmutableSet.builder();
        for (CQL3Type.Native primitive : CQL3Type.Native.values())
            builder.add(primitive.name().toLowerCase());
        PRIMITIVE_TYPES = builder.build();
    }

    public static AbstractType<?> parse(String keyspace, String unparsed, Types userTypes)
    {
        String lowercased = unparsed.toLowerCase();

        // fast path for the common case of a primitive type
        if (PRIMITIVE_TYPES.contains(lowercased))
            return CQL3Type.Native.valueOf(unparsed.toUpperCase()).getType();

        // special-case top-level UDTs
        UserType udt = userTypes.getNullable(bytes(lowercased));
        if (udt != null)
            return udt;

        return parseRaw(unparsed).prepareInternal(keyspace, userTypes).getType();
    }

    static CQL3Type.Raw parseRaw(String type)
    {
        return CQLFragmentParser.parseAny(CqlParser::comparatorType, type, "CQL type");
    }
}
