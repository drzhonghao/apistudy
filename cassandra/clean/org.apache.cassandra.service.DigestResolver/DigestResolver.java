import org.apache.cassandra.service.*;


import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import org.apache.cassandra.db.*;
import org.apache.cassandra.db.partitions.PartitionIterator;
import org.apache.cassandra.db.partitions.UnfilteredPartitionIterators;
import org.apache.cassandra.net.MessageIn;

public class DigestResolver extends ResponseResolver
{
    private volatile ReadResponse dataResponse;

    public DigestResolver(Keyspace keyspace, ReadCommand command, ConsistencyLevel consistency, int maxResponseCount)
    {
        super(keyspace, command, consistency, maxResponseCount);
    }

    @Override
    public void preprocess(MessageIn<ReadResponse> message)
    {
        super.preprocess(message);
        if (dataResponse == null && !message.payload.isDigestResponse())
            dataResponse = message.payload;
    }

    /**
     * Special case of resolve() so that CL.ONE reads never throw DigestMismatchException in the foreground
     */
    public PartitionIterator getData()
    {
        assert isDataPresent();
        return UnfilteredPartitionIterators.filter(dataResponse.makeIterator(command), command.nowInSec());
    }

    /*
     * This method handles two different scenarios:
     *
     * a) we're handling the initial read of data from the closest replica + digests
     *    from the rest. In this case we check the digests against each other,
     *    throw an exception if there is a mismatch, otherwise return the data row.
     *
     * b) we're checking additional digests that arrived after the minimum to handle
     *    the requested ConsistencyLevel, i.e. asynchronous read repair check
     */
    public PartitionIterator resolve() throws DigestMismatchException
    {
        if (responses.size() == 1)
            return getData();

        if (logger.isTraceEnabled())
            logger.trace("resolving {} responses", responses.size());

        compareResponses();

        return UnfilteredPartitionIterators.filter(dataResponse.makeIterator(command), command.nowInSec());
    }

    public void compareResponses() throws DigestMismatchException
    {
        long start = System.nanoTime();

        // validate digests against each other; throw immediately on mismatch.
        ByteBuffer digest = null;
        for (MessageIn<ReadResponse> message : responses)
        {
            ReadResponse response = message.payload;

            ByteBuffer newDigest = response.digest(command);
            if (digest == null)
                digest = newDigest;
            else if (!digest.equals(newDigest))
                // rely on the fact that only single partition queries use digests
                throw new DigestMismatchException(((SinglePartitionReadCommand)command).partitionKey(), digest, newDigest);
        }

        if (logger.isTraceEnabled())
            logger.trace("resolve: {} ms.", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
    }

    public boolean isDataPresent()
    {
        return dataResponse != null;
    }
}
