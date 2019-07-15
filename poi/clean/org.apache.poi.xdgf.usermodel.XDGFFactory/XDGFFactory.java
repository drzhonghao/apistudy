import org.apache.poi.xdgf.usermodel.XDGFDocument;
import org.apache.poi.xdgf.usermodel.XDGFRelation;
import org.apache.poi.xdgf.usermodel.*;


import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.apache.poi.ooxml.POIXMLDocumentPart;
import org.apache.poi.ooxml.POIXMLFactory;
import org.apache.poi.ooxml.POIXMLRelation;

/**
 * Instantiates sub-classes of POIXMLDocumentPart depending on their relationship type
 */
public class XDGFFactory extends POIXMLFactory {

    private final XDGFDocument document;

    public XDGFFactory(XDGFDocument document) {
        this.document = document;
    }

    /**
     * @since POI 3.14-Beta1
     */
    protected POIXMLRelation getDescriptor(String relationshipType) {
        return XDGFRelation.getInstance(relationshipType);
    }

    /**
     * @since POI 3.14-Beta1
     */
    @Override
    protected POIXMLDocumentPart createDocumentPart
        (Class<? extends POIXMLDocumentPart> cls, Class<?>[] classes, Object[] values)
    throws SecurityException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        Class<?>[] cl;
        Object[] vals;
        if (classes == null) {
            cl = new Class<?>[]{XDGFDocument.class};
            vals = new Object[]{document};
        } else {
            cl = new Class<?>[classes.length+1];
            System.arraycopy(classes, 0, cl, 0, classes.length);
            cl[classes.length] = XDGFDocument.class;
            vals = new Object[values.length+1];
            System.arraycopy(values, 0, vals, 0, values.length);
            vals[values.length] = document;
        }
        
        Constructor<? extends POIXMLDocumentPart> constructor = cls.getDeclaredConstructor(cl);
        return constructor.newInstance(vals);
    }
}
