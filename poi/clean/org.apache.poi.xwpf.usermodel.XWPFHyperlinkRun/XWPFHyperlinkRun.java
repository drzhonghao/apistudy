import org.apache.poi.xwpf.usermodel.XWPFHyperlink;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.*;


import org.apache.poi.util.Internal;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTHyperlink;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR;

/**
 * A run of text with a Hyperlink applied to it.
 * Any given Hyperlink may be made up of multiple of these.
 */
public class XWPFHyperlinkRun extends XWPFRun {
    private CTHyperlink hyperlink;

    public XWPFHyperlinkRun(CTHyperlink hyperlink, CTR run, IRunBody p) {
        super(run, p);
        this.hyperlink = hyperlink;
    }

    @Internal
    public CTHyperlink getCTHyperlink() {
        return hyperlink;
    }

    public String getAnchor() {
        return hyperlink.getAnchor();
    }

    /**
     * Returns the ID of the hyperlink, if one is set.
     */
    public String getHyperlinkId() {
        return hyperlink.getId();
    }

    public void setHyperlinkId(String id) {
        hyperlink.setId(id);
    }

    /**
     * If this Hyperlink is an external reference hyperlink,
     * return the object for it.
     */
    public XWPFHyperlink getHyperlink(XWPFDocument document) {
        String id = getHyperlinkId();
        if (id == null)
            return null;

        return document.getHyperlinkByID(id);
    }
}
