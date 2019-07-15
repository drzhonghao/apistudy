import org.apache.cassandra.tracing.*;


import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.*;

import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.config.SchemaConstants;
import org.apache.cassandra.db.Mutation;
import org.apache.cassandra.db.rows.Row;
import org.apache.cassandra.db.partitions.PartitionUpdate;
import org.apache.cassandra.schema.KeyspaceMetadata;
import org.apache.cassandra.schema.KeyspaceParams;
import org.apache.cassandra.schema.Tables;
import org.apache.cassandra.utils.FBUtilities;
import org.apache.cassandra.utils.UUIDGen;

public final class TraceKeyspace
{
    private TraceKeyspace()
    {
    }

    public static final String SESSIONS = "sessions";
    public static final String EVENTS = "events";

    private static final CFMetaData Sessions =
        compile(SESSIONS,
                "tracing sessions",
                "CREATE TABLE %s ("
                + "session_id uuid,"
                + "command text,"
                + "client inet,"
                + "coordinator inet,"
                + "duration int,"
                + "parameters map<text, text>,"
                + "request text,"
                + "started_at timestamp,"
                + "PRIMARY KEY ((session_id)))");

    private static final CFMetaData Events =
        compile(EVENTS,
                "tracing events",
                "CREATE TABLE %s ("
                + "session_id uuid,"
                + "event_id timeuuid,"
                + "activity text,"
                + "source inet,"
                + "source_elapsed int,"
                + "thread text,"
                + "PRIMARY KEY ((session_id), event_id))");

    private static CFMetaData compile(String name, String description, String schema)
    {
        return CFMetaData.compile(String.format(schema, name), SchemaConstants.TRACE_KEYSPACE_NAME)
                         .comment(description);
    }

    public static KeyspaceMetadata metadata()
    {
        return KeyspaceMetadata.create(SchemaConstants.TRACE_KEYSPACE_NAME, KeyspaceParams.simple(2), Tables.of(Sessions, Events));
    }

    static Mutation makeStartSessionMutation(ByteBuffer sessionId,
                                             InetAddress client,
                                             Map<String, String> parameters,
                                             String request,
                                             long startedAt,
                                             String command,
                                             int ttl)
    {
        PartitionUpdate.SimpleBuilder builder = PartitionUpdate.simpleBuilder(Sessions, sessionId);
        builder.row()
               .ttl(ttl)
               .add("client", client)
               .add("coordinator", FBUtilities.getBroadcastAddress())
               .add("request", request)
               .add("started_at", new Date(startedAt))
               .add("command", command)
               .appendAll("parameters", parameters);

        return builder.buildAsMutation();
    }

    static Mutation makeStopSessionMutation(ByteBuffer sessionId, int elapsed, int ttl)
    {
        PartitionUpdate.SimpleBuilder builder = PartitionUpdate.simpleBuilder(Sessions, sessionId);
        builder.row()
               .ttl(ttl)
               .add("duration", elapsed);
        return builder.buildAsMutation();
    }

    static Mutation makeEventMutation(ByteBuffer sessionId, String message, int elapsed, String threadName, int ttl)
    {
        PartitionUpdate.SimpleBuilder builder = PartitionUpdate.simpleBuilder(Events, sessionId);
        Row.SimpleBuilder rowBuilder = builder.row(UUIDGen.getTimeUUID())
                                              .ttl(ttl);

        rowBuilder.add("activity", message)
                  .add("source", FBUtilities.getBroadcastAddress())
                  .add("thread", threadName);

        if (elapsed >= 0)
            rowBuilder.add("source_elapsed", elapsed);

        return builder.buildAsMutation();
    }
}
