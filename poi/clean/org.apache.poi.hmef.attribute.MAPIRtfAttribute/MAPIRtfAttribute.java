import org.apache.poi.hmef.attribute.MAPIAttribute;
import org.apache.poi.hmef.attribute.*;


import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.apache.poi.hmef.Attachment;
import org.apache.poi.hmef.CompressedRTF;
import org.apache.poi.hmef.HMEFMessage;
import org.apache.poi.hsmf.datatypes.MAPIProperty;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.StringUtil;

/**
 * A pure-MAPI attribute holding RTF (compressed or not), which applies 
 *  to a {@link HMEFMessage} or one of its {@link Attachment}s.
 */
public final class MAPIRtfAttribute extends MAPIAttribute {

   //arbitrarily selected; may need to increase
   private static final int MAX_RECORD_LENGTH = 1_000_000;

   private final byte[] decompressed;
   private final String data;
   
   public MAPIRtfAttribute(MAPIProperty property, int type, byte[] data) throws IOException {
      super(property, type, data);
      
      // Decompress it, removing any trailing padding as needed
      CompressedRTF rtf = new CompressedRTF();
      byte[] tmp = rtf.decompress(new ByteArrayInputStream(data));
      if(tmp.length > rtf.getDeCompressedSize()) {
         this.decompressed = IOUtils.safelyAllocate(rtf.getDeCompressedSize(), MAX_RECORD_LENGTH);
         System.arraycopy(tmp, 0, decompressed, 0, decompressed.length);
      } else {
         this.decompressed = tmp;
      }
      
      // Turn the RTF data into a more useful string
      this.data = StringUtil.getFromCompressedUnicode(decompressed, 0, decompressed.length);
   }
   
   /**
    * Returns the original, compressed RTF
    */
   public byte[] getRawData() {
      return super.getData();
   }
   
   /**
    * Returns the raw uncompressed RTF data
    */
   public byte[] getData() {
      return decompressed;
   }
   
   /**
    * Returns the uncompressed RTF as a string
    */
   public String getDataString() {
      return data;
   }
   
   public String toString() {
      return getProperty() + " " + data;
   }
}
