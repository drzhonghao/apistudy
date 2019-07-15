import org.apache.poi.hssf.usermodel.helpers.*;


import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.ss.formula.FormulaShifter;
import org.apache.poi.ss.formula.eval.NotImplementedException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.helpers.RowShifter;
import org.apache.poi.util.NotImplemented;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;

/**
 * Helper for shifting rows up or down
 */
// non-Javadoc: When possible, code should be implemented in the RowShifter abstract class to avoid duplication with
// {@link org.apache.poi.xssf.usermodel.helpers.XSSFRowShifter}
public final class HSSFRowShifter extends RowShifter {
    private static final POILogger logger = POILogFactory.getLogger(HSSFRowShifter.class);

    public HSSFRowShifter(HSSFSheet sh) {
        super(sh);
    }

    @Override
    @NotImplemented
    public void updateNamedRanges(FormulaShifter formulaShifter) {
        throw new NotImplementedException("HSSFRowShifter.updateNamedRanges");
    }

    @Override
    @NotImplemented
    public void updateFormulas(FormulaShifter formulaShifter) {
        throw new NotImplementedException("updateFormulas");
    }

    @Override
    @NotImplemented
    public void updateConditionalFormatting(FormulaShifter formulaShifter) {
        throw new NotImplementedException("updateConditionalFormatting");
    }

    @Override
    @NotImplemented
    public void updateHyperlinks(FormulaShifter formulaShifter) {
        throw new NotImplementedException("updateHyperlinks");
    }

}
