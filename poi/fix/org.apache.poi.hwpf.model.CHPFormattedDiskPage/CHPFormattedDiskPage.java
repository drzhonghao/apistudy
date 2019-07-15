

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.poi.hwpf.model.CHPX;
import org.apache.poi.hwpf.model.CharIndexTranslator;
import org.apache.poi.hwpf.model.FormattedDiskPage;
import org.apache.poi.hwpf.model.PropertyNode;
import org.apache.poi.hwpf.model.TextPieceTable;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.Internal;
import org.apache.poi.util.LittleEndian;
import org.apache.poi.util.RecordFormatException;


@Internal
public final class CHPFormattedDiskPage extends FormattedDiskPage {
	private static final int FC_SIZE = 4;

	private static final int MAX_RECORD_LENGTH = 100000;

	private ArrayList<CHPX> _chpxList = new ArrayList<>();

	private ArrayList<CHPX> _overFlow;

	public CHPFormattedDiskPage() {
	}

	@Deprecated
	public CHPFormattedDiskPage(byte[] documentStream, int offset, int fcMin, TextPieceTable tpt) {
		this(documentStream, offset, tpt);
	}

	public CHPFormattedDiskPage(byte[] documentStream, int offset, CharIndexTranslator translator) {
		super(documentStream, offset);
		for (int x = 0; x < (_crun); x++) {
			int bytesStartAt = getStart(x);
			int bytesEndAt = getEnd(x);
			for (int[] range : translator.getCharIndexRanges(bytesStartAt, bytesEndAt)) {
			}
		}
	}

	public CHPX getCHPX(int index) {
		return _chpxList.get(index);
	}

	public List<CHPX> getCHPXs() {
		return Collections.unmodifiableList(_chpxList);
	}

	public void fill(List<CHPX> filler) {
		_chpxList.addAll(filler);
	}

	public ArrayList<CHPX> getOverflow() {
		return _overFlow;
	}

	@Override
	protected byte[] getGrpprl(int index) {
		int chpxOffset = 2 * (LittleEndian.getUByte(_fkp, ((_offset) + ((((_crun) + 1) * 4) + index))));
		if (chpxOffset == 0) {
			return new byte[0];
		}
		int size = LittleEndian.getUByte(_fkp, ((_offset) + chpxOffset));
		byte[] chpx = IOUtils.safelyAllocate(size, CHPFormattedDiskPage.MAX_RECORD_LENGTH);
		System.arraycopy(_fkp, ((_offset) + (++chpxOffset)), chpx, 0, size);
		return chpx;
	}

	protected byte[] toByteArray(CharIndexTranslator translator) {
		byte[] buf = new byte[512];
		int size = _chpxList.size();
		int grpprlOffset = 511;
		int offsetOffset = 0;
		int fcOffset = 0;
		int totalSize = (CHPFormattedDiskPage.FC_SIZE) + 2;
		int index = 0;
		for (; index < size; index++) {
			int grpprlLength = _chpxList.get(index).getGrpprl().length;
			totalSize += ((CHPFormattedDiskPage.FC_SIZE) + 2) + grpprlLength;
			if (totalSize > (511 + (index % 2))) {
				totalSize -= ((CHPFormattedDiskPage.FC_SIZE) + 2) + grpprlLength;
				break;
			}
			if (((1 + grpprlLength) % 2) > 0) {
				totalSize += 1;
			}
		}
		if (index == 0) {
			throw new RecordFormatException("empty grpprl entry.");
		}
		if (index != size) {
			_overFlow = new ArrayList<>();
			_overFlow.addAll(_chpxList.subList(index, size));
		}
		buf[511] = ((byte) (index));
		offsetOffset = ((CHPFormattedDiskPage.FC_SIZE) * index) + (CHPFormattedDiskPage.FC_SIZE);
		int chpxEnd = 0;
		for (CHPX chpx : _chpxList.subList(0, index)) {
			int chpxStart = translator.getByteIndex(chpx.getStart());
			chpxEnd = translator.getByteIndex(chpx.getEnd());
			LittleEndian.putInt(buf, fcOffset, chpxStart);
			byte[] grpprl = chpx.getGrpprl();
			grpprlOffset -= 1 + (grpprl.length);
			grpprlOffset -= grpprlOffset % 2;
			buf[offsetOffset] = ((byte) (grpprlOffset / 2));
			buf[grpprlOffset] = ((byte) (grpprl.length));
			System.arraycopy(grpprl, 0, buf, (grpprlOffset + 1), grpprl.length);
			offsetOffset += 1;
			fcOffset += CHPFormattedDiskPage.FC_SIZE;
		}
		LittleEndian.putInt(buf, fcOffset, chpxEnd);
		return buf;
	}
}

