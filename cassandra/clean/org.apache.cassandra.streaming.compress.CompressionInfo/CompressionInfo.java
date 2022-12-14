import org.apache.cassandra.streaming.compress.*;


import java.io.IOException;

import org.apache.cassandra.db.TypeSizes;
import org.apache.cassandra.io.IVersionedSerializer;
import org.apache.cassandra.io.compress.CompressionMetadata;
import org.apache.cassandra.schema.CompressionParams;
import org.apache.cassandra.io.util.DataInputPlus;
import org.apache.cassandra.io.util.DataOutputPlus;

/**
 * Container that carries compression parameters and chunks to decompress data from stream.
 */
public class CompressionInfo
{
    public static final IVersionedSerializer<CompressionInfo> serializer = new CompressionInfoSerializer();

    public final CompressionMetadata.Chunk[] chunks;
    public final CompressionParams parameters;

    public CompressionInfo(CompressionMetadata.Chunk[] chunks, CompressionParams parameters)
    {
        assert chunks != null && parameters != null;
        this.chunks = chunks;
        this.parameters = parameters;
    }

    static class CompressionInfoSerializer implements IVersionedSerializer<CompressionInfo>
    {
        public void serialize(CompressionInfo info, DataOutputPlus out, int version) throws IOException
        {
            if (info == null)
            {
                out.writeInt(-1);
                return;
            }

            int chunkCount = info.chunks.length;
            out.writeInt(chunkCount);
            for (int i = 0; i < chunkCount; i++)
                CompressionMetadata.Chunk.serializer.serialize(info.chunks[i], out, version);
            // compression params
            CompressionParams.serializer.serialize(info.parameters, out, version);
        }

        public CompressionInfo deserialize(DataInputPlus in, int version) throws IOException
        {
            // chunks
            int chunkCount = in.readInt();
            if (chunkCount < 0)
                return null;

            CompressionMetadata.Chunk[] chunks = new CompressionMetadata.Chunk[chunkCount];
            for (int i = 0; i < chunkCount; i++)
                chunks[i] = CompressionMetadata.Chunk.serializer.deserialize(in, version);

            // compression params
            CompressionParams parameters = CompressionParams.serializer.deserialize(in, version);
            return new CompressionInfo(chunks, parameters);
        }

        public long serializedSize(CompressionInfo info, int version)
        {
            if (info == null)
                return TypeSizes.sizeof(-1);

            // chunks
            int chunkCount = info.chunks.length;
            long size = TypeSizes.sizeof(chunkCount);
            for (int i = 0; i < chunkCount; i++)
                size += CompressionMetadata.Chunk.serializer.serializedSize(info.chunks[i], version);
            // compression params
            size += CompressionParams.serializer.serializedSize(info.parameters, version);
            return size;
        }
    }
}
