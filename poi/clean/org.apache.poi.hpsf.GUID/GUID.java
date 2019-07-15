import org.apache.poi.hpsf.*;


import org.apache.poi.util.Internal;
import org.apache.poi.util.LittleEndianByteArrayInputStream;

@Internal
public class GUID {
    private int _data1;
    private short _data2;
    private short _data3;
    private long _data4;

    public void read( LittleEndianByteArrayInputStream lei ) {
        _data1 = lei.readInt();
        _data2 = lei.readShort();
        _data3 = lei.readShort();
        _data4 = lei.readLong();
    }
}
