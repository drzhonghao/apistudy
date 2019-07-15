import org.apache.poi.hmef.attribute.MAPIAttribute;
import org.apache.poi.hmef.attribute.*;


import java.nio.charset.Charset;

import org.apache.poi.hmef.Attachment;
import org.apache.poi.hmef.HMEFMessage;
import org.apache.poi.hsmf.datatypes.MAPIProperty;
import org.apache.poi.hsmf.datatypes.Types;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;
import org.apache.poi.util.StringUtil;

/**
 * A pure-MAPI attribute holding a String, which applies 
 *  to a {@link HMEFMessage} or one of its {@link Attachment}s.
 */
public final class MAPIStringAttribute extends MAPIAttribute {
   private final static POILogger logger = POILogFactory.getLogger(MAPIStringAttribute.class);
   private static final String CODEPAGE = "CP1252";
   private final String data;
   
   public MAPIStringAttribute(MAPIProperty property, int type, byte[] data) {
      super(property, type, data);
      
      String tmpData = null;
      if(type == Types.ASCII_STRING.getId()) {
         tmpData = new String(data, Charset.forName(CODEPAGE));
      } else if(type == Types.UNICODE_STRING.getId()) {
         tmpData = StringUtil.getFromUnicodeLE(data);
      } else {
         throw new IllegalArgumentException("Not a string type " + type);
      }
      
      // Strip off the null terminator if present
      if(tmpData.endsWith("\0")) {
         tmpData = tmpData.substring(0, tmpData.length()-1);
      }
      this.data = tmpData;
   }
   
   public String getDataString() {
      return data;
   }
   
   public String toString() {
      return getProperty() + " " + data;
   }
   
   /**
    * Returns the string of a Attribute, converting as appropriate
    */
   public static String getAsString(MAPIAttribute attr) {
      if(attr == null) {
         return null;
      }
      if(attr instanceof MAPIStringAttribute) {
         return ((MAPIStringAttribute)attr).getDataString();
      }
      if(attr instanceof MAPIRtfAttribute) {
         return ((MAPIRtfAttribute)attr).getDataString();
      }
      
      logger.log(POILogger.WARN, "Warning, non string property found: " + attr);
      return null;
  }
}
