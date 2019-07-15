

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.FieldPosition;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Formatter;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import org.apache.poi.ss.format.CellFormatPart;
import org.apache.poi.ss.format.CellFormatType;
import org.apache.poi.ss.format.CellFormatter;
import org.apache.poi.ss.format.CellNumberPartHandler;
import org.apache.poi.ss.format.CellNumberStringMod;
import org.apache.poi.ss.format.SimpleFraction;
import org.apache.poi.util.LocaleUtil;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;


public class CellNumberFormatter extends CellFormatter {
	private static final POILogger LOG = POILogFactory.getLogger(CellNumberFormatter.class);

	private final String desc;

	private final String printfFmt;

	private final double scale;

	private CellNumberFormatter.Special decimalPoint = null;

	private CellNumberFormatter.Special slash = null;

	private final CellNumberFormatter.Special exponent = null;

	private CellNumberFormatter.Special numerator = null;

	private final CellNumberFormatter.Special afterInteger;

	private final CellNumberFormatter.Special afterFractional;

	private final boolean showGroupingSeparator;

	private final List<CellNumberFormatter.Special> specials = new ArrayList<>();

	private final List<CellNumberFormatter.Special> integerSpecials = new ArrayList<>();

	private final List<CellNumberFormatter.Special> fractionalSpecials = new ArrayList<>();

	private final List<CellNumberFormatter.Special> numeratorSpecials = new ArrayList<>();

	private final List<CellNumberFormatter.Special> denominatorSpecials = new ArrayList<>();

	private final List<CellNumberFormatter.Special> exponentSpecials = new ArrayList<>();

	private final List<CellNumberFormatter.Special> exponentDigitSpecials = new ArrayList<>();

	private final int maxDenominator;

	private final String numeratorFmt;

	private final String denominatorFmt;

	private final boolean improperFraction;

	private final DecimalFormat decimalFmt;

	private final CellFormatter SIMPLE_NUMBER = new CellNumberFormatter.GeneralNumberFormatter(locale);

	private static class GeneralNumberFormatter extends CellFormatter {
		private GeneralNumberFormatter(Locale locale) {
			super(locale, "General");
		}

		public void formatValue(StringBuffer toAppendTo, Object value) {
			if (value == null) {
				return;
			}
			CellFormatter cf;
			if (value instanceof Number) {
				Number num = ((Number) (value));
				cf = (((num.doubleValue()) % 1.0) == 0) ? new CellNumberFormatter(locale, "#") : new CellNumberFormatter(locale, "#.#");
			}else {
			}
			cf = null;
			cf.formatValue(toAppendTo, value);
		}

		public void simpleValue(StringBuffer toAppendTo, Object value) {
			formatValue(toAppendTo, value);
		}
	}

	static class Special {
		final char ch;

		int pos;

		Special(char ch, int pos) {
			this.ch = ch;
			this.pos = pos;
		}

		@Override
		public String toString() {
			return (("'" + (ch)) + "' @ ") + (pos);
		}
	}

	public CellNumberFormatter(String format) {
		this(LocaleUtil.getUserLocale(), format);
	}

