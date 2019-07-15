import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFAbstractFootnoteEndnote;
import org.apache.poi.xwpf.usermodel.*;


import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages IDs for footnotes and endnotes.
 * <p>Footnotes and endnotes are managed in separate parts but
 * represent a single namespace of IDs.</p>
 */
public class FootnoteEndnoteIdManager {

    private XWPFDocument document;    

    public FootnoteEndnoteIdManager(XWPFDocument document) {
        this.document = document;
    }

    /**
     * Gets the next ID number.
     *
     * @return ID number to use.
     */
    public BigInteger nextId() {
        
        List<BigInteger> ids = new ArrayList<BigInteger>();
        for (XWPFAbstractFootnoteEndnote note : document.getFootnotes()) {
            ids.add(note.getId());
        }
        for (XWPFAbstractFootnoteEndnote note : document.getEndnotes()) {
            ids.add(note.getId());
        }
        int cand = ids.size();
        BigInteger newId = BigInteger.valueOf(cand);
        while (ids.contains(newId)) {
            cand++;
            newId = BigInteger.valueOf(cand);
        } 
        
        return newId;
    }


}
