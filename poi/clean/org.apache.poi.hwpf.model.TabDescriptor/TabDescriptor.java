import org.apache.poi.hwpf.model.*;


import org.apache.poi.hwpf.model.types.TBDAbstractType;
import org.apache.poi.hwpf.usermodel.ParagraphProperties;

/**
 * Tab descriptor. Part of {@link ParagraphProperties}.
 * 
 * @author vlsergey
 */
public class TabDescriptor extends TBDAbstractType
{

    public TabDescriptor()
    {
    }

    public TabDescriptor( byte[] bytes, int offset )
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