	public CellNumberFormatter(Locale locale, String format) {
		super(locale, format);
		CellNumberPartHandler ph = new CellNumberPartHandler();
		StringBuffer descBuf = CellFormatPart.parseFormat(format, CellFormatType.NUMBER, ph);
		improperFraction = ph.isImproperFraction();
		if ((((ph.getDecimalPoint()) != null) || ((ph.getExponent()) != null)) && ((ph.getSlash()) != null)) {
			slash = null;
			numerator = null;
		}else {
		}
		int fractionPartWidth = 0;
		if ((ph.getDecimalPoint()) != null) {
		}else {
			decimalPoint = null;
		}
		if ((decimalPoint) != null) {
			afterInteger = decimalPoint;
		}else
			if ((exponent) != null) {
				afterInteger = exponent;
			}else
				if ((numerator) != null) {
					afterInteger = numerator;
				}else {
					afterInteger = null;
				}


		if ((exponent) != null) {
			afterFractional = exponent;
		}else
			if ((numerator) != null) {
				afterFractional = numerator;
			}else {
				afterFractional = null;
			}

		double[] scaleByRef = new double[]{ ph.getScale() };
		showGroupingSeparator = CellNumberFormatter.interpretIntegerCommas(descBuf, specials, decimalPoint, integerEnd(), fractionalEnd(), scaleByRef);
		if ((exponent) == null) {
			scale = scaleByRef[0];
		}else {
			scale = 1;
		}
		if ((exponent) != null) {
			int exponentPos = specials.indexOf(exponent);
			exponentSpecials.addAll(specialsFor(exponentPos, 2));
			exponentDigitSpecials.addAll(specialsFor((exponentPos + 2)));
		}
		if ((slash) != null) {
			if ((numerator) != null) {
				numeratorSpecials.addAll(specialsFor(specials.indexOf(numerator)));
			}
			denominatorSpecials.addAll(specialsFor(((specials.indexOf(slash)) + 1)));
			if (denominatorSpecials.isEmpty()) {
				numeratorSpecials.clear();
				maxDenominator = 1;
				numeratorFmt = null;
				denominatorFmt = null;
			}else {
				maxDenominator = CellNumberFormatter.maxValue(denominatorSpecials);
				numeratorFmt = CellNumberFormatter.singleNumberFormat(numeratorSpecials);
				denominatorFmt = CellNumberFormatter.singleNumberFormat(denominatorSpecials);
			}
		}else {
			maxDenominator = 1;
			numeratorFmt = null;
			denominatorFmt = null;
		}
		integerSpecials.addAll(specials.subList(0, integerEnd()));
		if ((exponent) == null) {
			StringBuffer fmtBuf = new StringBuffer("%");
			int integerPartWidth = calculateIntegerPartWidth();
			int totalWidth = integerPartWidth + fractionPartWidth;
			fmtBuf.append("f");
			printfFmt = fmtBuf.toString();
			decimalFmt = null;
		}else {
			StringBuffer fmtBuf = new StringBuffer();
			boolean first = true;
			if ((integerSpecials.size()) == 1) {
				fmtBuf.append("0");
				first = false;
			}else
				for (CellNumberFormatter.Special s : integerSpecials) {
					if (CellNumberFormatter.isDigitFmt(s)) {
						fmtBuf.append((first ? '#' : '0'));
						first = false;
					}
				}

			if ((fractionalSpecials.size()) > 0) {
				fmtBuf.append('.');
				for (CellNumberFormatter.Special s : fractionalSpecials) {
					if (CellNumberFormatter.isDigitFmt(s)) {
						if (!first)
							fmtBuf.append('0');

						first = false;
					}
				}
			}
			fmtBuf.append('E');
			CellNumberFormatter.placeZeros(fmtBuf, exponentSpecials.subList(2, exponentSpecials.size()));
			decimalFmt = new DecimalFormat(fmtBuf.toString(), getDecimalFormatSymbols());
			printfFmt = null;
		}
		desc = descBuf.toString();
		slash = null;
	}

	private DecimalFormatSymbols getDecimalFormatSymbols() {
		return DecimalFormatSymbols.getInstance(locale);
	}

	private static void placeZeros(StringBuffer sb, List<CellNumberFormatter.Special> specials) {
		for (CellNumberFormatter.Special s : specials) {
			if (CellNumberFormatter.isDigitFmt(s)) {
				sb.append('0');
			}
		}
	}

	private static CellNumberStringMod insertMod(CellNumberFormatter.Special special, CharSequence toAdd, int where) {
		return null;
	}

	private static CellNumberStringMod deleteMod(CellNumberFormatter.Special start, boolean startInclusive, CellNumberFormatter.Special end, boolean endInclusive) {
		return null;
	}

