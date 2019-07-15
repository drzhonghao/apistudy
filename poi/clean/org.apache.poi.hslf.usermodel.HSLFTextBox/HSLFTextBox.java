import org.apache.poi.hslf.usermodel.HSLFShape;
import org.apache.poi.hslf.usermodel.*;


import org.apache.poi.ddf.EscherContainerRecord;
import org.apache.poi.ddf.EscherProperties;
import org.apache.poi.sl.usermodel.ShapeContainer;
import org.apache.poi.sl.usermodel.ShapeType;
import org.apache.poi.sl.usermodel.TextBox;
import org.apache.poi.sl.usermodel.VerticalAlignment;

/**
 * Represents a TextFrame shape in PowerPoint.
 * <p>
 * Contains the text in a text frame as well as the properties and methods
 * that control alignment and anchoring of the text.
 * </p>
 *
 * @author Yegor Kozlov
 */
public class HSLFTextBox extends HSLFTextShape implements TextBox<HSLFShape,HSLFTextParagraph> {

    /**
     * Create a TextBox object and initialize it from the supplied Record container.
     *
     * @param escherRecord       <code>EscherSpContainer</code> container which holds information about this shape
     * @param parent    the parent of the shape
     */
   protected HSLFTextBox(EscherContainerRecord escherRecord, ShapeContainer<HSLFShape,HSLFTextParagraph> parent){
        super(escherRecord, parent);

    }

    /**
     * Create a new TextBox. This constructor is used when a new shape is created.
     *
     * @param parent    the parent of this Shape. For example, if this text box is a cell
     * in a table then the parent is Table.
     */
    public HSLFTextBox(ShapeContainer<HSLFShape,HSLFTextParagraph> parent){
        super(parent);
    }

    /**
     * Create a new TextBox. This constructor is used when a new shape is created.
     *
     */
    public HSLFTextBox(){
        this(null);
    }

    /**
     * Create a new TextBox and initialize its internal structures
     *
     * @return the created <code>EscherContainerRecord</code> which holds shape data
     */
    @Override
    protected EscherContainerRecord createSpContainer(boolean isChild){
        EscherContainerRecord ecr = super.createSpContainer(isChild);

        setShapeType(ShapeType.TEXT_BOX);

        //set default properties for a TextBox
        setEscherProperty(EscherProperties.FILL__FILLCOLOR, 0x8000004);
        setEscherProperty(EscherProperties.FILL__FILLBACKCOLOR, 0x8000000);
        setEscherProperty(EscherProperties.FILL__NOFILLHITTEST, 0x100000);
        setEscherProperty(EscherProperties.LINESTYLE__COLOR, 0x8000001);
        setEscherProperty(EscherProperties.LINESTYLE__NOLINEDRAWDASH, 0x80000);
        setEscherProperty(EscherProperties.SHADOWSTYLE__COLOR, 0x8000002);

        // init paragraphs
        getTextParagraphs();

        return ecr;
    }

    @Override
    protected void setDefaultTextProperties(HSLFTextParagraph _txtrun){
        setVerticalAlignment(VerticalAlignment.TOP);
        setEscherProperty(EscherProperties.TEXT__SIZE_TEXT_TO_FIT_SHAPE, 0x20002);
    }
}
