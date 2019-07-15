import org.apache.poi.hwpf.usermodel.*;


import org.apache.poi.hwpf.model.types.SHDAbstractType;

/**
 * The SHD is a substructure of the CHP, PAP, and TC for Word 2000.
 * 
 * @author vlsergey
 */
public final class ShadingDescriptor extends SHDAbstractType implements
        Cloneable
{

    public ShadingDescriptor()
    {
    }

    public ShadingDescriptor( byte[] buf, int offset )
    {
        super();
        fillFields( buf, offset );
    }

    public ShadingDescriptor clone() throws CloneNotSupportedException
    {
        return (ShadingDescriptor) super.clone();
    }

    public boolean isEmpty()
    {
        return field_3_ipat == 0;
    }

    public byte[] serialize()
    {
        byte[] result = new byte[getSize()];
        serialize( result, 0 );
        return result;
    }

    @Override
    public String toString()
    {
        if ( isEmpty() )
            return "[SHD] EMPTY";

        return "[SHD] (cvFore: " + getCvFore() + "; cvBack: " + getCvBack()
                + "; iPat: " + getIpat() + ")";
    }

}
