import org.apache.cassandra.db.TypeSizes;
import org.apache.cassandra.db.*;


import java.io.IOException;

import org.apache.cassandra.io.IVersionedSerializer;
import org.apache.cassandra.io.util.DataInputPlus;
import org.apache.cassandra.io.util.DataOutputPlus;
import org.apache.cassandra.net.MessageOut;
import org.apache.cassandra.net.MessagingService;

public class SnapshotCommand
{
    public static final SnapshotCommandSerializer serializer = new SnapshotCommandSerializer();

    public final String keyspace;
    public final String column_family;
    public final String snapshot_name;
    public final boolean clear_snapshot;

    public SnapshotCommand(String keyspace, String columnFamily, String snapshotName, boolean clearSnapshot)
    {
        this.keyspace = keyspace;
        this.column_family = columnFamily;
        this.snapshot_name = snapshotName;
        this.clear_snapshot = clearSnapshot;
    }

    public MessageOut createMessage()
    {
        return new MessageOut<SnapshotCommand>(MessagingService.Verb.SNAPSHOT, this, serializer);
    }

    @Override
    public String toString()
    {
        return "SnapshotCommand{" + "keyspace='" + keyspace + '\'' +
                                  ", column_family='" + column_family + '\'' +
                                  ", snapshot_name=" + snapshot_name +
                                  ", clear_snapshot=" + clear_snapshot + '}';
    }
}

class SnapshotCommandSerializer implements IVersionedSerializer<SnapshotCommand>
{
    public void serialize(SnapshotCommand snapshot_command, DataOutputPlus out, int version) throws IOException
    {
        out.writeUTF(snapshot_command.keyspace);
        out.writeUTF(snapshot_command.column_family);
        out.writeUTF(snapshot_command.snapshot_name);
        out.writeBoolean(snapshot_command.clear_snapshot);
    }

    public SnapshotCommand deserialize(DataInputPlus in, int version) throws IOException
    {
        String keyspace = in.readUTF();
        String column_family = in.readUTF();
        String snapshot_name = in.readUTF();
        boolean clear_snapshot = in.readBoolean();
        return new SnapshotCommand(keyspace, column_family, snapshot_name, clear_snapshot);
    }

    public long serializedSize(SnapshotCommand sc, int version)
    {
        return TypeSizes.sizeof(sc.keyspace)
             + TypeSizes.sizeof(sc.column_family)
             + TypeSizes.sizeof(sc.snapshot_name)
             + TypeSizes.sizeof(sc.clear_snapshot);
    }
}
