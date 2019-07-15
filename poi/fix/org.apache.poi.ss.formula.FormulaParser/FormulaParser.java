

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.formula.FormulaParsingWorkbook;
import org.apache.poi.ss.formula.FormulaType;
import org.apache.poi.ss.formula.NameIdentifier;
import org.apache.poi.ss.formula.SheetIdentifier;
import org.apache.poi.ss.formula.SheetRangeIdentifier;
import org.apache.poi.ss.formula.constant.ErrorConstant;
import org.apache.poi.ss.formula.function.FunctionMetadata;
import org.apache.poi.ss.formula.function.FunctionMetadataRegistry;
import org.apache.poi.ss.formula.ptg.Area3DPxg;
import org.apache.poi.ss.formula.ptg.EqualPtg;
import org.apache.poi.ss.formula.ptg.GreaterEqualPtg;
import org.apache.poi.ss.formula.ptg.GreaterThanPtg;
import org.apache.poi.ss.formula.ptg.IntPtg;
import org.apache.poi.ss.formula.ptg.LessEqualPtg;
import org.apache.poi.ss.formula.ptg.LessThanPtg;
import org.apache.poi.ss.formula.ptg.NotEqualPtg;
import org.apache.poi.ss.formula.ptg.NumberPtg;
import org.apache.poi.ss.formula.ptg.Ptg;
import org.apache.poi.ss.formula.ptg.ValueOperatorPtg;
import org.apache.poi.ss.usermodel.FormulaError;
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.util.Internal;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;

import static org.apache.poi.ss.util.CellReference.NameType.CELL;


@Internal
public final class FormulaParser {
	private static final POILogger log = POILogFactory.getLogger(FormulaParser.class);

	private final String _formulaString;

	private final int _formulaLength;

	private int _pointer;

	private static final char TAB = '\t';

	private static final char CR = '\r';

	private static final char LF = '\n';

	private int look;

	private boolean _inIntersection;

	private final FormulaParsingWorkbook _book;

	private final SpreadsheetVersion _ssVersion;

	private final int _sheetIndex;

	private final int _rowIndex;

	private FormulaParser(String formula, FormulaParsingWorkbook book, int sheetIndex, int rowIndex) {
		_formulaString = formula;
		_pointer = 0;
		_book = book;
		_ssVersion = (book == null) ? SpreadsheetVersion.EXCEL97 : book.getSpreadsheetVersion();
		_formulaLength = _formulaString.length();
		_sheetIndex = sheetIndex;
		_rowIndex = rowIndex;
	}

	public static Ptg[] parse(String formula, FormulaParsingWorkbook workbook, FormulaType formulaType, int sheetIndex, int rowIndex) {
		FormulaParser fp = new FormulaParser(formula, workbook, sheetIndex, rowIndex);
		fp.parse();
		return fp.getRPNPtg(formulaType);
	}

	public static Ptg[] parse(String formula, FormulaParsingWorkbook workbook, FormulaType formulaType, int sheetIndex) {
		return FormulaParser.parse(formula, workbook, formulaType, sheetIndex, (-1));
	}

	public static Area3DPxg parseStructuredReference(String tableText, FormulaParsingWorkbook workbook, int rowIndex) {
		final int sheetIndex = -1;
		Ptg[] arr = FormulaParser.parse(tableText, workbook, FormulaType.CELL, sheetIndex, rowIndex);
		if (((arr.length) != 1) || (!((arr[0]) instanceof Area3DPxg))) {
			throw new IllegalStateException("Illegal structured reference");
		}
		return ((Area3DPxg) (arr[0]));
	}

	private void GetChar() {
		if (FormulaParser.IsWhite(look)) {
			if ((look) == ' ') {
				_inIntersection = true;
			}
		}else {
			_inIntersection = false;
		}
		if ((_pointer) > (_formulaLength)) {
			throw new RuntimeException("too far");
		}
		if ((_pointer) < (_formulaLength)) {
			look = _formulaString.codePointAt(_pointer);
		}else {
			look = ((char) (0));
			_inIntersection = false;
		}
		_pointer += Character.charCount(look);
	}

