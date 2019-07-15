import org.apache.cassandra.cql3.functions.NativeScalarFunction;
import org.apache.cassandra.cql3.functions.FunctionName;
import org.apache.cassandra.cql3.functions.*;


import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.db.marshal.UTF8Type;
import org.apache.cassandra.exceptions.InvalidRequestException;
import org.apache.cassandra.transport.ProtocolVersion;
import org.apache.cassandra.utils.ByteBufferUtil;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ToJsonFct extends NativeScalarFunction
{
    public static final FunctionName NAME = FunctionName.nativeFunction("tojson");

    private static final Map<AbstractType<?>, ToJsonFct> instances = new ConcurrentHashMap<>();

    public static ToJsonFct getInstance(List<AbstractType<?>> argTypes) throws InvalidRequestException
    {
        if (argTypes.size() != 1)
            throw new InvalidRequestException(String.format("toJson() only accepts one argument (got %d)", argTypes.size()));

        AbstractType<?> fromType = argTypes.get(0);
        ToJsonFct func = instances.get(fromType);
        if (func == null)
        {
            func = new ToJsonFct(fromType);
            instances.put(fromType, func);
        }
        return func;
    }

    private ToJsonFct(AbstractType<?> argType)
    {
        super("tojson", UTF8Type.instance, argType);
    }

    public ByteBuffer execute(ProtocolVersion protocolVersion, List<ByteBuffer> parameters) throws InvalidRequestException
    {
        assert parameters.size() == 1 : "Expected 1 argument for toJson(), but got " + parameters.size();
        ByteBuffer parameter = parameters.get(0);
        if (parameter == null)
            return ByteBufferUtil.bytes("null");

        return ByteBufferUtil.bytes(argTypes.get(0).toJSONString(parameter, protocolVersion));
    }
}
