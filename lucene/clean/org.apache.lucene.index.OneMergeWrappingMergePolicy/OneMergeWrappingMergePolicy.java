import org.apache.lucene.index.MergePolicy;
import org.apache.lucene.index.*;


import java.io.IOException;
import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * A wrapping merge policy that wraps the {@link org.apache.lucene.index.MergePolicy.OneMerge}
 * objects returned by the wrapped merge policy.
 *
 * @lucene.experimental
 */
public class OneMergeWrappingMergePolicy extends FilterMergePolicy {

  private final UnaryOperator<OneMerge> wrapOneMerge;

  /**
   * Constructor
   *
   * @param in - the wrapped merge policy
   * @param wrapOneMerge - operator for wrapping OneMerge objects
   */
  public OneMergeWrappingMergePolicy(MergePolicy in, UnaryOperator<OneMerge> wrapOneMerge) {
    super(in);
    this.wrapOneMerge = wrapOneMerge;
  }

  @Override
  public MergeSpecification findMerges(MergeTrigger mergeTrigger, SegmentInfos segmentInfos, MergeContext mergeContext)
      throws IOException {
    return wrapSpec(in.findMerges(mergeTrigger, segmentInfos, mergeContext));
  }

  @Override
  public MergeSpecification findForcedMerges(SegmentInfos segmentInfos, int maxSegmentCount,
                                             Map<SegmentCommitInfo,Boolean> segmentsToMerge, MergeContext mergeContext) throws IOException {
    return wrapSpec(in.findForcedMerges(segmentInfos, maxSegmentCount, segmentsToMerge, mergeContext));
  }

  @Override
  public MergeSpecification findForcedDeletesMerges(SegmentInfos segmentInfos, MergeContext mergeContext)
    throws IOException {
    return wrapSpec(in.findForcedDeletesMerges(segmentInfos, mergeContext));
  }

  private MergeSpecification wrapSpec(MergeSpecification spec) {
    MergeSpecification wrapped = spec == null ? null : new MergeSpecification();
    if (wrapped != null) {
      for (OneMerge merge : spec.merges) {
        wrapped.add(wrapOneMerge.apply(merge));
      }
    }
    return wrapped;
  }

}
