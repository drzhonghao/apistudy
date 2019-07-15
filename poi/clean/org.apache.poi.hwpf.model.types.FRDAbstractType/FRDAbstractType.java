import org.apache.poi.hwpf.model.types.*;


import org.apache.poi.util.Internal;
import org.apache.poi.util.LittleEndian;

/**
 * Footnote Reference Descriptor (FRD).
 * <p>
 * Class and fields descriptions are quoted from Microsoft Office Word 97-2007
 * Binary File Format (.doc) Specification
 * 
 * NOTE: This source is automatically generated please do not modify this file.
 * Either subclass or remove the record in src/types/definitions.
 * 
 * @author Sergey Vladimirov; according to Microsoft Office Word 97-2007 Binary
 *         File Format (.doc) Specification
 */
@Internal
public abstract class FRDAbstractType
{

    protected short field_1_nAuto;

    protected FRDAbstractType()
    {
    }

    protected void fillFields( byte[] data, int offset )
    {
        field_1_nAuto = LittleEndian.getShort( data, 0x0 + offset );
    }

    public void serialize( byte[] data, int offset )
    {
        LittleEndian.putShort( data, 0x0 + offset, field_1_nAuto );
    }

    /**
     * Size of record
     */
    public static int getSize()
    {
        return 0 + 2;
    }

    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append( "[FRD]\n" );
        builder.append( "    .nAuto                = " );
        builder.append( " (" ).append( getNAuto() ).append( " )\n" );

        builder.append( "[/FRD]\n" );
        return builder.toString();
    }

    /**
     * If > 0, the note is an automatically numbered note, otherwise it has a
     * custom mark.
     */
    public short getNAuto()
    {
        return field_1_nAuto;
    }

    /**
     * If > 0, the note is an automatically numbered note, otherwise it has a
     * custom mark.
     */
    public void setNAuto( short field_1_nAuto )
    {
        this.field_1_nAuto = field_1_nAuto;
    }

} // END OF CLASS
