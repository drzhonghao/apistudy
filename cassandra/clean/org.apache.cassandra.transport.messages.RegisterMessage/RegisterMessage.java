import org.apache.cassandra.transport.Event;
import org.apache.cassandra.transport.CBUtil;
import org.apache.cassandra.transport.Connection;
import org.apache.cassandra.transport.messages.*;


import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.ByteBuf;

import org.apache.cassandra.service.QueryState;
import org.apache.cassandra.transport.*;

public class RegisterMessage extends Message.Request
{
    public static final Message.Codec<RegisterMessage> codec = new Message.Codec<RegisterMessage>()
    {
        public RegisterMessage decode(ByteBuf body, ProtocolVersion version)
        {
            int length = body.readUnsignedShort();
            List<Event.Type> eventTypes = new ArrayList<>(length);
            for (int i = 0; i < length; ++i)
                eventTypes.add(CBUtil.readEnumValue(Event.Type.class, body));
            return new RegisterMessage(eventTypes);
        }

        public void encode(RegisterMessage msg, ByteBuf dest, ProtocolVersion version)
        {
            dest.writeShort(msg.eventTypes.size());
            for (Event.Type type : msg.eventTypes)
                CBUtil.writeEnumValue(type, dest);
        }

        public int encodedSize(RegisterMessage msg, ProtocolVersion version)
        {
            int size = 2;
            for (Event.Type type : msg.eventTypes)
                size += CBUtil.sizeOfEnumValue(type);
            return size;
        }
    };

    public final List<Event.Type> eventTypes;

    public RegisterMessage(List<Event.Type> eventTypes)
    {
        super(Message.Type.REGISTER);
        this.eventTypes = eventTypes;
    }

    public Response execute(QueryState state, long queryStartNanoTime)
    {
        assert connection instanceof ServerConnection;
        Connection.Tracker tracker = connection.getTracker();
        assert tracker instanceof Server.ConnectionTracker;
        for (Event.Type type : eventTypes)
        {
            if (type.minimumVersion.isGreaterThan(connection.getVersion()))
                throw new ProtocolException("Event " + type.name() + " not valid for protocol version " + connection.getVersion());
            ((Server.ConnectionTracker) tracker).register(type, connection().channel());
        }
        return new ReadyMessage();
    }

    @Override
    public String toString()
    {
        return "REGISTER " + eventTypes;
    }
}
