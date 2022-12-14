import org.apache.poi.hwpf.model.types.*;



import org.apache.poi.hwpf.model.Colorref;
import org.apache.poi.util.Internal;
import org.apache.poi.util.LittleEndian;

/**
 * The Shd structure specifies the colors and pattern that are used for background shading. <p>Class
        and
        fields descriptions are quoted from Word (.doc) Binary File Format by Microsoft Corporation
    
 * <p>
 * NOTE: This source is automatically generated please do not modify this file.  Either subclass or
 *       remove the record in src/types/definitions.
 * <p>
 * This class is internal. It content or properties may change without notice 
 * due to changes in our knowledge of internal Microsoft Word binary structures.

 * @author Sergey Vladimirov; according to Word (.doc) Binary File Format by Microsoft Corporation.
    
 */
@Internal
public abstract class SHDAbstractType
{

    protected Colorref field_1_cvFore;
    protected Colorref field_2_cvBack;
    protected int field_3_ipat;

    protected SHDAbstractType()
    {
        this.field_1_cvFore = new Colorref();
        this.field_2_cvBack = new Colorref();
    }

    protected void fillFields( byte[] data, int offset )
    {
        field_1_cvFore                 = new Colorref( data, 0x0 + offset );
        field_2_cvBack                 = new Colorref( data, 0x4 + offset );
        field_3_ipat                   = LittleEndian.getShort( data, 0x8 + offset );
    }

    public void serialize( byte[] data, int offset )
    {
        field_1_cvFore.serialize( data, 0x0 + offset );
        field_2_cvBack.serialize( data, 0x4 + offset );
        LittleEndian.putUShort( data, 0x8 + offset, field_3_ipat );
    }

    public byte[] serialize()
    {
        final byte[] result = new byte[ getSize() ];
        serialize( result, 0 );
        return result;
    }

    /**
     * Size of record
     */
    public static int getSize()
    {
        return 0 + 4 + 4 + 2;
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
        SHDAbstractType other = (SHDAbstractType) obj;
        if ( field_1_cvFore != other.field_1_cvFore )
            return false;
        if ( field_2_cvBack != other.field_2_cvBack )
            return false;
        if ( field_3_ipat != other.field_3_ipat )
            return false;
        return true;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + field_1_cvFore.hashCode();
        result = prime * result + field_2_cvBack.hashCode();
        result = prime * result + field_3_ipat;
        return result;
    }

    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("[SHD]\n");
        builder.append("    .cvFore               = ");
        builder.append(" (").append(getCvFore()).append(" )\n");
        builder.append("    .cvBack               = ");
        builder.append(" (").append(getCvBack()).append(" )\n");
        builder.append("    .ipat                 = ");
        builder.append(" (").append(getIpat()).append(" )\n");

        builder.append("[/SHD]\n");
        return builder.toString();
    }

    /**
     * A COLORREF that specifies the foreground color of ipat.
     */
    @Internal
    public Colorref getCvFore()
    {
        return field_1_cvFore;
    }

    /**
     * A COLORREF that specifies the foreground color of ipat.
     */
    @Internal
    public void setCvFore( Colorref field_1_cvFore )
    {
        this.field_1_cvFore = field_1_cvFore;
    }

    /**
     * A COLORREF that specifies the background color of ipat.
     */
    @Internal
    public Colorref getCvBack()
    {
        return field_2_cvBack;
    }

    /**
     * A COLORREF that specifies the background color of ipat.
     */
    @Internal
    public void setCvBack( Colorref field_2_cvBack )
    {
        this.field_2_cvBack = field_2_cvBack;
    }

    /**
     * An Ipat that specifies the pattern used for shading.
     */
    @Internal
    public int getIpat()
    {
        return field_3_ipat;
    }

    /**
     * An Ipat that specifies the pattern used for shading.
     */
    @Internal
    public void setIpat( int field_3_ipat )
    {
        this.field_3_ipat = field_3_ipat;
    }

}  // END OF CLASS
