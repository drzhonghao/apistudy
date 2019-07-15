

import java.util.HashMap;
import java.util.Map;
import org.apache.poi.ss.usermodel.ComparisonOperator;
import org.apache.poi.ss.usermodel.ConditionFilterData;
import org.apache.poi.ss.usermodel.ConditionFilterType;
import org.apache.poi.ss.usermodel.ConditionType;
import org.apache.poi.ss.usermodel.ConditionalFormattingRule;
import org.apache.poi.ss.usermodel.ConditionalFormattingThreshold;
import org.apache.poi.ss.usermodel.ExcelNumberFormat;
import org.apache.poi.ss.usermodel.IconMultiStateFormatting;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFBorderFormatting;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFColorScaleFormatting;
import org.apache.poi.xssf.usermodel.XSSFDataBarFormatting;
import org.apache.poi.xssf.usermodel.XSSFFontFormatting;
import org.apache.poi.xssf.usermodel.XSSFIconMultiStateFormatting;
import org.apache.poi.xssf.usermodel.XSSFPatternFormatting;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.xmlbeans.StringEnumAbstractBase;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTBorder;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCfRule;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCfvo;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTColor;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTColorScale;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTDataBar;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTDxf;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTFill;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTFont;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTIconSet;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTNumFmt;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.STCfType;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.STCfType.Enum;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.STCfvoType;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.STConditionalFormattingOperator;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.STIconSetType;

import static org.apache.poi.ss.usermodel.ConditionalFormattingThreshold.RangeType.MAX;
import static org.apache.poi.ss.usermodel.ConditionalFormattingThreshold.RangeType.MIN;
import static org.apache.poi.ss.usermodel.ConditionalFormattingThreshold.RangeType.PERCENT;
import static org.apache.poi.ss.usermodel.ConditionalFormattingThreshold.RangeType.PERCENTILE;
import static org.openxmlformats.schemas.spreadsheetml.x2006.main.CTDxf.Factory.newInstance;


public class XSSFConditionalFormattingRule implements ConditionalFormattingRule {
	private final CTCfRule _cfRule;

	private XSSFSheet _sh;

	private static Map<STCfType.Enum, ConditionType> typeLookup = new HashMap<>();

	private static Map<STCfType.Enum, ConditionFilterType> filterTypeLookup = new HashMap<>();

