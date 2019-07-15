import org.apache.lucene.index.*;



import java.util.List;

/**
 * An {@link IndexDeletionPolicy} which keeps all index commits around, never
 * deleting them. This class is a singleton and can be accessed by referencing
 * {@link #INSTANCE}.
 */
public final class NoDeletionPolicy extends IndexDeletionPolicy {

  /** The single instance of this class. */
  public static final IndexDeletionPolicy INSTANCE = new NoDeletionPolicy();
  
  private NoDeletionPolicy() {
    // keep private to avoid instantiation
  }
  
  @Override
  public void onCommit(List<? extends IndexCommit> commits) {}

  @Override
  public void onInit(List<? extends IndexCommit> commits) {}
}
