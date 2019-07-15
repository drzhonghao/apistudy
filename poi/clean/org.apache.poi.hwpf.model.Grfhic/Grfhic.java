import org.apache.poi.hwpf.model.*;


import org.apache.poi.hwpf.model.types.GrfhicAbstractType;
import org.apache.poi.util.Internal;

/**
 * The grfhic structure is a set of HTML incompatibility flags that specify the
 * HTML incompatibilities of a list structure. The values specify possible
 * incompatibilities between an LVL or LVLF and HTML lists. The values do not
 * define list properties.
 * <p>
 * Class and fields descriptions are quoted from [MS-DOC] -- v20110315 Word
 * (.doc) Binary File Format specification
 * <p>
 * This class is internal. It content or properties may change without notice
 * due to changes in our knowledge of internal Microsoft Word binary structures.
 * 
 * @author Sergey Vladimirov; according to [MS-DOC] -- v20110315 Word (.doc)
 *         Binary File Format specification
 */
@Internal
public class Grfhic extends GrfhicAbstractType
{

    public Grfhic()
    {
    }

    public Grfhic( byte[] bytes, int offset )
    {
        fillFields( bytes, offset );
    }

    public byte[] toByteArray()
    {
        byte[] buf = new byte[getSize()];
        serialize( buf, 0 );
        return buf;
    }

}