	static {
		XSSFConditionalFormattingRule.typeLookup.put(STCfType.CELL_IS, ConditionType.CELL_VALUE_IS);
		XSSFConditionalFormattingRule.typeLookup.put(STCfType.EXPRESSION, ConditionType.FORMULA);
		XSSFConditionalFormattingRule.typeLookup.put(STCfType.COLOR_SCALE, ConditionType.COLOR_SCALE);
		XSSFConditionalFormattingRule.typeLookup.put(STCfType.DATA_BAR, ConditionType.DATA_BAR);
		XSSFConditionalFormattingRule.typeLookup.put(STCfType.ICON_SET, ConditionType.ICON_SET);
		XSSFConditionalFormattingRule.typeLookup.put(STCfType.TOP_10, ConditionType.FILTER);
		XSSFConditionalFormattingRule.typeLookup.put(STCfType.UNIQUE_VALUES, ConditionType.FILTER);
		XSSFConditionalFormattingRule.typeLookup.put(STCfType.DUPLICATE_VALUES, ConditionType.FILTER);
		XSSFConditionalFormattingRule.typeLookup.put(STCfType.CONTAINS_TEXT, ConditionType.FILTER);
		XSSFConditionalFormattingRule.typeLookup.put(STCfType.NOT_CONTAINS_TEXT, ConditionType.FILTER);
		XSSFConditionalFormattingRule.typeLookup.put(STCfType.BEGINS_WITH, ConditionType.FILTER);
		XSSFConditionalFormattingRule.typeLookup.put(STCfType.ENDS_WITH, ConditionType.FILTER);
		XSSFConditionalFormattingRule.typeLookup.put(STCfType.CONTAINS_BLANKS, ConditionType.FILTER);
		XSSFConditionalFormattingRule.typeLookup.put(STCfType.NOT_CONTAINS_BLANKS, ConditionType.FILTER);
		XSSFConditionalFormattingRule.typeLookup.put(STCfType.CONTAINS_ERRORS, ConditionType.FILTER);
		XSSFConditionalFormattingRule.typeLookup.put(STCfType.NOT_CONTAINS_ERRORS, ConditionType.FILTER);
		XSSFConditionalFormattingRule.typeLookup.put(STCfType.TIME_PERIOD, ConditionType.FILTER);
		XSSFConditionalFormattingRule.typeLookup.put(STCfType.ABOVE_AVERAGE, ConditionType.FILTER);
		XSSFConditionalFormattingRule.filterTypeLookup.put(STCfType.TOP_10, ConditionFilterType.TOP_10);
		XSSFConditionalFormattingRule.filterTypeLookup.put(STCfType.UNIQUE_VALUES, ConditionFilterType.UNIQUE_VALUES);
		XSSFConditionalFormattingRule.filterTypeLookup.put(STCfType.DUPLICATE_VALUES, ConditionFilterType.DUPLICATE_VALUES);
		XSSFConditionalFormattingRule.filterTypeLookup.put(STCfType.CONTAINS_TEXT, ConditionFilterType.CONTAINS_TEXT);
		XSSFConditionalFormattingRule.filterTypeLookup.put(STCfType.NOT_CONTAINS_TEXT, ConditionFilterType.NOT_CONTAINS_TEXT);
		XSSFConditionalFormattingRule.filterTypeLookup.put(STCfType.BEGINS_WITH, ConditionFilterType.BEGINS_WITH);
		XSSFConditionalFormattingRule.filterTypeLookup.put(STCfType.ENDS_WITH, ConditionFilterType.ENDS_WITH);
		XSSFConditionalFormattingRule.filterTypeLookup.put(STCfType.CONTAINS_BLANKS, ConditionFilterType.CONTAINS_BLANKS);
		XSSFConditionalFormattingRule.filterTypeLookup.put(STCfType.NOT_CONTAINS_BLANKS, ConditionFilterType.NOT_CONTAINS_BLANKS);
		XSSFConditionalFormattingRule.filterTypeLookup.put(STCfType.CONTAINS_ERRORS, ConditionFilterType.CONTAINS_ERRORS);
		XSSFConditionalFormattingRule.filterTypeLookup.put(STCfType.NOT_CONTAINS_ERRORS, ConditionFilterType.NOT_CONTAINS_ERRORS);
		XSSFConditionalFormattingRule.filterTypeLookup.put(STCfType.TIME_PERIOD, ConditionFilterType.TIME_PERIOD);
		XSSFConditionalFormattingRule.filterTypeLookup.put(STCfType.ABOVE_AVERAGE, ConditionFilterType.ABOVE_AVERAGE);
	}

	XSSFConditionalFormattingRule(XSSFSheet sh) {
		_cfRule = CTCfRule.Factory.newInstance();
		_sh = sh;
	}

	XSSFConditionalFormattingRule(XSSFSheet sh, CTCfRule cfRule) {
		_cfRule = cfRule;
		_sh = sh;
	}

	CTCfRule getCTCfRule() {
		return _cfRule;
	}

	CTDxf getDxf(boolean create) {
		StylesTable styles = _sh.getWorkbook().getStylesSource();
		CTDxf dxf = null;
		if (((styles._getDXfsSize()) > 0) && (_cfRule.isSetDxfId())) {
			int dxfId = ((int) (_cfRule.getDxfId()));
			dxf = styles.getDxfAt(dxfId);
		}
		if (create && (dxf == null)) {
			dxf = newInstance();
			int dxfId = styles.putDxf(dxf);
			_cfRule.setDxfId((dxfId - 1));
		}
		return dxf;
	}

