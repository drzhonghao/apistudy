import org.apache.poi.xwpf.usermodel.*;


import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.apache.poi.ooxml.POIXMLDocumentPart;
import org.apache.poi.ooxml.POIXMLFactory;
import org.apache.poi.ooxml.POIXMLRelation;

/**
 * Instantiates sub-classes of POIXMLDocumentPart depending on their relationship type
 */
public final class XWPFFactory extends POIXMLFactory {
    private XWPFFactory() {

    }

    private static final XWPFFactory inst = new XWPFFactory();

    public static XWPFFactory getInstance() {
        return inst;
    }

    /**
     * @since POI 3.14-Beta1
     */
    @Override
    protected POIXMLRelation getDescriptor(String relationshipType) {
        return XWPFRelation.getInstance(relationshipType);
    }

    /**
     * @since POI 3.14-Beta1
     */
    @Override
    protected POIXMLDocumentPart createDocumentPart
        (Class<? extends POIXMLDocumentPart> cls, Class<?>[] classes, Object[] values)
    throws SecurityException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        Constructor<? extends POIXMLDocumentPart> constructor = cls.getDeclaredConstructor(classes);
        return constructor.newInstance(values);
    }
}
