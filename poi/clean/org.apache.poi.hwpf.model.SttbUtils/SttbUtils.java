import org.apache.poi.hwpf.model.Sttb;
import org.apache.poi.hwpf.model.*;


import java.io.IOException;
import java.io.OutputStream;

import org.apache.poi.util.Internal;

/**
 * Utils class for storing and reading "STring TaBle stored in File"
 * 
 * @author Sergey Vladimirov (vlsergey {at} gmail {dot} com)
 */
@Internal
class SttbUtils
{

    private static final int CDATA_SIZE_STTB_SAVED_BY = 2; // bytes

    private static final int CDATA_SIZE_STTBF_BKMK = 2; // bytes

    private static final int CDATA_SIZE_STTBF_R_MARK = 2; // bytes

    static String[] readSttbfBkmk( byte[] buffer, int startOffset )
    {
        return new Sttb( CDATA_SIZE_STTBF_BKMK, buffer, startOffset ).getData();
    }

    static String[] readSttbfRMark( byte[] buffer, int startOffset )
    {
        return new Sttb( CDATA_SIZE_STTBF_R_MARK, buffer, startOffset )
                .getData();
    }

    static String[] readSttbSavedBy( byte[] buffer, int startOffset )
    {
        return new Sttb( CDATA_SIZE_STTB_SAVED_BY, buffer, startOffset )
                .getData();
    }

    static void writeSttbfBkmk( String[] data, OutputStream tableStream )
            throws IOException
    {
        tableStream.write( new Sttb( CDATA_SIZE_STTBF_BKMK, data ).serialize() );
    }

    static void writeSttbfRMark( String[] data, OutputStream tableStream )
            throws IOException
    {
        tableStream.write( new Sttb( CDATA_SIZE_STTBF_R_MARK, data ).serialize() );
    }

    static void writeSttbSavedBy( String[] data, OutputStream tableStream )
            throws IOException
    {
        tableStream.write( new Sttb( CDATA_SIZE_STTB_SAVED_BY, data ).serialize() );
    }

}
