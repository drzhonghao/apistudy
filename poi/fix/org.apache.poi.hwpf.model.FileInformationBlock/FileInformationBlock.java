

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Locale;
import org.apache.poi.hwpf.model.FIBFieldHandler;
import org.apache.poi.hwpf.model.FSPADocumentPart;
import org.apache.poi.hwpf.model.FibBase;
import org.apache.poi.hwpf.model.FibRgLw;
import org.apache.poi.hwpf.model.FibRgLw97;
import org.apache.poi.hwpf.model.FibRgW97;
import org.apache.poi.hwpf.model.FieldsDocumentPart;
import org.apache.poi.hwpf.model.NoteType;
import org.apache.poi.hwpf.model.SubdocumentType;
import org.apache.poi.hwpf.model.types.FibBaseAbstractType;
import org.apache.poi.hwpf.model.types.FibRgLw97AbstractType;
import org.apache.poi.hwpf.model.types.FibRgW97AbstractType;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.Internal;
import org.apache.poi.util.LittleEndian;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;


@Internal
public final class FileInformationBlock {
	private static final int MAX_RECORD_LENGTH = 100000;

	public static final POILogger logger = POILogFactory.getLogger(FileInformationBlock.class);

	private FibBase _fibBase;

	private int _csw;

	private FibRgW97 _fibRgW;

	private int _cslw;

	private FibRgLw _fibRgLw;

	private int _cbRgFcLcb;

	private FIBFieldHandler _fieldHandler;

	private int _cswNew;

	private int _nFibNew;

	private byte[] _fibRgCswNew;

	public FileInformationBlock(byte[] mainDocument) {
		int offset = 0;
		_fibBase = new FibBase(mainDocument, offset);
		offset = FibBase.getSize();
		assert offset == 32;
		_csw = LittleEndian.getUShort(mainDocument, offset);
		offset += LittleEndian.SHORT_SIZE;
		assert offset == 34;
		_fibRgW = new FibRgW97(mainDocument, offset);
		offset += FibRgW97.getSize();
		assert offset == 62;
		_cslw = LittleEndian.getUShort(mainDocument, offset);
		offset += LittleEndian.SHORT_SIZE;
		assert offset == 64;
		if ((_fibBase.getNFib()) < 105) {
			offset += FibRgLw97.getSize();
			_cbRgFcLcb = 74;
			offset += ((_cbRgFcLcb) * (LittleEndian.INT_SIZE)) * 2;
			_cswNew = LittleEndian.getUShort(mainDocument, offset);
			offset += LittleEndian.SHORT_SIZE;
			_cswNew = 0;
			_nFibNew = -1;
			_fibRgCswNew = new byte[0];
			return;
		}
		_fibRgLw = new FibRgLw97(mainDocument, offset);
		offset += FibRgLw97.getSize();
		assert offset == 152;
		_cbRgFcLcb = LittleEndian.getUShort(mainDocument, offset);
		offset += LittleEndian.SHORT_SIZE;
		assert offset == 154;
		offset += ((_cbRgFcLcb) * (LittleEndian.INT_SIZE)) * 2;
		_cswNew = LittleEndian.getUShort(mainDocument, offset);
		offset += LittleEndian.SHORT_SIZE;
		if ((_cswNew) != 0) {
			_nFibNew = LittleEndian.getUShort(mainDocument, offset);
			offset += LittleEndian.SHORT_SIZE;
			final int fibRgCswNewLength = ((_cswNew) - 1) * (LittleEndian.SHORT_SIZE);
			_fibRgCswNew = IOUtils.safelyAllocate(fibRgCswNewLength, FileInformationBlock.MAX_RECORD_LENGTH);
			LittleEndian.getByteArray(mainDocument, offset, fibRgCswNewLength, FileInformationBlock.MAX_RECORD_LENGTH);
			offset += fibRgCswNewLength;
		}else {
			_nFibNew = -1;
			_fibRgCswNew = new byte[0];
		}
		assertCbRgFcLcb();
		assertCswNew();
	}

