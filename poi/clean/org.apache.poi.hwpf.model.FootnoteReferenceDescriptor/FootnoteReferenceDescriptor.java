import org.apache.poi.hwpf.model.*;


import org.apache.poi.hwpf.model.types.FRDAbstractType;
import org.apache.poi.util.Internal;

@Internal
public final class FootnoteReferenceDescriptor extends FRDAbstractType
        implements Cloneable
{
    public FootnoteReferenceDescriptor()
    {
    }

    public FootnoteReferenceDescriptor( byte[] data, int offset )
    {
        fillFields( data, offset );
    }

    @Override
    protected FootnoteReferenceDescriptor clone()
    {
        try
        {
            return (FootnoteReferenceDescriptor) super.clone();
        }
        catch ( CloneNotSupportedException e )
        {
            throw new RuntimeException( e );
        }
    }

    @Override
    public boolean equals( Object obj )
    {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( getClass() != obj.getClass() )
            return false;
        FootnoteReferenceDescriptor other = (FootnoteReferenceDescriptor) obj;
        if ( field_1_nAuto != other.field_1_nAuto )
            return false;
        return true;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + field_1_nAuto;
        return result;
    }

    public boolean isEmpty()
    {
        return field_1_nAuto == 0;
    }

    @Override
    public String toString()
    {
        if ( isEmpty() )
            return "[FRD] EMPTY";

        return super.toString();
    }
}
