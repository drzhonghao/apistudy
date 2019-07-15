import org.apache.cassandra.locator.AbstractReplicationStrategy;
import org.apache.cassandra.locator.*;


import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.cassandra.exceptions.ConfigurationException;
import org.apache.cassandra.dht.Token;


/**
 * This class returns the nodes responsible for a given
 * key but does not respect rack awareness. Basically
 * returns the RF nodes that lie right next to each other
 * on the ring.
 */
public class SimpleStrategy extends AbstractReplicationStrategy
{
    public SimpleStrategy(String keyspaceName, TokenMetadata tokenMetadata, IEndpointSnitch snitch, Map<String, String> configOptions)
    {
        super(keyspaceName, tokenMetadata, snitch, configOptions);
    }

    public List<InetAddress> calculateNaturalEndpoints(Token token, TokenMetadata metadata)
    {
        int replicas = getReplicationFactor();
        ArrayList<Token> tokens = metadata.sortedTokens();
        List<InetAddress> endpoints = new ArrayList<InetAddress>(replicas);

        if (tokens.isEmpty())
            return endpoints;

        // Add the token at the index by default
        Iterator<Token> iter = TokenMetadata.ringIterator(tokens, token, false);
        while (endpoints.size() < replicas && iter.hasNext())
        {
            InetAddress ep = metadata.getEndpoint(iter.next());
            if (!endpoints.contains(ep))
                endpoints.add(ep);
        }
        return endpoints;
    }

    public int getReplicationFactor()
    {
        return Integer.parseInt(this.configOptions.get("replication_factor"));
    }

    public void validateOptions() throws ConfigurationException
    {
        String rf = configOptions.get("replication_factor");
        if (rf == null)
            throw new ConfigurationException("SimpleStrategy requires a replication_factor strategy option.");
        validateReplicationFactor(rf);
    }

    public Collection<String> recognizedOptions()
    {
        return Collections.<String>singleton("replication_factor");
    }
}
