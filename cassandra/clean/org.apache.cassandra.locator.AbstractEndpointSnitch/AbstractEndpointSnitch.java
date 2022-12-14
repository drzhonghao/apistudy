import org.apache.cassandra.locator.*;


import java.net.InetAddress;
import java.util.*;

import org.apache.cassandra.config.DatabaseDescriptor;

public abstract class AbstractEndpointSnitch implements IEndpointSnitch
{
    public abstract int compareEndpoints(InetAddress target, InetAddress a1, InetAddress a2);

    /**
     * Sorts the <tt>Collection</tt> of node addresses by proximity to the given address
     * @param address the address to sort by proximity to
     * @param unsortedAddress the nodes to sort
     * @return a new sorted <tt>List</tt>
     */
    public List<InetAddress> getSortedListByProximity(InetAddress address, Collection<InetAddress> unsortedAddress)
    {
        List<InetAddress> preferred = new ArrayList<InetAddress>(unsortedAddress);
        sortByProximity(address, preferred);
        return preferred;
    }

    /**
     * Sorts the <tt>List</tt> of node addresses, in-place, by proximity to the given address
     * @param address the address to sort the proximity by
     * @param addresses the nodes to sort
     */
    public void sortByProximity(final InetAddress address, List<InetAddress> addresses)
    {
        Collections.sort(addresses, new Comparator<InetAddress>()
        {
            public int compare(InetAddress a1, InetAddress a2)
            {
                return compareEndpoints(address, a1, a2);
            }
        });
    }

    public void gossiperStarting()
    {
        // noop by default
    }

    public boolean isWorthMergingForRangeQuery(List<InetAddress> merged, List<InetAddress> l1, List<InetAddress> l2)
    {
        // Querying remote DC is likely to be an order of magnitude slower than
        // querying locally, so 2 queries to local nodes is likely to still be
        // faster than 1 query involving remote ones
        boolean mergedHasRemote = hasRemoteNode(merged);
        return mergedHasRemote
             ? hasRemoteNode(l1) || hasRemoteNode(l2)
             : true;
    }

    private boolean hasRemoteNode(List<InetAddress> l)
    {
        String localDc = DatabaseDescriptor.getLocalDataCenter();
        for (InetAddress ep : l)
        {
            if (!localDc.equals(getDatacenter(ep)))
                return true;
        }
        return false;
    }
}