	private void assertCbRgFcLcb() {
		int nfib = getNFib();
		String nfibHex = String.format(Locale.ROOT, "%04X", nfib);
		switch (nfib) {
			case 113 :
				break;
			case 190 :
			case 191 :
			case 192 :
			case 193 :
			case 194 :
			case 195 :
				FileInformationBlock.assertCbRgFcLcb(nfibHex, 93, "0x005D", _cbRgFcLcb);
				break;
			case 216 :
			case 217 :
				FileInformationBlock.assertCbRgFcLcb(nfibHex, 108, "0x006C", _cbRgFcLcb);
				break;
			case 257 :
				FileInformationBlock.assertCbRgFcLcb("0x0101", 136, "0x0088", _cbRgFcLcb);
				break;
			case 267 :
			case 268 :
				FileInformationBlock.assertCbRgFcLcb(nfibHex, 164, "0x00A4", _cbRgFcLcb);
				break;
			case 274 :
				FileInformationBlock.assertCbRgFcLcb("0x0112", 183, "0x00B7", _cbRgFcLcb);
				break;
			default :
				FileInformationBlock.logger.log(POILogger.WARN, (((("Invalid file format version number: " + nfib) + "(") + nfibHex) + ")"));
		}
	}

	private static void assertCbRgFcLcb(final String strNFib, final int expectedCbRgFcLcb, final String strCbRgFcLcb, final int cbRgFcLcb) {
		if (cbRgFcLcb == expectedCbRgFcLcb)
			return;

		FileInformationBlock.logger.log(POILogger.WARN, "Since FIB.nFib == ", strNFib, " value of FIB.cbRgFcLcb MUST be ", (strCbRgFcLcb + ", not 0x"), Integer.toHexString(cbRgFcLcb));
	}

	private void assertCswNew() {
		switch (getNFib()) {
			case 193 :
				FileInformationBlock.assertCswNew("0x00C1", 0, "0x0000", _cswNew);
				break;
			case 217 :
				FileInformationBlock.assertCswNew("0x00D9", 2, "0x0002", _cswNew);
				break;
			case 257 :
				FileInformationBlock.assertCswNew("0x0101", 2, "0x0002", _cswNew);
				break;
			case 268 :
				FileInformationBlock.assertCswNew("0x010C", 2, "0x0002", _cswNew);
				break;
			case 274 :
				FileInformationBlock.assertCswNew("0x0112", 5, "0x0005", _cswNew);
				break;
			default :
				FileInformationBlock.logger.log(POILogger.WARN, ("Invalid file format version number: " + (getNFib())));
		}
	}

	private static void assertCswNew(final String strNFib, final int expectedCswNew, final String strExpectedCswNew, final int cswNew) {
		if (cswNew == expectedCswNew)
			return;

		FileInformationBlock.logger.log(POILogger.WARN, "Since FIB.nFib == ", strNFib, " value of FIB.cswNew MUST be ", (strExpectedCswNew + ", not 0x"), Integer.toHexString(cswNew));
	}

	public void fillVariableFields(byte[] mainDocument, byte[] tableStream) {
		HashSet<Integer> knownFieldSet = new HashSet<>();
		knownFieldSet.add(Integer.valueOf(FIBFieldHandler.STSHF));
		knownFieldSet.add(Integer.valueOf(FIBFieldHandler.CLX));
		knownFieldSet.add(Integer.valueOf(FIBFieldHandler.DOP));
		knownFieldSet.add(Integer.valueOf(FIBFieldHandler.PLCFBTECHPX));
		knownFieldSet.add(Integer.valueOf(FIBFieldHandler.PLCFBTEPAPX));
		knownFieldSet.add(Integer.valueOf(FIBFieldHandler.PLCFSED));
		knownFieldSet.add(Integer.valueOf(FIBFieldHandler.PLFLST));
		knownFieldSet.add(Integer.valueOf(FIBFieldHandler.PLFLFO));
		for (FieldsDocumentPart part : FieldsDocumentPart.values())
			knownFieldSet.add(Integer.valueOf(part.getFibFieldsField()));

		knownFieldSet.add(Integer.valueOf(FIBFieldHandler.PLCFBKF));
		knownFieldSet.add(Integer.valueOf(FIBFieldHandler.PLCFBKL));
		knownFieldSet.add(Integer.valueOf(FIBFieldHandler.STTBFBKMK));
		for (NoteType noteType : NoteType.values()) {
			knownFieldSet.add(Integer.valueOf(noteType.getFibDescriptorsFieldIndex()));
			knownFieldSet.add(Integer.valueOf(noteType.getFibTextPositionsFieldIndex()));
		}
		knownFieldSet.add(Integer.valueOf(FIBFieldHandler.STTBFFFN));
		knownFieldSet.add(Integer.valueOf(FIBFieldHandler.STTBFRMARK));
		knownFieldSet.add(Integer.valueOf(FIBFieldHandler.STTBSAVEDBY));
		knownFieldSet.add(Integer.valueOf(FIBFieldHandler.MODIFIED));
	}

