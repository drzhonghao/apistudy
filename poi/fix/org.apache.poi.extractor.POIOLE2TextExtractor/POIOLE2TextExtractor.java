

import org.apache.poi.POIDocument;
import org.apache.poi.extractor.POITextExtractor;
import org.apache.poi.hpsf.DocumentSummaryInformation;
import org.apache.poi.hpsf.SummaryInformation;
import org.apache.poi.poifs.filesystem.DirectoryEntry;
import org.apache.poi.poifs.filesystem.DirectoryNode;


public abstract class POIOLE2TextExtractor extends POITextExtractor {
	protected POIDocument document;

	public POIOLE2TextExtractor(POIDocument document) {
		this.document = document;
		setFilesystem(document);
	}

	protected POIOLE2TextExtractor(POIOLE2TextExtractor otherExtractor) {
		this.document = otherExtractor.document;
	}

	public DocumentSummaryInformation getDocSummaryInformation() {
		return document.getDocumentSummaryInformation();
	}

	public SummaryInformation getSummaryInformation() {
		return document.getSummaryInformation();
	}

	@Override
	public POITextExtractor getMetadataTextExtractor() {
		return null;
	}

	public DirectoryEntry getRoot() {
		return document.getDirectory();
	}

	@Override
	public POIDocument getDocument() {
		return document;
	}
}

