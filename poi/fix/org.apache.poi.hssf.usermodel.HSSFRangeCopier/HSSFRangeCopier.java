

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.RangeCopier;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.util.Beta;


@Beta
public class HSSFRangeCopier extends RangeCopier {
	public HSSFRangeCopier(Sheet sourceSheet, Sheet destSheet) {
		super(sourceSheet, destSheet);
	}

	protected void adjustCellReferencesInsideFormula(Cell cell, Sheet destSheet, int deltaX, int deltaY) {
		int destSheetIndex = destSheet.getWorkbook().getSheetIndex(destSheet);
	}
}

