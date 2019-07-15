import org.apache.lucene.search.suggest.fst.*;


import java.io.Closeable;
import java.io.IOException;
import java.util.Comparator;

import org.apache.lucene.codecs.CodecUtil;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefIterator;
import org.apache.lucene.util.IOUtils;
import org.apache.lucene.util.OfflineSorter;

/**
 * Builds and iterates over sequences stored on disk.
 * @lucene.experimental
 * @lucene.internal
 */
public class ExternalRefSorter implements BytesRefSorter, Closeable {
  private final OfflineSorter sorter;
  private OfflineSorter.ByteSequencesWriter writer;
  private IndexOutput input;
  private String sortedFileName;
  
  /**
   * Will buffer all sequences to a temporary file and then sort (all on-disk).
   */
  public ExternalRefSorter(OfflineSorter sorter) throws IOException {
    this.sorter = sorter;
    this.input = sorter.getDirectory().createTempOutput(sorter.getTempFileNamePrefix(), "RefSorterRaw", IOContext.DEFAULT);
    this.writer = new OfflineSorter.ByteSequencesWriter(this.input);
  }
  
  @Override
  public void add(BytesRef utf8) throws IOException {
    if (writer == null) {
      throw new IllegalStateException();
    }
    writer.write(utf8);
  }
  
  @Override
  public BytesRefIterator iterator() throws IOException {
    if (sortedFileName == null) {
      closeWriter();
      
      boolean success = false;
      try {
        sortedFileName = sorter.sort(input.getName());
        success = true;
      } finally {
        if (success) {
          sorter.getDirectory().deleteFile(input.getName());
        } else {
          IOUtils.deleteFilesIgnoringExceptions(sorter.getDirectory(), input.getName());
        }
      }
      
      input = null;
    }
    
    return new ByteSequenceIterator(new OfflineSorter.ByteSequencesReader(sorter.getDirectory().openChecksumInput(sortedFileName, IOContext.READONCE), sortedFileName));
  }
  
  private void closeWriter() throws IOException {
    if (writer != null) {
      CodecUtil.writeFooter(input);
      writer.close();
      writer = null;
    }
  }
  
  /**
   * Removes any written temporary files.
   */
  @Override
  public void close() throws IOException {
    try {
      closeWriter();
    } finally {
      IOUtils.deleteFilesIgnoringExceptions(sorter.getDirectory(),
                                            input == null ? null : input.getName(),
                                            sortedFileName);
    }
  }
  
  /**
   * Iterate over byte refs in a file.
   */
  // TODO: this class is a bit silly ... sole purpose is to "remove" Closeable from what #iterator returns:
  static class ByteSequenceIterator implements BytesRefIterator {
    private final OfflineSorter.ByteSequencesReader reader;
    private BytesRef scratch;
    
    public ByteSequenceIterator(OfflineSorter.ByteSequencesReader reader) {
      this.reader = reader;
    }
    
    @Override
    public BytesRef next() throws IOException {
      boolean success = false;
      try {
        scratch = reader.next();
        if (scratch == null) {
          reader.close();
        }
        success = true;
        return scratch;
      } finally {
        if (!success) {
          IOUtils.closeWhileHandlingException(reader);
        }
      }
    }
  }

  @Override
  public Comparator<BytesRef> getComparator() {
    return sorter.getComparator();
  }
}
