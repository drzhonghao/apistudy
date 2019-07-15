import org.apache.cassandra.auth.IResource;
import org.apache.cassandra.auth.RoleResource;
import org.apache.cassandra.auth.DataResource;
import org.apache.cassandra.auth.FunctionResource;
import org.apache.cassandra.auth.JMXResource;
import org.apache.cassandra.auth.*;


import java.util.ArrayList;
import java.util.List;

import org.apache.cassandra.utils.Hex;

public final class Resources
{
    /**
     * Construct a chain of resource parents starting with the resource and ending with the root.
     *
     * @param resource The staring point.
     * @return list of resource in the chain form start to the root.
     */
    public static List<? extends IResource> chain(IResource resource)
    {
        List<IResource> chain = new ArrayList<IResource>();
        while (true)
        {
           chain.add(resource);
           if (!resource.hasParent())
               break;
           resource = resource.getParent();
        }
        return chain;
    }

    /**
     * Creates an IResource instance from its external name.
     * Resource implementation class is inferred by matching against the known IResource
     * impls' root level resources.
     * @param name
     * @return an IResource instance created from the name
     */
    public static IResource fromName(String name)
    {
        if (name.startsWith(RoleResource.root().getName()))
            return RoleResource.fromName(name);
        else if (name.startsWith(DataResource.root().getName()))
            return DataResource.fromName(name);
        else if (name.startsWith(FunctionResource.root().getName()))
            return FunctionResource.fromName(name);
        else if (name.startsWith(JMXResource.root().getName()))
            return JMXResource.fromName(name);
        else
            throw new IllegalArgumentException(String.format("Name %s is not valid for any resource type", name));
    }

    @Deprecated
    public final static String ROOT = "cassandra";
    @Deprecated
    public final static String KEYSPACES = "keyspaces";

    @Deprecated
    public static String toString(List<Object> resource)
    {
        StringBuilder buff = new StringBuilder();
        for (Object component : resource)
        {
            buff.append("/");
            if (component instanceof byte[])
                buff.append(Hex.bytesToHex((byte[])component));
            else
                buff.append(component);
        }
        return buff.toString();
    }
}
