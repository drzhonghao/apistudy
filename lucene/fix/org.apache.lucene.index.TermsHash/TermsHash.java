

import java.io.IOException;
import org.apache.lucene.util.ByteBlockPool;
import org.apache.lucene.util.Counter;
import org.apache.lucene.util.IntBlockPool;


abstract class TermsHash {
	final TermsHash nextTermsHash = null;

	final IntBlockPool intPool = null;

	final ByteBlockPool bytePool = null;

	ByteBlockPool termBytePool;

	final Counter bytesUsed = null;

	final boolean trackAllocations = false;

	public void abort() {
		try {
			reset();
		} finally {
			if ((nextTermsHash) != null) {
				nextTermsHash.abort();
			}
		}
	}

	void reset() {
		intPool.reset(false, false);
		bytePool.reset(false, false);
	}

	void finishDocument() throws IOException {
		if ((nextTermsHash) != null) {
			nextTermsHash.finishDocument();
		}
	}

	void startDocument() throws IOException {
		if ((nextTermsHash) != null) {
			nextTermsHash.startDocument();
		}
	}
}

