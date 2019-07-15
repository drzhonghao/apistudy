

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.lucene.index.MergePolicy;
import org.apache.lucene.index.MergeTrigger;
import org.apache.lucene.index.SegmentCommitInfo;
import org.apache.lucene.index.SegmentInfo;
import org.apache.lucene.index.SegmentInfos;


public abstract class LogMergePolicy extends MergePolicy {
	public static final double LEVEL_LOG_SPAN = 0.75;

	public static final int DEFAULT_MERGE_FACTOR = 10;

	public static final int DEFAULT_MAX_MERGE_DOCS = Integer.MAX_VALUE;

	public static final double DEFAULT_NO_CFS_RATIO = 0.1;

	protected int mergeFactor = LogMergePolicy.DEFAULT_MERGE_FACTOR;

	protected long minMergeSize;

	protected long maxMergeSize;

	protected long maxMergeSizeForForcedMerge = Long.MAX_VALUE;

	protected int maxMergeDocs = LogMergePolicy.DEFAULT_MAX_MERGE_DOCS;

	protected boolean calibrateSizeByDeletes = true;

	public LogMergePolicy() {
		super(LogMergePolicy.DEFAULT_NO_CFS_RATIO, MergePolicy.DEFAULT_MAX_CFS_SEGMENT_SIZE);
	}

	public int getMergeFactor() {
		return mergeFactor;
	}

	public void setMergeFactor(int mergeFactor) {
		if (mergeFactor < 2)
			throw new IllegalArgumentException("mergeFactor cannot be less than 2");

		this.mergeFactor = mergeFactor;
	}

	public void setCalibrateSizeByDeletes(boolean calibrateSizeByDeletes) {
		this.calibrateSizeByDeletes = calibrateSizeByDeletes;
	}

	public boolean getCalibrateSizeByDeletes() {
		return calibrateSizeByDeletes;
	}

	protected long sizeDocs(SegmentCommitInfo info, MergePolicy.MergeContext mergeContext) throws IOException {
		if (calibrateSizeByDeletes) {
			int delCount = mergeContext.numDeletesToMerge(info);
			assert assertDelCount(delCount, info);
			return (info.info.maxDoc()) - ((long) (delCount));
		}else {
			return info.info.maxDoc();
		}
	}

	protected long sizeBytes(SegmentCommitInfo info, MergePolicy.MergeContext mergeContext) throws IOException {
		if (calibrateSizeByDeletes) {
			return super.size(info, mergeContext);
		}
		return info.sizeInBytes();
	}

	protected boolean isMerged(SegmentInfos infos, int maxNumSegments, Map<SegmentCommitInfo, Boolean> segmentsToMerge, MergePolicy.MergeContext mergeContext) throws IOException {
		final int numSegments = infos.size();
		int numToMerge = 0;
		SegmentCommitInfo mergeInfo = null;
		boolean segmentIsOriginal = false;
		for (int i = 0; (i < numSegments) && (numToMerge <= maxNumSegments); i++) {
			final SegmentCommitInfo info = infos.info(i);
			final Boolean isOriginal = segmentsToMerge.get(info);
			if (isOriginal != null) {
				segmentIsOriginal = isOriginal;
				numToMerge++;
				mergeInfo = info;
			}
		}
		return (numToMerge <= maxNumSegments) && (((numToMerge != 1) || (!segmentIsOriginal)) || (isMerged(infos, mergeInfo, mergeContext)));
	}

