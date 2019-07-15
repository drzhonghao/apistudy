import org.apache.poi.hwpf.model.*;


import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.poi.hwpf.model.types.DOPAbstractType;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.Internal;
import org.apache.poi.util.LittleEndian;

/**
 * Comment me
 * 
 * @author Ryan Ackley
 */
@Internal
public final class DocumentProperties extends DOPAbstractType
{

    //arbitrarily selected; may need to increase
    private static final int MAX_RECORD_LENGTH = 100_000;

    private byte[] _preserved;

    /**
     * @deprecated Use {@link #DocumentProperties(byte[],int,int)} instead
     */
    public DocumentProperties( byte[] tableStream, int offset )
    {
        this( tableStream, offset, DOPAbstractType.getSize() );
    }

    public DocumentProperties( byte[] tableStream, int offset, int length )
    {
        super.fillFields( tableStream, offset );

        final int supportedSize = DOPAbstractType.getSize();
        if ( length != supportedSize )
        {
            this._preserved = LittleEndian.getByteArray( tableStream, offset
                    + supportedSize, length - supportedSize, MAX_RECORD_LENGTH );
        }
        else
        {
            _preserved = new byte[0];
        }
    }

    @Override
    public void serialize( byte[] data, int offset )
    {
        super.serialize( data, offset );
    }

    public void writeTo( ByteArrayOutputStream tableStream ) throws IOException
    {
        byte[] supported = new byte[getSize()];
        serialize( supported, 0 );

        tableStream.write( supported );
        tableStream.write( _preserved );
    }
}
