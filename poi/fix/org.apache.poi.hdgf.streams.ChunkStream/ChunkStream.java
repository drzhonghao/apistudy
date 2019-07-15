

import java.util.ArrayList;
import org.apache.poi.hdgf.chunks.Chunk;
import org.apache.poi.hdgf.chunks.ChunkFactory;
import org.apache.poi.hdgf.pointers.Pointer;
import org.apache.poi.hdgf.streams.Stream;
import org.apache.poi.hdgf.streams.StreamStore;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;


public final class ChunkStream extends Stream {
	private static final POILogger logger = POILogFactory.getLogger(ChunkStream.class);

	private ChunkFactory chunkFactory;

	private Chunk[] chunks;

	protected ChunkStream(Pointer pointer, StreamStore store, ChunkFactory chunkFactory) {
		super(pointer, store);
		this.chunkFactory = chunkFactory;
	}

	public Chunk[] getChunks() {
		return chunks;
	}

	public void findChunks() {
		ArrayList<Chunk> chunksA = new ArrayList<>();
		if ((getPointer().getOffset()) == 25779) {
			int i = 0;
			i++;
		}
		int pos = 0;
		try {
		} catch (Exception e) {
			ChunkStream.logger.log(POILogger.ERROR, ((("Failed to create chunk at " + pos) + ", ignoring rest of data.") + e));
		}
		chunks = chunksA.toArray(new Chunk[chunksA.size()]);
	}
}

