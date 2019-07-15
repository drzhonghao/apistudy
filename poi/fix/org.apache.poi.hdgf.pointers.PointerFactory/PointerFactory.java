

import org.apache.poi.hdgf.pointers.Pointer;
import org.apache.poi.hdgf.pointers.PointerV5;
import org.apache.poi.hdgf.pointers.PointerV6;


public final class PointerFactory {
	private int version;

	public PointerFactory(int version) {
		this.version = version;
	}

	public int getVersion() {
		return version;
	}

	public Pointer createPointer(byte[] data, int offset) {
		Pointer p;
		if ((version) >= 6) {
			p = new PointerV6();
			return p;
		}else
			if ((version) == 5) {
				p = new PointerV5();
				return p;
			}else {
				throw new IllegalArgumentException(("Visio files with versions below 5 are not supported, yours was " + (version)));
			}

	}

	public Pointer[] createContainerPointers(Pointer parent, byte[] data) {
		int numPointersOffset = parent.getNumPointersOffset(data);
		int numPointers = parent.getNumPointers(numPointersOffset, data);
		int skip = parent.getPostNumPointersSkip();
		int pos = numPointersOffset + skip;
		Pointer[] childPointers = new Pointer[numPointers];
		for (int i = 0; i < numPointers; i++) {
			childPointers[i] = this.createPointer(data, pos);
			pos += childPointers[i].getSizeInBytes();
		}
		return childPointers;
	}
}

