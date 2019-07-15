

import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.nio.charset.Charset;
import org.apache.poi.hwmf.draw.HwmfDrawProperties;
import org.apache.poi.hwmf.draw.HwmfGraphics;
import org.apache.poi.hwmf.record.HwmfColorRef;
import org.apache.poi.hwmf.record.HwmfFont;
import org.apache.poi.hwmf.record.HwmfObjectTableEntry;
import org.apache.poi.hwmf.record.HwmfRecord;
import org.apache.poi.hwmf.record.HwmfRecordType;
import org.apache.poi.util.BitField;
import org.apache.poi.util.BitFieldFactory;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.LittleEndianConsts;
import org.apache.poi.util.LittleEndianInputStream;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;


public class HwmfText {
	private static final POILogger logger = POILogFactory.getLogger(HwmfText.class);

	private static final int MAX_RECORD_LENGTH = 1000000;

	public static class WmfSetTextCharExtra implements HwmfRecord {
		private int charExtra;

		@Override
		public HwmfRecordType getRecordType() {
			return HwmfRecordType.setTextCharExtra;
		}

		@Override
		public int init(LittleEndianInputStream leis, long recordSize, int recordFunction) throws IOException {
			charExtra = leis.readUShort();
			return LittleEndianConsts.SHORT_SIZE;
		}

		@Override
		public void draw(HwmfGraphics ctx) {
		}
	}

	public static class WmfSetTextColor implements HwmfRecord {
		private HwmfColorRef colorRef;

		@Override
		public HwmfRecordType getRecordType() {
			return HwmfRecordType.setTextColor;
		}

		@Override
		public int init(LittleEndianInputStream leis, long recordSize, int recordFunction) throws IOException {
			colorRef = new HwmfColorRef();
			return colorRef.init(leis);
		}

		@Override
		public void draw(HwmfGraphics ctx) {
			ctx.getProperties().setTextColor(colorRef);
		}
	}

	public static class WmfSetTextJustification implements HwmfRecord {
		private int breakCount;

		private int breakExtra;

		@Override
		public HwmfRecordType getRecordType() {
			return HwmfRecordType.setBkColor;
		}

		@Override
		public int init(LittleEndianInputStream leis, long recordSize, int recordFunction) throws IOException {
			breakCount = leis.readUShort();
			breakExtra = leis.readUShort();
			return 2 * (LittleEndianConsts.SHORT_SIZE);
		}

		@Override
		public void draw(HwmfGraphics ctx) {
		}
	}

	public static class WmfTextOut implements HwmfRecord {
		private int stringLength;

		private byte[] rawTextBytes;

		private int yStart;

		private int xStart;

		@Override
		public HwmfRecordType getRecordType() {
			return HwmfRecordType.textOut;
		}

		@Override
		public int init(LittleEndianInputStream leis, long recordSize, int recordFunction) throws IOException {
			stringLength = leis.readShort();
			rawTextBytes = IOUtils.safelyAllocate(((stringLength) + ((stringLength) & 1)), HwmfText.MAX_RECORD_LENGTH);
			leis.readFully(rawTextBytes);
			yStart = leis.readShort();
			xStart = leis.readShort();
			return (3 * (LittleEndianConsts.SHORT_SIZE)) + (rawTextBytes.length);
		}

		@Override
		public void draw(HwmfGraphics ctx) {
			Rectangle2D bounds = new Rectangle2D.Double(xStart, yStart, 0, 0);
			ctx.drawString(getTextBytes(), bounds);
		}

		public String getText(Charset charset) {
			return new String(getTextBytes(), charset);
		}

		private byte[] getTextBytes() {
			byte[] ret = IOUtils.safelyAllocate(stringLength, HwmfText.MAX_RECORD_LENGTH);
			System.arraycopy(rawTextBytes, 0, ret, 0, stringLength);
			return ret;
		}
	}

	public static class WmfExtTextOut implements HwmfRecord {
		private static final BitField ETO_OPAQUE = BitFieldFactory.getInstance(2);

		private static final BitField ETO_CLIPPED = BitFieldFactory.getInstance(4);

		private static final BitField ETO_GLYPH_INDEX = BitFieldFactory.getInstance(16);

		private static final BitField ETO_RTLREADING = BitFieldFactory.getInstance(128);

		private static final BitField ETO_NUMERICSLOCAL = BitFieldFactory.getInstance(1024);

		private static final BitField ETO_NUMERICSLATIN = BitFieldFactory.getInstance(2048);

		private static final BitField ETO_PDY = BitFieldFactory.getInstance(8192);

		private int y;

		private int x;

		private int stringLength;

		private int fwOpts;

		private int left;

		private int top;

		private int right;

		private int bottom;

		private byte[] rawTextBytes;

		private int[] dx;

		@Override
		public HwmfRecordType getRecordType() {
			return HwmfRecordType.extTextOut;
		}

