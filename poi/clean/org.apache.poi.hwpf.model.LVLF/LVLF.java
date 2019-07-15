import org.apache.poi.hwpf.model.*;


import org.apache.poi.hwpf.model.types.LVLFAbstractType;
import org.apache.poi.util.Internal;

/**
 * The LVLF structure contains formatting properties for an individual level in
 * a list
 * 
 * @author Sergey Vladimirov; according to Microsoft Office Word 97-2007 Binary
 *         File Format Specification [*.doc] and [MS-DOC] - v20110608 Word
 *         (.doc) Binary File Format
 */
@Internal
class LVLF extends LVLFAbstractType
{

    public LVLF()
    {
    }

    public LVLF( byte[] std, int offset )
    {
        fillFields( std, offset );
    }

}