	public int getPriority() {
		final int priority = _cfRule.getPriority();
		return priority >= 1 ? priority : 0;
	}

	public boolean getStopIfTrue() {
		return _cfRule.getStopIfTrue();
	}

	public XSSFBorderFormatting createBorderFormatting() {
		CTDxf dxf = getDxf(true);
		CTBorder border;
		if (!(dxf.isSetBorder())) {
			border = dxf.addNewBorder();
		}else {
			border = dxf.getBorder();
		}
		return null;
	}

	public XSSFBorderFormatting getBorderFormatting() {
		CTDxf dxf = getDxf(false);
		if ((dxf == null) || (!(dxf.isSetBorder())))
			return null;

		return null;
	}

	public XSSFFontFormatting createFontFormatting() {
		CTDxf dxf = getDxf(true);
		CTFont font;
		if (!(dxf.isSetFont())) {
			font = dxf.addNewFont();
		}else {
			font = dxf.getFont();
		}
		return null;
	}

	public XSSFFontFormatting getFontFormatting() {
		CTDxf dxf = getDxf(false);
		if ((dxf == null) || (!(dxf.isSetFont())))
			return null;

		return null;
	}

	public XSSFPatternFormatting createPatternFormatting() {
		CTDxf dxf = getDxf(true);
		CTFill fill;
		if (!(dxf.isSetFill())) {
			fill = dxf.addNewFill();
		}else {
			fill = dxf.getFill();
		}
		return null;
	}

	public XSSFPatternFormatting getPatternFormatting() {
		CTDxf dxf = getDxf(false);
		if ((dxf == null) || (!(dxf.isSetFill())))
			return null;

		return null;
	}

	public XSSFDataBarFormatting createDataBarFormatting(XSSFColor color) {
		if ((_cfRule.isSetDataBar()) && ((_cfRule.getType()) == (STCfType.DATA_BAR)))
			return getDataBarFormatting();

		_cfRule.setType(STCfType.DATA_BAR);
		CTDataBar bar = null;
		if (_cfRule.isSetDataBar()) {
			bar = _cfRule.getDataBar();
		}else {
			bar = _cfRule.addNewDataBar();
		}
		bar.setColor(color.getCTColor());
		CTCfvo min = bar.addNewCfvo();
		min.setType(STCfvoType.Enum.forString(MIN.name));
		CTCfvo max = bar.addNewCfvo();
		max.setType(STCfvoType.Enum.forString(MAX.name));
		return null;
	}

	public XSSFDataBarFormatting getDataBarFormatting() {
		if (_cfRule.isSetDataBar()) {
			CTDataBar bar = _cfRule.getDataBar();
		}else {
			return null;
		}
		return null;
	}

	public XSSFIconMultiStateFormatting createMultiStateFormatting(IconMultiStateFormatting.IconSet iconSet) {
		if ((_cfRule.isSetIconSet()) && ((_cfRule.getType()) == (STCfType.ICON_SET)))
			return getMultiStateFormatting();

		_cfRule.setType(STCfType.ICON_SET);
		CTIconSet icons = null;
		if (_cfRule.isSetIconSet()) {
			icons = _cfRule.getIconSet();
		}else {
			icons = _cfRule.addNewIconSet();
		}
		if ((iconSet.name) != null) {
			STIconSetType.Enum xIconSet = STIconSetType.Enum.forString(iconSet.name);
			icons.setIconSet(xIconSet);
		}
		int jump = 100 / (iconSet.num);
		STCfvoType.Enum type = STCfvoType.Enum.forString(PERCENT.name);
		for (int i = 0; i < (iconSet.num); i++) {
			CTCfvo cfvo = icons.addNewCfvo();
			cfvo.setType(type);
			cfvo.setVal(Integer.toString((i * jump)));
		}
		return null;
	}

