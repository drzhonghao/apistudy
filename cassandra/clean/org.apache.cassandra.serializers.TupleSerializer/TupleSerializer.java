import org.apache.cassandra.serializers.*;


import java.nio.ByteBuffer;
import java.util.List;

import org.apache.cassandra.utils.ByteBufferUtil;

public class TupleSerializer extends BytesSerializer
{
    public final List<TypeSerializer<?>> fields;

    public TupleSerializer(List<TypeSerializer<?>> fields)
    {
        this.fields = fields;
    }

    @Override
    public void validate(ByteBuffer bytes) throws MarshalException
    {
        ByteBuffer input = bytes.duplicate();
        for (int i = 0; i < fields.size(); i++)
        {
            // we allow the input to have less fields than declared so as to support field addition.
            if (!input.hasRemaining())
                return;

            if (input.remaining() < Integer.BYTES)
                throw new MarshalException(String.format("Not enough bytes to read size of %dth component", i));

            int size = input.getInt();

            // size < 0 means null value
            if (size < 0)
                continue;

            if (input.remaining() < size)
                throw new MarshalException(String.format("Not enough bytes to read %dth component", i));

            ByteBuffer field = ByteBufferUtil.readBytes(input, size);
            fields.get(i).validate(field);
        }

        // We're allowed to get less fields than declared, but not more
        if (input.hasRemaining())
            throw new MarshalException("Invalid remaining data after end of tuple value");
    }
}