	@Override
	public String toString() {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(_fibBase);
		stringBuilder.append("[FIB2]\n");
		stringBuilder.append("\tSubdocuments info:\n");
		for (SubdocumentType type : SubdocumentType.values()) {
			stringBuilder.append("\t\t");
			stringBuilder.append(type);
			stringBuilder.append(" has length of ");
			stringBuilder.append(getSubdocumentTextStreamLength(type));
			stringBuilder.append(" char(s)\n");
		}
		stringBuilder.append("\tFields PLCF info:\n");
		for (FieldsDocumentPart part : FieldsDocumentPart.values()) {
			stringBuilder.append("\t\t");
			stringBuilder.append(part);
			stringBuilder.append(": PLCF starts at ");
			stringBuilder.append(getFieldsPlcfOffset(part));
			stringBuilder.append(" and have length of ");
			stringBuilder.append(getFieldsPlcfLength(part));
			stringBuilder.append("\n");
		}
		stringBuilder.append("\tNotes PLCF info:\n");
		for (NoteType noteType : NoteType.values()) {
			stringBuilder.append("\t\t");
			stringBuilder.append(noteType);
			stringBuilder.append(": descriptions starts ");
			stringBuilder.append(getNotesDescriptorsOffset(noteType));
			stringBuilder.append(" and have length of ");
			stringBuilder.append(getNotesDescriptorsSize(noteType));
			stringBuilder.append(" bytes\n");
			stringBuilder.append("\t\t");
			stringBuilder.append(noteType);
			stringBuilder.append(": text positions starts ");
			stringBuilder.append(getNotesTextPositionsOffset(noteType));
			stringBuilder.append(" and have length of ");
			stringBuilder.append(getNotesTextPositionsSize(noteType));
			stringBuilder.append(" bytes\n");
		}
		stringBuilder.append(_fieldHandler);
		try {
			stringBuilder.append("\tJava reflection info:\n");
			for (Method method : FileInformationBlock.class.getMethods()) {
				if ((((!(method.getName().startsWith("get"))) || (!(Modifier.isPublic(method.getModifiers())))) || (Modifier.isStatic(method.getModifiers()))) || ((method.getParameterTypes().length) > 0))
					continue;

				stringBuilder.append("\t\t");
				stringBuilder.append(method.getName());
				stringBuilder.append(" => ");
				stringBuilder.append(method.invoke(this));
				stringBuilder.append("\n");
			}
		} catch (Exception exc) {
			stringBuilder.append("(exc: ").append(exc.getMessage()).append(")");
		}
		stringBuilder.append("[/FIB2]\n");
		return stringBuilder.toString();
	}

	public int getNFib() {
		if ((_cswNew) == 0)
			return _fibBase.getNFib();

		return _nFibNew;
	}

	public int getFcDop() {
		return _fieldHandler.getFieldOffset(FIBFieldHandler.DOP);
	}

	public void setFcDop(int fcDop) {
		_fieldHandler.setFieldOffset(FIBFieldHandler.DOP, fcDop);
	}

	public int getLcbDop() {
		return _fieldHandler.getFieldSize(FIBFieldHandler.DOP);
	}

	public void setLcbDop(int lcbDop) {
		_fieldHandler.setFieldSize(FIBFieldHandler.DOP, lcbDop);
	}

	public int getFcStshf() {
		return _fieldHandler.getFieldOffset(FIBFieldHandler.STSHF);
	}

	public int getLcbStshf() {
		return _fieldHandler.getFieldSize(FIBFieldHandler.STSHF);
	}

