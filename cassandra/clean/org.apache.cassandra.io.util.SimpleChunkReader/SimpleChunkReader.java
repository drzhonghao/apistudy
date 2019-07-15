import org.apache.cassandra.io.util.AbstractReaderFileProxy;
import org.apache.cassandra.io.util.BufferManagingRebufferer;
import org.apache.cassandra.io.util.*;


import java.nio.ByteBuffer;

import org.apache.cassandra.io.compress.BufferType;

class SimpleChunkReader extends AbstractReaderFileProxy implements ChunkReader
{
    private final int bufferSize;
    private final BufferType bufferType;

    SimpleChunkReader(ChannelProxy channel, long fileLength, BufferType bufferType, int bufferSize)
    {
        super(channel, fileLength);
        this.bufferSize = bufferSize;
        this.bufferType = bufferType;
    }

    @Override
    public void readChunk(long position, ByteBuffer buffer)
    {
        buffer.clear();
        channel.read(buffer, position);
        buffer.flip();
    }

    @Override
    public int chunkSize()
    {
        return bufferSize;
    }

    @Override
    public BufferType preferredBufferType()
    {
        return bufferType;
    }

    @Override
    public Rebufferer instantiateRebufferer()
    {
        return new BufferManagingRebufferer.Unaligned(this);
    }

    @Override
    public String toString()
    {
        return String.format("%s(%s - chunk length %d, data length %d)",
                             getClass().getSimpleName(),
                             channel.filePath(),
                             bufferSize,
                             fileLength());
    }
}