	private void resetPointer(int ptr) {
		_pointer = ptr;
		if ((_pointer) <= (_formulaLength)) {
			look = _formulaString.codePointAt(((_pointer) - (Character.charCount(look))));
		}else {
			look = ((char) (0));
		}
	}

	private RuntimeException expected(String s) {
		String msg;
		if (((look) == '=') && ((_formulaString.substring(0, ((_pointer) - 1)).trim().length()) < 1)) {
			msg = ("The specified formula '" + (_formulaString)) + "' starts with an equals sign which is not allowed.";
		}else {
			msg = new StringBuilder("Parse error near char ").append(((_pointer) - 1)).append(" '").appendCodePoint(look).append("'").append(" in specified formula '").append(_formulaString).append("'. Expected ").append(s).toString();
		}
		return null;
	}

	private static boolean IsAlpha(int c) {
		return ((Character.isLetter(c)) || (c == '$')) || (c == '_');
	}

	private static boolean IsDigit(int c) {
		return Character.isDigit(c);
	}

	private static boolean IsWhite(int c) {
		return (((c == ' ') || (c == (FormulaParser.TAB))) || (c == (FormulaParser.CR))) || (c == (FormulaParser.LF));
	}

	private void SkipWhite() {
		while (FormulaParser.IsWhite(look)) {
			GetChar();
		} 
	}

	private void Match(int x) {
		if ((look) != x) {
			throw expected(new StringBuilder().append("'").appendCodePoint(x).append("'").toString());
		}
		GetChar();
	}

	private String GetNum() {
		StringBuilder value = new StringBuilder();
		while (FormulaParser.IsDigit(this.look)) {
			value.appendCodePoint(this.look);
			GetChar();
		} 
		return (value.length()) == 0 ? null : value.toString();
	}

	private static final String specHeaders = "Headers";

	private static final String specAll = "All";

	private static final String specData = "Data";

	private static final String specTotals = "Totals";

	private static final String specThisRow = "This Row";

	private String parseAsColumnQuantifier() {
		if ((look) != '[') {
			return null;
		}
		GetChar();
		if ((look) == '#') {
			return null;
		}
		if ((look) == '@') {
			GetChar();
		}
		StringBuilder name = new StringBuilder();
		while ((look) != ']') {
			name.appendCodePoint(look);
			GetChar();
		} 
		Match(']');
		return name.toString();
	}

	private String parseAsSpecialQuantifier() {
		if ((look) != '[') {
			return null;
		}
		GetChar();
		if ((look) != '#') {
			return null;
		}
		GetChar();
		String name = parseAsName();
		if (name.equals("This")) {
			name = (name + ' ') + (parseAsName());
		}
		Match(']');
		return name;
	}

	private String parseAsName() {
		StringBuilder sb = new StringBuilder();
		if (((!(Character.isLetter(look))) && ((look) != '_')) && ((look) != '\\')) {
			throw expected("number, string, defined name, or data table");
		}
		while (FormulaParser.isValidDefinedNameChar(look)) {
			sb.appendCodePoint(look);
			GetChar();
		} 
		SkipWhite();
		return sb.toString();
	}

	private static boolean isValidDefinedNameChar(int ch) {
		if (Character.isLetterOrDigit(ch)) {
			return true;
		}
		if (ch > 128) {
			return true;
		}
		switch (ch) {
			case '.' :
			case '_' :
			case '?' :
			case '\\' :
				return true;
		}
		return false;
	}

