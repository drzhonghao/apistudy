

import org.apache.poi.ddf.EscherChildAnchorRecord;
import org.apache.poi.ddf.EscherClientAnchorRecord;
import org.apache.poi.ddf.EscherContainerRecord;
import org.apache.poi.ddf.EscherRecord;
import org.apache.poi.ss.usermodel.ChildAnchor;


public abstract class HSSFAnchor implements ChildAnchor {
	protected boolean _isHorizontallyFlipped;

	protected boolean _isVerticallyFlipped;

	public HSSFAnchor() {
		createEscherAnchor();
	}

	public HSSFAnchor(int dx1, int dy1, int dx2, int dy2) {
		createEscherAnchor();
		setDx1(dx1);
		setDy1(dy1);
		setDx2(dx2);
		setDy2(dy2);
	}

	public static HSSFAnchor createAnchorFromEscher(EscherContainerRecord container) {
		if (null != (container.getChildById(EscherChildAnchorRecord.RECORD_ID))) {
		}else {
			if (null != (container.getChildById(EscherClientAnchorRecord.RECORD_ID))) {
			}
			return null;
		}
		return null;
	}

	public abstract boolean isHorizontallyFlipped();

	public abstract boolean isVerticallyFlipped();

	protected abstract EscherRecord getEscherAnchor();

	protected abstract void createEscherAnchor();
}

