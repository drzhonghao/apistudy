import org.apache.cassandra.cql3.functions.*;


import com.google.common.base.Objects;

import org.apache.cassandra.config.SchemaConstants;

public final class FunctionName
{
    public final String keyspace;
    public final String name;

    public static FunctionName nativeFunction(String name)
    {
        return new FunctionName(SchemaConstants.SYSTEM_KEYSPACE_NAME, name);
    }

    public FunctionName(String keyspace, String name)
    {
        assert name != null : "Name parameter must not be null";
        this.keyspace = keyspace;
        this.name = name;
    }

    public FunctionName asNativeFunction()
    {
        return FunctionName.nativeFunction(name);
    }

    public boolean hasKeyspace()
    {
        return keyspace != null;
    }

    @Override
    public final int hashCode()
    {
        return Objects.hashCode(keyspace, name);
    }

    @Override
    public final boolean equals(Object o)
    {
        if (!(o instanceof FunctionName))
            return false;

        FunctionName that = (FunctionName)o;
        return Objects.equal(this.keyspace, that.keyspace)
            && Objects.equal(this.name, that.name);
    }

    public final boolean equalsNativeFunction(FunctionName nativeFunction)
    {
        assert nativeFunction.keyspace.equals(SchemaConstants.SYSTEM_KEYSPACE_NAME);
        if (this.hasKeyspace() && !this.keyspace.equals(SchemaConstants.SYSTEM_KEYSPACE_NAME))
            return false;

        return Objects.equal(this.name, nativeFunction.name);
    }

    @Override
    public String toString()
    {
        return keyspace == null ? name : keyspace + "." + name;
    }
}
