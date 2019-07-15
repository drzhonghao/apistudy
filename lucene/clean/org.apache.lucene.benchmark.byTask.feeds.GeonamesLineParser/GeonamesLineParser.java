import org.apache.lucene.benchmark.byTask.feeds.LineDocSource;
import org.apache.lucene.benchmark.byTask.feeds.DocData;
import org.apache.lucene.benchmark.byTask.feeds.*;



/**
 * A line parser for Geonames.org data.
 * See <a href="http://download.geonames.org/export/dump/readme.txt">'geoname' table</a>.
 * Requires {@link SpatialDocMaker}.
 */
public class GeonamesLineParser extends LineDocSource.LineParser {

  /** This header will be ignored; the geonames format is fixed and doesn't have a header line. */
  public GeonamesLineParser(String[] header) {
    super(header);
  }

  @Override
  public void parseLine(DocData docData, String line) {
    String[] parts = line.split("\\t", 7);//no more than first 6 fields needed

    //    Sample data line:
    // 3578267, Morne du Vitet, Morne du Vitet, 17.88333, -62.8, ...
    // ID, Name, Alternate name (unused), Lat, Lon, ...

    docData.setID(Integer.parseInt(parts[0]));//note: overwrites ID assigned by LineDocSource
    docData.setName(parts[1]);
    String latitude = parts[4];
    String longitude = parts[5];
    docData.setBody("POINT("+longitude+" "+latitude+")");//WKT is x y order
  }
}