	private MergePolicy.MergeSpecification findForcedMergesSizeLimit(SegmentInfos infos, int last, MergePolicy.MergeContext mergeContext) throws IOException {
		MergePolicy.MergeSpecification spec = new MergePolicy.MergeSpecification();
		final List<SegmentCommitInfo> segments = infos.asList();
		int start = last - 1;
		while (start >= 0) {
			SegmentCommitInfo info = infos.info(start);
			if (((size(info, mergeContext)) > (maxMergeSizeForForcedMerge)) || ((sizeDocs(info, mergeContext)) > (maxMergeDocs))) {
				if (verbose(mergeContext)) {
					message((((((("findForcedMergesSizeLimit: skip segment=" + info) + ": size is > maxMergeSize (") + (maxMergeSizeForForcedMerge)) + ") or sizeDocs is > maxMergeDocs (") + (maxMergeDocs)) + ")"), mergeContext);
				}
				if ((((last - start) - 1) > 1) || ((start != (last - 1)) && (!(isMerged(infos, infos.info((start + 1)), mergeContext))))) {
					spec.add(new MergePolicy.OneMerge(segments.subList((start + 1), last)));
				}
				last = start;
			}else
				if ((last - start) == (mergeFactor)) {
					spec.add(new MergePolicy.OneMerge(segments.subList(start, last)));
					last = start;
				}

			--start;
		} 
		if ((last > 0) && ((((++start) + 1) < last) || (!(isMerged(infos, infos.info(start), mergeContext))))) {
			spec.add(new MergePolicy.OneMerge(segments.subList(start, last)));
		}
		return (spec.merges.size()) == 0 ? null : spec;
	}

	private MergePolicy.MergeSpecification findForcedMergesMaxNumSegments(SegmentInfos infos, int maxNumSegments, int last, MergePolicy.MergeContext mergeContext) throws IOException {
		MergePolicy.MergeSpecification spec = new MergePolicy.MergeSpecification();
		final List<SegmentCommitInfo> segments = infos.asList();
		while (((last - maxNumSegments) + 1) >= (mergeFactor)) {
			spec.add(new MergePolicy.OneMerge(segments.subList((last - (mergeFactor)), last)));
			last -= mergeFactor;
		} 
		if (0 == (spec.merges.size())) {
			if (maxNumSegments == 1) {
				if ((last > 1) || (!(isMerged(infos, infos.info(0), mergeContext)))) {
					spec.add(new MergePolicy.OneMerge(segments.subList(0, last)));
				}
			}else
				if (last > maxNumSegments) {
					final int finalMergeSize = (last - maxNumSegments) + 1;
					long bestSize = 0;
					int bestStart = 0;
					for (int i = 0; i < ((last - finalMergeSize) + 1); i++) {
						long sumSize = 0;
						for (int j = 0; j < finalMergeSize; j++) {
							sumSize += size(infos.info((j + i)), mergeContext);
						}
						if ((i == 0) || ((sumSize < (2 * (size(infos.info((i - 1)), mergeContext)))) && (sumSize < bestSize))) {
							bestStart = i;
							bestSize = sumSize;
						}
					}
					spec.add(new MergePolicy.OneMerge(segments.subList(bestStart, (bestStart + finalMergeSize))));
				}

		}
		return (spec.merges.size()) == 0 ? null : spec;
	}

	@Override
	public MergePolicy.MergeSpecification findForcedMerges(SegmentInfos infos, int maxNumSegments, Map<SegmentCommitInfo, Boolean> segmentsToMerge, MergePolicy.MergeContext mergeContext) throws IOException {
		assert maxNumSegments > 0;
		if (verbose(mergeContext)) {
			message(((("findForcedMerges: maxNumSegs=" + maxNumSegments) + " segsToMerge=") + segmentsToMerge), mergeContext);
		}
		if (isMerged(infos, maxNumSegments, segmentsToMerge, mergeContext)) {
			if (verbose(mergeContext)) {
				message("already merged; skip", mergeContext);
			}
			return null;
		}
		int last = infos.size();
		while (last > 0) {
			final SegmentCommitInfo info = infos.info((--last));
			if ((segmentsToMerge.get(info)) != null) {
				last++;
				break;
			}
		} 
		if (last == 0) {
			if (verbose(mergeContext)) {
				message("last == 0; skip", mergeContext);
			}
			return null;
		}
		if (((maxNumSegments == 1) && (last == 1)) && (isMerged(infos, infos.info(0), mergeContext))) {
			if (verbose(mergeContext)) {
				message("already 1 seg; skip", mergeContext);
			}
			return null;
		}
		boolean anyTooLarge = false;
		for (int i = 0; i < last; i++) {
			SegmentCommitInfo info = infos.info(i);
			if (((size(info, mergeContext)) > (maxMergeSizeForForcedMerge)) || ((sizeDocs(info, mergeContext)) > (maxMergeDocs))) {
				anyTooLarge = true;
				break;
			}
		}
		if (anyTooLarge) {
			return findForcedMergesSizeLimit(infos, last, mergeContext);
		}else {
			return findForcedMergesMaxNumSegments(infos, maxNumSegments, last, mergeContext);
		}
	}

