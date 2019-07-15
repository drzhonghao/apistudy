import org.apache.poi.hwpf.model.GenericPropertyNode;
import org.apache.poi.hwpf.model.*;


import org.apache.poi.util.IOUtils;
import org.apache.poi.util.Internal;
import org.apache.poi.util.LittleEndian;

/**
 * This class holds all of the section formatting 
 *  properties from Old (Word 6 / Word 95) documents.
 * Unlike with Word 97+, it all gets held in the
 *  same stream.
 * In common with the rest of the old support, it 
 *  is read only
 */
@Internal
public final class OldSectionTable extends SectionTable
{
    /**
     * @deprecated Use {@link #OldSectionTable(byte[],int,int)} instead
     */
    @Deprecated
    public OldSectionTable( byte[] documentStream, int offset, int size,
            int fcMin, TextPieceTable tpt )
    {
        this( documentStream, offset, size );
    }

    public OldSectionTable( byte[] documentStream, int offset, int size )
    {
    PlexOfCps sedPlex = new PlexOfCps( documentStream, offset, size, 12 );

    int length = sedPlex.length();

    for (int x = 0; x < length; x++)
    {
      GenericPropertyNode node = sedPlex.getProperty(x);
      SectionDescriptor sed = new SectionDescriptor(node.getBytes(), 0);

      int fileOffset = sed.getFc();
      int startAt = node.getStart();
      int endAt = node.getEnd();

      SEPX sepx;
      // check for the optimization
      if (fileOffset == 0xffffffff)
      {
        sepx = new SEPX(sed, startAt, endAt, new byte[0]);
      }
      else
      {
        // The first short at the offset is the size of the grpprl.
        int sepxSize = LittleEndian.getShort(documentStream, fileOffset);
        // Because we don't properly know about all the details of the old
        //  section properties, and we're trying to decode them as if they
        //  were the new ones, we sometimes "need" more data than we have.
        // As a workaround, have a few extra 0 bytes on the end!
        byte[] buf = IOUtils.safelyAllocate(sepxSize+2, Short.MAX_VALUE+2);
        fileOffset += LittleEndian.SHORT_SIZE;
        System.arraycopy(documentStream, fileOffset, buf, 0, buf.length);
        sepx = new SEPX(sed, startAt, endAt, buf);
      }

            _sections.add( sepx );
    }
    _sections.sort(PropertyNode.StartComparator.instance);
  }
}
