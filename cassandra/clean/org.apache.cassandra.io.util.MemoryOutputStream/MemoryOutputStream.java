import org.apache.cassandra.io.util.Memory;
import org.apache.cassandra.io.util.*;


import java.io.IOException;
import java.io.OutputStream;

/**
 * This class provides a way to stream the writes into the {@link Memory}
 */
public class MemoryOutputStream extends OutputStream
{

    private final Memory mem;
    private int position = 0;

    public MemoryOutputStream(Memory mem)
    {
        this.mem = mem;
    }

    public void write(int b)
    {
        mem.setByte(position++, (byte) b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException
    {
        mem.setBytes(position, b, off, len);
        position += len;
    }

    public int position()
    {
        return position;
    }
}