	private static CellNumberStringMod replaceMod(CellNumberFormatter.Special start, boolean startInclusive, CellNumberFormatter.Special end, boolean endInclusive, char withChar) {
		return null;
	}

	private static String singleNumberFormat(List<CellNumberFormatter.Special> numSpecials) {
		return ("%0" + (numSpecials.size())) + "d";
	}

	private static int maxValue(List<CellNumberFormatter.Special> s) {
		return ((int) (Math.round(((Math.pow(10, s.size())) - 1))));
	}

	private List<CellNumberFormatter.Special> specialsFor(int pos, int takeFirst) {
		if (pos >= (specials.size())) {
			return Collections.emptyList();
		}
		ListIterator<CellNumberFormatter.Special> it = specials.listIterator((pos + takeFirst));
		CellNumberFormatter.Special last = it.next();
		int end = pos + takeFirst;
		while (it.hasNext()) {
			CellNumberFormatter.Special s = it.next();
			if ((!(CellNumberFormatter.isDigitFmt(s))) || (((s.pos) - (last.pos)) > 1))
				break;

			end++;
			last = s;
		} 
		return specials.subList(pos, (end + 1));
	}

	private List<CellNumberFormatter.Special> specialsFor(int pos) {
		return specialsFor(pos, 0);
	}

	private static boolean isDigitFmt(CellNumberFormatter.Special s) {
		return (((s.ch) == '0') || ((s.ch) == '?')) || ((s.ch) == '#');
	}

	private int calculateIntegerPartWidth() {
		int digitCount = 0;
		for (CellNumberFormatter.Special s : specials) {
			if (s == (afterInteger)) {
				break;
			}else
				if (CellNumberFormatter.isDigitFmt(s)) {
					digitCount++;
				}

		}
		return digitCount;
	}

	private static int interpretPrecision(CellNumberFormatter.Special decimalPoint, List<CellNumberFormatter.Special> specials) {
		int idx = specials.indexOf(decimalPoint);
		int precision = 0;
		if (idx != (-1)) {
			ListIterator<CellNumberFormatter.Special> it = specials.listIterator((idx + 1));
			while (it.hasNext()) {
				CellNumberFormatter.Special s = it.next();
				if (!(CellNumberFormatter.isDigitFmt(s))) {
					break;
				}
				precision++;
			} 
		}
		return precision;
	}

	private static boolean interpretIntegerCommas(StringBuffer sb, List<CellNumberFormatter.Special> specials, CellNumberFormatter.Special decimalPoint, int integerEnd, int fractionalEnd, double[] scale) {
		ListIterator<CellNumberFormatter.Special> it = specials.listIterator(integerEnd);
		boolean stillScaling = true;
		boolean integerCommas = false;
		while (it.hasPrevious()) {
			CellNumberFormatter.Special s = it.previous();
			if ((s.ch) != ',') {
				stillScaling = false;
			}else {
				if (stillScaling) {
					scale[0] /= 1000;
				}else {
					integerCommas = true;
				}
			}
		} 
		if (decimalPoint != null) {
			it = specials.listIterator(fractionalEnd);
			while (it.hasPrevious()) {
				CellNumberFormatter.Special s = it.previous();
				if ((s.ch) != ',') {
					break;
				}else {
					scale[0] /= 1000;
				}
			} 
		}
		it = specials.listIterator();
		int removed = 0;
		while (it.hasNext()) {
			CellNumberFormatter.Special s = it.next();
			s.pos -= removed;
			if ((s.ch) == ',') {
				removed++;
				it.remove();
				sb.deleteCharAt(s.pos);
			}
		} 
		return integerCommas;
	}

	private int integerEnd() {
		return (afterInteger) == null ? specials.size() : specials.indexOf(afterInteger);
	}

	private int fractionalEnd() {
		return (afterFractional) == null ? specials.size() : specials.indexOf(afterFractional);
	}