	@Override
	public MergePolicy.MergeSpecification findForcedDeletesMerges(SegmentInfos segmentInfos, MergePolicy.MergeContext mergeContext) throws IOException {
		final List<SegmentCommitInfo> segments = segmentInfos.asList();
		final int numSegments = segments.size();
		if (verbose(mergeContext)) {
			message((("findForcedDeleteMerges: " + numSegments) + " segments"), mergeContext);
		}
		MergePolicy.MergeSpecification spec = new MergePolicy.MergeSpecification();
		int firstSegmentWithDeletions = -1;
		assert mergeContext != null;
		for (int i = 0; i < numSegments; i++) {
			final SegmentCommitInfo info = segmentInfos.info(i);
			int delCount = mergeContext.numDeletesToMerge(info);
			assert assertDelCount(delCount, info);
			if (delCount > 0) {
				if (verbose(mergeContext)) {
					message((("  segment " + (info.info.name)) + " has deletions"), mergeContext);
				}
				if (firstSegmentWithDeletions == (-1))
					firstSegmentWithDeletions = i;
				else
					if ((i - firstSegmentWithDeletions) == (mergeFactor)) {
						if (verbose(mergeContext)) {
							message((((("  add merge " + firstSegmentWithDeletions) + " to ") + (i - 1)) + " inclusive"), mergeContext);
						}
						spec.add(new MergePolicy.OneMerge(segments.subList(firstSegmentWithDeletions, i)));
						firstSegmentWithDeletions = i;
					}

			}else
				if (firstSegmentWithDeletions != (-1)) {
					if (verbose(mergeContext)) {
						message((((("  add merge " + firstSegmentWithDeletions) + " to ") + (i - 1)) + " inclusive"), mergeContext);
					}
					spec.add(new MergePolicy.OneMerge(segments.subList(firstSegmentWithDeletions, i)));
					firstSegmentWithDeletions = -1;
				}

		}
		if (firstSegmentWithDeletions != (-1)) {
			if (verbose(mergeContext)) {
				message((((("  add merge " + firstSegmentWithDeletions) + " to ") + (numSegments - 1)) + " inclusive"), mergeContext);
			}
			spec.add(new MergePolicy.OneMerge(segments.subList(firstSegmentWithDeletions, numSegments)));
		}
		return spec;
	}

	private static class SegmentInfoAndLevel implements Comparable<LogMergePolicy.SegmentInfoAndLevel> {
		SegmentCommitInfo info;

		float level;

		public SegmentInfoAndLevel(SegmentCommitInfo info, float level) {
			this.info = info;
			this.level = level;
		}

		@Override
		public int compareTo(LogMergePolicy.SegmentInfoAndLevel other) {
			return Float.compare(other.level, level);
		}
	}

