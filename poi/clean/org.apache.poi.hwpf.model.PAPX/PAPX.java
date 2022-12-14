import org.apache.poi.hwpf.model.ParagraphHeight;
import org.apache.poi.hwpf.model.StyleSheet;
import org.apache.poi.hwpf.model.*;



import org.apache.poi.hwpf.sprm.ParagraphSprmUncompressor;
import org.apache.poi.hwpf.sprm.SprmBuffer;
import org.apache.poi.hwpf.sprm.SprmOperation;
import org.apache.poi.hwpf.usermodel.ParagraphProperties;
import org.apache.poi.util.Internal;
import org.apache.poi.util.LittleEndian;

/**
 * DANGER - works in bytes!
 *
 * Make sure you call getStart() / getEnd() when you want characters
 *    (normal use), but getStartByte() / getEndByte() when you're
 *    reading in / writing out!
 *
 * @author Ryan Ackley
 */
@Internal
@SuppressWarnings( "deprecation" )
public final class PAPX extends BytePropertyNode<PAPX> {

    private ParagraphHeight _phe;

    public PAPX( int charStart, int charEnd, byte[] papx, ParagraphHeight phe,
            byte[] dataStream )
    {
        super( charStart, charEnd, new SprmBuffer( papx, 2 ) );
        _phe = phe;
        SprmBuffer buf = findHuge( new SprmBuffer( papx, 2 ), dataStream );
        if ( buf != null )
            _buf = buf;
    }

    public PAPX( int charStart, int charEnd, SprmBuffer buf )
    {
        super( charStart, charEnd, buf );
        _phe = new ParagraphHeight();
    }

    private SprmBuffer findHuge(SprmBuffer buf, byte[] datastream)
    {
        byte[] grpprl = buf.toByteArray();
        if(grpprl.length==8 && datastream!=null) // then check for sprmPHugePapx
        {
            SprmOperation sprm = new SprmOperation(grpprl, 2);
            if ((sprm.getOperation()==0x45 || sprm.getOperation()==0x46)
                    && sprm.getSizeCode() == 3)
            {
                int hugeGrpprlOffset = sprm.getOperand();
                if(hugeGrpprlOffset+1 < datastream.length)
                {
                    int grpprlSize = LittleEndian.getShort(datastream, hugeGrpprlOffset);
                    if( hugeGrpprlOffset+grpprlSize < datastream.length)
                    {
                        byte[] hugeGrpprl = new byte[grpprlSize + 2];
                        // copy original istd into huge Grpprl
                        hugeGrpprl[0] = grpprl[0]; hugeGrpprl[1] = grpprl[1];
                        // copy Grpprl from dataStream
                        System.arraycopy(datastream, hugeGrpprlOffset + 2, hugeGrpprl, 2,
                                                         grpprlSize);
                        return new SprmBuffer(hugeGrpprl, 2);
                    }
                }
            }
        }
        return null;
    }


    public ParagraphHeight getParagraphHeight()
    {
        return _phe;
    }

    public byte[] getGrpprl()
    {
        if (_buf == null)
            return new byte[0];

        return ((SprmBuffer)_buf).toByteArray();
    }

    public short getIstd()
    {
        if ( _buf == null )
            return 0;
    
        byte[] buf = getGrpprl();
        if (buf.length == 0)
        {
            return 0;
        }
        if (buf.length == 1)
        {
            return LittleEndian.getUByte(buf, 0);
        }
        return LittleEndian.getShort(buf);
    }

    public SprmBuffer getSprmBuf()
    {
        return (SprmBuffer)_buf;
    }

    @Deprecated
    @Internal
    public ParagraphProperties getParagraphProperties(StyleSheet ss)
    {
        if(ss == null) {
            // TODO Fix up for Word 6/95
            return new ParagraphProperties();
        }
            
        short istd = getIstd();
        ParagraphProperties baseStyle = ss.getParagraphStyle(istd);
        return ParagraphSprmUncompressor.uncompressPAP(baseStyle, getGrpprl(), 2);
    }

    @Override
    public boolean equals(Object o)
    {
        if (super.equals(o))
        {
            return _phe.equals(((PAPX)o)._phe);
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
        return "PAPX from " + getStart() + " to " + getEnd() + " (in bytes "
                            + getStartBytes() + " to " + getEndBytes() + ")";
    }
}
