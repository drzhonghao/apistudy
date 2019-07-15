

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.NoSuchElementException;
import org.apache.poi.hwpf.model.FileInformationBlock;
import org.apache.poi.hwpf.model.LFO;
import org.apache.poi.hwpf.model.LFOData;
import org.apache.poi.hwpf.model.types.LFOAbstractType;
import org.apache.poi.util.LittleEndian;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;


public class PlfLfo {
	private static final POILogger log = POILogFactory.getLogger(PlfLfo.class);

	private int _lfoMac;

	private LFO[] _rgLfo;

	private LFOData[] _rgLfoData;

	PlfLfo(byte[] tableStream, int fcPlfLfo, int lcbPlfLfo) {
		int offset = fcPlfLfo;
		long lfoMacLong = LittleEndian.getUInt(tableStream, offset);
		offset += LittleEndian.INT_SIZE;
		if (lfoMacLong > (Integer.MAX_VALUE)) {
			throw new UnsupportedOperationException((("Apache POI doesn't support rgLfo/rgLfoData size large than " + (Integer.MAX_VALUE)) + " elements"));
		}
		this._lfoMac = ((int) (lfoMacLong));
		_rgLfo = new LFO[_lfoMac];
		_rgLfoData = new LFOData[_lfoMac];
		for (int x = 0; x < (_lfoMac); x++) {
			LFO lfo = new LFO(tableStream, offset);
			offset += LFO.getSize();
			_rgLfo[x] = lfo;
		}
		for (int x = 0; x < (_lfoMac); x++) {
		}
		if ((offset - fcPlfLfo) != lcbPlfLfo) {
			if (PlfLfo.log.check(POILogger.WARN)) {
				PlfLfo.log.log(POILogger.WARN, ((("Actual size of PlfLfo is " + (offset - fcPlfLfo)) + " bytes, but expected ") + lcbPlfLfo));
			}
		}
	}

	void add(LFO lfo, LFOData lfoData) {
		_rgLfo = Arrays.copyOf(_rgLfo, ((_lfoMac) + 1));
		_rgLfo[_lfoMac] = lfo;
		_rgLfoData = Arrays.copyOf(_rgLfoData, ((_lfoMac) + 1));
		_rgLfoData[_lfoMac] = lfoData;
		_lfoMac = (_lfoMac) + 1;
	}

	@Override
	public boolean equals(Object obj) {
		if ((this) == obj)
			return true;

		if (obj == null)
			return false;

		if ((getClass()) != (obj.getClass()))
			return false;

		PlfLfo other = ((PlfLfo) (obj));
		return (((_lfoMac) == (other._lfoMac)) && (Arrays.equals(_rgLfo, other._rgLfo))) && (Arrays.equals(_rgLfoData, other._rgLfoData));
	}

	public int getLfoMac() {
		return _lfoMac;
	}

	public int getIlfoByLsid(int lsid) {
		for (int i = 0; i < (_lfoMac); i++) {
			if ((_rgLfo[i].getLsid()) == lsid) {
				return i + 1;
			}
		}
		throw new NoSuchElementException((("LFO with lsid " + lsid) + " not found"));
	}

	public LFO getLfo(int ilfo) throws NoSuchElementException {
		if ((ilfo <= 0) || (ilfo > (_lfoMac))) {
			throw new NoSuchElementException(((("LFO with ilfo " + ilfo) + " not found. lfoMac is ") + (_lfoMac)));
		}
		return _rgLfo[(ilfo - 1)];
	}

	public LFOData getLfoData(int ilfo) throws NoSuchElementException {
		if ((ilfo <= 0) || (ilfo > (_lfoMac))) {
			throw new NoSuchElementException(((("LFOData with ilfo " + ilfo) + " not found. lfoMac is ") + (_lfoMac)));
		}
		return _rgLfoData[(ilfo - 1)];
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = (prime * result) + (_lfoMac);
		result = (prime * result) + (Arrays.hashCode(_rgLfo));
		result = (prime * result) + (Arrays.hashCode(_rgLfoData));
		return result;
	}

	void writeTo(FileInformationBlock fib, ByteArrayOutputStream outputStream) throws IOException {
		final int offset = outputStream.size();
		fib.setFcPlfLfo(offset);
		LittleEndian.putUInt(_lfoMac, outputStream);
		byte[] bs = new byte[(LFO.getSize()) * (_lfoMac)];
		for (int i = 0; i < (_lfoMac); i++) {
			_rgLfo[i].serialize(bs, (i * (LFO.getSize())));
		}
		outputStream.write(bs, 0, ((LFO.getSize()) * (_lfoMac)));
		for (int i = 0; i < (_lfoMac); i++) {
		}
		fib.setLcbPlfLfo(((outputStream.size()) - offset));
	}
}

