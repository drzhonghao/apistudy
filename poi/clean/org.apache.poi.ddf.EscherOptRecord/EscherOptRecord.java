import org.apache.poi.ddf.AbstractEscherOptRecord;
import org.apache.poi.ddf.*;


import org.apache.poi.util.Internal;

/**
 * The opt record is used to store property values for a shape. It is the key to
 * determining the attributes of a shape. Properties can be of two types: simple
 * or complex. Simple types are fixed length. Complex properties are variable
 * length.
 */
public class EscherOptRecord extends AbstractEscherOptRecord
{
    public static final String RECORD_DESCRIPTION = "msofbtOPT";
    public static final short RECORD_ID = (short) 0xF00B;

    @Override
    public short getInstance()
    {
        setInstance( (short) getEscherProperties().size() );
        return super.getInstance();
    }

    /**
     * Automatically recalculate the correct option
     */
    @Override
    @Internal
    public short getOptions()
    {
        // update values
        getInstance();
        getVersion();
        return super.getOptions();
    }

    @Override
    public String getRecordName()
    {
        return "Opt";
    }

    @Override
    public short getVersion()
    {
        setVersion( (short) 0x3 );
        return super.getVersion();
    }

    @Override
    public void setVersion( short value )
    {
        if ( value != 0x3 ) {
            throw new IllegalArgumentException( RECORD_DESCRIPTION
                    + " can have only '0x3' version" );
        }

        super.setVersion( value );
    }
}
