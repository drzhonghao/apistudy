import org.apache.cassandra.transport.messages.*;


import java.util.UUID;

import com.google.common.collect.ImmutableMap;
import io.netty.buffer.ByteBuf;

import org.apache.cassandra.service.ClientState;
import org.apache.cassandra.service.QueryState;
import org.apache.cassandra.tracing.Tracing;
import org.apache.cassandra.transport.*;
import org.apache.cassandra.utils.JVMStabilityInspector;
import org.apache.cassandra.utils.UUIDGen;

public class PrepareMessage extends Message.Request
{
    public static final Message.Codec<PrepareMessage> codec = new Message.Codec<PrepareMessage>()
    {
        public PrepareMessage decode(ByteBuf body, ProtocolVersion version)
        {
            String query = CBUtil.readLongString(body);
            return new PrepareMessage(query);
        }

        public void encode(PrepareMessage msg, ByteBuf dest, ProtocolVersion version)
        {
            CBUtil.writeLongString(msg.query, dest);
        }

        public int encodedSize(PrepareMessage msg, ProtocolVersion version)
        {
            return CBUtil.sizeOfLongString(msg.query);
        }
    };

    private final String query;

    public PrepareMessage(String query)
    {
        super(Message.Type.PREPARE);
        this.query = query;
    }

    public Message.Response execute(QueryState state, long queryStartNanoTime)
    {
        try
        {
            UUID tracingId = null;
            if (isTracingRequested())
            {
                tracingId = UUIDGen.getTimeUUID();
                state.prepareTracingSession(tracingId);
            }

            if (state.traceNextQuery())
            {
                state.createTracingSession(getCustomPayload());
                Tracing.instance.begin("Preparing CQL3 query", state.getClientAddress(), ImmutableMap.of("query", query));
            }

            Message.Response response = ClientState.getCQLQueryHandler().prepare(query, state, getCustomPayload());

            if (tracingId != null)
                response.setTracingId(tracingId);

            return response;
        }
        catch (Exception e)
        {
            JVMStabilityInspector.inspectThrowable(e);
            return ErrorMessage.fromException(e);
        }
        finally
        {
            Tracing.instance.stopSession();
        }
    }

    @Override
    public String toString()
    {
        return "PREPARE " + query;
    }
}
