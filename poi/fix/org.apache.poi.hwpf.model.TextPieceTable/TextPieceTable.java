

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import org.apache.poi.hwpf.model.CharIndexTranslator;
import org.apache.poi.hwpf.model.GenericPropertyNode;
import org.apache.poi.hwpf.model.PieceDescriptor;
import org.apache.poi.hwpf.model.PlexOfCps;
import org.apache.poi.hwpf.model.PropertyNode;
import org.apache.poi.hwpf.model.TextPiece;
import org.apache.poi.poifs.common.POIFSConstants;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.Internal;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;


@Internal
public class TextPieceTable implements CharIndexTranslator {
	private static final POILogger logger = POILogFactory.getLogger(TextPieceTable.class);

	private static final int MAX_RECORD_LENGTH = 100000000;

	int _cpMin;

	protected ArrayList<TextPiece> _textPieces = new ArrayList<>();

	protected ArrayList<TextPiece> _textPiecesFCOrder = new ArrayList<>();

	public TextPieceTable() {
	}

	public TextPieceTable(byte[] documentStream, byte[] tableStream, int offset, int size, int fcMin) {
		PlexOfCps pieceTable = new PlexOfCps(tableStream, offset, size, PieceDescriptor.getSizeInBytes());
		int length = pieceTable.length();
		PieceDescriptor[] pieces = new PieceDescriptor[length];
		for (int x = 0; x < length; x++) {
			GenericPropertyNode node = pieceTable.getProperty(x);
			pieces[x] = new PieceDescriptor(node.getBytes(), 0);
		}
		_cpMin = (pieces[0].getFilePosition()) - fcMin;
		for (PieceDescriptor piece : pieces) {
			int start = (piece.getFilePosition()) - fcMin;
			if (start < (_cpMin)) {
				_cpMin = start;
			}
		}
		for (int x = 0; x < (pieces.length); x++) {
			int start = pieces[x].getFilePosition();
			GenericPropertyNode node = pieceTable.getProperty(x);
			int nodeStartChars = node.getStart();
			int nodeEndChars = node.getEnd();
			boolean unicode = pieces[x].isUnicode();
			int multiple = 1;
			if (unicode) {
				multiple = 2;
			}
			int textSizeChars = nodeEndChars - nodeStartChars;
			int textSizeBytes = textSizeChars * multiple;
			byte[] buf = IOUtils.safelyAllocate(textSizeBytes, TextPieceTable.MAX_RECORD_LENGTH);
			System.arraycopy(documentStream, start, buf, 0, textSizeBytes);
			final TextPiece newTextPiece = newTextPiece(nodeStartChars, nodeEndChars, buf, pieces[x]);
			_textPieces.add(newTextPiece);
		}
		Collections.sort(_textPieces);
		_textPiecesFCOrder = new ArrayList<>(_textPieces);
		_textPiecesFCOrder.sort(new TextPieceTable.FCComparator());
	}

	protected TextPiece newTextPiece(int nodeStartChars, int nodeEndChars, byte[] buf, PieceDescriptor pd) {
		return new TextPiece(nodeStartChars, nodeEndChars, buf, pd);
	}

	public void add(TextPiece piece) {
		_textPieces.add(piece);
		_textPiecesFCOrder.add(piece);
		Collections.sort(_textPieces);
		_textPiecesFCOrder.sort(new TextPieceTable.FCComparator());
	}

	public int adjustForInsert(int listIndex, int length) {
		int size = _textPieces.size();
		TextPiece tp = _textPieces.get(listIndex);
		tp.setEnd(((tp.getEnd()) + length));
		for (int x = listIndex + 1; x < size; x++) {
			tp = _textPieces.get(x);
			tp.setStart(((tp.getStart()) + length));
			tp.setEnd(((tp.getEnd()) + length));
		}
		return length;
	}