	public void setFcStshf(int fcStshf) {
		_fieldHandler.setFieldOffset(FIBFieldHandler.STSHF, fcStshf);
	}

	public void setLcbStshf(int lcbStshf) {
		_fieldHandler.setFieldSize(FIBFieldHandler.STSHF, lcbStshf);
	}

	public int getFcClx() {
		return _fieldHandler.getFieldOffset(FIBFieldHandler.CLX);
	}

	public int getLcbClx() {
		return _fieldHandler.getFieldSize(FIBFieldHandler.CLX);
	}

	public void setFcClx(int fcClx) {
		_fieldHandler.setFieldOffset(FIBFieldHandler.CLX, fcClx);
	}

	public void setLcbClx(int lcbClx) {
		_fieldHandler.setFieldSize(FIBFieldHandler.CLX, lcbClx);
	}

	public int getFcPlcfbteChpx() {
		return _fieldHandler.getFieldOffset(FIBFieldHandler.PLCFBTECHPX);
	}

	public int getLcbPlcfbteChpx() {
		return _fieldHandler.getFieldSize(FIBFieldHandler.PLCFBTECHPX);
	}

	public void setFcPlcfbteChpx(int fcPlcfBteChpx) {
		_fieldHandler.setFieldOffset(FIBFieldHandler.PLCFBTECHPX, fcPlcfBteChpx);
	}

	public void setLcbPlcfbteChpx(int lcbPlcfBteChpx) {
		_fieldHandler.setFieldSize(FIBFieldHandler.PLCFBTECHPX, lcbPlcfBteChpx);
	}

	public int getFcPlcfbtePapx() {
		return _fieldHandler.getFieldOffset(FIBFieldHandler.PLCFBTEPAPX);
	}

	public int getLcbPlcfbtePapx() {
		return _fieldHandler.getFieldSize(FIBFieldHandler.PLCFBTEPAPX);
	}

	public void setFcPlcfbtePapx(int fcPlcfBtePapx) {
		_fieldHandler.setFieldOffset(FIBFieldHandler.PLCFBTEPAPX, fcPlcfBtePapx);
	}

	public void setLcbPlcfbtePapx(int lcbPlcfBtePapx) {
		_fieldHandler.setFieldSize(FIBFieldHandler.PLCFBTEPAPX, lcbPlcfBtePapx);
	}

	public int getFcPlcfsed() {
		return _fieldHandler.getFieldOffset(FIBFieldHandler.PLCFSED);
	}

	public int getLcbPlcfsed() {
		return _fieldHandler.getFieldSize(FIBFieldHandler.PLCFSED);
	}

	public void setFcPlcfsed(int fcPlcfSed) {
		_fieldHandler.setFieldOffset(FIBFieldHandler.PLCFSED, fcPlcfSed);
	}

	public void setLcbPlcfsed(int lcbPlcfSed) {
		_fieldHandler.setFieldSize(FIBFieldHandler.PLCFSED, lcbPlcfSed);
	}

	@Deprecated
	public int getFcPlcfLst() {
		return _fieldHandler.getFieldOffset(FIBFieldHandler.PLCFLST);
	}

	public int getFcPlfLst() {
		return _fieldHandler.getFieldOffset(FIBFieldHandler.PLFLST);
	}

	@Deprecated
	public int getLcbPlcfLst() {
		return _fieldHandler.getFieldSize(FIBFieldHandler.PLCFLST);
	}

	public int getLcbPlfLst() {
		return _fieldHandler.getFieldSize(FIBFieldHandler.PLFLST);
	}

	@Deprecated
	public void setFcPlcfLst(int fcPlcfLst) {
		_fieldHandler.setFieldOffset(FIBFieldHandler.PLCFLST, fcPlcfLst);
	}

	public void setFcPlfLst(int fcPlfLst) {
		_fieldHandler.setFieldOffset(FIBFieldHandler.PLFLST, fcPlfLst);
	}

	@Deprecated
	public void setLcbPlcfLst(int lcbPlcfLst) {
		_fieldHandler.setFieldSize(FIBFieldHandler.PLCFLST, lcbPlcfLst);
	}

	public void setLcbPlfLst(int lcbPlfLst) {
		_fieldHandler.setFieldSize(FIBFieldHandler.PLFLST, lcbPlfLst);
	}

