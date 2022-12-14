import org.apache.poi.xwpf.usermodel.PositionInParagraph;
import org.apache.poi.xwpf.usermodel.*;



/**
 * saves the begin and end position  of a text in a Paragraph
 */
public class TextSegment {
    private PositionInParagraph beginPos;
    private PositionInParagraph endPos;

    public TextSegment() {
        this.beginPos = new PositionInParagraph();
        this.endPos = new PositionInParagraph();
    }

    public TextSegment(int beginRun, int endRun, int beginText, int endText, int beginChar, int endChar) {
        PositionInParagraph beginPos = new PositionInParagraph(beginRun, beginText, beginChar);
        PositionInParagraph endPos = new PositionInParagraph(endRun, endText, endChar);
        this.beginPos = beginPos;
        this.endPos = endPos;
    }

    public TextSegment(PositionInParagraph beginPos, PositionInParagraph endPos) {
        this.beginPos = beginPos;
        this.endPos = endPos;
    }

    public PositionInParagraph getBeginPos() {
        return beginPos;
    }

    public PositionInParagraph getEndPos() {
        return endPos;
    }

    public int getBeginRun() {
        return beginPos.getRun();
    }

    public void setBeginRun(int beginRun) {
        beginPos.setRun(beginRun);
    }

    public int getBeginText() {
        return beginPos.getText();
    }

    public void setBeginText(int beginText) {
        beginPos.setText(beginText);
    }

    public int getBeginChar() {
        return beginPos.getChar();
    }

    public void setBeginChar(int beginChar) {
        beginPos.setChar(beginChar);
    }

    public int getEndRun() {
        return endPos.getRun();
    }

    public void setEndRun(int endRun) {
        endPos.setRun(endRun);
    }

    public int getEndText() {
        return endPos.getText();
    }

    public void setEndText(int endText) {
        endPos.setText(endText);
    }

    public int getEndChar() {
        return endPos.getChar();
    }

    public void setEndChar(int endChar) {
        endPos.setChar(endChar);
    }
}
