import org.apache.poi.hpsf.TypedPropertyValue;
import org.apache.poi.hpsf.Variant;
import org.apache.poi.hpsf.*;


import java.util.ArrayList;
import java.util.List;

import org.apache.poi.util.Internal;
import org.apache.poi.util.LittleEndianByteArrayInputStream;

/**
 * Holder for vector-type properties
 */
@Internal
public class Vector {
    private final short _type;

    private TypedPropertyValue[] _values;

    public Vector( short type ) {
        this._type = type;
    }

    public void read( LittleEndianByteArrayInputStream lei ) {
        final long longLength = lei.readUInt();

        if ( longLength > Integer.MAX_VALUE ) {
            throw new UnsupportedOperationException( "Vector is too long -- " + longLength );
        }
        final int length = (int) longLength;

        //BUG-61295 -- avoid OOM on corrupt file.  Build list instead
        //of allocating array of length "length".
        //If the length is corrupted and crazily big but < Integer.MAX_VALUE,
        //this will trigger a RuntimeException "Buffer overrun" in lei.checkPosition
        List<TypedPropertyValue> values = new ArrayList<>();
        int paddedType = (_type == Variant.VT_VARIANT) ? 0 : _type;
        for ( int i = 0; i < length; i++ ) {
            TypedPropertyValue value = new TypedPropertyValue(paddedType, null);
            if (paddedType == 0) {
                value.read(lei);
            } else {
                value.readValue(lei);
            }
            values.add(value);
        }
        _values = values.toArray(new TypedPropertyValue[values.size()]);
    }

    public TypedPropertyValue[] getValues(){
        return _values;
    }
}
