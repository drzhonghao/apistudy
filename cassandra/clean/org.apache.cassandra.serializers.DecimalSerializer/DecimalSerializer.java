import org.apache.cassandra.serializers.*;


import org.apache.cassandra.utils.ByteBufferUtil;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;

public class DecimalSerializer implements TypeSerializer<BigDecimal>
{
    public static final DecimalSerializer instance = new DecimalSerializer();

    public BigDecimal deserialize(ByteBuffer bytes)
    {
        if (bytes == null || bytes.remaining() == 0)
            return null;

        // do not consume the contents of the ByteBuffer
        bytes = bytes.duplicate();
        int scale = bytes.getInt();
        byte[] bibytes = new byte[bytes.remaining()];
        bytes.get(bibytes);

        BigInteger bi = new BigInteger(bibytes);
        return new BigDecimal(bi, scale);
    }

    public ByteBuffer serialize(BigDecimal value)
    {
        if (value == null)
            return ByteBufferUtil.EMPTY_BYTE_BUFFER;

        BigInteger bi = value.unscaledValue();
        int scale = value.scale();
        byte[] bibytes = bi.toByteArray();

        ByteBuffer bytes = ByteBuffer.allocate(4 + bibytes.length);
        bytes.putInt(scale);
        bytes.put(bibytes);
        bytes.rewind();
        return bytes;
    }

    public void validate(ByteBuffer bytes) throws MarshalException
    {
        // We at least store the scale.
        if (bytes.remaining() != 0 && bytes.remaining() < 4)
            throw new MarshalException(String.format("Expected 0 or at least 4 bytes (%d)", bytes.remaining()));
    }

    public String toString(BigDecimal value)
    {
        return value == null ? "" : value.toPlainString();
    }

    public Class<BigDecimal> getType()
    {
        return BigDecimal.class;
    }
}
