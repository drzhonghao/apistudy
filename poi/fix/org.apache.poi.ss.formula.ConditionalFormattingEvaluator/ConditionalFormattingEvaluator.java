

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.formula.EvaluationConditionalFormatRule;
import org.apache.poi.ss.formula.WorkbookEvaluator;
import org.apache.poi.ss.formula.WorkbookEvaluatorProvider;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.ConditionalFormatting;
import org.apache.poi.ss.usermodel.ConditionalFormattingRule;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.SheetConditionalFormatting;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressBase;
import org.apache.poi.ss.util.CellReference;


public class ConditionalFormattingEvaluator {
	private final WorkbookEvaluator workbookEvaluator;

	private final Workbook workbook;

	private final Map<String, List<EvaluationConditionalFormatRule>> formats = new HashMap<>();

	private final Map<CellReference, List<EvaluationConditionalFormatRule>> values = new HashMap<>();

	public ConditionalFormattingEvaluator(Workbook wb, WorkbookEvaluatorProvider provider) {
		this.workbook = wb;
		this.workbookEvaluator = provider._getWorkbookEvaluator();
	}

	protected WorkbookEvaluator getWorkbookEvaluator() {
		return workbookEvaluator;
	}

	public void clearAllCachedFormats() {
		formats.clear();
	}

	public void clearAllCachedValues() {
		values.clear();
	}

	protected List<EvaluationConditionalFormatRule> getRules(Sheet sheet) {
		final String sheetName = sheet.getSheetName();
		List<EvaluationConditionalFormatRule> rules = formats.get(sheetName);
		if (rules == null) {
			if (formats.containsKey(sheetName)) {
				return Collections.emptyList();
			}
			final SheetConditionalFormatting scf = sheet.getSheetConditionalFormatting();
			final int count = scf.getNumConditionalFormattings();
			rules = new ArrayList<>(count);
			formats.put(sheetName, rules);
			for (int i = 0; i < count; i++) {
				ConditionalFormatting f = scf.getConditionalFormattingAt(i);
				final CellRangeAddress[] regions = f.getFormattingRanges();
				for (int r = 0; r < (f.getNumberOfRules()); r++) {
					ConditionalFormattingRule rule = f.getRule(r);
					rules.add(new EvaluationConditionalFormatRule(workbookEvaluator, sheet, f, i, rule, r, regions));
				}
			}
			Collections.sort(rules);
		}
		return Collections.unmodifiableList(rules);
	}

	public List<EvaluationConditionalFormatRule> getConditionalFormattingForCell(final CellReference cellRef) {
		List<EvaluationConditionalFormatRule> rules = values.get(cellRef);
		if (rules == null) {
			rules = new ArrayList<>();
			final Sheet sheet;
			if ((cellRef.getSheetName()) != null) {
				sheet = workbook.getSheet(cellRef.getSheetName());
			}else {
				sheet = workbook.getSheetAt(workbook.getActiveSheetIndex());
			}
			boolean stopIfTrue = false;
			for (EvaluationConditionalFormatRule rule : getRules(sheet)) {
				if (stopIfTrue) {
					continue;
				}
			}
			Collections.sort(rules);
			values.put(cellRef, rules);
		}
		return Collections.unmodifiableList(rules);
	}

	public List<EvaluationConditionalFormatRule> getConditionalFormattingForCell(Cell cell) {
		return getConditionalFormattingForCell(ConditionalFormattingEvaluator.getRef(cell));
	}

	public static CellReference getRef(Cell cell) {
		return new CellReference(cell.getSheet().getSheetName(), cell.getRowIndex(), cell.getColumnIndex(), false, false);
	}

	public List<EvaluationConditionalFormatRule> getFormatRulesForSheet(String sheetName) {
		return getFormatRulesForSheet(workbook.getSheet(sheetName));
	}

	public List<EvaluationConditionalFormatRule> getFormatRulesForSheet(Sheet sheet) {
		return getRules(sheet);
	}

	public List<Cell> getMatchingCells(Sheet sheet, int conditionalFormattingIndex, int ruleIndex) {
		for (EvaluationConditionalFormatRule rule : getRules(sheet)) {
			if (((rule.getSheet().equals(sheet)) && ((rule.getFormattingIndex()) == conditionalFormattingIndex)) && ((rule.getRuleIndex()) == ruleIndex)) {
				return getMatchingCells(rule);
			}
		}
		return Collections.emptyList();
	}

	public List<Cell> getMatchingCells(EvaluationConditionalFormatRule rule) {
		final List<Cell> cells = new ArrayList<>();
		final Sheet sheet = rule.getSheet();
		for (CellRangeAddress region : rule.getRegions()) {
			for (int r = region.getFirstRow(); r <= (region.getLastRow()); r++) {
				final Row row = sheet.getRow(r);
				if (row == null) {
					continue;
				}
				for (int c = region.getFirstColumn(); c <= (region.getLastColumn()); c++) {
					final Cell cell = row.getCell(c);
					if (cell == null) {
						continue;
					}
					List<EvaluationConditionalFormatRule> cellRules = getConditionalFormattingForCell(cell);
					if (cellRules.contains(rule)) {
						cells.add(cell);
					}
				}
			}
		}
		return Collections.unmodifiableList(cells);
	}
}