	public boolean equals(Object o) {
		if (!(o instanceof TextPieceTable))
			return false;

		TextPieceTable tpt = ((TextPieceTable) (o));
		int size = tpt._textPieces.size();
		if (size == (_textPieces.size())) {
			for (int x = 0; x < size; x++) {
				if (!(tpt._textPieces.get(x).equals(_textPieces.get(x)))) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	public int getByteIndex(int charPos) {
		int byteCount = 0;
		for (TextPiece tp : _textPieces) {
			if (charPos >= (tp.getEnd())) {
				byteCount = (tp.getPieceDescriptor().getFilePosition()) + (((tp.getEnd()) - (tp.getStart())) * (tp.isUnicode() ? 2 : 1));
				if (charPos == (tp.getEnd()))
					break;

				continue;
			}
			if (charPos < (tp.getEnd())) {
				int left = charPos - (tp.getStart());
				byteCount = (tp.getPieceDescriptor().getFilePosition()) + (left * (tp.isUnicode() ? 2 : 1));
				break;
			}
		}
		return byteCount;
	}

	@Deprecated
	public int getCharIndex(int bytePos) {
		return getCharIndex(bytePos, 0);
	}

	@Deprecated
	public int getCharIndex(int startBytePos, int startCP) {
		int charCount = 0;
		int bytePos = lookIndexForward(startBytePos);
		for (TextPiece tp : _textPieces) {
			int pieceStart = tp.getPieceDescriptor().getFilePosition();
			int bytesLength = tp.bytesLength();
			int pieceEnd = pieceStart + bytesLength;
			int toAdd;
			if ((bytePos < pieceStart) || (bytePos > pieceEnd)) {
				toAdd = bytesLength;
			}else
				if ((bytePos > pieceStart) && (bytePos < pieceEnd)) {
					toAdd = bytePos - pieceStart;
				}else {
					toAdd = bytesLength - (pieceEnd - bytePos);
				}

			if (tp.isUnicode()) {
				charCount += toAdd / 2;
			}else {
				charCount += toAdd;
			}
			if (((bytePos >= pieceStart) && (bytePos <= pieceEnd)) && (charCount >= startCP)) {
				break;
			}
		}
		return charCount;
	}

	@Override
	public int[][] getCharIndexRanges(int startBytePosInclusive, int endBytePosExclusive) {
		List<int[]> result = new LinkedList<>();
		for (TextPiece textPiece : _textPiecesFCOrder) {
			final int tpStart = textPiece.getPieceDescriptor().getFilePosition();
			final int tpEnd = (textPiece.getPieceDescriptor().getFilePosition()) + (textPiece.bytesLength());
			if (startBytePosInclusive > tpEnd)
				continue;

			if (endBytePosExclusive <= tpStart)
				break;

			final int rangeStartBytes = Math.max(tpStart, startBytePosInclusive);
			final int rangeEndBytes = Math.min(tpEnd, endBytePosExclusive);
			final int rangeLengthBytes = rangeEndBytes - rangeStartBytes;
			if (rangeStartBytes > rangeEndBytes)
				continue;

			final int encodingMultiplier = getEncodingMultiplier(textPiece);
			final int rangeStartCp = (textPiece.getStart()) + ((rangeStartBytes - tpStart) / encodingMultiplier);
			final int rangeEndCp = rangeStartCp + (rangeLengthBytes / encodingMultiplier);
			result.add(new int[]{ rangeStartCp, rangeEndCp });
		}
		return result.toArray(new int[result.size()][]);
	}

	protected int getEncodingMultiplier(TextPiece textPiece) {
		return textPiece.isUnicode() ? 2 : 1;
	}

	public int getCpMin() {
		return _cpMin;
	}

	public StringBuilder getText() {
		final long start = System.currentTimeMillis();
		StringBuilder docText = new StringBuilder();
		for (TextPiece textPiece : _textPieces) {
			String toAppend = textPiece.getStringBuilder().toString();
			int toAppendLength = toAppend.length();
			if (toAppendLength != ((textPiece.getEnd()) - (textPiece.getStart()))) {
				TextPieceTable.logger.log(POILogger.WARN, "Text piece has boundaries [", Integer.valueOf(textPiece.getStart()), "; ", Integer.valueOf(textPiece.getEnd()), ") but length ", Integer.valueOf(((textPiece.getEnd()) - (textPiece.getStart()))));
			}
			docText.replace(textPiece.getStart(), ((textPiece.getStart()) + toAppendLength), toAppend);
		}
		TextPieceTable.logger.log(POILogger.DEBUG, "Document text were rebuilded in ", Long.valueOf(((System.currentTimeMillis()) - start)), " ms (", Integer.valueOf(docText.length()), " chars)");
		return docText;
	}

	public List<TextPiece> getTextPieces() {
		return _textPieces;
	}

	@Override
	public int hashCode() {
		return _textPieces.size();
	}

	public boolean isIndexInTable(int bytePos) {
		for (TextPiece tp : _textPiecesFCOrder) {
			int pieceStart = tp.getPieceDescriptor().getFilePosition();
			if (bytePos > (pieceStart + (tp.bytesLength()))) {
				continue;
			}
			if (pieceStart > bytePos) {
				return false;
			}
			return true;
		}
		return false;
	}

	boolean isIndexInTable(int startBytePos, int endBytePos) {
		for (TextPiece tp : _textPiecesFCOrder) {
			int pieceStart = tp.getPieceDescriptor().getFilePosition();
			if (startBytePos >= (pieceStart + (tp.bytesLength()))) {
				continue;
			}
			int left = Math.max(startBytePos, pieceStart);
			int right = Math.min(endBytePos, (pieceStart + (tp.bytesLength())));
			if (left >= right)
				return false;

			return true;
		}
		return false;
	}

	public int lookIndexBackward(final int startBytePos) {
		int bytePos = startBytePos;
		int lastEnd = 0;
		for (TextPiece tp : _textPiecesFCOrder) {
			int pieceStart = tp.getPieceDescriptor().getFilePosition();
			if (bytePos > (pieceStart + (tp.bytesLength()))) {
				lastEnd = pieceStart + (tp.bytesLength());
				continue;
			}
			if (pieceStart > bytePos) {
				bytePos = lastEnd;
			}
			break;
		}
		return bytePos;
	}

	public int lookIndexForward(final int startBytePos) {
		if (_textPiecesFCOrder.isEmpty())
			throw new IllegalStateException("Text pieces table is empty");

		if ((_textPiecesFCOrder.get(0).getPieceDescriptor().getFilePosition()) > startBytePos)
			return _textPiecesFCOrder.get(0).getPieceDescriptor().getFilePosition();

		if ((_textPiecesFCOrder.get(((_textPiecesFCOrder.size()) - 1)).getPieceDescriptor().getFilePosition()) <= startBytePos)
			return startBytePos;

		int low = 0;
		int high = (_textPiecesFCOrder.size()) - 1;
		while (low <= high) {
			int mid = (low + high) >>> 1;
			final TextPiece textPiece = _textPiecesFCOrder.get(mid);
			int midVal = textPiece.getPieceDescriptor().getFilePosition();
			if (midVal < startBytePos)
				low = mid + 1;
			else
				if (midVal > startBytePos)
					high = mid - 1;
				else
					return textPiece.getPieceDescriptor().getFilePosition();


		} 
		assert low == high;
		assert (_textPiecesFCOrder.get(low).getPieceDescriptor().getFilePosition()) < startBytePos;
		assert (_textPiecesFCOrder.get((low + 1)).getPieceDescriptor().getFilePosition()) > startBytePos;
		return _textPiecesFCOrder.get((low + 1)).getPieceDescriptor().getFilePosition();
	}

	public byte[] writeTo(ByteArrayOutputStream docStream) throws IOException {
		PlexOfCps textPlex = new PlexOfCps(PieceDescriptor.getSizeInBytes());
		for (TextPiece next : _textPieces) {
			PieceDescriptor pd = next.getPieceDescriptor();
			int offset = docStream.size();
			int mod = offset % (POIFSConstants.SMALLER_BIG_BLOCK_SIZE);
			if (mod != 0) {
				mod = (POIFSConstants.SMALLER_BIG_BLOCK_SIZE) - mod;
				byte[] buf = IOUtils.safelyAllocate(mod, TextPieceTable.MAX_RECORD_LENGTH);
				docStream.write(buf);
			}
			pd.setFilePosition(docStream.size());
			docStream.write(next.getRawBytes());
			int nodeStart = next.getStart();
			int nodeEnd = next.getEnd();
		}
		return textPlex.toByteArray();
	}

	protected static class FCComparator implements Serializable , Comparator<TextPiece> {
		public int compare(TextPiece textPiece, TextPiece textPiece1) {
			return 0;
		}
	}
}

