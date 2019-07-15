import org.apache.poi.hwpf.model.*;


import org.apache.poi.hwpf.model.types.FLDAbstractType;
import org.apache.poi.util.Internal;

/**
 * Class for the FLD structure.
 * 
 * @author Cedric Bosdonnat <cbosdonnat@novell.com>
 */
@Internal
public final class FieldDescriptor extends FLDAbstractType
{
    public static final int FIELD_BEGIN_MARK = 0x13;
    public static final int FIELD_SEPARATOR_MARK = 0x14;
    public static final int FIELD_END_MARK = 0x15;

    public FieldDescriptor( byte[] data )
    {
        fillFields( data, 0 );
    }

    public int getBoundaryType()
    {
        return getCh();
    }

    public int getFieldType()
    {
        if ( getCh() != FIELD_BEGIN_MARK )
            throw new UnsupportedOperationException(
                    "This field is only defined for begin marks." );
        return getFlt();
    }

    public boolean isZombieEmbed()
    {
        if ( getCh() != FIELD_END_MARK )
            throw new UnsupportedOperationException(
                    "This field is only defined for end marks." );
        return isFZombieEmbed();
    }

    public boolean isResultDirty()
    {
        if ( getCh() != FIELD_END_MARK )
            throw new UnsupportedOperationException(
                    "This field is only defined for end marks." );
        return isFResultDirty();
    }

    public boolean isResultEdited()
    {
        if ( getCh() != FIELD_END_MARK )
            throw new UnsupportedOperationException(
                    "This field is only defined for end marks." );
        return isFResultEdited();
    }

    public boolean isLocked()
    {
        if ( getCh() != FIELD_END_MARK )
            throw new UnsupportedOperationException(
                    "This field is only defined for end marks." );
        return isFLocked();
    }

    public boolean isPrivateResult()
    {
        if ( getCh() != FIELD_END_MARK )
            throw new UnsupportedOperationException(
                    "This field is only defined for end marks." );
        return isFPrivateResult();
    }

    public boolean isNested()
    {
        if ( getCh() != FIELD_END_MARK )
            throw new UnsupportedOperationException(
                    "This field is only defined for end marks." );
        return isFNested();
    }

    public boolean isHasSep()
    {
        if ( getCh() != FIELD_END_MARK )
            throw new UnsupportedOperationException(
                    "This field is only defined for end marks." );
        return isFHasSep();
    }
}
