import org.apache.poi.hslf.usermodel.*;


import org.apache.poi.ddf.AbstractEscherOptRecord;
import org.apache.poi.ddf.EscherContainerRecord;
import org.apache.poi.ddf.EscherProperties;
import org.apache.poi.ddf.EscherSpRecord;
import org.apache.poi.sl.usermodel.Line;
import org.apache.poi.sl.usermodel.ShapeContainer;
import org.apache.poi.sl.usermodel.ShapeType;

/**
 * Represents a line in a PowerPoint drawing
 *
 *  @author Yegor Kozlov
 */
public final class HSLFLine extends HSLFTextShape implements Line<HSLFShape,HSLFTextParagraph> {
    public HSLFLine(EscherContainerRecord escherRecord, ShapeContainer<HSLFShape,HSLFTextParagraph> parent){
        super(escherRecord, parent);
    }

    public HSLFLine(ShapeContainer<HSLFShape,HSLFTextParagraph> parent){
        super(null, parent);
        createSpContainer(parent instanceof HSLFGroupShape);
    }

    public HSLFLine(){
        this(null);
    }

    @Override
    protected EscherContainerRecord createSpContainer(boolean isChild){
        EscherContainerRecord ecr = super.createSpContainer(isChild);
        
        setShapeType(ShapeType.LINE);

        EscherSpRecord spRecord = ecr.getChildById(EscherSpRecord.RECORD_ID);
        short type = (short)((ShapeType.LINE.nativeId << 4) | 0x2);
        spRecord.setOptions(type);

        //set default properties for a line
        AbstractEscherOptRecord opt = getEscherOptRecord();

        //default line properties
        setEscherProperty(opt, EscherProperties.GEOMETRY__SHAPEPATH, 4);
        setEscherProperty(opt, EscherProperties.GEOMETRY__FILLOK, 0x10000);
        setEscherProperty(opt, EscherProperties.FILL__NOFILLHITTEST, 0x100000);
        setEscherProperty(opt, EscherProperties.LINESTYLE__COLOR, 0x8000001);
        setEscherProperty(opt, EscherProperties.LINESTYLE__NOLINEDRAWDASH, 0xA0008);
        setEscherProperty(opt, EscherProperties.SHADOWSTYLE__COLOR, 0x8000002);

        return ecr;
    }
    
//    /**
//     * Sets the orientation of the line, if inverse is false, then line goes
//     * from top-left to bottom-right, otherwise use inverse equals true 
//     *
//     * @param inverse the orientation of the line
//     */
//    public void setInverse(boolean inverse) {
//        setShapeType(inverse ? ShapeType.LINE_INV : ShapeType.LINE);
//    }
//    
//    /**
//     * Gets the orientation of the line, if inverse is false, then line goes
//     * from top-left to bottom-right, otherwise inverse equals true 
//     *
//     * @return inverse the orientation of the line
//     */
//    public boolean isInverse() {
//        return (getShapeType() == ShapeType.LINE_INV);
//    }
}
