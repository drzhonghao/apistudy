import org.apache.poi.hmef.attribute.TNEFAttribute;
import org.apache.poi.hmef.attribute.TNEFProperty;
import org.apache.poi.hmef.attribute.*;


import java.io.IOException;
import java.io.InputStream;

import org.apache.poi.hmef.Attachment;
import org.apache.poi.hmef.HMEFMessage;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;
import org.apache.poi.util.StringUtil;

/**
 * A String attribute which applies to a {@link HMEFMessage}
 *  or one of its {@link Attachment}s.
 */
public final class TNEFStringAttribute extends TNEFAttribute {
   private final static POILogger logger = POILogFactory.getLogger(TNEFStringAttribute.class);
   private String data;
   
   /**
    * Constructs a single new string attribute from the id, type,
    *  and the contents of the stream
    */
   protected TNEFStringAttribute(int id, int type, InputStream inp) throws IOException {
      super(id, type, inp);
      
      String tmpData = null;
      byte[] data = getData();
      if(getType() == TNEFProperty.TYPE_TEXT) {
         tmpData = StringUtil.getFromUnicodeLE(data);
      } else {
         tmpData = StringUtil.getFromCompressedUnicode(
              data, 0, data.length
         );
      }
      
      // Strip off the null terminator if present
      if(tmpData.endsWith("\0")) {
         tmpData = tmpData.substring(0, tmpData.length()-1);
      }
      this.data = tmpData;
   }

   public String getString() {
      return this.data;
   }
   
   public String toString() {
      return "Attribute " + getProperty() + ", type=" + getType() +
             ", data=" + getString(); 
   }
   
   /**
    * Returns the string of a Attribute, converting as appropriate
    */
   public static String getAsString(TNEFAttribute attr) {
      if(attr == null) {
         return null;
      }
      if(attr instanceof TNEFStringAttribute) {
         return ((TNEFStringAttribute)attr).getString();
      }
      
      logger.log(POILogger.WARN, "Warning, non string property found: " + attr);
      return null;
  }
}
