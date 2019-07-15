import org.apache.lucene.util.*;



import org.apache.lucene.index.IndexWriter; // javadocs
import org.apache.lucene.index.SegmentInfos; // javadocs
import java.io.Closeable;

/** 
 * Debugging API for Lucene classes such as {@link IndexWriter} 
 * and {@link SegmentInfos}.
 * <p>
 * NOTE: Enabling infostreams may cause performance degradation
 * in some components.
 * 
 * @lucene.internal 
 */
public abstract class InfoStream implements Closeable {

  /** Instance of InfoStream that does no logging at all. */
  public static final InfoStream NO_OUTPUT = new NoOutput();
  private static final class NoOutput extends InfoStream {
    @Override
    public void message(String component, String message) {
      assert false: "message() should not be called when isEnabled returns false";
    }
    
    @Override
    public boolean isEnabled(String component) {
      return false;
    }

    @Override
    public void close() {}
  }
  
  /** prints a message */
  public abstract void message(String component, String message);
  
  /** returns true if messages are enabled and should be posted to {@link #message}. */
  public abstract boolean isEnabled(String component);
  
  private static InfoStream defaultInfoStream = NO_OUTPUT;
  
  /** The default {@code InfoStream} used by a newly instantiated classes.
   * @see #setDefault */
  public static synchronized InfoStream getDefault() {
    return defaultInfoStream;
  }
  
  /** Sets the default {@code InfoStream} used
   * by a newly instantiated classes. It cannot be {@code null},
   * to disable logging use {@link #NO_OUTPUT}.
   * @see #getDefault */
  public static synchronized void setDefault(InfoStream infoStream) {
    if (infoStream == null) {
      throw new IllegalArgumentException("Cannot set InfoStream default implementation to null. "+
        "To disable logging use InfoStream.NO_OUTPUT");
    }
    defaultInfoStream = infoStream;
  }
}
