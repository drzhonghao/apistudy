import org.apache.poi.hmef.attribute.MAPIAttribute;
import org.apache.poi.hmef.attribute.*;


import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.poi.hmef.Attachment;
import org.apache.poi.hmef.HMEFMessage;

/**
 * A TNEF Attribute holding MAPI Attributes, which applies to a 
 *  {@link HMEFMessage} or one of its {@link Attachment}s.
 */
public final class TNEFMAPIAttribute extends TNEFAttribute {
   private final List<MAPIAttribute> attributes;
   
   /**
    * Constructs a single new mapi containing attribute from the 
    *  id, type, and the contents of the stream
    */
   protected TNEFMAPIAttribute(int id, int type, InputStream inp) throws IOException {
      super(id, type, inp);
      
      attributes = MAPIAttribute.create(this);
   }

   public List<MAPIAttribute> getMAPIAttributes() {
      return attributes;
   }
   
   public String toString() {
      return "Attribute " + getProperty() + ", type=" + getType() +
             ", " + attributes.size() + " MAPI Attributes"; 
   }
}
