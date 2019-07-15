

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.SegmentCommitInfo;
import org.apache.lucene.index.SegmentReader;
import org.apache.lucene.replicator.nrt.FileMetaData;
import org.apache.lucene.replicator.nrt.Node;
import org.apache.lucene.replicator.nrt.PrimaryNode;


class PreCopyMergedSegmentWarmer implements IndexWriter.IndexReaderWarmer {
	private final PrimaryNode primary;

	public PreCopyMergedSegmentWarmer(PrimaryNode primary) {
		this.primary = primary;
	}

	@Override
	public void warm(LeafReader reader) throws IOException {
		long startNS = System.nanoTime();
		final SegmentCommitInfo info = ((SegmentReader) (reader)).getSegmentInfo();
		Map<String, FileMetaData> filesMetaData = new HashMap<>();
		for (String fileName : info.files()) {
			FileMetaData metaData = primary.readLocalFileMetaData(fileName);
			assert metaData != null;
			assert (filesMetaData.containsKey(fileName)) == false;
			filesMetaData.put(fileName, metaData);
		}
		primary.message(String.format(Locale.ROOT, (("top: done warm merge " + info) + ": took %.3f sec, %.1f MB"), (((System.nanoTime()) - startNS) / 1.0E9), (((info.sizeInBytes()) / 1024) / 1024.0)));
	}
}

