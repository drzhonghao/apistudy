import org.apache.poi.hwpf.model.*;


import org.apache.poi.hwpf.model.types.StdfPost2000AbstractType;
import org.apache.poi.util.Internal;

/**
 * The StdfBase structure specifies general information about a style.
 */
@Internal
class StdfPost2000 extends StdfPost2000AbstractType
{

    public StdfPost2000()
    {
    }

    public StdfPost2000( byte[] std, int offset )
    {
        fillFields( std, offset );
    }

    @Override
    public boolean equals( Object obj )
    {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( getClass() != obj.getClass() )
            return false;
        StdfPost2000 other = (StdfPost2000) obj;
        if ( field_1_info1 != other.field_1_info1 )
            return false;
        if ( field_2_rsid != other.field_2_rsid )
            return false;
        if ( field_3_info3 != other.field_3_info3 )
            return false;
        return true;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + field_1_info1;
        result = prime * result
                + (int) ( field_2_rsid ^ ( field_2_rsid >>> 32 ) );
        result = prime * result + field_3_info3;
        return result;
    }

    public byte[] serialize()
    {
        byte[] result = new byte[getSize()];
        serialize( result, 0 );
        return result;
    }
}
