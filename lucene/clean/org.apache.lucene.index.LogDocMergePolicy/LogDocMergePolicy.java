import org.apache.lucene.index.LogMergePolicy;
import org.apache.lucene.index.*;


import java.io.IOException;


/** This is a {@link LogMergePolicy} that measures size of a
 *  segment as the number of documents (not taking deletions
 *  into account). */

public class LogDocMergePolicy extends LogMergePolicy {

  /** Default minimum segment size.  @see setMinMergeDocs */
  public static final int DEFAULT_MIN_MERGE_DOCS = 1000;

  /** Sole constructor, setting all settings to their
   *  defaults. */
  public LogDocMergePolicy() {
    minMergeSize = DEFAULT_MIN_MERGE_DOCS;
    
    // maxMergeSize(ForForcedMerge) are never used by LogDocMergePolicy; set
    // it to Long.MAX_VALUE to disable it
    maxMergeSize = Long.MAX_VALUE;
    maxMergeSizeForForcedMerge = Long.MAX_VALUE;
  }

  @Override
  protected long size(SegmentCommitInfo info, MergeContext mergeContext) throws IOException {
    return sizeDocs(info, mergeContext);
  }

  /** Sets the minimum size for the lowest level segments.
   * Any segments below this size are considered to be on
   * the same level (even if they vary drastically in size)
   * and will be merged whenever there are mergeFactor of
   * them.  This effectively truncates the "long tail" of
   * small segments that would otherwise be created into a
   * single level.  If you set this too large, it could
   * greatly increase the merging cost during indexing (if
   * you flush many small segments). */
  public void setMinMergeDocs(int minMergeDocs) {
    minMergeSize = minMergeDocs;
  }

  /** Get the minimum size for a segment to remain
   *  un-merged.
   *  @see #setMinMergeDocs **/
  public int getMinMergeDocs() {
    return (int) minMergeSize;
  }
}
