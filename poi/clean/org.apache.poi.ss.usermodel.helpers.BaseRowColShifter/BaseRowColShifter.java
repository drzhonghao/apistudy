import org.apache.poi.ss.usermodel.helpers.*;



import org.apache.poi.ss.formula.FormulaShifter;
import org.apache.poi.ss.formula.ptg.AreaErrPtg;
import org.apache.poi.ss.formula.ptg.AreaPtg;
import org.apache.poi.ss.formula.ptg.Ptg;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.util.Internal;

import java.util.List;

/**
 * Class for code common to {@link RowShifter} and {@link ColumnShifter}
 * Helper for shifting rows up or down and columns left and right
 *
 * @since POI 4.0.0
 */
@Internal
public abstract class BaseRowColShifter {

    /**
     * Update named ranges
     */
    public abstract void updateNamedRanges(FormulaShifter formulaShifter);

    /**
     * Update formulas.
     */
    public abstract void updateFormulas(FormulaShifter formulaShifter);

    /**
     * Shifts, grows, or shrinks the merged regions due to a row shift
     * ({@link RowShifter}) or column shift ({@link ColumnShifter}).
     * Merged regions that are completely overlaid by shifting will be deleted.
     *
     * @param start the first row or column to be shifted
     * @param end   the last row or column to be shifted
     * @param n     the number of rows or columns to shift
     * @return a list of affected merged regions, excluding contain deleted ones
     */
    public abstract List<CellRangeAddress> shiftMergedRegions(int start, int end, int n);

    /**
     * Update conditional formatting
     * @param formulaShifter
     */
    public abstract void updateConditionalFormatting(FormulaShifter formulaShifter);

    /**
     * Shift the Hyperlink anchors (not the hyperlink text, even if the hyperlink
     * is of type LINK_DOCUMENT and refers to a cell that was shifted). Hyperlinks
     * do not track the content they point to.
     *
     * @param formulaShifter the formula shifting policy
     */
    public abstract void updateHyperlinks(FormulaShifter formulaShifter);

    public static CellRangeAddress shiftRange(FormulaShifter formulaShifter, CellRangeAddress cra, int currentExternSheetIx) {
        // FormulaShifter works well in terms of Ptgs - so convert CellRangeAddress to AreaPtg (and back) here
        AreaPtg aptg = new AreaPtg(cra.getFirstRow(), cra.getLastRow(), cra.getFirstColumn(), cra.getLastColumn(), false, false, false, false);
        Ptg[] ptgs = { aptg, };

        if (!formulaShifter.adjustFormula(ptgs, currentExternSheetIx)) {
            return cra;
        }
        Ptg ptg0 = ptgs[0];
        if (ptg0 instanceof AreaPtg) {
            AreaPtg bptg = (AreaPtg) ptg0;
            return new CellRangeAddress(bptg.getFirstRow(), bptg.getLastRow(), bptg.getFirstColumn(), bptg.getLastColumn());
        }
        if (ptg0 instanceof AreaErrPtg) {
            return null;
        }
        throw new IllegalStateException("Unexpected shifted ptg class (" + ptg0.getClass().getName() + ")");
    }

}