	private AreaReference createAreaRef(FormulaParser.SimpleRangePart part1, FormulaParser.SimpleRangePart part2) {
		if (!(part1.isCompatibleForArea(part2))) {
		}
		if (part1.isRow()) {
			return AreaReference.getWholeRow(_ssVersion, part1.getRep(), part2.getRep());
		}
		if (part1.isColumn()) {
			return AreaReference.getWholeColumn(_ssVersion, part1.getRep(), part2.getRep());
		}
		return new AreaReference(part1.getCellReference(), part2.getCellReference(), _ssVersion);
	}

	private static final Pattern CELL_REF_PATTERN = Pattern.compile("(\\$?[A-Za-z]+)?(\\$?[0-9]+)?");

	private FormulaParser.SimpleRangePart parseSimpleRangePart() {
		int ptr = (_pointer) - 1;
		boolean hasDigits = false;
		boolean hasLetters = false;
		while (ptr < (_formulaLength)) {
			char ch = _formulaString.charAt(ptr);
			if (Character.isDigit(ch)) {
				hasDigits = true;
			}else
				if (Character.isLetter(ch)) {
					hasLetters = true;
				}else
					if ((ch == '$') || (ch == '_')) {
					}else {
						break;
					}


			ptr++;
		} 
		if (ptr <= ((_pointer) - 1)) {
			return null;
		}
		String rep = _formulaString.substring(((_pointer) - 1), ptr);
		if (!(FormulaParser.CELL_REF_PATTERN.matcher(rep).matches())) {
			return null;
		}
		if (hasLetters && hasDigits) {
			if (!(isValidCellReference(rep))) {
				return null;
			}
		}else
			if (hasLetters) {
				if (!(CellReference.isColumnWithinRange(rep.replace("$", ""), _ssVersion))) {
					return null;
				}
			}else
				if (hasDigits) {
					int i;
					try {
						i = Integer.parseInt(rep.replace("$", ""));
					} catch (NumberFormatException e) {
						return null;
					}
					if ((i < 1) || (i > (_ssVersion.getMaxRows()))) {
						return null;
					}
				}else {
					return null;
				}


		resetPointer((ptr + 1));
		return new FormulaParser.SimpleRangePart(rep, hasLetters, hasDigits);
	}

	private static final class SimpleRangePart {
		private enum Type {

			CELL,
			ROW,
			COLUMN;
			public static FormulaParser.SimpleRangePart.Type get(boolean hasLetters, boolean hasDigits) {
				if (hasLetters) {
					return hasDigits ? FormulaParser.SimpleRangePart.Type.CELL : FormulaParser.SimpleRangePart.Type.COLUMN;
				}
				if (!hasDigits) {
					throw new IllegalArgumentException("must have either letters or numbers");
				}
				return FormulaParser.SimpleRangePart.Type.ROW;
			}
		}

		private final FormulaParser.SimpleRangePart.Type _type;

		private final String _rep;

		public SimpleRangePart(String rep, boolean hasLetters, boolean hasNumbers) {
			_rep = rep;
			_type = FormulaParser.SimpleRangePart.Type.get(hasLetters, hasNumbers);
		}

		public boolean isCell() {
			return (_type) == (FormulaParser.SimpleRangePart.Type.CELL);
		}

		public boolean isRowOrColumn() {
			return (_type) != (FormulaParser.SimpleRangePart.Type.CELL);
		}

		public CellReference getCellReference() {
			if ((_type) != (FormulaParser.SimpleRangePart.Type.CELL)) {
				throw new IllegalStateException("Not applicable to this type");
			}
			return new CellReference(_rep);
		}

		public boolean isColumn() {
			return (_type) == (FormulaParser.SimpleRangePart.Type.COLUMN);
		}

		public boolean isRow() {
			return (_type) == (FormulaParser.SimpleRangePart.Type.ROW);
		}

		public String getRep() {
			return _rep;
		}

		public boolean isCompatibleForArea(FormulaParser.SimpleRangePart part2) {
			return (_type) == (part2._type);
		}

