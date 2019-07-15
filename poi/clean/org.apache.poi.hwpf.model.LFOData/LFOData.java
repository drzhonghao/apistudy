import org.apache.poi.hwpf.model.ListFormatOverrideLevel;
import org.apache.poi.hwpf.model.*;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import org.apache.poi.util.Internal;
import org.apache.poi.util.LittleEndian;

/**
 * The LFOData structure contains the Main Document CP of the corresponding LFO,
 * as well as an array of LVL override data.
 * 
 * @author Sergey Vladimirov (vlsergey {at} gmail {dot} com)
 */
@Internal
public class LFOData
{
    private int _cp;

    private ListFormatOverrideLevel[] _rgLfoLvl;

    public LFOData()
    {
        _cp = 0;
        _rgLfoLvl = new ListFormatOverrideLevel[0];
    }

    LFOData( byte[] buf, int startOffset, int cLfolvl )
    {
        int offset = startOffset;

        _cp = LittleEndian.getInt( buf, offset );
        offset += LittleEndian.INT_SIZE;

        _rgLfoLvl = new ListFormatOverrideLevel[cLfolvl];
        for ( int x = 0; x < cLfolvl; x++ )
        {
            _rgLfoLvl[x] = new ListFormatOverrideLevel( buf, offset );
            offset += _rgLfoLvl[x].getSizeInBytes();
        }
    }

    public int getCp()
    {
        return _cp;
    }

    public ListFormatOverrideLevel[] getRgLfoLvl()
    {
        return _rgLfoLvl;
    }

    public int getSizeInBytes()
    {
        int result = 0;
        result += LittleEndian.INT_SIZE;

        for ( ListFormatOverrideLevel lfolvl : _rgLfoLvl )
            result += lfolvl.getSizeInBytes();

        return result;
    }

    void writeTo( ByteArrayOutputStream tableStream ) throws IOException
    {
        LittleEndian.putInt( _cp, tableStream );
        for ( ListFormatOverrideLevel lfolvl : _rgLfoLvl )
        {
            tableStream.write( lfolvl.toByteArray() );
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LFOData lfoData = (LFOData) o;

        if (_cp != lfoData._cp) return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        return Arrays.equals(_rgLfoLvl, lfoData._rgLfoLvl);

    }

    @Override
    public int hashCode() {
        int result = _cp;
        result = 31 * result + Arrays.hashCode(_rgLfoLvl);
        return result;
    }
}
