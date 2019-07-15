import org.apache.cassandra.cql3.*;


import java.util.Locale;
import java.nio.ByteBuffer;

import org.apache.cassandra.db.marshal.UTF8Type;
import org.apache.cassandra.exceptions.SyntaxException;
import org.apache.cassandra.serializers.MarshalException;

/**
 * Identifies a field in a UDT.
 */
public class FieldIdentifier
{
    public final ByteBuffer bytes;

    public FieldIdentifier(ByteBuffer bytes)
    {
        this.bytes = bytes;
    }

    /**
     * Creates a {@code FieldIdentifier} from an unquoted identifier string.
     */
    public static FieldIdentifier forUnquoted(String text)
    {
        return new FieldIdentifier(convert(text.toLowerCase(Locale.US)));
    }

    /**
     * Creates a {@code FieldIdentifier} from a quoted identifier string.
     */
    public static FieldIdentifier forQuoted(String text)
    {
        return new FieldIdentifier(convert(text));
    }

    /**
     * Creates a {@code FieldIdentifier} from an internal string.
     */
    public static FieldIdentifier forInternalString(String text)
    {
        // If we store a field internally, we consider it as quoted, i.e. we preserve
        // whatever case the text has.
        return forQuoted(text);
    }

    private static ByteBuffer convert(String text)
    {
        try
        {
            return UTF8Type.instance.decompose(text);
        }
        catch (MarshalException e)
        {
            throw new SyntaxException(String.format("For field name %s: %s", text, e.getMessage()));
        }
    }

    @Override
    public String toString()
    {
        return UTF8Type.instance.compose(bytes);
    }

    @Override
    public final int hashCode()
    {
        return bytes.hashCode();
    }

    @Override
    public final boolean equals(Object o)
    {
        if(!(o instanceof FieldIdentifier))
            return false;
        FieldIdentifier that = (FieldIdentifier)o;
        return this.bytes.equals(that.bytes);
    }
}
