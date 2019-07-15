import org.apache.cassandra.cql3.functions.AbstractFunction;
import org.apache.cassandra.cql3.functions.FunctionName;
import org.apache.cassandra.cql3.functions.*;


import java.util.Arrays;

import org.apache.cassandra.db.marshal.AbstractType;

/**
 * Base class for our native/hardcoded functions.
 */
public abstract class NativeFunction extends AbstractFunction
{
    protected NativeFunction(String name, AbstractType<?> returnType, AbstractType<?>... argTypes)
    {
        super(FunctionName.nativeFunction(name), Arrays.asList(argTypes), returnType);
    }

    public boolean isNative()
    {
        return true;
    }
}
