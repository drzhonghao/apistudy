import org.apache.poi.hwpf.model.*;


import org.apache.poi.hwpf.model.types.StdfBaseAbstractType;
import org.apache.poi.util.Internal;

/**
 * The StdfBase structure specifies general information about a style.
 */
@Internal
class StdfBase extends StdfBaseAbstractType
{

    public StdfBase()
    {
    }

    public StdfBase( byte[] std, int offset )
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
        StdfBase other = (StdfBase) obj;
        if ( field_1_info1 != other.field_1_info1 )
            return false;
        if ( field_2_info2 != other.field_2_info2 )
            return false;
        if ( field_3_info3 != other.field_3_info3 )
            return false;
        if ( field_4_bchUpe != other.field_4_bchUpe )
            return false;
        if ( field_5_grfstd != other.field_5_grfstd )
            return false;
        return true;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + field_1_info1;
        result = prime * result + field_2_info2;
        result = prime * result + field_3_info3;
        result = prime * result + field_4_bchUpe;
        result = prime * result + field_5_grfstd;
        return result;
    }

    public byte[] serialize()
    {
        byte[] result = new byte[getSize()];
        serialize( result, 0 );
        return result;
    }
}