	public int getFcPlfLfo() {
		return _fieldHandler.getFieldOffset(FIBFieldHandler.PLFLFO);
	}

	public int getLcbPlfLfo() {
		return _fieldHandler.getFieldSize(FIBFieldHandler.PLFLFO);
	}

	public int getFcSttbfbkmk() {
		return _fieldHandler.getFieldOffset(FIBFieldHandler.STTBFBKMK);
	}

	public void setFcSttbfbkmk(int offset) {
		_fieldHandler.setFieldOffset(FIBFieldHandler.STTBFBKMK, offset);
	}

	public int getLcbSttbfbkmk() {
		return _fieldHandler.getFieldSize(FIBFieldHandler.STTBFBKMK);
	}

	public void setLcbSttbfbkmk(int length) {
		_fieldHandler.setFieldSize(FIBFieldHandler.STTBFBKMK, length);
	}

	public int getFcPlcfbkf() {
		return _fieldHandler.getFieldOffset(FIBFieldHandler.PLCFBKF);
	}

	public void setFcPlcfbkf(int offset) {
		_fieldHandler.setFieldOffset(FIBFieldHandler.PLCFBKF, offset);
	}

	public int getLcbPlcfbkf() {
		return _fieldHandler.getFieldSize(FIBFieldHandler.PLCFBKF);
	}

	public void setLcbPlcfbkf(int length) {
		_fieldHandler.setFieldSize(FIBFieldHandler.PLCFBKF, length);
	}

	public int getFcPlcfbkl() {
		return _fieldHandler.getFieldOffset(FIBFieldHandler.PLCFBKL);
	}

	public void setFcPlcfbkl(int offset) {
		_fieldHandler.setFieldOffset(FIBFieldHandler.PLCFBKL, offset);
	}

	public int getLcbPlcfbkl() {
		return _fieldHandler.getFieldSize(FIBFieldHandler.PLCFBKL);
	}

	public void setLcbPlcfbkl(int length) {
		_fieldHandler.setFieldSize(FIBFieldHandler.PLCFBKL, length);
	}

	public void setFcPlfLfo(int fcPlfLfo) {
		_fieldHandler.setFieldOffset(FIBFieldHandler.PLFLFO, fcPlfLfo);
	}

	public void setLcbPlfLfo(int lcbPlfLfo) {
		_fieldHandler.setFieldSize(FIBFieldHandler.PLFLFO, lcbPlfLfo);
	}

	public int getFcSttbfffn() {
		return _fieldHandler.getFieldOffset(FIBFieldHandler.STTBFFFN);
	}

	public int getLcbSttbfffn() {
		return _fieldHandler.getFieldSize(FIBFieldHandler.STTBFFFN);
	}

	public void setFcSttbfffn(int fcSttbFffn) {
		_fieldHandler.setFieldOffset(FIBFieldHandler.STTBFFFN, fcSttbFffn);
	}

	public void setLcbSttbfffn(int lcbSttbFffn) {
		_fieldHandler.setFieldSize(FIBFieldHandler.STTBFFFN, lcbSttbFffn);
	}

	public int getFcSttbfRMark() {
		return _fieldHandler.getFieldOffset(FIBFieldHandler.STTBFRMARK);
	}

	public int getLcbSttbfRMark() {
		return _fieldHandler.getFieldSize(FIBFieldHandler.STTBFRMARK);
	}

	public void setFcSttbfRMark(int fcSttbfRMark) {
		_fieldHandler.setFieldOffset(FIBFieldHandler.STTBFRMARK, fcSttbfRMark);
	}

	public void setLcbSttbfRMark(int lcbSttbfRMark) {
		_fieldHandler.setFieldSize(FIBFieldHandler.STTBFRMARK, lcbSttbfRMark);
	}

	public int getPlcfHddOffset() {
		return _fieldHandler.getFieldOffset(FIBFieldHandler.PLCFHDD);
	}

	public int getPlcfHddSize() {
		return _fieldHandler.getFieldSize(FIBFieldHandler.PLCFHDD);
	}

	public void setPlcfHddOffset(int fcPlcfHdd) {
		_fieldHandler.setFieldOffset(FIBFieldHandler.PLCFHDD, fcPlcfHdd);
	}

