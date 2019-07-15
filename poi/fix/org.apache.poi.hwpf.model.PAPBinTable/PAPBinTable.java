

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.poi.hwpf.model.CharIndexTranslator;
import org.apache.poi.hwpf.model.ComplexFileTable;
import org.apache.poi.hwpf.model.GenericPropertyNode;
import org.apache.poi.hwpf.model.PAPFormattedDiskPage;
import org.apache.poi.hwpf.model.PAPX;
import org.apache.poi.hwpf.model.PieceDescriptor;
import org.apache.poi.hwpf.model.PlexOfCps;
import org.apache.poi.hwpf.model.PropertyModifier;
import org.apache.poi.hwpf.model.PropertyNode;
import org.apache.poi.hwpf.model.TextPiece;
import org.apache.poi.hwpf.model.TextPieceTable;
import org.apache.poi.hwpf.sprm.SprmBuffer;
import org.apache.poi.hwpf.sprm.SprmIterator;
import org.apache.poi.hwpf.sprm.SprmOperation;
import org.apache.poi.poifs.common.POIFSConstants;
import org.apache.poi.util.Internal;
import org.apache.poi.util.LittleEndian;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;

import static org.apache.poi.hwpf.model.PropertyNode.EndComparator.instance;


@Internal
public class PAPBinTable {
	private static final POILogger logger = POILogFactory.getLogger(PAPBinTable.class);

	protected final ArrayList<PAPX> _paragraphs = new ArrayList<>();

	public PAPBinTable() {
	}

	public PAPBinTable(byte[] documentStream, byte[] tableStream, byte[] dataStream, int offset, int size, CharIndexTranslator charIndexTranslator) {
		long start = System.currentTimeMillis();
		{
			PlexOfCps binTable = new PlexOfCps(tableStream, offset, size, 4);
			int length = binTable.length();
			for (int x = 0; x < length; x++) {
				GenericPropertyNode node = binTable.getProperty(x);
				int pageNum = LittleEndian.getInt(node.getBytes());
				int pageOffset = (POIFSConstants.SMALLER_BIG_BLOCK_SIZE) * pageNum;
				PAPFormattedDiskPage pfkp = new PAPFormattedDiskPage(documentStream, dataStream, pageOffset, charIndexTranslator);
				for (PAPX papx : pfkp.getPAPXs()) {
					if (papx != null)
						_paragraphs.add(papx);

				}
			}
		}
		PAPBinTable.logger.log(POILogger.DEBUG, "PAPX tables loaded in ", Long.valueOf(((System.currentTimeMillis()) - start)), " ms (", Integer.valueOf(_paragraphs.size()), " elements)");
		if (_paragraphs.isEmpty()) {
			PAPBinTable.logger.log(POILogger.WARN, "PAPX FKPs are empty");
			_paragraphs.add(new PAPX(0, 0, new SprmBuffer(2)));
		}
	}

	public void rebuild(final StringBuilder docText, ComplexFileTable complexFileTable) {
		PAPBinTable.rebuild(docText, complexFileTable, _paragraphs);
	}

