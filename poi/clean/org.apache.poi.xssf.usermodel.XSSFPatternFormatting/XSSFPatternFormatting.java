import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.*;


import org.apache.poi.ss.usermodel.Color;
import org.apache.poi.ss.usermodel.PatternFormatting;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTFill;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTPatternFill;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTColor;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.STPatternType;

/**
 * @author Yegor Kozlov
 */
public class XSSFPatternFormatting implements PatternFormatting {
    IndexedColorMap _colorMap;
    CTFill _fill;

    XSSFPatternFormatting(CTFill fill, IndexedColorMap colorMap) {
        _fill = fill;
        _colorMap = colorMap;
    }

    public XSSFColor getFillBackgroundColorColor() {
        if(!_fill.isSetPatternFill()) return null;
        return XSSFColor.from(_fill.getPatternFill().getBgColor(), _colorMap);
    }
    public XSSFColor getFillForegroundColorColor() {
        if(!_fill.isSetPatternFill() || ! _fill.getPatternFill().isSetFgColor())
            return null;
        return XSSFColor.from(_fill.getPatternFill().getFgColor(), _colorMap);
    }

    public short getFillPattern() {
        if(!_fill.isSetPatternFill() || !_fill.getPatternFill().isSetPatternType()) return NO_FILL;

        return (short)(_fill.getPatternFill().getPatternType().intValue() - 1);
     }

    public short getFillBackgroundColor() {
        XSSFColor color = getFillBackgroundColorColor();
        if (color == null) return 0;
        return color.getIndexed();
    }
    public short getFillForegroundColor() {
        XSSFColor color = getFillForegroundColorColor();
        if (color == null) return 0;
        return color.getIndexed();
    }

    public void setFillBackgroundColor(Color bg) {
        XSSFColor xcolor = XSSFColor.toXSSFColor(bg);
        if (xcolor == null) setFillBackgroundColor((CTColor)null);
        else setFillBackgroundColor(xcolor.getCTColor());
    }
    public void setFillBackgroundColor(short bg) {
        CTColor bgColor = CTColor.Factory.newInstance();
        bgColor.setIndexed(bg);
        setFillBackgroundColor(bgColor);
    }
    private void setFillBackgroundColor(CTColor color) {
        CTPatternFill ptrn = _fill.isSetPatternFill() ? _fill.getPatternFill() : _fill.addNewPatternFill();
        if (color == null) {
            ptrn.unsetBgColor();
        } else {
            ptrn.setBgColor(color);
        }
    }
    
    public void setFillForegroundColor(Color fg) {
        XSSFColor xcolor = XSSFColor.toXSSFColor(fg);
        if (xcolor == null) setFillForegroundColor((CTColor)null);
        else setFillForegroundColor(xcolor.getCTColor());
    }
    public void setFillForegroundColor(short fg) {
        CTColor fgColor = CTColor.Factory.newInstance();
        fgColor.setIndexed(fg);
        setFillForegroundColor(fgColor);
    }
    private void setFillForegroundColor(CTColor color) {
        CTPatternFill ptrn = _fill.isSetPatternFill() ? _fill.getPatternFill() : _fill.addNewPatternFill();
        if (color == null) {
            ptrn.unsetFgColor();
        } else {
            ptrn.setFgColor(color);
        }
    }

    public void setFillPattern(short fp){
        CTPatternFill ptrn = _fill.isSetPatternFill() ? _fill.getPatternFill() : _fill.addNewPatternFill();
        if(fp == NO_FILL) ptrn.unsetPatternType();
        else ptrn.setPatternType(STPatternType.Enum.forInt(fp + 1));
    }
}
