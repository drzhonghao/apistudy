import org.apache.cassandra.io.util.UnbufferedDataOutputStreamPlus;
import org.apache.cassandra.io.util.*;


import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.WritableByteChannel;

/**
 * When possible use {@link WrappedDataOutputStreamPlus} instead of this class, as it will
 * be more efficient when using Plus methods. This class is only for situations where it cannot be used.
 *
 * The channel provided by this class is just a wrapper around the output stream.
 */
public class WrappedDataOutputStreamPlus extends UnbufferedDataOutputStreamPlus
{
    protected final OutputStream out;
    public WrappedDataOutputStreamPlus(OutputStream out)
    {
        super();
        this.out = out;
    }

    public WrappedDataOutputStreamPlus(OutputStream out, WritableByteChannel channel)
    {
        super(channel);
        this.out = out;
    }

    @Override
    public void write(byte[] buffer, int offset, int count) throws IOException
    {
        out.write(buffer, offset, count);
    }

    @Override
    public void write(int oneByte) throws IOException
    {
        out.write(oneByte);
    }

    @Override
    public void close() throws IOException
    {
        out.close();
    }

    @Override
    public void flush() throws IOException
    {
        out.flush();
    }
}
