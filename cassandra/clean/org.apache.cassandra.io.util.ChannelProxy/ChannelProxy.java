import org.apache.cassandra.io.util.*;


import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.StandardOpenOption;

import org.apache.cassandra.io.FSReadError;
import org.apache.cassandra.utils.NativeLibrary;
import org.apache.cassandra.utils.concurrent.RefCounted;
import org.apache.cassandra.utils.concurrent.SharedCloseableImpl;

/**
 * A proxy of a FileChannel that:
 *
 * - implements reference counting
 * - exports only thread safe FileChannel operations
 * - wraps IO exceptions into runtime exceptions
 *
 * Tested by RandomAccessReaderTest.
 */
public final class ChannelProxy extends SharedCloseableImpl
{
    private final String filePath;
    private final FileChannel channel;

    public static FileChannel openChannel(File file)
    {
        try
        {
            return FileChannel.open(file.toPath(), StandardOpenOption.READ);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public ChannelProxy(String path)
    {
        this (new File(path));
    }

    public ChannelProxy(File file)
    {
        this(file.getPath(), openChannel(file));
    }

    public ChannelProxy(String filePath, FileChannel channel)
    {
        super(new Cleanup(filePath, channel));

        this.filePath = filePath;
        this.channel = channel;
    }

    public ChannelProxy(ChannelProxy copy)
    {
        super(copy);

        this.filePath = copy.filePath;
        this.channel = copy.channel;
    }

    private final static class Cleanup implements RefCounted.Tidy
    {
        final String filePath;
        final FileChannel channel;

        Cleanup(String filePath, FileChannel channel)
        {
            this.filePath = filePath;
            this.channel = channel;
        }

        public String name()
        {
            return filePath;
        }

        public void tidy()
        {
            try
            {
                channel.close();
            }
            catch (IOException e)
            {
                throw new FSReadError(e, filePath);
            }
        }
    }

    public ChannelProxy sharedCopy()
    {
        return new ChannelProxy(this);
    }

    public String filePath()
    {
        return filePath;
    }

    public int read(ByteBuffer buffer, long position)
    {
        try
        {
            // FIXME: consider wrapping in a while loop
            return channel.read(buffer, position);
        }
        catch (IOException e)
        {
            throw new FSReadError(e, filePath);
        }
    }

    public long transferTo(long position, long count, WritableByteChannel target)
    {
        try
        {
            return channel.transferTo(position, count, target);
        }
        catch (IOException e)
        {
            throw new FSReadError(e, filePath);
        }
    }

    public MappedByteBuffer map(FileChannel.MapMode mode, long position, long size)
    {
        try
        {
            return channel.map(mode, position, size);
        }
        catch (IOException e)
        {
            throw new FSReadError(e, filePath);
        }
    }

    public long size()
    {
        try
        {
            return channel.size();
        }
        catch (IOException e)
        {
            throw new FSReadError(e, filePath);
        }
    }

    public int getFileDescriptor()
    {
        return NativeLibrary.getfd(channel);
    }

    @Override
    public String toString()
    {
        return filePath();
    }
}
