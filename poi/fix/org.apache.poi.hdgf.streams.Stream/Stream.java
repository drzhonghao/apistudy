

import org.apache.poi.hdgf.chunks.ChunkFactory;
import org.apache.poi.hdgf.pointers.Pointer;
import org.apache.poi.hdgf.pointers.PointerFactory;
import org.apache.poi.hdgf.streams.StreamStore;


public abstract class Stream {
	private Pointer pointer;

	private StreamStore store;

	public Pointer getPointer() {
		return pointer;
	}

	protected StreamStore getStore() {
		return store;
	}

	public StreamStore _getStore() {
		return store;
	}

	public int _getContentsLength() {
		return 0;
	}

	protected Stream(Pointer pointer, StreamStore store) {
		this.pointer = pointer;
		this.store = store;
	}

	public static Stream createStream(Pointer pointer, byte[] documentData, ChunkFactory chunkFactory, PointerFactory pointerFactory) {
		StreamStore store;
		if (pointer.destinationCompressed()) {
		}else {
		}
		if ((pointer.getType()) == 20) {
		}else
			if (pointer.destinationHasPointers()) {
			}else
				if (pointer.destinationHasChunks()) {
				}else
					if (pointer.destinationHasStrings()) {
					}



		return null;
	}
}

