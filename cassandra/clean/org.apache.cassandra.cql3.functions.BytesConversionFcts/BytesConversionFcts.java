import org.apache.cassandra.cql3.functions.NativeScalarFunction;
import org.apache.cassandra.cql3.functions.*;


import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.cassandra.cql3.CQL3Type;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.db.marshal.BytesType;
import org.apache.cassandra.db.marshal.UTF8Type;
import org.apache.cassandra.transport.ProtocolVersion;
import org.apache.cassandra.utils.ByteBufferUtil;
import org.apache.cassandra.exceptions.InvalidRequestException;
import org.apache.cassandra.serializers.MarshalException;

public abstract class BytesConversionFcts
{
    public static Collection<Function> all()
    {
        Collection<Function> functions = new ArrayList<>();

        // because text and varchar ends up being synonymous, our automatic makeToBlobFunction doesn't work
        // for varchar, so we special case it below. We also skip blob for obvious reasons.
        for (CQL3Type type : CQL3Type.Native.values())
        {
            if (type != CQL3Type.Native.VARCHAR && type != CQL3Type.Native.BLOB)
            {
                functions.add(makeToBlobFunction(type.getType()));
                functions.add(makeFromBlobFunction(type.getType()));
            }
        }

        functions.add(VarcharAsBlobFct);
        functions.add(BlobAsVarcharFct);

        return functions;
    }

    // Most of the XAsBlob and blobAsX functions are basically no-op since everything is
    // bytes internally. They only "trick" the type system.
    public static Function makeToBlobFunction(AbstractType<?> fromType)
    {
        String name = fromType.asCQL3Type() + "asblob";
        return new NativeScalarFunction(name, BytesType.instance, fromType)
        {
            public ByteBuffer execute(ProtocolVersion protocolVersion, List<ByteBuffer> parameters)
            {
                return parameters.get(0);
            }
        };
    }

    public static Function makeFromBlobFunction(final AbstractType<?> toType)
    {
        final String name = "blobas" + toType.asCQL3Type();
        return new NativeScalarFunction(name, toType, BytesType.instance)
        {
            public ByteBuffer execute(ProtocolVersion protocolVersion, List<ByteBuffer> parameters) throws InvalidRequestException
            {
                ByteBuffer val = parameters.get(0);
                try
                {
                    if (val != null)
                        toType.validate(val);
                    return val;
                }
                catch (MarshalException e)
                {
                    throw new InvalidRequestException(String.format("In call to function %s, value 0x%s is not a valid binary representation for type %s",
                                                                    name, ByteBufferUtil.bytesToHex(val), toType.asCQL3Type()));
                }
            }
        };
    }

    public static final Function VarcharAsBlobFct = new NativeScalarFunction("varcharasblob", BytesType.instance, UTF8Type.instance)
    {
        public ByteBuffer execute(ProtocolVersion protocolVersion, List<ByteBuffer> parameters)
        {
            return parameters.get(0);
        }
    };

    public static final Function BlobAsVarcharFct = new NativeScalarFunction("blobasvarchar", UTF8Type.instance, BytesType.instance)
    {
        public ByteBuffer execute(ProtocolVersion protocolVersion, List<ByteBuffer> parameters)
        {
            return parameters.get(0);
        }
    };
}
