

import org.apache.poi.hpsf.GUID;
import org.apache.poi.util.Internal;
import org.apache.poi.util.LittleEndianByteArrayInputStream;


@Internal
public class VersionedStream {
	private final GUID _versionGuid = new GUID();

	public VersionedStream() {
	}

	public void read(LittleEndianByteArrayInputStream lei) {
		_versionGuid.read(lei);
	}
}

