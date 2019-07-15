import org.apache.poi.hpbf.model.qcbits.QCBit;
import org.apache.poi.hpbf.model.qcbits.*;


import org.apache.poi.util.IOUtils;
import org.apache.poi.util.StringUtil;

/**
 * A Text based bit of Quill Contents
 */
public final class QCTextBit extends QCBit {

	//arbitrarily selected; may need to increase
	private static final int MAX_RECORD_LENGTH = 1_000_000;

	public QCTextBit(String thingType, String bitType, byte[] data) {
		super(thingType, bitType, data);
	}

	/**
	 * Returns the text. Note that line endings
	 *  are \r and not \n
	 */
	public String getText() {
		return StringUtil.getFromUnicodeLE(getData());
	}

	public void setText(String text) {
		byte data[] = IOUtils.safelyAllocate(text.length()*2, MAX_RECORD_LENGTH);
		StringUtil.putUnicodeLE(text, data, 0);
		setData(data);
	}
}
