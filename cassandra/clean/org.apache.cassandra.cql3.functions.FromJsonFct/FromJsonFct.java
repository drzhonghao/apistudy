import org.apache.cassandra.cql3.functions.NativeScalarFunction;
import org.apache.cassandra.cql3.functions.FunctionName;
import org.apache.cassandra.cql3.functions.*;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.cassandra.cql3.Json;

import org.apache.cassandra.cql3.QueryOptions;
import org.apache.cassandra.db.marshal.*;
import org.apache.cassandra.exceptions.FunctionExecutionException;
import org.apache.cassandra.serializers.MarshalException;
import org.apache.cassandra.transport.ProtocolVersion;

public class FromJsonFct extends NativeScalarFunction
{
    public static final FunctionName NAME = FunctionName.nativeFunction("fromjson");

    private static final Map<AbstractType<?>, FromJsonFct> instances = new ConcurrentHashMap<>();

    public static FromJsonFct getInstance(AbstractType<?> returnType)
    {
        FromJsonFct func = instances.get(returnType);
        if (func == null)
        {
            func = new FromJsonFct(returnType);
            instances.put(returnType, func);
        }
        return func;
    }

    private FromJsonFct(AbstractType<?> returnType)
    {
        super("fromjson", returnType, UTF8Type.instance);
    }

    public ByteBuffer execute(ProtocolVersion protocolVersion, List<ByteBuffer> parameters)
    {
        assert parameters.size() == 1 : "Unexpectedly got " + parameters.size() + " arguments for fromJson()";
        ByteBuffer argument = parameters.get(0);
        if (argument == null)
            return null;

        String jsonArg = UTF8Type.instance.getSerializer().deserialize(argument);
        try
        {
            Object object = Json.JSON_OBJECT_MAPPER.readValue(jsonArg, Object.class);
            if (object == null)
                return null;
            return returnType.fromJSONObject(object).bindAndGet(QueryOptions.forProtocolVersion(protocolVersion));
        }
        catch (IOException exc)
        {
            throw new FunctionExecutionException(NAME, Collections.singletonList("text"), String.format("Could not decode JSON string '%s': %s", jsonArg, exc.toString()));
        }
        catch (MarshalException exc)
        {
            throw FunctionExecutionException.create(this, exc);
        }
    }
}