		@Override
		public int init(LittleEndianInputStream leis, long recordSize, int recordFunction) throws IOException {
			final int remainingRecordSize = ((int) (recordSize - 6));
			y = leis.readShort();
			x = leis.readShort();
			stringLength = leis.readShort();
			fwOpts = leis.readUShort();
			int size = 4 * (LittleEndianConsts.SHORT_SIZE);
			if (((HwmfText.WmfExtTextOut.ETO_OPAQUE.isSet(fwOpts)) || (HwmfText.WmfExtTextOut.ETO_CLIPPED.isSet(fwOpts))) && ((size + 8) <= remainingRecordSize)) {
				left = leis.readShort();
				top = leis.readShort();
				right = leis.readShort();
				bottom = leis.readShort();
				size += 4 * (LittleEndianConsts.SHORT_SIZE);
			}
			rawTextBytes = IOUtils.safelyAllocate(((stringLength) + ((stringLength) & 1)), HwmfText.MAX_RECORD_LENGTH);
			leis.readFully(rawTextBytes);
			size += rawTextBytes.length;
			if (size >= remainingRecordSize) {
				HwmfText.logger.log(POILogger.INFO, "META_EXTTEXTOUT doesn't contain character tracking info");
				return size;
			}
			int dxLen = Math.min(stringLength, ((remainingRecordSize - size) / (LittleEndianConsts.SHORT_SIZE)));
			if (dxLen < (stringLength)) {
				HwmfText.logger.log(POILogger.WARN, "META_EXTTEXTOUT tracking info doesn't cover all characters");
			}
			dx = new int[stringLength];
			for (int i = 0; i < dxLen; i++) {
				dx[i] = leis.readShort();
				size += LittleEndianConsts.SHORT_SIZE;
			}
			return size;
		}

		@Override
		public void draw(HwmfGraphics ctx) {
			Rectangle2D bounds = new Rectangle2D.Double(x, y, 0, 0);
			ctx.drawString(getTextBytes(), bounds, dx);
		}

		public String getText(Charset charset) {
			return new String(getTextBytes(), charset);
		}

		private byte[] getTextBytes() {
			byte[] ret = IOUtils.safelyAllocate(stringLength, HwmfText.MAX_RECORD_LENGTH);
			System.arraycopy(rawTextBytes, 0, ret, 0, stringLength);
			return ret;
		}
	}

	public enum HwmfTextAlignment {

		LEFT,
		RIGHT,
		CENTER;}

	public enum HwmfTextVerticalAlignment {

		TOP,
		BOTTOM,
		BASELINE;}

	public static class WmfSetTextAlign implements HwmfRecord {
		@SuppressWarnings("unused")
		private static final BitField TA_NOUPDATECP = BitFieldFactory.getInstance(0);

		@SuppressWarnings("unused")
		private static final BitField TA_LEFT = BitFieldFactory.getInstance(0);

		@SuppressWarnings("unused")
		private static final BitField TA_TOP = BitFieldFactory.getInstance(0);

		@SuppressWarnings("unused")
		private static final BitField TA_UPDATECP = BitFieldFactory.getInstance(1);

		private static final BitField TA_RIGHT = BitFieldFactory.getInstance(2);

		private static final BitField TA_CENTER = BitFieldFactory.getInstance(6);

		private static final BitField TA_BOTTOM = BitFieldFactory.getInstance(8);

		private static final BitField TA_BASELINE = BitFieldFactory.getInstance(24);

		@SuppressWarnings("unused")
		private static final BitField TA_RTLREADING = BitFieldFactory.getInstance(256);

		@SuppressWarnings("unused")
		private static final BitField VTA_TOP = BitFieldFactory.getInstance(0);

		@SuppressWarnings("unused")
		private static final BitField VTA_RIGHT = BitFieldFactory.getInstance(0);

		private static final BitField VTA_BOTTOM = BitFieldFactory.getInstance(2);

		private static final BitField VTA_CENTER = BitFieldFactory.getInstance(6);

		private static final BitField VTA_LEFT = BitFieldFactory.getInstance(8);

		private static final BitField VTA_BASELINE = BitFieldFactory.getInstance(24);

		private int textAlignmentMode;

		@Override
		public HwmfRecordType getRecordType() {
			return HwmfRecordType.setTextAlign;
		}

		@Override
		public int init(LittleEndianInputStream leis, long recordSize, int recordFunction) throws IOException {
			textAlignmentMode = leis.readUShort();
			return LittleEndianConsts.SHORT_SIZE;
		}

		@Override
		public void draw(HwmfGraphics ctx) {
			HwmfDrawProperties props = ctx.getProperties();
			if (HwmfText.WmfSetTextAlign.TA_CENTER.isSet(textAlignmentMode)) {
			}else
				if (HwmfText.WmfSetTextAlign.TA_RIGHT.isSet(textAlignmentMode)) {
				}else {
				}

			if (HwmfText.WmfSetTextAlign.VTA_CENTER.isSet(textAlignmentMode)) {
			}else
				if (HwmfText.WmfSetTextAlign.VTA_LEFT.isSet(textAlignmentMode)) {
				}else {
				}

			if (HwmfText.WmfSetTextAlign.TA_BASELINE.isSet(textAlignmentMode)) {
			}else
				if (HwmfText.WmfSetTextAlign.TA_BOTTOM.isSet(textAlignmentMode)) {
				}else {
				}

			if (HwmfText.WmfSetTextAlign.VTA_BASELINE.isSet(textAlignmentMode)) {
			}else
				if (HwmfText.WmfSetTextAlign.VTA_BOTTOM.isSet(textAlignmentMode)) {
				}else {
				}

		}
	}

	public static class WmfCreateFontIndirect implements HwmfObjectTableEntry , HwmfRecord {
		private HwmfFont font;

		@Override
		public HwmfRecordType getRecordType() {
			return HwmfRecordType.createFontIndirect;
		}

		@Override
		public int init(LittleEndianInputStream leis, long recordSize, int recordFunction) throws IOException {
			font = new HwmfFont();
			return font.init(leis);
		}

		@Override
		public void draw(HwmfGraphics ctx) {
			ctx.addObjectTableEntry(this);
		}

		@Override
		public void applyObject(HwmfGraphics ctx) {
			ctx.getProperties().setFont(font);
		}

		public HwmfFont getFont() {
			return font;
		}
	}
}

