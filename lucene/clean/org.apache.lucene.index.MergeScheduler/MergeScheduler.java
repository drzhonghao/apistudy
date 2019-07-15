import org.apache.lucene.index.*;



import java.io.Closeable;
import java.io.IOException;

import org.apache.lucene.index.MergePolicy.OneMerge;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RateLimitedIndexOutput;
import org.apache.lucene.util.InfoStream;

/** <p>Expert: {@link IndexWriter} uses an instance
 *  implementing this interface to execute the merges
 *  selected by a {@link MergePolicy}.  The default
 *  MergeScheduler is {@link ConcurrentMergeScheduler}.</p>
 * @lucene.experimental
*/
public abstract class MergeScheduler implements Closeable {

  /** Sole constructor. (For invocation by subclass 
   *  constructors, typically implicit.) */
  protected MergeScheduler() {
  }

  /** Run the merges provided by {@link IndexWriter#getNextMerge()}.
   * @param writer the {@link IndexWriter} to obtain the merges from.
   * @param trigger the {@link MergeTrigger} that caused this merge to happen
   * @param newMergesFound <code>true</code> iff any new merges were found by the caller otherwise <code>false</code>
   * */
  public abstract void merge(IndexWriter writer, MergeTrigger trigger, boolean newMergesFound) throws IOException;

  /** 
   * Wraps the incoming {@link Directory} so that we can merge-throttle it
   * using {@link RateLimitedIndexOutput}. 
   */
  public Directory wrapForMerge(OneMerge merge, Directory in) {
    // A no-op by default.
    return in;
  }

  /** Close this MergeScheduler. */
  @Override
  public abstract void close() throws IOException;

  /** For messages about merge scheduling */
  protected InfoStream infoStream;

  /** IndexWriter calls this on init. */
  final void setInfoStream(InfoStream infoStream) {
    this.infoStream = infoStream;
  }

  /**
   * Returns true if infoStream messages are enabled. This method is usually used in
   * conjunction with {@link #message(String)}:
   * 
   * <pre class="prettyprint">
   * if (verbose()) {
   *   message(&quot;your message&quot;);
   * }
   * </pre>
   */
  protected boolean verbose() {
    return infoStream != null && infoStream.isEnabled("MS");
  }
 
  /**
   * Outputs the given message - this method assumes {@link #verbose()} was
   * called and returned true.
   */
  protected void message(String message) {
    infoStream.message("MS", message);
  }
}
