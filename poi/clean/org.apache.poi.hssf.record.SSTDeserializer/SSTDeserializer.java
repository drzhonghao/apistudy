import org.apache.poi.hssf.record.RecordInputStream;
import org.apache.poi.hssf.record.*;


import org.apache.poi.hssf.record.common.UnicodeString;
import org.apache.poi.util.IntMapper;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;

/**
 * Handles the task of deserializing a SST string.  The two main entry points are
 *
 * @author Glen Stampoultzis (glens at apache.org)
 * @author Jason Height (jheight at apache.org)
 */
class SSTDeserializer
{
	private static POILogger logger = POILogFactory.getLogger(SSTDeserializer.class);
    private IntMapper<UnicodeString> strings;

    public SSTDeserializer( IntMapper<UnicodeString> strings )
    {
        this.strings = strings;
    }

    /**
     * This is the starting point where strings are constructed.  Note that
     * strings may span across multiple continuations. Read the SST record
     * carefully before beginning to hack.
     */
    public void manufactureStrings( int stringCount, RecordInputStream in )
    {
      for (int i=0;i<stringCount;i++) {
         // Extract exactly the count of strings from the SST record.
         UnicodeString str;
         if(in.available() == 0 && ! in.hasNextRecord()) {
        	 logger.log( POILogger.ERROR, "Ran out of data before creating all the strings! String at index " + i + "");
            str = new UnicodeString("");
         } else {
            str = new UnicodeString(in);
         }
         addToStringTable( strings, str );
      }
    }

    static public void addToStringTable( IntMapper<UnicodeString> strings, UnicodeString string )
    {
      strings.add(string);
    }
}
