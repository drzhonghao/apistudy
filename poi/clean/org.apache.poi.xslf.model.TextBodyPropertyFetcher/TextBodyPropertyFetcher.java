import org.apache.poi.xslf.model.*;


import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.xmlbeans.XmlObject;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextBodyProperties;

/**
 * Created by IntelliJ IDEA.
 * User: yegor
 * Date: Oct 21, 2011
 * Time: 1:18:52 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class TextBodyPropertyFetcher<T> extends PropertyFetcher<T> {

    public boolean fetch(XSLFShape shape) {

        XmlObject[] o = shape.getXmlObject().selectPath(
                "declare namespace p='http://schemas.openxmlformats.org/presentationml/2006/main' " +
                "declare namespace a='http://schemas.openxmlformats.org/drawingml/2006/main' " +
                ".//p:txBody/a:bodyPr"
        );
        if (o.length == 1) {
            CTTextBodyProperties props = (CTTextBodyProperties) o[0];
            return fetch(props);
        }

        return false;
    }

    public abstract boolean fetch(CTTextBodyProperties props);

}
