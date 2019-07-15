import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.*;


import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTComment;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP;

/**
 * Sketch of XWPF comment class
 *
 * @author Yury Batrakov (batrakov at gmail.com)
 */
public class XWPFComment {
    protected String id;
    protected String author;
    protected StringBuilder text;

    public XWPFComment(CTComment comment, XWPFDocument document) {
        text = new StringBuilder(64);
        id = comment.getId().toString();
        author = comment.getAuthor();

        for (CTP ctp : comment.getPArray()) {
            XWPFParagraph p = new XWPFParagraph(ctp, document);
            text.append(p.getText());
        }
    }

    public String getId() {
        return id;
    }

    public String getAuthor() {
        return author;
    }

    public String getText() {
        return text.toString();
    }
}
