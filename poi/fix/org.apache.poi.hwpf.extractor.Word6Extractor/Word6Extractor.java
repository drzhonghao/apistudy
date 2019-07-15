

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.apache.poi.extractor.POIOLE2TextExtractor;
import org.apache.poi.hwpf.HWPFOldDocument;
import org.apache.poi.hwpf.converter.AbstractWordConverter;
import org.apache.poi.hwpf.converter.WordToTextConverter;
import org.apache.poi.hwpf.model.TextPiece;
import org.apache.poi.hwpf.model.TextPieceTable;
import org.apache.poi.hwpf.usermodel.Range;
import org.apache.poi.poifs.filesystem.DirectoryNode;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;


public final class Word6Extractor extends POIOLE2TextExtractor {
	private HWPFOldDocument doc;

	public Word6Extractor(InputStream is) throws IOException {
		this(new POIFSFileSystem(is));
	}

	public Word6Extractor(POIFSFileSystem fs) throws IOException {
		this(fs.getRoot());
	}

	@Deprecated
	public Word6Extractor(DirectoryNode dir, POIFSFileSystem fs) throws IOException {
		this(dir);
	}

	public Word6Extractor(DirectoryNode dir) throws IOException {
		this(new HWPFOldDocument(dir));
	}

	public Word6Extractor(HWPFOldDocument doc) {
		super(doc);
		this.doc = doc;
	}

	@Deprecated
	public String[] getParagraphText() {
		String[] ret;
		try {
			Range r = doc.getRange();
		} catch (Exception e) {
			ret = new String[doc.getTextTable().getTextPieces().size()];
			for (int i = 0; i < (ret.length); i++) {
				ret[i] = doc.getTextTable().getTextPieces().get(i).getStringBuilder().toString();
				ret[i] = ret[i].replaceAll("\r", "\ufffe");
				ret[i] = ret[i].replaceAll("\ufffe", "\r\n");
			}
		}
		ret = null;
		return ret;
	}

	public String getText() {
		try {
			WordToTextConverter wordToTextConverter = new WordToTextConverter();
			wordToTextConverter.processDocument(doc);
			return wordToTextConverter.getText();
		} catch (Exception exc) {
			StringBuffer text = new StringBuffer();
			for (String t : getParagraphText()) {
				text.append(t);
			}
			return text.toString();
		}
	}
}

