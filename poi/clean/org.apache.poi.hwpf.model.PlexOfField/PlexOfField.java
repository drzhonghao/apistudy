import org.apache.poi.hwpf.model.GenericPropertyNode;
import org.apache.poi.hwpf.model.FieldDescriptor;
import org.apache.poi.hwpf.model.*;


import java.util.Locale;

import org.apache.poi.util.Internal;

/**
 * Structure describing the Plex for fields (contained plclfd* in the spec).
 * 
 * @author Cedric Bosdonnat <cbosdonnat@novell.com>
 */
@Internal
public class PlexOfField
{

    private final GenericPropertyNode propertyNode;
    private final FieldDescriptor fld;

    @Deprecated
    public PlexOfField( int fcStart, int fcEnd, byte[] data )
    {
        propertyNode = new GenericPropertyNode( fcStart, fcEnd, data );
        fld = new FieldDescriptor( data );
    }

    public PlexOfField( GenericPropertyNode propertyNode )
    {
        this.propertyNode = propertyNode;
        fld = new FieldDescriptor( propertyNode.getBytes() );
    }

    public int getFcStart()
    {
        return propertyNode.getStart();
    }

    public int getFcEnd()
    {
        return propertyNode.getEnd();
    }

    public FieldDescriptor getFld()
    {
        return fld;
    }

    public String toString() {
        return String.format(Locale.ROOT, "[%d, %d) - FLD - 0x%x; 0x%x"
            , getFcStart(), getFcEnd(), fld.getBoundaryType(), fld.getFlt());
    }
}
