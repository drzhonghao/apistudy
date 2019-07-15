import org.apache.poi.hwpf.model.*;


import org.apache.poi.hwpf.model.types.LSTFAbstractType;

/**
 * The LSTF structure contains formatting properties that apply to an entire
 * list.
 * <p>
 * Class and fields descriptions are quoted from Microsoft Office Word 97-2007
 * Binary File Format and [MS-DOC] - v20110608 Word (.doc) Binary File Format
 * 
 * @author Sergey Vladimirov; according to Microsoft Office Word 97-2007 Binary
 *         File Format Specification [*.doc] and [MS-DOC] - v20110608 Word
 *         (.doc) Binary File Format
 */
class LSTF extends LSTFAbstractType
{
    LSTF()
    {
    }

    LSTF( byte[] buf, int offset )
    {
        super();
        fillFields( buf, offset );
    }

}
