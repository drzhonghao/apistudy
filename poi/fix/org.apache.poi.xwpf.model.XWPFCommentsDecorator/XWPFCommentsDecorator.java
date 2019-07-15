

import java.math.BigInteger;
import org.apache.poi.xwpf.model.XWPFParagraphDecorator;
import org.apache.poi.xwpf.usermodel.XWPFComment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTMarkup;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTMarkupRange;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP;


public class XWPFCommentsDecorator extends XWPFParagraphDecorator {
	private StringBuilder commentText;

	public XWPFCommentsDecorator(XWPFParagraph paragraph, XWPFParagraphDecorator nextDecorator) {
		super(paragraph, nextDecorator);
		XWPFComment comment;
		commentText = new StringBuilder(64);
		for (CTMarkupRange anchor : paragraph.getCTP().getCommentRangeStartArray()) {
			if ((comment = paragraph.getDocument().getCommentByID(anchor.getId().toString())) != null) {
				commentText.append("\tComment by ").append(comment.getAuthor()).append(": ").append(comment.getText());
			}
		}
	}

	public String getCommentText() {
		return commentText.toString();
	}

	public String getText() {
		return (super.getText()) + (commentText);
	}
}

