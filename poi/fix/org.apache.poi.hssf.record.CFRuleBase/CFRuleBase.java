

import org.apache.poi.hssf.model.HSSFFormulaParser;
import org.apache.poi.hssf.record.RecordInputStream;
import org.apache.poi.hssf.record.StandardRecord;
import org.apache.poi.hssf.record.cf.BorderFormatting;
import org.apache.poi.hssf.record.cf.FontFormatting;
import org.apache.poi.hssf.record.cf.PatternFormatting;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.formula.Formula;
import org.apache.poi.ss.formula.FormulaType;
import org.apache.poi.ss.formula.ptg.Ptg;
import org.apache.poi.util.BitField;
import org.apache.poi.util.BitFieldFactory;
import org.apache.poi.util.LittleEndianOutput;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;


public abstract class CFRuleBase extends StandardRecord implements Cloneable {
	public static final class ComparisonOperator {
		public static final byte NO_COMPARISON = 0;

		public static final byte BETWEEN = 1;

		public static final byte NOT_BETWEEN = 2;

		public static final byte EQUAL = 3;

		public static final byte NOT_EQUAL = 4;

		public static final byte GT = 5;

		public static final byte LT = 6;

		public static final byte GE = 7;

		public static final byte LE = 8;

		private static final byte max_operator = 8;
	}

	protected static final POILogger logger = POILogFactory.getLogger(CFRuleBase.class);

	private byte condition_type;

	public static final byte CONDITION_TYPE_CELL_VALUE_IS = 1;

	public static final byte CONDITION_TYPE_FORMULA = 2;

	public static final byte CONDITION_TYPE_COLOR_SCALE = 3;

	public static final byte CONDITION_TYPE_DATA_BAR = 4;

	public static final byte CONDITION_TYPE_FILTER = 5;

	public static final byte CONDITION_TYPE_ICON_SET = 6;

	private byte comparison_operator;

	public static final int TEMPLATE_CELL_VALUE = 0;

	public static final int TEMPLATE_FORMULA = 1;

	public static final int TEMPLATE_COLOR_SCALE_FORMATTING = 2;

	public static final int TEMPLATE_DATA_BAR_FORMATTING = 3;

	public static final int TEMPLATE_ICON_SET_FORMATTING = 4;

	public static final int TEMPLATE_FILTER = 5;

	public static final int TEMPLATE_UNIQUE_VALUES = 7;

	public static final int TEMPLATE_CONTAINS_TEXT = 8;

	public static final int TEMPLATE_CONTAINS_BLANKS = 9;

	public static final int TEMPLATE_CONTAINS_NO_BLANKS = 10;

	public static final int TEMPLATE_CONTAINS_ERRORS = 11;

	public static final int TEMPLATE_CONTAINS_NO_ERRORS = 12;

	public static final int TEMPLATE_TODAY = 15;

	public static final int TEMPLATE_TOMORROW = 16;

	public static final int TEMPLATE_YESTERDAY = 17;

	public static final int TEMPLATE_LAST_7_DAYS = 18;

	public static final int TEMPLATE_LAST_MONTH = 19;

	public static final int TEMPLATE_NEXT_MONTH = 20;

	public static final int TEMPLATE_THIS_WEEK = 21;

	public static final int TEMPLATE_NEXT_WEEK = 22;

	public static final int TEMPLATE_LAST_WEEK = 23;

	public static final int TEMPLATE_THIS_MONTH = 24;

	public static final int TEMPLATE_ABOVE_AVERAGE = 25;

	public static final int TEMPLATE_BELOW_AVERAGE = 26;

	public static final int TEMPLATE_DUPLICATE_VALUES = 27;

	public static final int TEMPLATE_ABOVE_OR_EQUAL_TO_AVERAGE = 29;

	public static final int TEMPLATE_BELOW_OR_EQUAL_TO_AVERAGE = 30;

	static final BitField modificationBits = CFRuleBase.bf(4194303);

	static final BitField alignHor = CFRuleBase.bf(1);

	static final BitField alignVer = CFRuleBase.bf(2);

	static final BitField alignWrap = CFRuleBase.bf(4);

	static final BitField alignRot = CFRuleBase.bf(8);

	static final BitField alignJustLast = CFRuleBase.bf(16);

	static final BitField alignIndent = CFRuleBase.bf(32);

	static final BitField alignShrin = CFRuleBase.bf(64);

	static final BitField mergeCell = CFRuleBase.bf(128);

	static final BitField protLocked = CFRuleBase.bf(256);

	static final BitField protHidden = CFRuleBase.bf(512);

	static final BitField bordLeft = CFRuleBase.bf(1024);

	static final BitField bordRight = CFRuleBase.bf(2048);

	static final BitField bordTop = CFRuleBase.bf(4096);

	static final BitField bordBot = CFRuleBase.bf(8192);

	static final BitField bordTlBr = CFRuleBase.bf(16384);

	static final BitField bordBlTr = CFRuleBase.bf(32768);

	static final BitField pattStyle = CFRuleBase.bf(65536);

