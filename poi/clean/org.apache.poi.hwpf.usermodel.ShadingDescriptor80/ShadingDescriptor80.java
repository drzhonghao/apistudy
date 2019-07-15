import org.apache.poi.hwpf.usermodel.*;


import org.apache.poi.hwpf.model.Colorref;

import org.apache.poi.hwpf.model.types.SHD80AbstractType;

/**
 * The SHD80 is a substructure of the CHP and PAP, and TC for Word 97.
 */
public final class ShadingDescriptor80 extends SHD80AbstractType implements
        Cloneable
{

    public ShadingDescriptor80()
    {
    }

    public ShadingDescriptor80( byte[] buf, int offset )
    {
        super();
        fillFields( buf, offset );
    }

    public ShadingDescriptor80( short value )
    {
        super();
        field_1_value = value;
    }

    public ShadingDescriptor80 clone() throws CloneNotSupportedException
    {
        return (ShadingDescriptor80) super.clone();
    }

    public boolean isEmpty()
    {
        return field_1_value == 0;
    }

    public byte[] serialize()
    {
        byte[] result = new byte[getSize()];
        serialize( result, 0 );
        return result;
    }

    public ShadingDescriptor toShadingDescriptor()
    {
        ShadingDescriptor result = new ShadingDescriptor();
        result.setCvFore( Colorref.valueOfIco( getIcoFore() ) );
        result.setCvBack( Colorref.valueOfIco( getIcoBack() ) );
        result.setIpat( getIpat() );
        return result;
    }

    @Override
    public String toString()
    {
        if ( isEmpty() )
            return "[SHD80] EMPTY";

        return "[SHD80] (icoFore: " + getIcoFore() + "; icoBack: "
                + getIcoBack() + "; iPat: " + getIpat() + ")";
    }

}
