import org.apache.poi.hpsf.*;


import org.apache.poi.util.Internal;
import org.apache.poi.util.LittleEndianByteArrayInputStream;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;

@Internal
public class VariantBool {
    private final static POILogger LOG = POILogFactory.getLogger( VariantBool.class );

    static final int SIZE = 2;

    private boolean _value;

    public void read( LittleEndianByteArrayInputStream lei ) {
        short value = lei.readShort();
        switch (value) {
            case 0:
                _value = false;
                break;
            case -1:
                _value = true;
                break;
            default:
                LOG.log( POILogger.WARN, "VARIANT_BOOL value '"+value+"' is incorrect" );
                _value = true;
                break;
        }
    }

    public boolean getValue() {
        return _value;
    }

    public void setValue( boolean value ) {
        this._value = value;
    }
}