	static final BitField pattCol = CFRuleBase.bf(131072);

	static final BitField pattBgCol = CFRuleBase.bf(262144);

	static final BitField notUsed2 = CFRuleBase.bf(3670016);

	static final BitField undocumented = CFRuleBase.bf(62914560);

	static final BitField fmtBlockBits = CFRuleBase.bf(2080374784);

	static final BitField font = CFRuleBase.bf(67108864);

	static final BitField align = CFRuleBase.bf(134217728);

	static final BitField bord = CFRuleBase.bf(268435456);

	static final BitField patt = CFRuleBase.bf(536870912);

	static final BitField prot = CFRuleBase.bf(1073741824);

	static final BitField alignTextDir = CFRuleBase.bf(-2147483648);

	private static BitField bf(int i) {
		return BitFieldFactory.getInstance(i);
	}

	protected int formatting_options;

	protected short formatting_not_used;

	protected FontFormatting _fontFormatting;

	protected BorderFormatting _borderFormatting;

	protected PatternFormatting _patternFormatting;

	private Formula formula1;

	private Formula formula2;

	protected CFRuleBase(byte conditionType, byte comparisonOperation) {
		setConditionType(conditionType);
		setComparisonOperation(comparisonOperation);
		formula1 = Formula.create(Ptg.EMPTY_PTG_ARRAY);
		formula2 = Formula.create(Ptg.EMPTY_PTG_ARRAY);
	}

	protected CFRuleBase(byte conditionType, byte comparisonOperation, Ptg[] formula1, Ptg[] formula2) {
		this(conditionType, comparisonOperation);
		this.formula1 = Formula.create(formula1);
		this.formula2 = Formula.create(formula2);
	}

	protected CFRuleBase() {
	}

	protected int readFormatOptions(RecordInputStream in) {
		formatting_options = in.readInt();
		formatting_not_used = in.readShort();
		int len = 6;
		if (containsFontFormattingBlock()) {
			_fontFormatting = new FontFormatting(in);
			len += _fontFormatting.getDataLength();
		}
		if (containsBorderFormattingBlock()) {
			_borderFormatting = new BorderFormatting(in);
			len += _borderFormatting.getDataLength();
		}
		if (containsPatternFormattingBlock()) {
			_patternFormatting = new PatternFormatting(in);
			len += _patternFormatting.getDataLength();
		}
		return len;
	}

	public byte getConditionType() {
		return condition_type;
	}

	protected void setConditionType(byte condition_type) {
		this.condition_type = condition_type;
	}

	public void setComparisonOperation(byte operation) {
		if ((operation < 0) || (operation > (CFRuleBase.ComparisonOperator.max_operator)))
			throw new IllegalArgumentException(("Valid operators are only in the range 0 to " + (CFRuleBase.ComparisonOperator.max_operator)));

		this.comparison_operator = operation;
	}

	public byte getComparisonOperation() {
		return comparison_operator;
	}

	public boolean containsFontFormattingBlock() {
		return getOptionFlag(CFRuleBase.font);
	}

	public void setFontFormatting(FontFormatting fontFormatting) {
		_fontFormatting = fontFormatting;
		setOptionFlag((fontFormatting != null), CFRuleBase.font);
	}

	public FontFormatting getFontFormatting() {
		if (containsFontFormattingBlock()) {
			return _fontFormatting;
		}
		return null;
	}

	public boolean containsAlignFormattingBlock() {
		return getOptionFlag(CFRuleBase.align);
	}

	public void setAlignFormattingUnchanged() {
		setOptionFlag(false, CFRuleBase.align);
	}

	public boolean containsBorderFormattingBlock() {
		return getOptionFlag(CFRuleBase.bord);
	}

	public void setBorderFormatting(BorderFormatting borderFormatting) {
		_borderFormatting = borderFormatting;
		setOptionFlag((borderFormatting != null), CFRuleBase.bord);
	}

	public BorderFormatting getBorderFormatting() {
		if (containsBorderFormattingBlock()) {
			return _borderFormatting;
		}
		return null;
	}

	public boolean containsPatternFormattingBlock() {
		return getOptionFlag(CFRuleBase.patt);
	}

	public void setPatternFormatting(PatternFormatting patternFormatting) {
		_patternFormatting = patternFormatting;
		setOptionFlag((patternFormatting != null), CFRuleBase.patt);
	}

	public PatternFormatting getPatternFormatting() {
		if (containsPatternFormattingBlock()) {
			return _patternFormatting;
		}
		return null;
	}

	public boolean containsProtectionFormattingBlock() {
		return getOptionFlag(CFRuleBase.prot);
	}

	public void setProtectionFormattingUnchanged() {
		setOptionFlag(false, CFRuleBase.prot);
	}

	public int getOptions() {
		return formatting_options;
	}

	private boolean isModified(BitField field) {
		return !(field.isSet(formatting_options));
	}

	private void setModified(boolean modified, BitField field) {
		formatting_options = field.setBoolean(formatting_options, (!modified));
	}