	public void setPlcfHddSize(int lcbPlcfHdd) {
		_fieldHandler.setFieldSize(FIBFieldHandler.PLCFHDD, lcbPlcfHdd);
	}

	public int getFcSttbSavedBy() {
		return _fieldHandler.getFieldOffset(FIBFieldHandler.STTBSAVEDBY);
	}

	public int getLcbSttbSavedBy() {
		return _fieldHandler.getFieldSize(FIBFieldHandler.STTBSAVEDBY);
	}

	public void setFcSttbSavedBy(int fcSttbSavedBy) {
		_fieldHandler.setFieldOffset(FIBFieldHandler.STTBSAVEDBY, fcSttbSavedBy);
	}

	public void setLcbSttbSavedBy(int fcSttbSavedBy) {
		_fieldHandler.setFieldSize(FIBFieldHandler.STTBSAVEDBY, fcSttbSavedBy);
	}

	public int getModifiedLow() {
		return _fieldHandler.getFieldOffset(FIBFieldHandler.PLFLFO);
	}

	public int getModifiedHigh() {
		return _fieldHandler.getFieldSize(FIBFieldHandler.PLFLFO);
	}

	public void setModifiedLow(int modifiedLow) {
		_fieldHandler.setFieldOffset(FIBFieldHandler.PLFLFO, modifiedLow);
	}

	public void setModifiedHigh(int modifiedHigh) {
		_fieldHandler.setFieldSize(FIBFieldHandler.PLFLFO, modifiedHigh);
	}

	public int getCbMac() {
		return _fibRgLw.getCbMac();
	}

	public void setCbMac(int cbMac) {
		_fibRgLw.setCbMac(cbMac);
	}

	public int getSubdocumentTextStreamLength(SubdocumentType type) {
		if (type == null)
			throw new IllegalArgumentException("argument 'type' is null");

		return _fibRgLw.getSubdocumentTextStreamLength(type);
	}

	public void setSubdocumentTextStreamLength(SubdocumentType type, int length) {
		if (type == null)
			throw new IllegalArgumentException("argument 'type' is null");

		if (length < 0)
			throw new IllegalArgumentException((((("Subdocument length can't be less than 0 (passed value is " + length) + "). ") + "If there is no subdocument ") + "length must be set to zero."));

		_fibRgLw.setSubdocumentTextStreamLength(type, length);
	}

	public void clearOffsetsSizes() {
		_fieldHandler.clearFields();
	}

	public int getFieldsPlcfOffset(FieldsDocumentPart part) {
		return _fieldHandler.getFieldOffset(part.getFibFieldsField());
	}

	public int getFieldsPlcfLength(FieldsDocumentPart part) {
		return _fieldHandler.getFieldSize(part.getFibFieldsField());
	}

	public void setFieldsPlcfOffset(FieldsDocumentPart part, int offset) {
		_fieldHandler.setFieldOffset(part.getFibFieldsField(), offset);
	}

	public void setFieldsPlcfLength(FieldsDocumentPart part, int length) {
		_fieldHandler.setFieldSize(part.getFibFieldsField(), length);
	}

	@Deprecated
	public int getFcPlcffldAtn() {
		return _fieldHandler.getFieldOffset(FIBFieldHandler.PLCFFLDATN);
	}

	@Deprecated
	public int getLcbPlcffldAtn() {
		return _fieldHandler.getFieldSize(FIBFieldHandler.PLCFFLDATN);
	}

	@Deprecated
	public void setFcPlcffldAtn(int offset) {
		_fieldHandler.setFieldOffset(FIBFieldHandler.PLCFFLDATN, offset);
	}

	@Deprecated
	public void setLcbPlcffldAtn(int size) {
		_fieldHandler.setFieldSize(FIBFieldHandler.PLCFFLDATN, size);
	}

	@Deprecated
	public int getFcPlcffldEdn() {
		return _fieldHandler.getFieldOffset(FIBFieldHandler.PLCFFLDEDN);
	}

	@Deprecated
	public int getLcbPlcffldEdn() {
		return _fieldHandler.getFieldSize(FIBFieldHandler.PLCFFLDEDN);
	}

