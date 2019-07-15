import org.apache.cassandra.streaming.*;


import java.io.IOException;
import java.io.Serializable;
import java.util.UUID;

import com.google.common.base.Objects;

import org.apache.cassandra.db.TypeSizes;
import org.apache.cassandra.io.IVersionedSerializer;
import org.apache.cassandra.io.util.DataInputPlus;
import org.apache.cassandra.io.util.DataOutputPlus;
import org.apache.cassandra.net.MessagingService;
import org.apache.cassandra.utils.UUIDSerializer;

/**
 * Summary of streaming.
 */
public class StreamSummary implements Serializable
{
    public static final IVersionedSerializer<StreamSummary> serializer = new StreamSummarySerializer();

    public final UUID cfId;

    /**
     * Number of files to transfer. Can be 0 if nothing to transfer for some streaming request.
     */
    public final int files;
    public final long totalSize;

    public StreamSummary(UUID cfId, int files, long totalSize)
    {
        this.cfId = cfId;
        this.files = files;
        this.totalSize = totalSize;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StreamSummary summary = (StreamSummary) o;
        return files == summary.files && totalSize == summary.totalSize && cfId.equals(summary.cfId);
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(cfId, files, totalSize);
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder("StreamSummary{");
        sb.append("path=").append(cfId);
        sb.append(", files=").append(files);
        sb.append(", totalSize=").append(totalSize);
        sb.append('}');
        return sb.toString();
    }

    public static class StreamSummarySerializer implements IVersionedSerializer<StreamSummary>
    {
        // arbitrary version is fine for UUIDSerializer for now...
        public void serialize(StreamSummary summary, DataOutputPlus out, int version) throws IOException
        {
            UUIDSerializer.serializer.serialize(summary.cfId, out, MessagingService.current_version);
            out.writeInt(summary.files);
            out.writeLong(summary.totalSize);
        }

        public StreamSummary deserialize(DataInputPlus in, int version) throws IOException
        {
            UUID cfId = UUIDSerializer.serializer.deserialize(in, MessagingService.current_version);
            int files = in.readInt();
            long totalSize = in.readLong();
            return new StreamSummary(cfId, files, totalSize);
        }

        public long serializedSize(StreamSummary summary, int version)
        {
            long size = UUIDSerializer.serializer.serializedSize(summary.cfId, MessagingService.current_version);
            size += TypeSizes.sizeof(summary.files);
            size += TypeSizes.sizeof(summary.totalSize);
            return size;
        }
    }
}
