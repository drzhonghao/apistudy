import org.apache.poi.xwpf.usermodel.XWPFStyles;
import org.apache.poi.xwpf.usermodel.*;


import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTStyle;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STStyleType;

/**
 * @author Philipp Epp
 */
public class XWPFStyle {

    protected XWPFStyles styles;
    private CTStyle ctStyle;

    /**
     * constructor
     *
     * @param style
     */
    public XWPFStyle(CTStyle style) {
        this(style, null);
    }

    /**
     * constructor
     *
     * @param style
     * @param styles
     */
    public XWPFStyle(CTStyle style, XWPFStyles styles) {
        this.ctStyle = style;
        this.styles = styles;
    }

    /**
     * get StyleID of the style
     *
     * @return styleID        StyleID of the style
     */
    public String getStyleId() {
        return ctStyle.getStyleId();
    }

    /**
     * set styleID
     *
     * @param styleId
     */
    public void setStyleId(String styleId) {
        ctStyle.setStyleId(styleId);
    }

    /**
     * get Type of the Style
     *
     * @return ctType
     */
    public STStyleType.Enum getType() {
        return ctStyle.getType();
    }

    /**
     * set styleType
     *
     * @param type
     */
    public void setType(STStyleType.Enum type) {
        ctStyle.setType(type);
    }

    /**
     * set style
     *
     * @param style
     */
    public void setStyle(CTStyle style) {
        this.ctStyle = style;
    }

    /**
     * get ctStyle
     *
     * @return ctStyle
     */
    public CTStyle getCTStyle() {
        return this.ctStyle;
    }

    /**
     * get styles
     *
     * @return styles        the styles to which this style belongs
     */
    public XWPFStyles getStyles() {
        return styles;
    }

    public String getBasisStyleID() {
        if (ctStyle.getBasedOn() != null)
            return ctStyle.getBasedOn().getVal();
        else
            return null;
    }


    /**
     * get StyleID of the linked Style
     */
    public String getLinkStyleID() {
        if (ctStyle.getLink() != null)
            return ctStyle.getLink().getVal();
        else
            return null;
    }

    /**
     * get StyleID of the next style
     */
    public String getNextStyleID() {
        if (ctStyle.getNext() != null)
            return ctStyle.getNext().getVal();
        else
            return null;
    }

    public String getName() {
        if (ctStyle.isSetName())
            return ctStyle.getName().getVal();
        return null;
    }

    /**
     * compares the names of the Styles
     *
     * @param compStyle
     */
    public boolean hasSameName(XWPFStyle compStyle) {
        CTStyle ctCompStyle = compStyle.getCTStyle();
        String name = ctCompStyle.getName().getVal();
        return name.equals(ctStyle.getName().getVal());
    }

}//end class
