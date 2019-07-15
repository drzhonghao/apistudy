import org.apache.poi.hpsf.*;


import org.apache.poi.util.Internal;
import org.apache.poi.util.LittleEndianByteArrayInputStream;

@Internal
public class Decimal {
    /**
     * Findbugs: UNR_UNREAD_FIELD
     */
    private short field_1_wReserved;
    private byte field_2_scale;
    private byte field_3_sign;
    private int field_4_hi32;
    private long field_5_lo64;

    public void read( LittleEndianByteArrayInputStream lei ) {
        field_1_wReserved = lei.readShort();
        field_2_scale = lei.readByte();
        field_3_sign = lei.readByte();
        field_4_hi32 = lei.readInt();
        field_5_lo64 = lei.readLong();
    }
}
