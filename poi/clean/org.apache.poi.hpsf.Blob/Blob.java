import org.apache.poi.hpsf.*;


import org.apache.poi.util.IOUtils;
import org.apache.poi.util.Internal;
import org.apache.poi.util.LittleEndianInput;

@Internal
public class Blob {

    //arbitrarily selected; may need to increase
    private static final int MAX_RECORD_LENGTH = 1_000_000;

    private byte[] _value;

    public void read( LittleEndianInput lei ) {
        int size = lei.readInt();
        _value = IOUtils.safelyAllocate(size, MAX_RECORD_LENGTH);
        if ( size > 0 ) {
            lei.readFully(_value);
        }
    }
}
