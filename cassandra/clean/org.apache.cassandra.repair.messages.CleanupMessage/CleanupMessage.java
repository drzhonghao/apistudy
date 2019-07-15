import org.apache.cassandra.repair.messages.RepairMessage;
import org.apache.cassandra.repair.messages.*;


import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

import org.apache.cassandra.io.util.DataInputPlus;
import org.apache.cassandra.io.util.DataOutputPlus;
import org.apache.cassandra.utils.UUIDSerializer;

/**
 * Message to cleanup repair resources on replica nodes.
 *
 * @since 2.1.6
 */
public class CleanupMessage extends RepairMessage
{
    public static MessageSerializer serializer = new CleanupMessageSerializer();
    public final UUID parentRepairSession;

    public CleanupMessage(UUID parentRepairSession)
    {
        super(Type.CLEANUP, null);
        this.parentRepairSession = parentRepairSession;
    }

    @Override
    public boolean equals(Object o)
    {
        if (!(o instanceof CleanupMessage))
            return false;
        CleanupMessage other = (CleanupMessage) o;
        return messageType == other.messageType &&
               parentRepairSession.equals(other.parentRepairSession);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(messageType, parentRepairSession);
    }

    public static class CleanupMessageSerializer implements MessageSerializer<CleanupMessage>
    {
        public void serialize(CleanupMessage message, DataOutputPlus out, int version) throws IOException
        {
            UUIDSerializer.serializer.serialize(message.parentRepairSession, out, version);
        }

        public CleanupMessage deserialize(DataInputPlus in, int version) throws IOException
        {
            UUID parentRepairSession = UUIDSerializer.serializer.deserialize(in, version);
            return new CleanupMessage(parentRepairSession);
        }

        public long serializedSize(CleanupMessage message, int version)
        {
            return UUIDSerializer.serializer.serializedSize(message.parentRepairSession, version);
        }
    }
}
