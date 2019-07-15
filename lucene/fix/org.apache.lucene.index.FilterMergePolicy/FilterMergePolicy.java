

import java.io.IOException;
import java.util.Map;
import org.apache.lucene.index.CodecReader;
import org.apache.lucene.index.MergePolicy;
import org.apache.lucene.index.MergeTrigger;
import org.apache.lucene.index.SegmentCommitInfo;
import org.apache.lucene.index.SegmentInfos;
import org.apache.lucene.util.IOSupplier;


public class FilterMergePolicy extends MergePolicy {
	protected final MergePolicy in;

	public FilterMergePolicy(MergePolicy in) {
		this.in = in;
	}

	@Override
	public MergePolicy.MergeSpecification findMerges(MergeTrigger mergeTrigger, SegmentInfos segmentInfos, MergePolicy.MergeContext mergeContext) throws IOException {
		return in.findMerges(mergeTrigger, segmentInfos, mergeContext);
	}

	@Override
	public MergePolicy.MergeSpecification findForcedMerges(SegmentInfos segmentInfos, int maxSegmentCount, Map<SegmentCommitInfo, Boolean> segmentsToMerge, MergePolicy.MergeContext mergeContext) throws IOException {
		return in.findForcedMerges(segmentInfos, maxSegmentCount, segmentsToMerge, mergeContext);
	}

	@Override
	public MergePolicy.MergeSpecification findForcedDeletesMerges(SegmentInfos segmentInfos, MergePolicy.MergeContext mergeContext) throws IOException {
		return in.findForcedDeletesMerges(segmentInfos, mergeContext);
	}

	@Override
	public boolean useCompoundFile(SegmentInfos infos, SegmentCommitInfo mergedInfo, MergePolicy.MergeContext mergeContext) throws IOException {
		return in.useCompoundFile(infos, mergedInfo, mergeContext);
	}

	@Override
	protected long size(SegmentCommitInfo info, MergePolicy.MergeContext context) throws IOException {
		return 0l;
	}

	@Override
	public double getNoCFSRatio() {
		return in.getNoCFSRatio();
	}

	@Override
	public final void setNoCFSRatio(double noCFSRatio) {
		in.setNoCFSRatio(noCFSRatio);
	}

	@Override
	public final void setMaxCFSSegmentSizeMB(double v) {
		in.setMaxCFSSegmentSizeMB(v);
	}

	@Override
	public final double getMaxCFSSegmentSizeMB() {
		return in.getMaxCFSSegmentSizeMB();
	}

	@Override
	public String toString() {
		return (((getClass().getSimpleName()) + "(") + (in)) + ")";
	}

	@Override
	public boolean keepFullyDeletedSegment(IOSupplier<CodecReader> readerIOSupplier) throws IOException {
		return in.keepFullyDeletedSegment(readerIOSupplier);
	}

	@Override
	public int numDeletesToMerge(SegmentCommitInfo info, int delCount, IOSupplier<CodecReader> readerSupplier) throws IOException {
		return in.numDeletesToMerge(info, delCount, readerSupplier);
	}
}