	@Deprecated
	public void setFcPlcffldEdn(int offset) {
		_fieldHandler.setFieldOffset(FIBFieldHandler.PLCFFLDEDN, offset);
	}

	@Deprecated
	public void setLcbPlcffldEdn(int size) {
		_fieldHandler.setFieldSize(FIBFieldHandler.PLCFFLDEDN, size);
	}

	@Deprecated
	public int getFcPlcffldFtn() {
		return _fieldHandler.getFieldOffset(FIBFieldHandler.PLCFFLDFTN);
	}

	@Deprecated
	public int getLcbPlcffldFtn() {
		return _fieldHandler.getFieldSize(FIBFieldHandler.PLCFFLDFTN);
	}

	@Deprecated
	public void setFcPlcffldFtn(int offset) {
		_fieldHandler.setFieldOffset(FIBFieldHandler.PLCFFLDFTN, offset);
	}

	@Deprecated
	public void setLcbPlcffldFtn(int size) {
		_fieldHandler.setFieldSize(FIBFieldHandler.PLCFFLDFTN, size);
	}

	@Deprecated
	public int getFcPlcffldHdr() {
		return _fieldHandler.getFieldOffset(FIBFieldHandler.PLCFFLDHDR);
	}

	@Deprecated
	public int getLcbPlcffldHdr() {
		return _fieldHandler.getFieldSize(FIBFieldHandler.PLCFFLDHDR);
	}

	@Deprecated
	public void setFcPlcffldHdr(int offset) {
		_fieldHandler.setFieldOffset(FIBFieldHandler.PLCFFLDHDR, offset);
	}

	@Deprecated
	public void setLcbPlcffldHdr(int size) {
		_fieldHandler.setFieldSize(FIBFieldHandler.PLCFFLDHDR, size);
	}

	@Deprecated
	public int getFcPlcffldHdrtxbx() {
		return _fieldHandler.getFieldOffset(FIBFieldHandler.PLCFFLDHDRTXBX);
	}

	@Deprecated
	public int getLcbPlcffldHdrtxbx() {
		return _fieldHandler.getFieldSize(FIBFieldHandler.PLCFFLDHDRTXBX);
	}

	@Deprecated
	public void setFcPlcffldHdrtxbx(int offset) {
		_fieldHandler.setFieldOffset(FIBFieldHandler.PLCFFLDHDRTXBX, offset);
	}

	@Deprecated
	public void setLcbPlcffldHdrtxbx(int size) {
		_fieldHandler.setFieldSize(FIBFieldHandler.PLCFFLDHDRTXBX, size);
	}

	@Deprecated
	public int getFcPlcffldMom() {
		return _fieldHandler.getFieldOffset(FIBFieldHandler.PLCFFLDMOM);
	}

	@Deprecated
	public int getLcbPlcffldMom() {
		return _fieldHandler.getFieldSize(FIBFieldHandler.PLCFFLDMOM);
	}

	@Deprecated
	public void setFcPlcffldMom(int offset) {
		_fieldHandler.setFieldOffset(FIBFieldHandler.PLCFFLDMOM, offset);
	}

	@Deprecated
	public void setLcbPlcffldMom(int size) {
		_fieldHandler.setFieldSize(FIBFieldHandler.PLCFFLDMOM, size);
	}

	@Deprecated
	public int getFcPlcffldTxbx() {
		return _fieldHandler.getFieldOffset(FIBFieldHandler.PLCFFLDTXBX);
	}

	@Deprecated
	public int getLcbPlcffldTxbx() {
		return _fieldHandler.getFieldSize(FIBFieldHandler.PLCFFLDTXBX);
	}

	@Deprecated
	public void setFcPlcffldTxbx(int offset) {
		_fieldHandler.setFieldOffset(FIBFieldHandler.PLCFFLDTXBX, offset);
	}

	@Deprecated
	public void setLcbPlcffldTxbx(int size) {
		_fieldHandler.setFieldSize(FIBFieldHandler.PLCFFLDTXBX, size);
	}

	public int getFSPAPlcfOffset(FSPADocumentPart part) {
		return _fieldHandler.getFieldOffset(part.getFibFieldsField());
	}

	public int getFSPAPlcfLength(FSPADocumentPart part) {
		return _fieldHandler.getFieldSize(part.getFibFieldsField());
	}

