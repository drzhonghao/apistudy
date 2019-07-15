import org.apache.poi.hwpf.model.*;


import org.apache.poi.hwpf.model.types.LFOLVLBaseAbstractType;

/**
 * The LFOLVL structure contains information that is used to override the
 * formatting information of a corresponding LVL.
 * <p>
 * Class and fields descriptions are quoted from Microsoft Office Word 97-2007
 * Binary File Format and [MS-DOC] - v20110608 Word (.doc) Binary File Format
 * 
 * @author Sergey Vladimirov; according to Microsoft Office Word 97-2007 Binary
 *         File Format Specification [*.doc] and [MS-DOC] - v20110608 Word
 *         (.doc) Binary File Format
 */
class LFOLVLBase extends LFOLVLBaseAbstractType
{
    LFOLVLBase()
    {
    }

    LFOLVLBase( byte[] buf, int offset )
    {
        fillFields( buf, offset );
    }
}
