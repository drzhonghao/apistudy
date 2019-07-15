

import org.apache.lucene.index.FieldInfos;
import org.apache.lucene.index.SegmentInfo;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.util.FixedBitSet;
import org.apache.lucene.util.InfoStream;


public class SegmentWriteState {
	public final InfoStream infoStream;

	public final Directory directory;

	public final SegmentInfo segmentInfo;

	public final FieldInfos fieldInfos;

	public int delCountOnFlush;

	public int softDelCountOnFlush;

	public FixedBitSet liveDocs;

	public final String segmentSuffix;

	public final IOContext context;

	public SegmentWriteState(SegmentWriteState state, String segmentSuffix) {
		infoStream = state.infoStream;
		directory = state.directory;
		segmentInfo = state.segmentInfo;
		fieldInfos = state.fieldInfos;
		context = state.context;
		this.segmentSuffix = segmentSuffix;
		delCountOnFlush = state.delCountOnFlush;
		liveDocs = state.liveDocs;
	}

	private boolean assertSegmentSuffix(String segmentSuffix) {
		assert segmentSuffix != null;
		if (!(segmentSuffix.isEmpty())) {
			int numParts = segmentSuffix.split("_").length;
			if (numParts == 2) {
				return true;
			}else
				if (numParts == 1) {
					Long.parseLong(segmentSuffix, Character.MAX_RADIX);
					return true;
				}

			return false;
		}
		return true;
	}
}