	static void rebuild(final StringBuilder docText, ComplexFileTable complexFileTable, List<PAPX> paragraphs) {
		long start = System.currentTimeMillis();
		if (complexFileTable != null) {
			SprmBuffer[] sprmBuffers = complexFileTable.getGrpprls();
			for (TextPiece textPiece : complexFileTable.getTextPieceTable().getTextPieces()) {
				PropertyModifier prm = textPiece.getPieceDescriptor().getPrm();
				if (!(prm.isComplex()))
					continue;

				int igrpprl = prm.getIgrpprl();
				if ((igrpprl < 0) || (igrpprl >= (sprmBuffers.length))) {
					PAPBinTable.logger.log(POILogger.WARN, (textPiece + "'s PRM references to unknown grpprl"));
					continue;
				}
				boolean hasPap = false;
				SprmBuffer sprmBuffer = sprmBuffers[igrpprl];
				for (SprmIterator iterator = sprmBuffer.iterator(); iterator.hasNext();) {
					SprmOperation sprmOperation = iterator.next();
					if ((sprmOperation.getType()) == (SprmOperation.TYPE_PAP)) {
						hasPap = true;
						break;
					}
				}
				if (hasPap) {
					SprmBuffer newSprmBuffer = new SprmBuffer(2);
					newSprmBuffer.append(sprmBuffer.toByteArray());
					PAPX papx = new PAPX(textPiece.getStart(), textPiece.getEnd(), newSprmBuffer);
					paragraphs.add(papx);
				}
			}
			PAPBinTable.logger.log(POILogger.DEBUG, "Merged (?) with PAPX from complex file table in ", Long.valueOf(((System.currentTimeMillis()) - start)), " ms (", Integer.valueOf(paragraphs.size()), " elements in total)");
			start = System.currentTimeMillis();
		}
		List<PAPX> oldPapxSortedByEndPos = new ArrayList<>(paragraphs);
		oldPapxSortedByEndPos.sort(instance);
		PAPBinTable.logger.log(POILogger.DEBUG, "PAPX sorted by end position in ", Long.valueOf(((System.currentTimeMillis()) - start)), " ms");
		start = System.currentTimeMillis();
		final Map<PAPX, Integer> papxToFileOrder = new IdentityHashMap<>();
		{
			int counter = 0;
			for (PAPX papx : paragraphs) {
				papxToFileOrder.put(papx, Integer.valueOf((counter++)));
			}
		}
		final Comparator<PAPX> papxFileOrderComparator = new Comparator<PAPX>() {
			public int compare(PAPX o1, PAPX o2) {
				Integer i1 = papxToFileOrder.get(o1);
				Integer i2 = papxToFileOrder.get(o2);
				return i1.compareTo(i2);
			}
		};
		PAPBinTable.logger.log(POILogger.DEBUG, "PAPX's order map created in ", Long.valueOf(((System.currentTimeMillis()) - start)), " ms");
		start = System.currentTimeMillis();
		List<PAPX> newPapxs = new LinkedList<>();
		int lastParStart = 0;
		int lastPapxIndex = 0;
		for (int charIndex = 0; charIndex < (docText.length()); charIndex++) {
			final char c = docText.charAt(charIndex);
			if (((c != 13) && (c != 7)) && (c != 12))
				continue;

			final int startInclusive = lastParStart;
			final int endExclusive = charIndex + 1;
			boolean broken = false;
			List<PAPX> papxs = new LinkedList<>();
			for (int papxIndex = lastPapxIndex; papxIndex < (oldPapxSortedByEndPos.size()); papxIndex++) {
				broken = false;
				PAPX papx = oldPapxSortedByEndPos.get(papxIndex);
				assert ((startInclusive == 0) || ((papxIndex + 1) == (oldPapxSortedByEndPos.size()))) || ((papx.getEnd()) > startInclusive);
				if (((papx.getEnd()) - 1) > charIndex) {
					lastPapxIndex = papxIndex;
					broken = true;
					break;
				}
				papxs.add(papx);
			}
			if (!broken) {
				lastPapxIndex = (oldPapxSortedByEndPos.size()) - 1;
			}
			if ((papxs.size()) == 0) {
				PAPBinTable.logger.log(POILogger.WARN, "Paragraph [", Integer.valueOf(startInclusive), "; ", Integer.valueOf(endExclusive), ") has no PAPX. Creating new one.");
				PAPX papx = new PAPX(startInclusive, endExclusive, new SprmBuffer(2));
				newPapxs.add(papx);
				lastParStart = endExclusive;
				continue;
			}
			if ((papxs.size()) == 1) {
				PAPX existing = papxs.get(0);
				if (((existing.getStart()) == startInclusive) && ((existing.getEnd()) == endExclusive)) {
					newPapxs.add(existing);
					lastParStart = endExclusive;
					continue;
				}
			}
			papxs.sort(papxFileOrderComparator);
			SprmBuffer sprmBuffer = null;
			for (PAPX papx : papxs) {
				if (((papx.getGrpprl()) == null) || ((papx.getGrpprl().length) <= 2))
					continue;

				if (sprmBuffer == null) {
					sprmBuffer = papx.getSprmBuf().clone();
				}else {
					sprmBuffer.append(papx.getGrpprl(), 2);
				}
			}
			PAPX newPapx = new PAPX(startInclusive, endExclusive, sprmBuffer);
			newPapxs.add(newPapx);
			lastParStart = endExclusive;
			continue;
		}
		paragraphs.clear();
		paragraphs.addAll(newPapxs);
		PAPBinTable.logger.log(POILogger.DEBUG, "PAPX rebuilded from document text in ", Long.valueOf(((System.currentTimeMillis()) - start)), " ms (", Integer.valueOf(paragraphs.size()), " elements)");
	}

