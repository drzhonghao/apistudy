import org.apache.cassandra.streaming.messages.StreamMessage;
import org.apache.cassandra.streaming.messages.*;


import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.cassandra.io.util.DataInputPlus;
import org.apache.cassandra.io.util.DataInputPlus.DataInputStreamPlus;
import org.apache.cassandra.io.util.DataOutputStreamPlus;
import org.apache.cassandra.streaming.StreamRequest;
import org.apache.cassandra.streaming.StreamSession;
import org.apache.cassandra.streaming.StreamSummary;

public class PrepareMessage extends StreamMessage
{
    public static Serializer<PrepareMessage> serializer = new Serializer<PrepareMessage>()
    {
        @SuppressWarnings("resource") // Not closing constructed DataInputPlus's as the channel needs to remain open.
        public PrepareMessage deserialize(ReadableByteChannel in, int version, StreamSession session) throws IOException
        {
            DataInputPlus input = new DataInputStreamPlus(Channels.newInputStream(in));
            PrepareMessage message = new PrepareMessage();
            // requests
            int numRequests = input.readInt();
            for (int i = 0; i < numRequests; i++)
                message.requests.add(StreamRequest.serializer.deserialize(input, version));
            // summaries
            int numSummaries = input.readInt();
            for (int i = 0; i < numSummaries; i++)
                message.summaries.add(StreamSummary.serializer.deserialize(input, version));
            return message;
        }

        public void serialize(PrepareMessage message, DataOutputStreamPlus out, int version, StreamSession session) throws IOException
        {
            // requests
            out.writeInt(message.requests.size());
            for (StreamRequest request : message.requests)
                StreamRequest.serializer.serialize(request, out, version);
            // summaries
            out.writeInt(message.summaries.size());
            for (StreamSummary summary : message.summaries)
                StreamSummary.serializer.serialize(summary, out, version);
        }
    };

    /**
     * Streaming requests
     */
    public final Collection<StreamRequest> requests = new ArrayList<>();

    /**
     * Summaries of streaming out
     */
    public final Collection<StreamSummary> summaries = new ArrayList<>();

    public PrepareMessage()
    {
        super(Type.PREPARE);
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder("Prepare (");
        sb.append(requests.size()).append(" requests, ");
        int totalFile = 0;
        for (StreamSummary summary : summaries)
            totalFile += summary.files;
        sb.append(" ").append(totalFile).append(" files");
        sb.append('}');
        return sb.toString();
    }
}
