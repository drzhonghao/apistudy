import org.apache.poi.hwpf.model.GenericPropertyNode;
import org.apache.poi.hwpf.model.CHPX;
import org.apache.poi.hwpf.model.*;


import org.apache.poi.poifs.common.POIFSConstants;
import org.apache.poi.util.Internal;
import org.apache.poi.util.LittleEndian;

/**
 * This class holds all of the character formatting 
 *  properties from Old (Word 6 / Word 95) documents.
 * Unlike with Word 97+, it all gets held in the
 *  same stream.
 * In common with the rest of the old support, it 
 *  is read only
 */
@Internal
public final class OldCHPBinTable extends CHPBinTable
{
  /**
   * Constructor used to read an old-style binTable
   *  in from a Word document.
   *
   * @param documentStream
   * @param offset
   * @param size
   * @param fcMin
   */
  public OldCHPBinTable(byte[] documentStream, int offset,
                     int size, int fcMin, OldTextPieceTable tpt)
  {
    PlexOfCps binTable = new PlexOfCps(documentStream, offset, size, 2);

    int length = binTable.length();
    for (int x = 0; x < length; x++)
    {
      GenericPropertyNode node = binTable.getProperty(x);

      int pageNum = LittleEndian.getUShort(node.getBytes());
      int pageOffset = POIFSConstants.SMALLER_BIG_BLOCK_SIZE * pageNum;

      CHPFormattedDiskPage cfkp = new CHPFormattedDiskPage(documentStream,
        pageOffset, tpt);

            for ( CHPX chpx : cfkp.getCHPXs() )
            {
                if ( chpx != null )
                    _textRuns.add( chpx );
            }
    }
    _textRuns.sort(PropertyNode.StartComparator.instance);
  }
}