	public void insert(int listIndex, int cpStart, SprmBuffer buf) {
		PAPX forInsert = new PAPX(0, 0, buf);
		forInsert.setStart(cpStart);
		forInsert.setEnd(cpStart);
		if (listIndex == (_paragraphs.size())) {
			_paragraphs.add(forInsert);
		}else {
			PAPX currentPap = _paragraphs.get(listIndex);
			if ((currentPap != null) && ((currentPap.getStart()) < cpStart)) {
				SprmBuffer clonedBuf = currentPap.getSprmBuf().clone();
				PAPX clone = new PAPX(0, 0, clonedBuf);
				clone.setStart(cpStart);
				clone.setEnd(currentPap.getEnd());
				currentPap.setEnd(cpStart);
				_paragraphs.add((listIndex + 1), forInsert);
				_paragraphs.add((listIndex + 2), clone);
			}else {
				_paragraphs.add(listIndex, forInsert);
			}
		}
	}

	public void adjustForDelete(int listIndex, int offset, int length) {
		int size = _paragraphs.size();
		int endMark = offset + length;
		int endIndex = listIndex;
		PAPX papx = _paragraphs.get(endIndex);
		while ((papx.getEnd()) < endMark) {
			papx = _paragraphs.get((++endIndex));
		} 
		if (listIndex == endIndex) {
			papx = _paragraphs.get(endIndex);
			papx.setEnd((((papx.getEnd()) - endMark) + offset));
		}else {
			papx = _paragraphs.get(listIndex);
			papx.setEnd(offset);
			for (int x = listIndex + 1; x < endIndex; x++) {
				papx = _paragraphs.get(x);
				papx.setStart(offset);
				papx.setEnd(offset);
			}
			papx = _paragraphs.get(endIndex);
			papx.setEnd((((papx.getEnd()) - endMark) + offset));
		}
		for (int x = endIndex + 1; x < size; x++) {
			papx = _paragraphs.get(x);
			papx.setStart(((papx.getStart()) - length));
			papx.setEnd(((papx.getEnd()) - length));
		}
	}

	public void adjustForInsert(int listIndex, int length) {
		int size = _paragraphs.size();
		PAPX papx = _paragraphs.get(listIndex);
		papx.setEnd(((papx.getEnd()) + length));
		for (int x = listIndex + 1; x < size; x++) {
			papx = _paragraphs.get(x);
			papx.setStart(((papx.getStart()) + length));
			papx.setEnd(((papx.getEnd()) + length));
		}
	}

	public ArrayList<PAPX> getParagraphs() {
		return _paragraphs;
	}

	public void writeTo(ByteArrayOutputStream wordDocumentStream, ByteArrayOutputStream tableStream, CharIndexTranslator translator) throws IOException {
		PlexOfCps binTable = new PlexOfCps(4);
		int docOffset = wordDocumentStream.size();
		int mod = docOffset % (POIFSConstants.SMALLER_BIG_BLOCK_SIZE);
		if (mod != 0) {
			byte[] padding = new byte[(POIFSConstants.SMALLER_BIG_BLOCK_SIZE) - mod];
			wordDocumentStream.write(padding);
		}
		docOffset = wordDocumentStream.size();
		int pageNum = docOffset / (POIFSConstants.SMALLER_BIG_BLOCK_SIZE);
		int endingFc = translator.getByteIndex(_paragraphs.get(((_paragraphs.size()) - 1)).getEnd());
		ArrayList<PAPX> overflow = _paragraphs;
		do {
			PAPX startingProp = overflow.get(0);
			int start = translator.getByteIndex(startingProp.getStart());
			PAPFormattedDiskPage pfkp = new PAPFormattedDiskPage();
			pfkp.fill(overflow);
			int end = endingFc;
			if (overflow != null) {
				end = translator.getByteIndex(overflow.get(0).getStart());
			}
			byte[] intHolder = new byte[4];
			LittleEndian.putInt(intHolder, 0, (pageNum++));
			binTable.addProperty(new GenericPropertyNode(start, end, intHolder));
		} while (overflow != null );
		tableStream.write(binTable.toByteArray());
	}
}