		@Override
		public String toString() {
			return (((getClass().getName()) + " [") + (_rep)) + "]";
		}
	}

	private String getBookName() {
		StringBuilder sb = new StringBuilder();
		GetChar();
		while ((look) != ']') {
			sb.appendCodePoint(look);
			GetChar();
		} 
		GetChar();
		return sb.toString();
	}

	private SheetIdentifier parseSheetName() {
		String bookName;
		if ((look) == '[') {
			bookName = getBookName();
		}else {
			bookName = null;
		}
		if ((look) == '\'') {
			Match('\'');
			if ((look) == '[')
				bookName = getBookName();

			StringBuilder sb = new StringBuilder();
			boolean done = (look) == '\'';
			while (!done) {
				sb.appendCodePoint(look);
				GetChar();
				if ((look) == '\'') {
					Match('\'');
					done = (look) != '\'';
				}
			} 
			NameIdentifier iden = new NameIdentifier(sb.toString(), true);
			SkipWhite();
			if ((look) == '!') {
				GetChar();
				return new SheetIdentifier(bookName, iden);
			}
			if ((look) == ':') {
				return parseSheetRange(bookName, iden);
			}
			return null;
		}
		if (((look) == '_') || (Character.isLetter(look))) {
			StringBuilder sb = new StringBuilder();
			while (FormulaParser.isUnquotedSheetNameChar(look)) {
				sb.appendCodePoint(look);
				GetChar();
			} 
			NameIdentifier iden = new NameIdentifier(sb.toString(), false);
			SkipWhite();
			if ((look) == '!') {
				GetChar();
				return new SheetIdentifier(bookName, iden);
			}
			if ((look) == ':') {
				return parseSheetRange(bookName, iden);
			}
			return null;
		}
		if (((look) == '!') && (bookName != null)) {
			GetChar();
			return new SheetIdentifier(bookName, null);
		}
		return null;
	}

	private SheetIdentifier parseSheetRange(String bookname, NameIdentifier sheet1Name) {
		GetChar();
		SheetIdentifier sheet2 = parseSheetName();
		if (sheet2 != null) {
			return new SheetRangeIdentifier(bookname, sheet1Name, sheet2.getSheetIdentifier());
		}
		return null;
	}

	private static boolean isUnquotedSheetNameChar(int ch) {
		if (Character.isLetterOrDigit(ch)) {
			return true;
		}
		if (ch > 128) {
			return true;
		}
		switch (ch) {
			case '.' :
			case '_' :
				return true;
		}
		return false;
	}

	private boolean isValidCellReference(String str) {
		boolean result = (CellReference.classifyCellReference(str, _ssVersion)) == (CELL);
		if (result) {
			boolean isFunc = (FunctionMetadataRegistry.getFunctionByName(str.toUpperCase(Locale.ROOT))) != null;
			if (isFunc) {
				int savePointer = _pointer;
				resetPointer(((_pointer) + (str.length())));
				SkipWhite();
				result = (look) != '(';
				resetPointer(savePointer);
			}
		}
		return result;
	}

	private void addName(String functionName) {
		final Name name = _book.createName();
		name.setFunction(true);
		name.setNameName(functionName);
		name.setSheetIndex(_sheetIndex);
	}

	private void validateNumArgs(int numArgs, FunctionMetadata fm) {
		if (numArgs < (fm.getMinParams())) {
			String msg = ("Too few arguments to function '" + (fm.getName())) + "'. ";
			if (fm.hasFixedArgsLength()) {
				msg += "Expected " + (fm.getMinParams());
			}else {
				msg += ("At least " + (fm.getMinParams())) + " were expected";
			}
			msg += (" but got " + numArgs) + ".";
		}
		int maxArgs;
		if (fm.hasUnlimitedVarags()) {
			if ((_book) != null) {
				maxArgs = _book.getSpreadsheetVersion().getMaxFunctionArgs();
			}else {
				maxArgs = fm.getMaxParams();
			}
		}else {
			maxArgs = fm.getMaxParams();
		}
		if (numArgs > maxArgs) {
			String msg = ("Too many arguments to function '" + (fm.getName())) + "'. ";
			if (fm.hasFixedArgsLength()) {
				msg += "Expected " + maxArgs;
			}else {
				msg += ("At most " + maxArgs) + " were expected";
			}
			msg += (" but got " + numArgs) + ".";
		}
	}

