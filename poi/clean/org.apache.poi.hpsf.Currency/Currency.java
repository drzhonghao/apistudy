import org.apache.poi.hpsf.*;


import org.apache.poi.util.Internal;
import org.apache.poi.util.LittleEndianByteArrayInputStream;

@Internal
public class Currency {
    private static final int SIZE = 8;

    private final byte[] _value = new byte[SIZE];

    public void read( LittleEndianByteArrayInputStream lei ) {
        lei.readFully(_value);
    }
}
