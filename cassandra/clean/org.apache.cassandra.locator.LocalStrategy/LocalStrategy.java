import org.apache.cassandra.locator.*;


import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.cassandra.exceptions.ConfigurationException;
import org.apache.cassandra.dht.RingPosition;
import org.apache.cassandra.dht.Token;
import org.apache.cassandra.utils.FBUtilities;

public class LocalStrategy extends AbstractReplicationStrategy
{
    public LocalStrategy(String keyspaceName, TokenMetadata tokenMetadata, IEndpointSnitch snitch, Map<String, String> configOptions)
    {
        super(keyspaceName, tokenMetadata, snitch, configOptions);
    }

    /**
     * We need to override this even if we override calculateNaturalEndpoints,
     * because the default implementation depends on token calculations but
     * LocalStrategy may be used before tokens are set up.
     */
    @Override
    public ArrayList<InetAddress> getNaturalEndpoints(RingPosition searchPosition)
    {
        ArrayList<InetAddress> l = new ArrayList<InetAddress>(1);
        l.add(FBUtilities.getBroadcastAddress());
        return l;
    }

    public List<InetAddress> calculateNaturalEndpoints(Token token, TokenMetadata metadata)
    {
        return Collections.singletonList(FBUtilities.getBroadcastAddress());
    }

    public int getReplicationFactor()
    {
        return 1;
    }

    public void validateOptions() throws ConfigurationException
    {
    }

    public Collection<String> recognizedOptions()
    {
        // LocalStrategy doesn't expect any options.
        return Collections.<String>emptySet();
    }
}
