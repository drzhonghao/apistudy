import org.apache.poi.hpsf.examples.*;


import java.io.File;
import java.io.IOException;

import org.apache.poi.hpsf.PropertySetFactory;
import org.apache.poi.hpsf.SummaryInformation;
import org.apache.poi.poifs.eventfilesystem.POIFSReader;
import org.apache.poi.poifs.eventfilesystem.POIFSReaderEvent;
import org.apache.poi.poifs.eventfilesystem.POIFSReaderListener;

/**
 * <p>Sample application showing how to read a OLE 2 document's
 * title. Call it with the document's file name as command line
 * parameter.</p>
 *
 * <p>Explanations can be found in the HPSF HOW-TO.</p>
 */
public final class ReadTitle
{
    private ReadTitle() {}

    /**
     * <p>Runs the example program.</p>
     *
     * @param args Command-line arguments. The first command-line argument must
     * be the name of a POI filesystem to read.
     * @throws IOException if any I/O exception occurs.
     */
    public static void main(final String[] args) throws IOException
    {
        final String filename = args[0];
        POIFSReader r = new POIFSReader();
        r.registerListener(new MyPOIFSReaderListener(), SummaryInformation.DEFAULT_STREAM_NAME);
        r.read(new File(filename));
    }


    static class MyPOIFSReaderListener implements POIFSReaderListener
    {
        @Override
        public void processPOIFSReaderEvent(final POIFSReaderEvent event)
        {
            SummaryInformation si;
            try
            {
                si = (SummaryInformation)
                    PropertySetFactory.create(event.getStream());
            }
            catch (Exception ex)
            {
                throw new RuntimeException
                    ("Property set stream \"" +
                     event.getPath() + event.getName() + "\": " + ex);
            }
            final String title = si.getTitle();
            if (title != null)
                System.out.println("Title: \"" + title + "\"");
            else
                System.out.println("Document has no title.");
        }
    }

}
