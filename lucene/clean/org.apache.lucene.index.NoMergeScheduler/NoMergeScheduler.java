import org.apache.lucene.index.MergeScheduler;
import org.apache.lucene.index.*;


import org.apache.lucene.index.MergePolicy.OneMerge;
import org.apache.lucene.store.Directory;

/**
 * A {@link MergeScheduler} which never executes any merges. It is also a
 * singleton and can be accessed through {@link NoMergeScheduler#INSTANCE}. Use
 * it if you want to prevent an {@link IndexWriter} from ever executing merges,
 * regardless of the {@link MergePolicy} used. Note that you can achieve the
 * same thing by using {@link NoMergePolicy}, however with
 * {@link NoMergeScheduler} you also ensure that no unnecessary code of any
 * {@link MergeScheduler} implementation is ever executed. Hence it is
 * recommended to use both if you want to disable merges from ever happening.
 */
public final class NoMergeScheduler extends MergeScheduler {

  /** The single instance of {@link NoMergeScheduler} */
  public static final MergeScheduler INSTANCE = new NoMergeScheduler();

  private NoMergeScheduler() {
    // prevent instantiation
  }

  @Override
  public void close() {}

  @Override
  public void merge(IndexWriter writer, MergeTrigger trigger, boolean newMergesFound) {}
  
  @Override
  public Directory wrapForMerge(OneMerge merge, Directory in) {
    return in;
  }

  @Override
  public MergeScheduler clone() {
    return this;
  }
}
