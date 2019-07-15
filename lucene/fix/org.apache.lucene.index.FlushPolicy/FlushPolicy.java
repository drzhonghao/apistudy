

import org.apache.lucene.index.LiveIndexWriterConfig;
import org.apache.lucene.util.InfoStream;


abstract class FlushPolicy {
	protected LiveIndexWriterConfig indexWriterConfig;

	protected InfoStream infoStream;

	protected synchronized void init(LiveIndexWriterConfig indexWriterConfig) {
		this.indexWriterConfig = indexWriterConfig;
		infoStream = indexWriterConfig.getInfoStream();
	}

	private boolean assertMessage(String s) {
		if (infoStream.isEnabled("FP")) {
			infoStream.message("FP", s);
		}
		return true;
	}
}

