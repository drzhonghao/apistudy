import org.apache.cassandra.repair.messages.RepairMessage;
import org.apache.cassandra.repair.messages.*;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.apache.cassandra.dht.Range;
import org.apache.cassandra.dht.Token;
import org.apache.cassandra.io.util.DataInputPlus;
import org.apache.cassandra.io.util.DataOutputPlus;
import org.apache.cassandra.net.MessagingService;
import org.apache.cassandra.utils.UUIDSerializer;

public class AnticompactionRequest extends RepairMessage
{
    public static MessageSerializer serializer = new AnticompactionRequestSerializer();
    public final UUID parentRepairSession;
    /**
     * Successfully repaired ranges. Does not contain null.
     */
    public final Collection<Range<Token>> successfulRanges;

    public AnticompactionRequest(UUID parentRepairSession, Collection<Range<Token>> ranges)
    {
        super(Type.ANTICOMPACTION_REQUEST, null);
        this.parentRepairSession = parentRepairSession;
        this.successfulRanges = ranges;
    }

    @Override
    public boolean equals(Object o)
    {
        if (!(o instanceof AnticompactionRequest))
            return false;
        AnticompactionRequest other = (AnticompactionRequest)o;
        return messageType == other.messageType &&
               parentRepairSession.equals(other.parentRepairSession) &&
               successfulRanges.equals(other.successfulRanges);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(messageType, parentRepairSession, successfulRanges);
    }

    public static class AnticompactionRequestSerializer implements MessageSerializer<AnticompactionRequest>
    {
        public void serialize(AnticompactionRequest message, DataOutputPlus out, int version) throws IOException
        {
            UUIDSerializer.serializer.serialize(message.parentRepairSession, out, version);
            out.writeInt(message.successfulRanges.size());
            for (Range<Token> r : message.successfulRanges)
            {
                MessagingService.validatePartitioner(r);
                Range.tokenSerializer.serialize(r, out, version);
            }
        }

        public AnticompactionRequest deserialize(DataInputPlus in, int version) throws IOException
        {
            UUID parentRepairSession = UUIDSerializer.serializer.deserialize(in, version);
            int rangeCount = in.readInt();
            List<Range<Token>> ranges = new ArrayList<>(rangeCount);
            for (int i = 0; i < rangeCount; i++)
                ranges.add((Range<Token>) Range.tokenSerializer.deserialize(in, MessagingService.globalPartitioner(), version));
            return new AnticompactionRequest(parentRepairSession, ranges);
        }

        public long serializedSize(AnticompactionRequest message, int version)
        {
            long size = UUIDSerializer.serializer.serializedSize(message.parentRepairSession, version);
            size += Integer.BYTES; // count of items in successfulRanges
            for (Range<Token> r : message.successfulRanges)
                size += Range.tokenSerializer.serializedSize(r, version);
            return size;
        }
    }

    @Override
    public String toString()
    {
        return "AnticompactionRequest{" +
                "parentRepairSession=" + parentRepairSession +
                "} " + super.toString();
    }
}