	public XSSFIconMultiStateFormatting getMultiStateFormatting() {
		if (_cfRule.isSetIconSet()) {
			CTIconSet icons = _cfRule.getIconSet();
		}else {
			return null;
		}
		return null;
	}

	public XSSFColorScaleFormatting createColorScaleFormatting() {
		if ((_cfRule.isSetColorScale()) && ((_cfRule.getType()) == (STCfType.COLOR_SCALE)))
			return getColorScaleFormatting();

		_cfRule.setType(STCfType.COLOR_SCALE);
		CTColorScale scale = null;
		if (_cfRule.isSetColorScale()) {
			scale = _cfRule.getColorScale();
		}else {
			scale = _cfRule.addNewColorScale();
		}
		if ((scale.sizeOfCfvoArray()) == 0) {
			CTCfvo cfvo;
			cfvo = scale.addNewCfvo();
			cfvo.setType(STCfvoType.Enum.forString(MIN.name));
			cfvo = scale.addNewCfvo();
			cfvo.setType(STCfvoType.Enum.forString(PERCENTILE.name));
			cfvo.setVal("50");
			cfvo = scale.addNewCfvo();
			cfvo.setType(STCfvoType.Enum.forString(MAX.name));
			for (int i = 0; i < 3; i++) {
				scale.addNewColor();
			}
		}
		return null;
	}

	public XSSFColorScaleFormatting getColorScaleFormatting() {
		if (_cfRule.isSetColorScale()) {
			CTColorScale scale = _cfRule.getColorScale();
		}else {
			return null;
		}
		return null;
	}

	public ExcelNumberFormat getNumberFormat() {
		CTDxf dxf = getDxf(false);
		if ((dxf == null) || (!(dxf.isSetNumFmt())))
			return null;

		CTNumFmt numFmt = dxf.getNumFmt();
		return new ExcelNumberFormat(((int) (numFmt.getNumFmtId())), numFmt.getFormatCode());
	}

	@Override
	public ConditionType getConditionType() {
		return XSSFConditionalFormattingRule.typeLookup.get(_cfRule.getType());
	}

	public ConditionFilterType getConditionFilterType() {
		return XSSFConditionalFormattingRule.filterTypeLookup.get(_cfRule.getType());
	}

	public ConditionFilterData getFilterConfiguration() {
		return null;
	}

	@Override
	public byte getComparisonOperation() {
		STConditionalFormattingOperator.Enum op = _cfRule.getOperator();
		if (op == null)
			return ComparisonOperator.NO_COMPARISON;

		switch (op.intValue()) {
			case STConditionalFormattingOperator.INT_LESS_THAN :
				return ComparisonOperator.LT;
			case STConditionalFormattingOperator.INT_LESS_THAN_OR_EQUAL :
				return ComparisonOperator.LE;
			case STConditionalFormattingOperator.INT_GREATER_THAN :
				return ComparisonOperator.GT;
			case STConditionalFormattingOperator.INT_GREATER_THAN_OR_EQUAL :
				return ComparisonOperator.GE;
			case STConditionalFormattingOperator.INT_EQUAL :
				return ComparisonOperator.EQUAL;
			case STConditionalFormattingOperator.INT_NOT_EQUAL :
				return ComparisonOperator.NOT_EQUAL;
			case STConditionalFormattingOperator.INT_BETWEEN :
				return ComparisonOperator.BETWEEN;
			case STConditionalFormattingOperator.INT_NOT_BETWEEN :
				return ComparisonOperator.NOT_BETWEEN;
		}
		return ComparisonOperator.NO_COMPARISON;
	}

	public String getFormula1() {
		return (_cfRule.sizeOfFormulaArray()) > 0 ? _cfRule.getFormulaArray(0) : null;
	}

	public String getFormula2() {
		return (_cfRule.sizeOfFormulaArray()) == 2 ? _cfRule.getFormulaArray(1) : null;
	}

	public String getText() {
		return _cfRule.getText();
	}

	public int getStripeSize() {
		return 0;
	}
}

