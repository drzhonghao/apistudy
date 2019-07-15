import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.*;



import java.io.IOException;

import org.apache.lucene.index.IndexWriter.IndexReaderWarmer;
import org.apache.lucene.util.InfoStream;

/** 
 * A very simple merged segment warmer that just ensures 
 * data structures are initialized.
 */
public class SimpleMergedSegmentWarmer implements IndexReaderWarmer {
  private final InfoStream infoStream;

  /**
   * Creates a new SimpleMergedSegmentWarmer
   * @param infoStream InfoStream to log statistics about warming.
   */
  public SimpleMergedSegmentWarmer(InfoStream infoStream) {
    this.infoStream = infoStream;
  }
  
  @Override
  public void warm(LeafReader reader) throws IOException {
    long startTime = System.currentTimeMillis();
    int indexedCount = 0;
    int docValuesCount = 0;
    int normsCount = 0;
    for (FieldInfo info : reader.getFieldInfos()) {
      if (info.getIndexOptions() != IndexOptions.NONE) {
        reader.terms(info.name); 
        indexedCount++;
        
        if (info.hasNorms()) {
          reader.getNormValues(info.name);
          normsCount++;
        }
      }

      if (info.getDocValuesType() != DocValuesType.NONE) {
        switch(info.getDocValuesType()) {
          case NUMERIC:
            reader.getNumericDocValues(info.name);
            break;
          case BINARY:
            reader.getBinaryDocValues(info.name);
            break;
          case SORTED:
            reader.getSortedDocValues(info.name);
            break;
          case SORTED_NUMERIC:
            reader.getSortedNumericDocValues(info.name);
            break;
          case SORTED_SET:
            reader.getSortedSetDocValues(info.name);
            break;
          default:
            assert false; // unknown dv type
        }
        docValuesCount++;
      }   
    }
    
    reader.document(0);
    reader.getTermVectors(0);
    
    if (infoStream.isEnabled("SMSW")) {
      infoStream.message("SMSW", 
             "Finished warming segment: " + reader + 
             ", indexed=" + indexedCount + 
             ", docValues=" + docValuesCount +
             ", norms=" + normsCount +
             ", time=" + (System.currentTimeMillis() - startTime));
    }
  }
}
