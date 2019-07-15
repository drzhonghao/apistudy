import org.apache.cassandra.serializers.*;


import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import org.apache.cassandra.utils.ByteBufferUtil;

public class UserTypeSerializer extends BytesSerializer
{
    public final LinkedHashMap<String, TypeSerializer<?>> fields;

    public UserTypeSerializer(LinkedHashMap<String, TypeSerializer<?>> fields)
    {
        this.fields = fields;
    }

    @Override
    public void validate(ByteBuffer bytes) throws MarshalException
    {
        ByteBuffer input = bytes.duplicate();
        int i = 0;
        for (Entry<String, TypeSerializer<?>> entry : fields.entrySet())
        {
            // we allow the input to have less fields than declared so as to support field addition.
            if (!input.hasRemaining())
                return;

            if (input.remaining() < 4)
                throw new MarshalException(String.format("Not enough bytes to read size of %dth field %s", i, entry.getKey()));

            int size = input.getInt();

            // size < 0 means null value
            if (size < 0)
                continue;

            if (input.remaining() < size)
                throw new MarshalException(String.format("Not enough bytes to read %dth field %s", i, entry.getKey()));

            ByteBuffer field = ByteBufferUtil.readBytes(input, size);
            entry.getValue().validate(field);
            i++;
        }

        // We're allowed to get less fields than declared, but not more
        if (input.hasRemaining())
            throw new MarshalException("Invalid remaining data after end of UDT value");
    }
}
