import org.apache.cassandra.gms.EndpointState;
import org.apache.cassandra.gms.Gossiper;
import org.apache.cassandra.locator.*;


import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.cassandra.gms.*;
import org.apache.cassandra.net.MessagingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sidekick helper for snitches that want to reconnect from one IP addr for a node to another.
 * Typically, this is for situations like EC2 where a node will have a public address and a private address,
 * where we connect on the public, discover the private, and reconnect on the private.
 */
public class ReconnectableSnitchHelper implements IEndpointStateChangeSubscriber
{
    private static final Logger logger = LoggerFactory.getLogger(ReconnectableSnitchHelper.class);
    private final IEndpointSnitch snitch;
    private final String localDc;
    private final boolean preferLocal;

    public ReconnectableSnitchHelper(IEndpointSnitch snitch, String localDc, boolean preferLocal)
    {
        this.snitch = snitch;
        this.localDc = localDc;
        this.preferLocal = preferLocal;
    }

    private void reconnect(InetAddress publicAddress, VersionedValue localAddressValue)
    {
        try
        {
            reconnect(publicAddress, InetAddress.getByName(localAddressValue.value));
        }
        catch (UnknownHostException e)
        {
            logger.error("Error in getting the IP address resolved: ", e);
        }
    }

    private void reconnect(InetAddress publicAddress, InetAddress localAddress)
    {
        if (snitch.getDatacenter(publicAddress).equals(localDc)
                && !MessagingService.instance().getConnectionPool(publicAddress).endPoint().equals(localAddress))
        {
            MessagingService.instance().getConnectionPool(publicAddress).reset(localAddress);
            logger.debug("Initiated reconnect to an Internal IP {} for the {}", localAddress, publicAddress);
        }
    }

    public void beforeChange(InetAddress endpoint, EndpointState currentState, ApplicationState newStateKey, VersionedValue newValue)
    {
        // no-op
    }

    public void onJoin(InetAddress endpoint, EndpointState epState)
    {
        if (preferLocal && !Gossiper.instance.isDeadState(epState) && epState.getApplicationState(ApplicationState.INTERNAL_IP) != null)
            reconnect(endpoint, epState.getApplicationState(ApplicationState.INTERNAL_IP));
    }

    public void onChange(InetAddress endpoint, ApplicationState state, VersionedValue value)
    {
        if (preferLocal && state == ApplicationState.INTERNAL_IP && !Gossiper.instance.isDeadState(Gossiper.instance.getEndpointStateForEndpoint(endpoint)))
            reconnect(endpoint, value);
    }

    public void onAlive(InetAddress endpoint, EndpointState state)
    {
        if (preferLocal && state.getApplicationState(ApplicationState.INTERNAL_IP) != null)
            reconnect(endpoint, state.getApplicationState(ApplicationState.INTERNAL_IP));
    }

    public void onDead(InetAddress endpoint, EndpointState state)
    {
        // do nothing.
    }

    public void onRemove(InetAddress endpoint)
    {
        // do nothing.
    }

    public void onRestart(InetAddress endpoint, EndpointState state)
    {
        // do nothing.
    }
}