	public void formatValue(StringBuffer toAppendTo, Object valueObject) {
		double value = ((Number) (valueObject)).doubleValue();
		value *= scale;
		boolean negative = value < 0;
		if (negative)
			value = -value;

		double fractional = 0;
		if ((slash) != null) {
			if (improperFraction) {
				fractional = value;
				value = 0;
			}else {
				fractional = value % 1.0;
				value = ((long) (value));
			}
		}
		Set<CellNumberStringMod> mods = new TreeSet<>();
		StringBuffer output = new StringBuffer(localiseFormat(desc));
		if ((exponent) != null) {
			writeScientific(value, output, mods);
		}else
			if (improperFraction) {
				writeFraction(value, null, fractional, output, mods);
			}else {
				StringBuffer result = new StringBuffer();
				Formatter f = new Formatter(result, locale);
				try {
					f.format(locale, printfFmt, value);
				} finally {
					f.close();
				}
				if ((numerator) == null) {
					writeFractional(result, output);
					writeInteger(result, output, integerSpecials, mods, showGroupingSeparator);
				}else {
					writeFraction(value, result, fractional, output, mods);
				}
			}

		DecimalFormatSymbols dfs = getDecimalFormatSymbols();
		String groupingSeparator = Character.toString(dfs.getGroupingSeparator());
		Iterator<CellNumberStringMod> changes = mods.iterator();
		CellNumberStringMod nextChange = (changes.hasNext()) ? changes.next() : null;
		BitSet deletedChars = new BitSet();
		int adjust = 0;
		for (CellNumberFormatter.Special s : specials) {
			int adjustedPos = (s.pos) + adjust;
			if ((!(deletedChars.get(s.pos))) && ((output.charAt(adjustedPos)) == '#')) {
				output.deleteCharAt(adjustedPos);
				adjust--;
				deletedChars.set(s.pos);
			}
		}
		if (negative) {
			toAppendTo.append('-');
		}
		toAppendTo.append(output);
	}

	private void writeScientific(double value, StringBuffer output, Set<CellNumberStringMod> mods) {
		StringBuffer result = new StringBuffer();
		FieldPosition fractionPos = new FieldPosition(DecimalFormat.FRACTION_FIELD);
		decimalFmt.format(value, result, fractionPos);
		writeInteger(result, output, integerSpecials, mods, showGroupingSeparator);
		writeFractional(result, output);
		int ePos = fractionPos.getEndIndex();
		int signPos = ePos + 1;
		char expSignRes = result.charAt(signPos);
		if (expSignRes != '-') {
			expSignRes = '+';
			result.insert(signPos, '+');
		}
		ListIterator<CellNumberFormatter.Special> it = exponentSpecials.listIterator(1);
		CellNumberFormatter.Special expSign = it.next();
		char expSignFmt = expSign.ch;
		if ((expSignRes == '-') || (expSignFmt == '+')) {
			mods.add(CellNumberFormatter.replaceMod(expSign, true, expSign, true, expSignRes));
		}else {
			mods.add(CellNumberFormatter.deleteMod(expSign, true, expSign, true));
		}
		StringBuffer exponentNum = new StringBuffer(result.substring((signPos + 1)));
		writeInteger(exponentNum, output, exponentDigitSpecials, mods, false);
	}

