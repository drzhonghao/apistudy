import org.apache.lucene.index.SegmentInfo;
import org.apache.lucene.index.*;



import org.apache.lucene.codecs.PostingsFormat; // javadocs
import org.apache.lucene.codecs.perfield.PerFieldPostingsFormat; // javadocs
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;

/**
 * Holder class for common parameters used during read.
 * @lucene.experimental
 */
public class SegmentReadState {
  /** {@link Directory} where this segment is read from. */ 
  public final Directory directory;

  /** {@link SegmentInfo} describing this segment. */
  public final SegmentInfo segmentInfo;

  /** {@link FieldInfos} describing all fields in this
   *  segment. */
  public final FieldInfos fieldInfos;

  /** {@link IOContext} to pass to {@link
   *  Directory#openInput(String,IOContext)}. */
  public final IOContext context;

  /** Unique suffix for any postings files read for this
   *  segment.  {@link PerFieldPostingsFormat} sets this for
   *  each of the postings formats it wraps.  If you create
   *  a new {@link PostingsFormat} then any files you
   *  write/read must be derived using this suffix (use
   *  {@link IndexFileNames#segmentFileName(String,String,String)}). */
  public final String segmentSuffix;

  /** Create a {@code SegmentReadState}. */
  public SegmentReadState(Directory dir, SegmentInfo info,
      FieldInfos fieldInfos, IOContext context) {
    this(dir, info, fieldInfos,  context, "");
  }
  
  /** Create a {@code SegmentReadState}. */
  public SegmentReadState(Directory dir,
                          SegmentInfo info,
                          FieldInfos fieldInfos,
                          IOContext context,
                          String segmentSuffix) {
    this.directory = dir;
    this.segmentInfo = info;
    this.fieldInfos = fieldInfos;
    this.context = context;
    this.segmentSuffix = segmentSuffix;
  }

  /** Create a {@code SegmentReadState}. */
  public SegmentReadState(SegmentReadState other,
                          String newSegmentSuffix) {
    this.directory = other.directory;
    this.segmentInfo = other.segmentInfo;
    this.fieldInfos = other.fieldInfos;
    this.context = other.context;
    this.segmentSuffix = newSegmentSuffix;
  }
}
