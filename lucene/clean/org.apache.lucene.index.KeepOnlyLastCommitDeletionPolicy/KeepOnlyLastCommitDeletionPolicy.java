import org.apache.lucene.index.IndexDeletionPolicy;
import org.apache.lucene.index.IndexCommit;
import org.apache.lucene.index.*;



import java.util.List;

/**
 * This {@link IndexDeletionPolicy} implementation that
 * keeps only the most recent commit and immediately removes
 * all prior commits after a new commit is done.  This is
 * the default deletion policy.
 */

public final class KeepOnlyLastCommitDeletionPolicy extends IndexDeletionPolicy {

  /** Sole constructor. */
  public KeepOnlyLastCommitDeletionPolicy() {
  }

  /**
   * Deletes all commits except the most recent one.
   */
  @Override
  public void onInit(List<? extends IndexCommit> commits) {
    // Note that commits.size() should normally be 1:
    onCommit(commits);
  }

  /**
   * Deletes all commits except the most recent one.
   */
  @Override
  public void onCommit(List<? extends IndexCommit> commits) {
    // Note that commits.size() should normally be 2 (if not
    // called by onInit above):
    int size = commits.size();
    for(int i=0;i<size-1;i++) {
      commits.get(i).delete();
    }
  }
}
