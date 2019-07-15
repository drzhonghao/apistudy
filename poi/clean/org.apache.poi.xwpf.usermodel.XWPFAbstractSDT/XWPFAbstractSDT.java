import org.apache.poi.xwpf.usermodel.ISDTContents;
import org.apache.poi.xwpf.usermodel.IBody;
import org.apache.poi.xwpf.usermodel.ISDTContent;
import org.apache.poi.xwpf.usermodel.BodyType;
import org.apache.poi.xwpf.usermodel.BodyElementType;
import org.apache.poi.xwpf.usermodel.*;


import org.apache.poi.ooxml.POIXMLDocumentPart;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSdtPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTString;

/**
 * Experimental abstract class that is a base for XWPFSDT and XWPFSDTCell
 * <p>
 * WARNING - APIs expected to change rapidly.
 * <p>
 * These classes have so far been built only for read-only processing.
 */
public abstract class XWPFAbstractSDT implements ISDTContents {
    private final String title;
    private final String tag;
    private final IBody part;

    public XWPFAbstractSDT(CTSdtPr pr, IBody part) {
        if (pr == null) {
            title = "";
            tag = "";
        } else {
            CTString[] aliases = pr.getAliasArray();
            if (aliases != null && aliases.length > 0) {
                title = aliases[0].getVal();
            } else {
                title = "";
            }
            CTString[] tags = pr.getTagArray();
            if (tags != null && tags.length > 0) {
                tag = tags[0].getVal();
            } else {
                tag = "";
            }
        }
        this.part = part;

    }

    /**
     * @return first SDT Title
     */
    public String getTitle() {
        return title;
    }

    /**
     * @return first SDT Tag
     */
    public String getTag() {
        return tag;
    }

    /**
     * @return the content object
     */
    public abstract ISDTContent getContent();

    /**
     * @return null
     */
    public IBody getBody() {
        return null;
    }

    /**
     * @return document part
     */
    public POIXMLDocumentPart getPart() {
        return part.getPart();
    }

    /**
     * @return partType
     */
    public BodyType getPartType() {
        return BodyType.CONTENTCONTROL;
    }

    /**
     * @return element type
     */
    public BodyElementType getElementType() {
        return BodyElementType.CONTENTCONTROL;
    }

    public XWPFDocument getDocument() {
        return part.getXWPFDocument();
    }
}
