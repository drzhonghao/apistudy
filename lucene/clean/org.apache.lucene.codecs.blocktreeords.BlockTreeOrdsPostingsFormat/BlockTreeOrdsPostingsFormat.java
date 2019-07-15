import org.apache.lucene.codecs.blocktreeords.*;



import java.io.IOException;

import org.apache.lucene.codecs.FieldsConsumer;
import org.apache.lucene.codecs.FieldsProducer;
import org.apache.lucene.codecs.PostingsFormat;
import org.apache.lucene.codecs.PostingsReaderBase;
import org.apache.lucene.codecs.PostingsWriterBase;
import org.apache.lucene.codecs.blocktree.BlockTreeTermsWriter;
import org.apache.lucene.codecs.lucene50.Lucene50PostingsReader;
import org.apache.lucene.codecs.lucene50.Lucene50PostingsWriter;
import org.apache.lucene.index.SegmentReadState;
import org.apache.lucene.index.SegmentWriteState;
import org.apache.lucene.util.IOUtils;

/** Uses {@link OrdsBlockTreeTermsWriter} with {@link Lucene50PostingsWriter}. */
public class BlockTreeOrdsPostingsFormat extends PostingsFormat {

  private final int minTermBlockSize;
  private final int maxTermBlockSize;

  /**
   * Fixed packed block size, number of integers encoded in 
   * a single packed block.
   */
  // NOTE: must be multiple of 64 because of PackedInts long-aligned encoding/decoding
  public final static int BLOCK_SIZE = 128;

  /** Creates {@code Lucene41PostingsFormat} with default
   *  settings. */
  public BlockTreeOrdsPostingsFormat() {
    this(OrdsBlockTreeTermsWriter.DEFAULT_MIN_BLOCK_SIZE, OrdsBlockTreeTermsWriter.DEFAULT_MAX_BLOCK_SIZE);
  }

  /** Creates {@code Lucene41PostingsFormat} with custom
   *  values for {@code minBlockSize} and {@code
   *  maxBlockSize} passed to block terms dictionary.
   *  @see OrdsBlockTreeTermsWriter#OrdsBlockTreeTermsWriter(SegmentWriteState,PostingsWriterBase,int,int) */
  public BlockTreeOrdsPostingsFormat(int minTermBlockSize, int maxTermBlockSize) {
    super("BlockTreeOrds");
    this.minTermBlockSize = minTermBlockSize;
    this.maxTermBlockSize = maxTermBlockSize;
    BlockTreeTermsWriter.validateSettings(minTermBlockSize, maxTermBlockSize);
  }

  @Override
  public String toString() {
    return getName() + "(blocksize=" + BLOCK_SIZE + ")";
  }

  @Override
  public FieldsConsumer fieldsConsumer(SegmentWriteState state) throws IOException {
    PostingsWriterBase postingsWriter = new Lucene50PostingsWriter(state);

    boolean success = false;
    try {
      FieldsConsumer ret = new OrdsBlockTreeTermsWriter(state, 
                                                        postingsWriter,
                                                        minTermBlockSize, 
                                                        maxTermBlockSize);
      success = true;
      return ret;
    } finally {
      if (!success) {
        IOUtils.closeWhileHandlingException(postingsWriter);
      }
    }
  }

  @Override
  public FieldsProducer fieldsProducer(SegmentReadState state) throws IOException {
    PostingsReaderBase postingsReader = new Lucene50PostingsReader(state);
    boolean success = false;
    try {
      FieldsProducer ret = new OrdsBlockTreeTermsReader(postingsReader, state);
      success = true;
      return ret;
    } finally {
      if (!success) {
        IOUtils.closeWhileHandlingException(postingsReader);
      }
    }
  }
}