	private static boolean isArgumentDelimiter(int ch) {
		return (ch == ',') || (ch == ')');
	}

	private void checkRowLengths(Object[][] values2d, int nColumns) {
		for (int i = 0; i < (values2d.length); i++) {
			int rowLen = values2d[i].length;
			if (rowLen != nColumns) {
			}
		}
	}

	private Object[] parseArrayRow() {
		List<Object> temp = new ArrayList<>();
		while (true) {
			temp.add(parseArrayItem());
			SkipWhite();
			switch (look) {
				case '}' :
				case ';' :
					break;
				case ',' :
					Match(',');
					continue;
				default :
					throw expected("'}' or ','");
			}
			break;
		} 
		Object[] result = new Object[temp.size()];
		temp.toArray(result);
		return result;
	}

	private Object parseArrayItem() {
		SkipWhite();
		switch (look) {
			case '"' :
				return parseStringLiteral();
			case '#' :
				return ErrorConstant.valueOf(parseErrorLiteral());
			case 'F' :
			case 'f' :
			case 'T' :
			case 't' :
				return parseBooleanLiteral();
			case '-' :
				Match('-');
				SkipWhite();
				return FormulaParser.convertArrayNumber(parseNumber(), false);
		}
		return FormulaParser.convertArrayNumber(parseNumber(), true);
	}

	private Boolean parseBooleanLiteral() {
		String iden = parseUnquotedIdentifier();
		if ("TRUE".equalsIgnoreCase(iden)) {
			return Boolean.TRUE;
		}
		if ("FALSE".equalsIgnoreCase(iden)) {
			return Boolean.FALSE;
		}
		throw expected("'TRUE' or 'FALSE'");
	}

	private static Double convertArrayNumber(Ptg ptg, boolean isPositive) {
		double value;
		if (ptg instanceof IntPtg) {
			value = ((IntPtg) (ptg)).getValue();
		}else
			if (ptg instanceof NumberPtg) {
				value = ((NumberPtg) (ptg)).getValue();
			}else {
				throw new RuntimeException((("Unexpected ptg (" + (ptg.getClass().getName())) + ")"));
			}

		if (!isPositive) {
			value = -value;
		}
		return Double.valueOf(value);
	}

	private Ptg parseNumber() {
		String number2 = null;
		String exponent = null;
		String number1 = GetNum();
		if ((look) == '.') {
			GetChar();
			number2 = GetNum();
		}
		if ((look) == 'E') {
			GetChar();
			String sign = "";
			if ((look) == '+') {
				GetChar();
			}else
				if ((look) == '-') {
					GetChar();
					sign = "-";
				}

			String number = GetNum();
			if (number == null) {
				throw expected("Integer");
			}
			exponent = sign + number;
		}
		if ((number1 == null) && (number2 == null)) {
			throw expected("Integer");
		}
		return FormulaParser.getNumberPtgFromString(number1, number2, exponent);
	}

