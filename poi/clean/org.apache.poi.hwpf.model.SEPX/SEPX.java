import org.apache.poi.hwpf.model.PropertyNode;
import org.apache.poi.hwpf.model.SectionDescriptor;
import org.apache.poi.hwpf.model.*;


import org.apache.poi.hwpf.sprm.SectionSprmCompressor;
import org.apache.poi.hwpf.sprm.SectionSprmUncompressor;
import org.apache.poi.hwpf.sprm.SprmBuffer;
import org.apache.poi.hwpf.usermodel.SectionProperties;
import org.apache.poi.util.Internal;

@Internal
public final class SEPX extends PropertyNode<SEPX>
{

    SectionProperties sectionProperties;

    SectionDescriptor _sed;

    public SEPX( SectionDescriptor sed, int start, int end, byte[] grpprl )
    {
        super( start, end, new SprmBuffer( grpprl, 0 ) );
        _sed = sed;
    }

    public byte[] getGrpprl()
    {
        if ( sectionProperties != null )
        {
            byte[] grpprl = SectionSprmCompressor
                    .compressSectionProperty( sectionProperties );
            _buf = new SprmBuffer( grpprl, 0 );
        }

        return ( (SprmBuffer) _buf ).toByteArray();
    }

    public SectionDescriptor getSectionDescriptor()
    {
        return _sed;
    }

    public SectionProperties getSectionProperties()
    {
        if ( sectionProperties == null )
        {
            sectionProperties = SectionSprmUncompressor.uncompressSEP(
                    ( (SprmBuffer) _buf ).toByteArray(), 0 );
        }
        return sectionProperties;
    }

    @Override
    public boolean equals( Object o )
    {
        if (!(o instanceof SEPX)) return false;
        SEPX sepx = (SEPX) o;
        if ( super.equals( o ) )
        {
            return sepx._sed.equals( _sed );
        }
        return false;
    }

    @Override
    public int hashCode() {
        assert false : "hashCode not designed";
        return 42; // any arbitrary constant will do
    }

    public String toString()
    {
        return "SEPX from " + getStart() + " to " + getEnd();
    }
}
