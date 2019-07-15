

import org.apache.poi.hdgf.chunks.ChunkFactory;
import org.apache.poi.hdgf.pointers.Pointer;
import org.apache.poi.hdgf.pointers.PointerFactory;
import org.apache.poi.hdgf.streams.ChunkStream;
import org.apache.poi.hdgf.streams.Stream;
import org.apache.poi.hdgf.streams.StreamStore;


public class PointerContainingStream extends Stream {
	private Pointer[] childPointers;

	private Stream[] childStreams;

	private ChunkFactory chunkFactory;

	private PointerFactory pointerFactory;

	protected PointerContainingStream(Pointer pointer, StreamStore store, ChunkFactory chunkFactory, PointerFactory pointerFactory) {
		super(pointer, store);
		this.chunkFactory = chunkFactory;
		this.pointerFactory = pointerFactory;
	}

	protected Pointer[] getChildPointers() {
		return childPointers;
	}

	public Stream[] getPointedToStreams() {
		return childStreams;
	}

	public void findChildren(byte[] documentData) {
		childStreams = new Stream[childPointers.length];
		for (int i = 0; i < (childPointers.length); i++) {
			Pointer ptr = childPointers[i];
			childStreams[i] = Stream.createStream(ptr, documentData, chunkFactory, pointerFactory);
			if ((childStreams[i]) instanceof ChunkStream) {
				ChunkStream child = ((ChunkStream) (childStreams[i]));
				child.findChunks();
			}
			if ((childStreams[i]) instanceof PointerContainingStream) {
				PointerContainingStream child = ((PointerContainingStream) (childStreams[i]));
				child.findChildren(documentData);
			}
		}
	}
}

