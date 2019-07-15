import org.apache.poi.hwpf.model.FibRgLw;
import org.apache.poi.hwpf.model.SubdocumentType;
import org.apache.poi.hwpf.model.*;


import org.apache.poi.hwpf.model.types.FibRgLw97AbstractType;
import org.apache.poi.util.Internal;

/**
 * The FibRgLw97 structure is the third section of the FIB. This contains an
 * array of 4-byte values.
 * <p>
 * Class and fields descriptions are quoted from Microsoft Office Word 97-2007
 * Binary File Format and [MS-DOC] - v20110608 Word (.doc) Binary File Format
 * 
 * @author Sergey Vladimirov; according to Microsoft Office Word 97-2007 Binary
 *         File Format Specification [*.doc] and [MS-DOC] - v20110608 Word
 *         (.doc) Binary File Format
 */
@Internal
public class FibRgLw97 extends FibRgLw97AbstractType implements FibRgLw
{

    public FibRgLw97()
    {
    }

    public FibRgLw97( byte[] std, int offset )
    {
        fillFields( std, offset );
    }

    @SuppressWarnings( "deprecation" )
    public int getSubdocumentTextStreamLength( SubdocumentType subdocumentType )
    {
        switch ( subdocumentType )
        {
        case MAIN:
            return getCcpText();
        case FOOTNOTE:
            return getCcpFtn();
        case HEADER:
            return getCcpHdd();
        case MACRO:
            return field_7_reserved3;
        case ANNOTATION:
            return getCcpAtn();
        case ENDNOTE:
            return getCcpEdn();
        case TEXTBOX:
            return getCcpTxbx();
        case HEADER_TEXTBOX:
            return getCcpHdrTxbx();
        }
        throw new UnsupportedOperationException( "Unsupported: "
                + subdocumentType );
    }

    @SuppressWarnings( "deprecation" )
    public void setSubdocumentTextStreamLength(
            SubdocumentType subdocumentType, int newLength )
    {
        switch ( subdocumentType )
        {
        case MAIN:
            setCcpText( newLength );
            return;
        case FOOTNOTE:
            setCcpFtn( newLength );
            return;
        case HEADER:
            setCcpHdd( newLength );
            return;
        case MACRO:
            field_7_reserved3 = newLength;
            return;
        case ANNOTATION:
            setCcpAtn( newLength );
            return;
        case ENDNOTE:
            setCcpEdn( newLength );
            return;
        case TEXTBOX:
            setCcpTxbx( newLength );
            return;
        case HEADER_TEXTBOX:
            setCcpHdrTxbx( newLength );
            return;
        }
        throw new UnsupportedOperationException( "Unsupported: "
                + subdocumentType );
    }

}
