import org.apache.poi.hpsf.Variant;
import org.apache.poi.hpsf.*;


import org.apache.poi.util.HexDump;

/**
 * <p>This exception is thrown if HPSF encounters a variant type that isn't
 * supported yet. Although a variant type is unsupported the value can still be
 * retrieved using the {@link VariantTypeException#getValue} method.</p>
 * 
 * <p>Obviously this class should disappear some day.</p>
 */
public abstract class UnsupportedVariantTypeException
extends VariantTypeException
{

    /**
     * <p>Constructor.</p>
     * 
     * @param variantType The unsupported variant type
     * @param value The value who's variant type is not yet supported
     */
    public UnsupportedVariantTypeException(final long variantType,
                                           final Object value)
    {
        super(variantType, value,
              "HPSF does not yet support the variant type " + variantType + 
              " (" + Variant.getVariantName(variantType) + ", " +
              HexDump.toHex(variantType) + "). If you want support for " +
              "this variant type in one of the next POI releases please " +
              "submit a request for enhancement (RFE) to " +
              "<http://issues.apache.org/bugzilla/>! Thank you!");
    }



}