	@Override
	public MergePolicy.MergeSpecification findMerges(MergeTrigger mergeTrigger, SegmentInfos infos, MergePolicy.MergeContext mergeContext) throws IOException {
		final int numSegments = infos.size();
		if (verbose(mergeContext)) {
			message((("findMerges: " + numSegments) + " segments"), mergeContext);
		}
		final List<LogMergePolicy.SegmentInfoAndLevel> levels = new ArrayList<>(numSegments);
		final float norm = ((float) (Math.log(mergeFactor)));
		final Set<SegmentCommitInfo> mergingSegments = mergeContext.getMergingSegments();
		for (int i = 0; i < numSegments; i++) {
			final SegmentCommitInfo info = infos.info(i);
			long size = size(info, mergeContext);
			if (size < 1) {
				size = 1;
			}
			final LogMergePolicy.SegmentInfoAndLevel infoLevel = new LogMergePolicy.SegmentInfoAndLevel(info, (((float) (Math.log(size))) / norm));
			levels.add(infoLevel);
			if (verbose(mergeContext)) {
				final long segBytes = sizeBytes(info, mergeContext);
				String extra = (mergingSegments.contains(info)) ? " [merging]" : "";
				if (size >= (maxMergeSize)) {
					extra += " [skip: too large]";
				}
				message((((((("seg=" + (segString(mergeContext, Collections.singleton(info)))) + " level=") + (infoLevel.level)) + " size=") + (String.format(Locale.ROOT, "%.3f MB", ((segBytes / 1024) / 1024.0)))) + extra), mergeContext);
			}
		}
		final float levelFloor;
		if ((minMergeSize) <= 0)
			levelFloor = ((float) (0.0));
		else
			levelFloor = ((float) ((Math.log(minMergeSize)) / norm));

		MergePolicy.MergeSpecification spec = null;
		final int numMergeableSegments = levels.size();
		int start = 0;
		while (start < numMergeableSegments) {
			float maxLevel = levels.get(start).level;
			for (int i = 1 + start; i < numMergeableSegments; i++) {
				final float level = levels.get(i).level;
				if (level > maxLevel) {
					maxLevel = level;
				}
			}
			float levelBottom;
			if (maxLevel <= levelFloor) {
				levelBottom = -1.0F;
			}else {
				levelBottom = ((float) (maxLevel - (LogMergePolicy.LEVEL_LOG_SPAN)));
				if ((levelBottom < levelFloor) && (maxLevel >= levelFloor)) {
					levelBottom = levelFloor;
				}
			}
			int upto = numMergeableSegments - 1;
			while (upto >= start) {
				if ((levels.get(upto).level) >= levelBottom) {
					break;
				}
				upto--;
			} 
			if (verbose(mergeContext)) {
				message((((((("  level " + levelBottom) + " to ") + maxLevel) + ": ") + ((1 + upto) - start)) + " segments"), mergeContext);
			}
			int end = start + (mergeFactor);
			while (end <= (1 + upto)) {
				boolean anyTooLarge = false;
				boolean anyMerging = false;
				for (int i = start; i < end; i++) {
					final SegmentCommitInfo info = levels.get(i).info;
					anyTooLarge |= ((size(info, mergeContext)) >= (maxMergeSize)) || ((sizeDocs(info, mergeContext)) >= (maxMergeDocs));
					if (mergingSegments.contains(info)) {
						anyMerging = true;
						break;
					}
				}
				if (anyMerging) {
				}else
					if (!anyTooLarge) {
						if (spec == null)
							spec = new MergePolicy.MergeSpecification();

						final List<SegmentCommitInfo> mergeInfos = new ArrayList<>((end - start));
						for (int i = start; i < end; i++) {
							mergeInfos.add(levels.get(i).info);
						}
						if (verbose(mergeContext)) {
							message(((((("  add merge=" + (segString(mergeContext, mergeInfos))) + " start=") + start) + " end=") + end), mergeContext);
						}
						spec.add(new MergePolicy.OneMerge(mergeInfos));
					}else
						if (verbose(mergeContext)) {
							message((((("    " + start) + " to ") + end) + ": contains segment over maxMergeSize or maxMergeDocs; skipping"), mergeContext);
						}


				start = end;
				end = start + (mergeFactor);
			} 
			start = 1 + upto;
		} 
		return spec;
	}

	public void setMaxMergeDocs(int maxMergeDocs) {
		this.maxMergeDocs = maxMergeDocs;
	}

	public int getMaxMergeDocs() {
		return maxMergeDocs;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder((("[" + (getClass().getSimpleName())) + ": "));
		sb.append("minMergeSize=").append(minMergeSize).append(", ");
		sb.append("mergeFactor=").append(mergeFactor).append(", ");
		sb.append("maxMergeSize=").append(maxMergeSize).append(", ");
		sb.append("maxMergeSizeForForcedMerge=").append(maxMergeSizeForForcedMerge).append(", ");
		sb.append("calibrateSizeByDeletes=").append(calibrateSizeByDeletes).append(", ");
		sb.append("maxMergeDocs=").append(maxMergeDocs).append(", ");
		sb.append("maxCFSSegmentSizeMB=").append(getMaxCFSSegmentSizeMB()).append(", ");
		sb.append("noCFSRatio=").append(noCFSRatio);
		sb.append("]");
		return sb.toString();
	}
}