	private int parseErrorLiteral() {
		Match('#');
		String part1 = parseUnquotedIdentifier();
		if (part1 == null) {
			throw expected("remainder of error constant literal");
		}
		part1 = part1.toUpperCase(Locale.ROOT);
		switch (part1.charAt(0)) {
			case 'V' :
				{
					FormulaError fe = FormulaError.VALUE;
					if (part1.equals(fe.name())) {
						Match('!');
						return fe.getCode();
					}
					throw expected(fe.getString());
				}
			case 'R' :
				{
					FormulaError fe = FormulaError.REF;
					if (part1.equals(fe.name())) {
						Match('!');
						return fe.getCode();
					}
					throw expected(fe.getString());
				}
			case 'D' :
				{
					FormulaError fe = FormulaError.DIV0;
					if (part1.equals("DIV")) {
						Match('/');
						Match('0');
						Match('!');
						return fe.getCode();
					}
					throw expected(fe.getString());
				}
			case 'N' :
				{
					FormulaError fe = FormulaError.NAME;
					if (part1.equals(fe.name())) {
						Match('?');
						return fe.getCode();
					}
					fe = FormulaError.NUM;
					if (part1.equals(fe.name())) {
						Match('!');
						return fe.getCode();
					}
					fe = FormulaError.NULL;
					if (part1.equals(fe.name())) {
						Match('!');
						return fe.getCode();
					}
					fe = FormulaError.NA;
					if (part1.equals("N")) {
						Match('/');
						if (((look) != 'A') && ((look) != 'a')) {
							throw expected(fe.getString());
						}
						Match(look);
						return fe.getCode();
					}
					throw expected("#NAME?, #NUM!, #NULL! or #N/A");
				}
		}
		throw expected("#VALUE!, #REF!, #DIV/0!, #NAME?, #NUM!, #NULL! or #N/A");
	}

	private String parseUnquotedIdentifier() {
		if ((look) == '\'') {
			throw expected("unquoted identifier");
		}
		StringBuilder sb = new StringBuilder();
		while ((Character.isLetterOrDigit(look)) || ((look) == '.')) {
			sb.appendCodePoint(look);
			GetChar();
		} 
		if ((sb.length()) < 1) {
			return null;
		}
		return sb.toString();
	}

	private static Ptg getNumberPtgFromString(String number1, String number2, String exponent) {
		StringBuilder number = new StringBuilder();
		if (number2 == null) {
			number.append(number1);
			if (exponent != null) {
				number.append('E');
				number.append(exponent);
			}
			String numberStr = number.toString();
			int intVal;
			try {
				intVal = Integer.parseInt(numberStr);
			} catch (NumberFormatException e) {
				return new NumberPtg(numberStr);
			}
			if (IntPtg.isInRange(intVal)) {
				return new IntPtg(intVal);
			}
			return new NumberPtg(numberStr);
		}
		if (number1 != null) {
			number.append(number1);
		}
		number.append('.');
		number.append(number2);
		if (exponent != null) {
			number.append('E');
			number.append(exponent);
		}
		return new NumberPtg(number.toString());
	}

	private String parseStringLiteral() {
		Match('"');
		StringBuilder token = new StringBuilder();
		while (true) {
			if ((look) == '"') {
				GetChar();
				if ((look) != '"') {
					break;
				}
			}
			token.appendCodePoint(look);
			GetChar();
		} 
		return token.toString();
	}

	private Ptg getComparisonToken() {
		if ((look) == '=') {
			Match(look);
			return EqualPtg.instance;
		}
		boolean isGreater = (look) == '>';
		Match(look);
		if (isGreater) {
			if ((look) == '=') {
				Match('=');
				return GreaterEqualPtg.instance;
			}
			return GreaterThanPtg.instance;
		}
		switch (look) {
			case '=' :
				Match('=');
				return LessEqualPtg.instance;
			case '>' :
				Match('>');
				return NotEqualPtg.instance;
		}
		return LessThanPtg.instance;
	}

	private void parse() {
		_pointer = 0;
		GetChar();
		if ((_pointer) <= (_formulaLength)) {
			String msg = ((("Unused input [" + (_formulaString.substring(((_pointer) - 1)))) + "] after attempting to parse the formula [") + (_formulaString)) + "]";
		}
	}

	private Ptg[] getRPNPtg(FormulaType formulaType) {
		return null;
	}
}

