import org.apache.poi.xwpf.model.*;


import org.apache.poi.xwpf.usermodel.XWPFParagraph;

/**
 * Base decorator class for XWPFParagraph
 */
public abstract class XWPFParagraphDecorator {
    protected XWPFParagraph paragraph;
    protected XWPFParagraphDecorator nextDecorator;

    public XWPFParagraphDecorator(XWPFParagraph paragraph) {
        this(paragraph, null);
    }

    public XWPFParagraphDecorator(XWPFParagraph paragraph, XWPFParagraphDecorator nextDecorator) {
        this.paragraph = paragraph;
        this.nextDecorator = nextDecorator;
    }

    public String getText() {
        if (nextDecorator != null) {
            return nextDecorator.getText();
        }
        return paragraph.getText();
    }
}
