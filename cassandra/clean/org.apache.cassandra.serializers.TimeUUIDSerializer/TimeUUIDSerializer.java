import org.apache.cassandra.serializers.UUIDSerializer;
import org.apache.cassandra.serializers.*;


import java.nio.ByteBuffer;

public class TimeUUIDSerializer extends UUIDSerializer
{
    public static final TimeUUIDSerializer instance = new TimeUUIDSerializer();

    @Override
    public void validate(ByteBuffer bytes) throws MarshalException
    {
        super.validate(bytes);

        // Super class only validates the Time UUID
        ByteBuffer slice = bytes.slice();
        // version is bits 4-7 of byte 6.
        if (bytes.remaining() > 0)
        {
            slice.position(6);
            if ((slice.get() & 0xf0) != 0x10)
                throw new MarshalException("Invalid version for TimeUUID type.");
        }
    }
}
