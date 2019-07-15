import org.apache.poi.ddf.EscherProperty;
import org.apache.poi.ddf.EscherProperties;
import org.apache.poi.ddf.*;


import java.util.Arrays;

import org.apache.poi.util.HexDump;
import org.apache.poi.util.LittleEndian;

/**
 * A complex property differs from a simple property in that the data can not fit inside a 32 bit
 * integer.  See the specification for more detailed information regarding exactly what is
 * stored here.
 */
public class EscherComplexProperty extends EscherProperty {
    private byte[] _complexData;

    /**
     * Create a complex property using the property id and a byte array containing the complex
     * data value.
     *
     * @param id          The id consists of the property number, a flag indicating whether this is a blip id and a flag
     *                    indicating that this is a complex property.
     * @param complexData The value of this property.
     */
    public EscherComplexProperty(short id, byte[] complexData) {
        super(id);
        if (complexData == null) {
            throw new IllegalArgumentException("complexData can't be null");
        }
        _complexData = complexData.clone();
    }

    /**
     * Create a complex property using the property number, a flag to indicate whether this is a
     * blip reference and the complex property data.
     *
     * @param propertyNumber The property number
     * @param isBlipId       Whether this is a blip id.  Should be false.
     * @param complexData    The value of this complex property.
     */
    public EscherComplexProperty(short propertyNumber, boolean isBlipId, byte[] complexData) {
        super(propertyNumber, true, isBlipId);
        if (complexData == null) {
            throw new IllegalArgumentException("complexData can't be null");
        }
        _complexData = complexData.clone();
    }

    /**
     * Serializes the simple part of this property.  i.e. the first 6 bytes.
     */
    @Override
    public int serializeSimplePart(byte[] data, int pos) {
        LittleEndian.putShort(data, pos, getId());
        LittleEndian.putInt(data, pos + 2, _complexData.length);
        return 6;
    }

    /**
     * Serializes the complex part of this property
     *
     * @param data The data array to serialize to
     * @param pos  The offset within data to start serializing to.
     * @return The number of bytes serialized.
     */
    @Override
    public int serializeComplexPart(byte[] data, int pos) {
        System.arraycopy(_complexData, 0, data, pos, _complexData.length);
        return _complexData.length;
    }

    /**
     * Get the complex data value.
     *
     * @return the complex bytes
     */
    public byte[] getComplexData() {
        return _complexData;
    }

    protected void setComplexData(byte[] _complexData) {
        this._complexData = _complexData;
    }

    /**
     * Determine whether this property is equal to another property.
     *
     * @param o The object to compare to.
     * @return True if the objects are equal.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof EscherComplexProperty)) {
            return false;
        }

        EscherComplexProperty escherComplexProperty = (EscherComplexProperty) o;

        return Arrays.equals(_complexData, escherComplexProperty._complexData);

    }

    /**
     * Calculates the number of bytes required to serialize this property.
     *
     * @return Number of bytes
     */
    @Override
    public int getPropertySize() {
        return 6 + _complexData.length;
    }

    @Override
    public int hashCode() {
        return getId() * 11;
    }

    /**
     * Retrieves the string representation for this property.
     */
    @Override
    public String toString() {
        String dataStr = HexDump.toHex( _complexData, 32);

        return "propNum: " + getPropertyNumber()
                + ", propName: " + EscherProperties.getPropertyName( getPropertyNumber() )
                + ", complex: " + isComplex()
                + ", blipId: " + isBlipId()
                + ", data: " + System.getProperty("line.separator") + dataStr;
    }

    @Override
    public String toXml(String tab){
        return tab + "<" + getClass().getSimpleName() + " id=\"0x" + HexDump.toHex(getId()) +
                "\" name=\"" + getName() + "\" blipId=\"" +
                isBlipId() + "\">\n" +
                tab + "</" + getClass().getSimpleName() + ">";
        //builder.append("\t").append(tab).append(dataStr);
    }
}
