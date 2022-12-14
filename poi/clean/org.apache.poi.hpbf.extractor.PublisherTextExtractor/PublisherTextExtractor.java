import org.apache.poi.hpbf.extractor.*;


import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.poi.extractor.POIOLE2TextExtractor;
import org.apache.poi.hpbf.HPBFDocument;
import org.apache.poi.hpbf.model.qcbits.QCBit;
import org.apache.poi.hpbf.model.qcbits.QCTextBit;
import org.apache.poi.hpbf.model.qcbits.QCPLCBit.Type12;
import org.apache.poi.poifs.filesystem.DirectoryNode;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

/**
 * Extract text from HPBF Publisher files
 */
public final class PublisherTextExtractor extends POIOLE2TextExtractor {
   private HPBFDocument doc;
   private boolean hyperlinksByDefault;

   public PublisherTextExtractor(HPBFDocument doc) {
      super(doc);
      this.doc = doc;
   }
   public PublisherTextExtractor(DirectoryNode dir) throws IOException {
      this(new HPBFDocument(dir));
   }
   public PublisherTextExtractor(POIFSFileSystem fs) throws IOException {
      this(new HPBFDocument(fs));
   }
   public PublisherTextExtractor(InputStream is) throws IOException {
      this(new POIFSFileSystem(is));
   }

	/**
	 * Should a call to getText() return hyperlinks inline
	 *  with the text?
	 * Default is no
	 */
	public void setHyperlinksByDefault(boolean hyperlinksByDefault) {
		this.hyperlinksByDefault = hyperlinksByDefault;
	}


	public String getText() {
		StringBuffer text = new StringBuffer();

		// Get the text from the Quill Contents
		QCBit[] bits = doc.getQuillContents().getBits();
		for (QCBit bit1 : bits) {
			if (bit1 != null && bit1 instanceof QCTextBit) {
				QCTextBit t = (QCTextBit) bit1;
				text.append(t.getText().replace('\r', '\n'));
			}
		}

		// If requested, add in the hyperlinks
		// Ideally, we'd do these inline, but the hyperlink
		//  positions are relative to the text area the
		//  hyperlink is in, and we have yet to figure out
		//  how to tie that together.
		if(hyperlinksByDefault) {
			for (QCBit bit : bits) {
				if (bit != null && bit instanceof Type12) {
					Type12 hyperlinks = (Type12) bit;
					for (int j = 0; j < hyperlinks.getNumberOfHyperlinks(); j++) {
						text.append("<");
						text.append(hyperlinks.getHyperlink(j));
						text.append(">\n");
					}
				}
			}
		}

		// Get more text
		// TODO

		return text.toString();
	}


	public static void main(String[] args) throws Exception {
		if(args.length == 0) {
			System.err.println("Use:");
			System.err.println("  PublisherTextExtractor <file.pub>");
		}

		for (String arg : args) {
			try (FileInputStream fis = new FileInputStream(arg)) {
				PublisherTextExtractor te = new PublisherTextExtractor(fis);
				System.out.println(te.getText());
				te.close();
			}
		}
	}
}