	@SuppressWarnings("unchecked")
	private void writeFraction(double value, StringBuffer result, double fractional, StringBuffer output, Set<CellNumberStringMod> mods) {
		if (!(improperFraction)) {
			if ((fractional == 0) && (!(CellNumberFormatter.hasChar('0', numeratorSpecials)))) {
				writeInteger(result, output, integerSpecials, mods, false);
				CellNumberFormatter.Special start = CellNumberFormatter.lastSpecial(integerSpecials);
				CellNumberFormatter.Special end = CellNumberFormatter.lastSpecial(denominatorSpecials);
				if (CellNumberFormatter.hasChar('?', integerSpecials, numeratorSpecials, denominatorSpecials)) {
					mods.add(CellNumberFormatter.replaceMod(start, false, end, true, ' '));
				}else {
					mods.add(CellNumberFormatter.deleteMod(start, false, end, true));
				}
				return;
			}else {
				boolean numNoZero = !(CellNumberFormatter.hasChar('0', numeratorSpecials));
				boolean intNoZero = !(CellNumberFormatter.hasChar('0', integerSpecials));
				boolean intOnlyHash = (integerSpecials.isEmpty()) || (((integerSpecials.size()) == 1) && (CellNumberFormatter.hasChar('#', integerSpecials)));
				boolean removeBecauseZero = (fractional == 0) && (intOnlyHash || numNoZero);
				boolean removeBecauseFraction = (fractional != 0) && intNoZero;
				if ((value == 0) && (removeBecauseZero || removeBecauseFraction)) {
					CellNumberFormatter.Special start = CellNumberFormatter.lastSpecial(integerSpecials);
					boolean hasPlaceHolder = CellNumberFormatter.hasChar('?', integerSpecials, numeratorSpecials);
					CellNumberStringMod sm = (hasPlaceHolder) ? CellNumberFormatter.replaceMod(start, true, numerator, false, ' ') : CellNumberFormatter.deleteMod(start, true, numerator, false);
					mods.add(sm);
				}else {
					writeInteger(result, output, integerSpecials, mods, false);
				}
			}
		}
		try {
			int n;
			int d;
			if ((fractional == 0) || ((improperFraction) && ((fractional % 1) == 0))) {
				n = ((int) (Math.round(fractional)));
				d = 1;
			}else {
				SimpleFraction frac = SimpleFraction.buildFractionMaxDenominator(fractional, maxDenominator);
				n = frac.getNumerator();
				d = frac.getDenominator();
			}
			if (improperFraction) {
				n += Math.round((value * d));
			}
			writeSingleInteger(numeratorFmt, n, output, numeratorSpecials, mods);
			writeSingleInteger(denominatorFmt, d, output, denominatorSpecials, mods);
		} catch (RuntimeException ignored) {
			CellNumberFormatter.LOG.log(POILogger.ERROR, "error while fraction evaluation", ignored);
		}
	}

	private String localiseFormat(String format) {
		DecimalFormatSymbols dfs = getDecimalFormatSymbols();
		if ((format.contains(",")) && ((dfs.getGroupingSeparator()) != ',')) {
			if ((format.contains(".")) && ((dfs.getDecimalSeparator()) != '.')) {
				format = CellNumberFormatter.replaceLast(format, "\\.", "[DECIMAL_SEPARATOR]");
				format = format.replace(',', dfs.getGroupingSeparator()).replace("[DECIMAL_SEPARATOR]", Character.toString(dfs.getDecimalSeparator()));
			}else {
				format = format.replace(',', dfs.getGroupingSeparator());
			}
		}else
			if ((format.contains(".")) && ((dfs.getDecimalSeparator()) != '.')) {
				format = format.replace('.', dfs.getDecimalSeparator());
			}

		return format;
	}

	private static String replaceLast(String text, String regex, String replacement) {
		return text.replaceFirst(("(?s)(.*)" + regex), ("$1" + replacement));
	}

	private static boolean hasChar(char ch, List<CellNumberFormatter.Special>... numSpecials) {
		for (List<CellNumberFormatter.Special> specials : numSpecials) {
			for (CellNumberFormatter.Special s : specials) {
				if ((s.ch) == ch) {
					return true;
				}
			}
		}
		return false;
	}

	private void writeSingleInteger(String fmt, int num, StringBuffer output, List<CellNumberFormatter.Special> numSpecials, Set<CellNumberStringMod> mods) {
		StringBuffer sb = new StringBuffer();
		Formatter formatter = new Formatter(sb, locale);
		try {
			formatter.format(locale, fmt, num);
		} finally {
			formatter.close();
		}
		writeInteger(sb, output, numSpecials, mods, false);
	}

