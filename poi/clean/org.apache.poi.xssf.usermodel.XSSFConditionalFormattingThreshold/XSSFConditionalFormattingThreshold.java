import org.apache.poi.xssf.usermodel.*;


import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCfvo;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.STCfvoType;

/**
 * High level representation for Icon / Multi-State / Databar /
 *  Colour Scale change thresholds
 */
public class XSSFConditionalFormattingThreshold implements org.apache.poi.ss.usermodel.ConditionalFormattingThreshold {
    private CTCfvo cfvo;
    
    protected XSSFConditionalFormattingThreshold(CTCfvo cfvo) {
        this.cfvo = cfvo;
    }
    
    protected CTCfvo getCTCfvo() {
        return cfvo;
    }

    public RangeType getRangeType() {
        return RangeType.byName(cfvo.getType().toString());
    }
    public void setRangeType(RangeType type) {
        STCfvoType.Enum xtype = STCfvoType.Enum.forString(type.name);
        cfvo.setType(xtype);
    }

    public String getFormula() {
        if (cfvo.getType() == STCfvoType.FORMULA) {
            return cfvo.getVal();
        }
        return null;
    }
    public void setFormula(String formula) {
        cfvo.setVal(formula);
    }

    public Double getValue() {
        if (cfvo.getType() == STCfvoType.FORMULA ||
            cfvo.getType() == STCfvoType.MIN ||
            cfvo.getType() == STCfvoType.MAX) {
            return null;
        }
        if (cfvo.isSetVal()) {
            return Double.parseDouble(cfvo.getVal());
        } else {
            return null;
        }
    }
    public void setValue(Double value) {
        if (value == null) {
            cfvo.unsetVal();
        } else {
            cfvo.setVal(value.toString());
        }
    }
}
