import org.apache.poi.xslf.model.*;


import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.xmlbeans.XmlObject;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextParagraphProperties;

/**
 *
 * @author Yegor Kozlov
 */
public abstract class ParagraphPropertyFetcher<T> extends PropertyFetcher<T> {
    int _level;

    public ParagraphPropertyFetcher(int level) {
        _level = level;
    }

    public boolean fetch(XSLFShape shape) {

        XmlObject[] o = shape.getXmlObject().selectPath(
                "declare namespace p='http://schemas.openxmlformats.org/presentationml/2006/main' " +
                "declare namespace a='http://schemas.openxmlformats.org/drawingml/2006/main' " +
                ".//p:txBody/a:lstStyle/a:lvl" + (_level + 1) + "pPr"
        );
        if (o.length == 1) {
            CTTextParagraphProperties props = (CTTextParagraphProperties) o[0];
            return fetch(props);
        }
        return false;
    }

    public abstract boolean fetch(CTTextParagraphProperties props);
}
