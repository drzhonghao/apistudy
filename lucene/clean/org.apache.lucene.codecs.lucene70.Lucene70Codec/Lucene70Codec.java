import org.apache.lucene.codecs.lucene70.*;


import java.util.Objects;

import org.apache.lucene.codecs.Codec;
import org.apache.lucene.codecs.CompoundFormat;
import org.apache.lucene.codecs.DocValuesFormat;
import org.apache.lucene.codecs.FieldInfosFormat;
import org.apache.lucene.codecs.FilterCodec;
import org.apache.lucene.codecs.LiveDocsFormat;
import org.apache.lucene.codecs.NormsFormat;
import org.apache.lucene.codecs.PointsFormat;
import org.apache.lucene.codecs.PostingsFormat;
import org.apache.lucene.codecs.SegmentInfoFormat;
import org.apache.lucene.codecs.StoredFieldsFormat;
import org.apache.lucene.codecs.TermVectorsFormat;
import org.apache.lucene.codecs.lucene50.Lucene50CompoundFormat;
import org.apache.lucene.codecs.lucene50.Lucene50LiveDocsFormat;
import org.apache.lucene.codecs.lucene50.Lucene50StoredFieldsFormat;
import org.apache.lucene.codecs.lucene50.Lucene50StoredFieldsFormat.Mode;
import org.apache.lucene.codecs.lucene50.Lucene50TermVectorsFormat;
import org.apache.lucene.codecs.lucene60.Lucene60FieldInfosFormat;
import org.apache.lucene.codecs.lucene60.Lucene60PointsFormat;
import org.apache.lucene.codecs.perfield.PerFieldDocValuesFormat;
import org.apache.lucene.codecs.perfield.PerFieldPostingsFormat;

/**
 * Implements the Lucene 7.0 index format, with configurable per-field postings
 * and docvalues formats.
 * <p>
 * If you want to reuse functionality of this codec in another codec, extend
 * {@link FilterCodec}.
 *
 * @see org.apache.lucene.codecs.lucene70 package documentation for file format details.
 *
 * @lucene.experimental
 */
public class Lucene70Codec extends Codec {
  private final TermVectorsFormat vectorsFormat = new Lucene50TermVectorsFormat();
  private final FieldInfosFormat fieldInfosFormat = new Lucene60FieldInfosFormat();
  private final SegmentInfoFormat segmentInfosFormat = new Lucene70SegmentInfoFormat();
  private final LiveDocsFormat liveDocsFormat = new Lucene50LiveDocsFormat();
  private final CompoundFormat compoundFormat = new Lucene50CompoundFormat();
  
  private final PostingsFormat postingsFormat = new PerFieldPostingsFormat() {
    @Override
    public PostingsFormat getPostingsFormatForField(String field) {
      return Lucene70Codec.this.getPostingsFormatForField(field);
    }
  };
  
  private final DocValuesFormat docValuesFormat = new PerFieldDocValuesFormat() {
    @Override
    public DocValuesFormat getDocValuesFormatForField(String field) {
      return Lucene70Codec.this.getDocValuesFormatForField(field);
    }
  };
  
  private final StoredFieldsFormat storedFieldsFormat;

  /** 
   * Instantiates a new codec.
   */
  public Lucene70Codec() {
    this(Mode.BEST_SPEED);
  }
  
  /** 
   * Instantiates a new codec, specifying the stored fields compression
   * mode to use.
   * @param mode stored fields compression mode to use for newly 
   *             flushed/merged segments.
   */
  public Lucene70Codec(Mode mode) {
    super("Lucene70");
    this.storedFieldsFormat = new Lucene50StoredFieldsFormat(Objects.requireNonNull(mode));
  }
  
  @Override
  public final StoredFieldsFormat storedFieldsFormat() {
    return storedFieldsFormat;
  }
  
  @Override
  public final TermVectorsFormat termVectorsFormat() {
    return vectorsFormat;
  }

  @Override
  public final PostingsFormat postingsFormat() {
    return postingsFormat;
  }
  
  @Override
  public final FieldInfosFormat fieldInfosFormat() {
    return fieldInfosFormat;
  }
  
  @Override
  public final SegmentInfoFormat segmentInfoFormat() {
    return segmentInfosFormat;
  }
  
  @Override
  public final LiveDocsFormat liveDocsFormat() {
    return liveDocsFormat;
  }

  @Override
  public final CompoundFormat compoundFormat() {
    return compoundFormat;
  }

  @Override
  public final PointsFormat pointsFormat() {
    return new Lucene60PointsFormat();
  }

  /** Returns the postings format that should be used for writing 
   *  new segments of <code>field</code>.
   *  
   *  The default implementation always returns "Lucene50".
   *  <p>
   *  <b>WARNING:</b> if you subclass, you are responsible for index 
   *  backwards compatibility: future version of Lucene are only 
   *  guaranteed to be able to read the default implementation. 
   */
  public PostingsFormat getPostingsFormatForField(String field) {
    return defaultFormat;
  }
  
  /** Returns the docvalues format that should be used for writing 
   *  new segments of <code>field</code>.
   *  
   *  The default implementation always returns "Lucene70".
   *  <p>
   *  <b>WARNING:</b> if you subclass, you are responsible for index 
   *  backwards compatibility: future version of Lucene are only 
   *  guaranteed to be able to read the default implementation. 
   */
  public DocValuesFormat getDocValuesFormatForField(String field) {
    return defaultDVFormat;
  }
  
  @Override
  public final DocValuesFormat docValuesFormat() {
    return docValuesFormat;
  }

  private final PostingsFormat defaultFormat = PostingsFormat.forName("Lucene50");
  private final DocValuesFormat defaultDVFormat = DocValuesFormat.forName("Lucene70");

  private final NormsFormat normsFormat = new Lucene70NormsFormat();

  @Override
  public final NormsFormat normsFormat() {
    return normsFormat;
  }
}