	public boolean isLeftBorderModified() {
		return isModified(CFRuleBase.bordLeft);
	}

	public void setLeftBorderModified(boolean modified) {
		setModified(modified, CFRuleBase.bordLeft);
	}

	public boolean isRightBorderModified() {
		return isModified(CFRuleBase.bordRight);
	}

	public void setRightBorderModified(boolean modified) {
		setModified(modified, CFRuleBase.bordRight);
	}

	public boolean isTopBorderModified() {
		return isModified(CFRuleBase.bordTop);
	}

	public void setTopBorderModified(boolean modified) {
		setModified(modified, CFRuleBase.bordTop);
	}

	public boolean isBottomBorderModified() {
		return isModified(CFRuleBase.bordBot);
	}

	public void setBottomBorderModified(boolean modified) {
		setModified(modified, CFRuleBase.bordBot);
	}

	public boolean isTopLeftBottomRightBorderModified() {
		return isModified(CFRuleBase.bordTlBr);
	}

	public void setTopLeftBottomRightBorderModified(boolean modified) {
		setModified(modified, CFRuleBase.bordTlBr);
	}

	public boolean isBottomLeftTopRightBorderModified() {
		return isModified(CFRuleBase.bordBlTr);
	}

	public void setBottomLeftTopRightBorderModified(boolean modified) {
		setModified(modified, CFRuleBase.bordBlTr);
	}

	public boolean isPatternStyleModified() {
		return isModified(CFRuleBase.pattStyle);
	}

	public void setPatternStyleModified(boolean modified) {
		setModified(modified, CFRuleBase.pattStyle);
	}

	public boolean isPatternColorModified() {
		return isModified(CFRuleBase.pattCol);
	}

	public void setPatternColorModified(boolean modified) {
		setModified(modified, CFRuleBase.pattCol);
	}

	public boolean isPatternBackgroundColorModified() {
		return isModified(CFRuleBase.pattBgCol);
	}

	public void setPatternBackgroundColorModified(boolean modified) {
		setModified(modified, CFRuleBase.pattBgCol);
	}

	private boolean getOptionFlag(BitField field) {
		return field.isSet(formatting_options);
	}

	private void setOptionFlag(boolean flag, BitField field) {
		formatting_options = field.setBoolean(formatting_options, flag);
	}

	protected int getFormattingBlockSize() {
		return ((6 + (containsFontFormattingBlock() ? _fontFormatting.getRawRecord().length : 0)) + (containsBorderFormattingBlock() ? 8 : 0)) + (containsPatternFormattingBlock() ? 4 : 0);
	}

	protected void serializeFormattingBlock(LittleEndianOutput out) {
		out.writeInt(formatting_options);
		out.writeShort(formatting_not_used);
		if (containsFontFormattingBlock()) {
			byte[] fontFormattingRawRecord = _fontFormatting.getRawRecord();
			out.write(fontFormattingRawRecord);
		}
		if (containsBorderFormattingBlock()) {
			_borderFormatting.serialize(out);
		}
		if (containsPatternFormattingBlock()) {
			_patternFormatting.serialize(out);
		}
	}

	public Ptg[] getParsedExpression1() {
		return formula1.getTokens();
	}

	public void setParsedExpression1(Ptg[] ptgs) {
		formula1 = Formula.create(ptgs);
	}

	protected Formula getFormula1() {
		return formula1;
	}

	protected void setFormula1(Formula formula1) {
		this.formula1 = formula1;
	}

	public Ptg[] getParsedExpression2() {
		return Formula.getTokens(formula2);
	}

	public void setParsedExpression2(Ptg[] ptgs) {
		formula2 = Formula.create(ptgs);
	}

	protected Formula getFormula2() {
		return formula2;
	}

	protected void setFormula2(Formula formula2) {
		this.formula2 = formula2;
	}

	protected static int getFormulaSize(Formula formula) {
		return formula.getEncodedTokenSize();
	}

	public static Ptg[] parseFormula(String formula, HSSFSheet sheet) {
		if (formula == null) {
			return null;
		}
		int sheetIndex = sheet.getWorkbook().getSheetIndex(sheet);
		return HSSFFormulaParser.parse(formula, sheet.getWorkbook(), FormulaType.CELL, sheetIndex);
	}

	protected void copyTo(CFRuleBase rec) {
		rec.condition_type = condition_type;
		rec.comparison_operator = comparison_operator;
		rec.formatting_options = formatting_options;
		rec.formatting_not_used = formatting_not_used;
		if (containsFontFormattingBlock()) {
			rec._fontFormatting = _fontFormatting.clone();
		}
		if (containsBorderFormattingBlock()) {
			rec._borderFormatting = _borderFormatting.clone();
		}
		if (containsPatternFormattingBlock()) {
			rec._patternFormatting = ((PatternFormatting) (_patternFormatting.clone()));
		}
		rec.setFormula1(getFormula1().copy());
		rec.setFormula2(getFormula2().copy());
	}

	@Override
	public abstract CFRuleBase clone();
}

