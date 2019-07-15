import org.apache.poi.hslf.usermodel.HSLFSimpleShape;
import org.apache.poi.hslf.usermodel.HSLFShape;
import org.apache.poi.hslf.usermodel.HSLFGroupShape;
import org.apache.poi.hslf.usermodel.*;


import org.apache.poi.ddf.EscherContainerRecord;
import org.apache.poi.sl.usermodel.ConnectorShape;
import org.apache.poi.sl.usermodel.ShapeContainer;
import org.apache.poi.util.Beta;

/**
 * Specifies a connection shape.
 * 
 * This is currently only a dummy implementation.
 */
@Beta
public class HSLFConnectorShape extends HSLFSimpleShape
implements ConnectorShape<HSLFShape,HSLFTextParagraph> {

    /**
     * Create a ConnectorShape object and initialize it from the supplied Record container.
     *
     * @param escherRecord       <code>EscherSpContainer</code> container which holds information about this shape
     * @param parent    the parent of the shape
     */
   protected HSLFConnectorShape(EscherContainerRecord escherRecord, ShapeContainer<HSLFShape,HSLFTextParagraph> parent){
        super(escherRecord, parent);

    }

    /**
     * Create a new ConnectorShape. This constructor is used when a new shape is created.
     *
     * @param parent    the parent of this Shape. For example, if this text box is a cell
     * in a table then the parent is Table.
     */
    public HSLFConnectorShape(ShapeContainer<HSLFShape,HSLFTextParagraph> parent){
        super(null, parent);
        createSpContainer(parent instanceof HSLFGroupShape);
    }

    /**
     * Create a new ConnectorShape. This constructor is used when a new shape is created.
     *
     */
    public HSLFConnectorShape(){
        this(null);
    }
}
