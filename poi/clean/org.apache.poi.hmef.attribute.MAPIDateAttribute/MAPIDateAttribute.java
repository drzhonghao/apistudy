import org.apache.poi.hmef.attribute.MAPIAttribute;
import org.apache.poi.hmef.attribute.*;


import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.apache.poi.hmef.Attachment;
import org.apache.poi.hmef.HMEFMessage;
import org.apache.poi.hpsf.Filetime;
import org.apache.poi.hsmf.datatypes.MAPIProperty;
import org.apache.poi.util.LittleEndian;
import org.apache.poi.util.LocaleUtil;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;

/**
 * A pure-MAPI attribute holding a Date, which applies 
 * to a {@link HMEFMessage} or one of its {@link Attachment}s.
 * 
 * Timestamps are usually given in UTC.
 * 
 * @see <a href="https://msdn.microsoft.com/en-us/library/cc433482(v=exchg.80).aspx">[MS-OXOMSG]: Email Object Protocol</a>
 * @see <a href="https://msdn.microsoft.com/en-us/library/cc433490(v=exchg.80).aspx">[MS-OXPROPS]: Exchange Server Protocols Master Property List</a>
 */
public final class MAPIDateAttribute extends MAPIAttribute {
   private final static POILogger logger = POILogFactory.getLogger(MAPIDateAttribute.class);
   private Date data;
   
   /**
    * Constructs a single new date attribute from the id, type,
    *  and the contents of the stream
    */
   protected MAPIDateAttribute(MAPIProperty property, int type, byte[] data) {
      super(property, type, data);
      
      // The value is a 64 bit Windows Filetime
      this.data = Filetime.filetimeToDate(
            LittleEndian.getLong(data, 0)
      );
   }

   public Date getDate() {
      return this.data;
   }
   
   public String toString() {
       DateFormatSymbols dfs = DateFormatSymbols.getInstance(Locale.ROOT);
       DateFormat df = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", dfs);
       df.setTimeZone(LocaleUtil.TIMEZONE_UTC);
       return getProperty() + " " + df.format(data);
   }
   
   /**
    * Returns the Date of a Attribute, converting as appropriate
    */
   public static Date getAsDate(MAPIAttribute attr) {
      if(attr == null) {
         return null;
      }
      if(attr instanceof MAPIDateAttribute) {
         return ((MAPIDateAttribute)attr).getDate();
      }
      
      logger.log(POILogger.WARN, "Warning, non date property found: " + attr);
      return null;
  }
}