	private void writeInteger(StringBuffer result, StringBuffer output, List<CellNumberFormatter.Special> numSpecials, Set<CellNumberStringMod> mods, boolean showGroupingSeparator) {
		DecimalFormatSymbols dfs = getDecimalFormatSymbols();
		String decimalSeparator = Character.toString(dfs.getDecimalSeparator());
		String groupingSeparator = Character.toString(dfs.getGroupingSeparator());
		int pos = (result.indexOf(decimalSeparator)) - 1;
		if (pos < 0) {
			if (((exponent) != null) && (numSpecials == (integerSpecials))) {
				pos = (result.indexOf("E")) - 1;
			}else {
				pos = (result.length()) - 1;
			}
		}
		int strip;
		for (strip = 0; strip < pos; strip++) {
			char resultCh = result.charAt(strip);
			if ((resultCh != '0') && (resultCh != (dfs.getGroupingSeparator()))) {
				break;
			}
		}
		ListIterator<CellNumberFormatter.Special> it = numSpecials.listIterator(numSpecials.size());
		boolean followWithGroupingSeparator = false;
		CellNumberFormatter.Special lastOutputIntegerDigit = null;
		int digit = 0;
		while (it.hasPrevious()) {
			char resultCh;
			if (pos >= 0) {
				resultCh = result.charAt(pos);
			}else {
				resultCh = '0';
			}
			CellNumberFormatter.Special s = it.previous();
			followWithGroupingSeparator = (showGroupingSeparator && (digit > 0)) && ((digit % 3) == 0);
			boolean zeroStrip = false;
			if ((((resultCh != '0') || ((s.ch) == '0')) || ((s.ch) == '?')) || (pos >= strip)) {
				zeroStrip = ((s.ch) == '?') && (pos < strip);
				output.setCharAt(s.pos, (zeroStrip ? ' ' : resultCh));
				lastOutputIntegerDigit = s;
			}
			if (followWithGroupingSeparator) {
				mods.add(CellNumberFormatter.insertMod(s, (zeroStrip ? " " : groupingSeparator), CellNumberStringMod.AFTER));
				followWithGroupingSeparator = false;
			}
			digit++;
			--pos;
		} 
		StringBuffer extraLeadingDigits = new StringBuffer();
		if (pos >= 0) {
			++pos;
			extraLeadingDigits = new StringBuffer(result.substring(0, pos));
			if (showGroupingSeparator) {
				while (pos > 0) {
					if ((digit > 0) && ((digit % 3) == 0)) {
						extraLeadingDigits.insert(pos, groupingSeparator);
					}
					digit++;
					--pos;
				} 
			}
			mods.add(CellNumberFormatter.insertMod(lastOutputIntegerDigit, extraLeadingDigits, CellNumberStringMod.BEFORE));
		}
	}

	private void writeFractional(StringBuffer result, StringBuffer output) {
		int digit;
		int strip;
		if ((fractionalSpecials.size()) > 0) {
			String decimalSeparator = Character.toString(getDecimalFormatSymbols().getDecimalSeparator());
			digit = (result.indexOf(decimalSeparator)) + 1;
			if ((exponent) != null) {
				strip = (result.indexOf("e")) - 1;
			}else {
				strip = (result.length()) - 1;
			}
			while ((strip > digit) && ((result.charAt(strip)) == '0')) {
				strip--;
			} 
			for (CellNumberFormatter.Special s : fractionalSpecials) {
				char resultCh = result.charAt(digit);
				if (((resultCh != '0') || ((s.ch) == '0')) || (digit < strip)) {
					output.setCharAt(s.pos, resultCh);
				}else
					if ((s.ch) == '?') {
						output.setCharAt(s.pos, ' ');
					}

				digit++;
			}
		}
	}

	public void simpleValue(StringBuffer toAppendTo, Object value) {
		SIMPLE_NUMBER.formatValue(toAppendTo, value);
	}

	private static CellNumberFormatter.Special lastSpecial(List<CellNumberFormatter.Special> s) {
		return s.get(((s.size()) - 1));
	}
}