	public void setFSPAPlcfOffset(FSPADocumentPart part, int offset) {
		_fieldHandler.setFieldOffset(part.getFibFieldsField(), offset);
	}

	public void setFSPAPlcfLength(FSPADocumentPart part, int length) {
		_fieldHandler.setFieldSize(part.getFibFieldsField(), length);
	}

	@Deprecated
	public int getFcPlcspaMom() {
		return _fieldHandler.getFieldOffset(FIBFieldHandler.PLCSPAMOM);
	}

	@Deprecated
	public int getLcbPlcspaMom() {
		return _fieldHandler.getFieldSize(FIBFieldHandler.PLCSPAMOM);
	}

	public int getFcDggInfo() {
		return _fieldHandler.getFieldOffset(FIBFieldHandler.DGGINFO);
	}

	public int getLcbDggInfo() {
		return _fieldHandler.getFieldSize(FIBFieldHandler.DGGINFO);
	}

	public int getNotesDescriptorsOffset(NoteType noteType) {
		return _fieldHandler.getFieldOffset(noteType.getFibDescriptorsFieldIndex());
	}

	public void setNotesDescriptorsOffset(NoteType noteType, int offset) {
		_fieldHandler.setFieldOffset(noteType.getFibDescriptorsFieldIndex(), offset);
	}

	public int getNotesDescriptorsSize(NoteType noteType) {
		return _fieldHandler.getFieldSize(noteType.getFibDescriptorsFieldIndex());
	}

	public void setNotesDescriptorsSize(NoteType noteType, int offset) {
		_fieldHandler.setFieldSize(noteType.getFibDescriptorsFieldIndex(), offset);
	}

	public int getNotesTextPositionsOffset(NoteType noteType) {
		return _fieldHandler.getFieldOffset(noteType.getFibTextPositionsFieldIndex());
	}

	public void setNotesTextPositionsOffset(NoteType noteType, int offset) {
		_fieldHandler.setFieldOffset(noteType.getFibTextPositionsFieldIndex(), offset);
	}

	public int getNotesTextPositionsSize(NoteType noteType) {
		return _fieldHandler.getFieldSize(noteType.getFibTextPositionsFieldIndex());
	}

	public void setNotesTextPositionsSize(NoteType noteType, int offset) {
		_fieldHandler.setFieldSize(noteType.getFibTextPositionsFieldIndex(), offset);
	}

	public void writeTo(byte[] mainStream, ByteArrayOutputStream tableStream) throws IOException {
		_cbRgFcLcb = _fieldHandler.getFieldsCount();
		_fibBase.serialize(mainStream, 0);
		int offset = FibBase.getSize();
		LittleEndian.putUShort(mainStream, offset, _csw);
		offset += LittleEndian.SHORT_SIZE;
		_fibRgW.serialize(mainStream, offset);
		offset += FibRgW97.getSize();
		LittleEndian.putUShort(mainStream, offset, _cslw);
		offset += LittleEndian.SHORT_SIZE;
		((FibRgLw97) (_fibRgLw)).serialize(mainStream, offset);
		offset += FibRgLw97.getSize();
		LittleEndian.putUShort(mainStream, offset, _cbRgFcLcb);
		offset += LittleEndian.SHORT_SIZE;
		offset += ((_cbRgFcLcb) * (LittleEndian.INT_SIZE)) * 2;
		LittleEndian.putUShort(mainStream, offset, _cswNew);
		offset += LittleEndian.SHORT_SIZE;
		if ((_cswNew) != 0) {
			LittleEndian.putUShort(mainStream, offset, _nFibNew);
			offset += LittleEndian.SHORT_SIZE;
			System.arraycopy(_fibRgCswNew, 0, mainStream, offset, _fibRgCswNew.length);
			offset += _fibRgCswNew.length;
		}
	}

	public int getSize() {
		return ((((((FibBase.getSize()) + (LittleEndian.SHORT_SIZE)) + (FibRgW97.getSize())) + (LittleEndian.SHORT_SIZE)) + (FibRgLw97.getSize())) + (LittleEndian.SHORT_SIZE)) + (_fieldHandler.sizeInBytes());
	}

	public FibBase getFibBase() {
		return _fibBase;
	}
}

