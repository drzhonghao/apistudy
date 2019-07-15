import org.apache.poi.ddf.EscherSimpleProperty;
import org.apache.poi.ddf.*;


import org.apache.poi.util.HexDump;

/**
 * Represents a boolean property.  The actual utility of this property is in doubt because many
 * of the properties marked as boolean seem to actually contain special values.  In other words
 * they're not true booleans.
 *
 * @see EscherSimpleProperty
 * @see EscherProperty
 */
public class EscherBoolProperty
        extends EscherSimpleProperty
{
    /**
     * Create an instance of an escher boolean property.
     *
     * @param propertyNumber The property number (or id)
     * @param value      The 32 bit value of this bool property
     */
    public EscherBoolProperty( short propertyNumber, int value )
    {
        super(propertyNumber, value);
    }

    /**
     * Whether this boolean property is true
     * 
     * @return the boolean property value
     */
    public boolean isTrue()
    {
        return getPropertyValue() != 0;
    }

//    public String toString()
//    {
//        return "propNum: " + getPropertyNumber()
//                + ", complex: " + isComplex()
//                + ", blipId: " + isBlipId()
//                + ", value: " + (getValue() != 0);
//    }

    @Override
    public String toXml(String tab){
        StringBuilder builder = new StringBuilder();
        builder.append(tab).append("<").append(getClass().getSimpleName()).append(" id=\"0x").append(HexDump.toHex(getId()))
                .append("\" name=\"").append(getName()).append("\" simpleValue=\"").append(getPropertyValue()).append("\" blipId=\"")
                .append(isBlipId()).append("\" value=\"").append(isTrue()).append("\"").append("/>");
        return builder.toString();
    }
}
