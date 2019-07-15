import org.apache.poi.ddf.EscherSimpleProperty;
import org.apache.poi.ddf.*;


import org.apache.poi.util.HexDump;

/**
 * A color property.
 */
public class EscherRGBProperty
        extends EscherSimpleProperty
{

    public EscherRGBProperty( short propertyNumber, int rgbColor )
    {
        super( propertyNumber, rgbColor );
    }

    /**
     * @return the rgb color as int value
     */
    public int getRgbColor()
    {
        return getPropertyValue();
    }

    /**
     * @return the red part
     */
    public byte getRed()
    {
        return (byte) ( getRgbColor() & 0xFF );
    }

    /**
     * @return the green part
     */
    public byte getGreen()
    {
        return (byte) ( (getRgbColor() >> 8) & 0xFF );
    }

    /**
     * @return the blue part
     */
    public byte getBlue()
    {
        return (byte) ( (getRgbColor() >> 16) & 0xFF );
    }

    @Override
    public String toXml(String tab){
        StringBuilder builder = new StringBuilder();
        builder.append(tab).append("<").append(getClass().getSimpleName()).append(" id=\"0x").append(HexDump.toHex(getId()))
                .append("\" name=\"").append(getName()).append("\" blipId=\"")
                .append(isBlipId()).append("\" value=\"0x").append(HexDump.toHex(getRgbColor())).append("\"/>");
        return builder.toString();
    }
}
