import org.apache.poi.poifs.filesystem.POIFSDocumentPath;
import org.apache.poi.poifs.filesystem.*;


/**
 * Class DocumentDescriptor
 *
 * @author Marc Johnson (mjohnson at apache dot org)
 * @version %I%, %G%
 */

public class DocumentDescriptor
{
    private POIFSDocumentPath path;
    private String            name;
    private int               hashcode;

    /**
     * Trivial constructor
     *
     * @param path the Document path
     * @param name the Document name
     */

    public DocumentDescriptor(final POIFSDocumentPath path, final String name)
    {
        if (path == null)
        {
            throw new NullPointerException("path must not be null");
        }
        if (name == null)
        {
            throw new NullPointerException("name must not be null");
        }
        if (name.length() == 0)
        {
            throw new IllegalArgumentException("name cannot be empty");
        }
        this.path = path;
        this.name = name;
    }

    /**
     * equality. Two DocumentDescriptor instances are equal if they
     * have equal paths and names
     *
     * @param o the object we're checking equality for
     *
     * @return true if the object is equal to this object
     */

    public boolean equals(final Object o)
    {
        boolean rval = false;

        if ((o != null) && (o.getClass() == this.getClass()))
        {
            if (this == o)
            {
                rval = true;
            }
            else
            {
                DocumentDescriptor descriptor = ( DocumentDescriptor ) o;

                rval = this.path.equals(descriptor.path)
                       && this.name.equals(descriptor.name);
            }
        }
        return rval;
    }

    /**
     * calculate and return the hashcode
     *
     * @return hashcode
     */

    public int hashCode()
    {
        if (hashcode == 0)
        {
            hashcode = path.hashCode() ^ name.hashCode();
        }
        return hashcode;
    }

    public String toString()
    {
        StringBuffer buffer = new StringBuffer(40 * (path.length() + 1));

        for (int j = 0; j < path.length(); j++)
        {
            buffer.append(path.getComponent(j)).append("/");
        }
        buffer.append(name);
        return buffer.toString();
    }
}   // end public class DocumentDescriptor

