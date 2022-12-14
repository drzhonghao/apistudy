import org.apache.poi.ss.usermodel.helpers.*;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;

/**
 * Helper for shifting rows up or down
 */
// non-Javadoc: This abstract class exists to consolidate duplicated code between
// {@link org.apache.poi.hssf.usermodel.helpers.HSSFRowShifter} and
// {@link org.apache.poi.xssf.usermodel.helpers.XSSFRowShifter}
// (currently methods sprinkled throughout HSSFSheet)
public abstract class RowShifter extends BaseRowColShifter {
    protected final Sheet sheet;

    public RowShifter(Sheet sh) { 
        sheet = sh; 
    } 
 
  /**
     * Shifts, grows, or shrinks the merged regions due to a row shift.
     * Merged regions that are completely overlaid by shifting will be deleted.
     *
     * @param startRow the row to start shifting
     * @param endRow   the row to end shifting
     * @param n        the number of rows to shift
     * @return an array of affected merged regions, doesn't contain deleted ones
     */
    // Keep this code in sync with {@link ColumnShifter#shiftMergedRegions}
    @Override
    public List<CellRangeAddress> shiftMergedRegions(int startRow, int endRow, int n) {
        List<CellRangeAddress> shiftedRegions = new ArrayList<>();
        Set<Integer> removedIndices = new HashSet<>();
        //move merged regions completely if they fall within the new region boundaries when they are shifted
        int size = sheet.getNumMergedRegions();
        for (int i = 0; i < size; i++) {
            CellRangeAddress merged = sheet.getMergedRegion(i);

            // remove merged region that are replaced by the shifting,
            // i.e. where the area includes something in the overwritten area
            if(removalNeeded(merged, startRow, endRow, n)) {
                removedIndices.add(i);
                continue;
            }

            boolean inStart = (merged.getFirstRow() >= startRow || merged.getLastRow() >= startRow);
            boolean inEnd = (merged.getFirstRow() <= endRow || merged.getLastRow() <= endRow);

            //don't check if it's not within the shifted area
            if (!inStart || !inEnd) {
                continue;
            }

            //only shift if the region outside the shifted rows is not merged too
            if (!merged.containsRow(startRow - 1) && !merged.containsRow(endRow + 1)) {
                merged.setFirstRow(merged.getFirstRow() + n);
                merged.setLastRow(merged.getLastRow() + n);
                //have to remove/add it back
                shiftedRegions.add(merged);
                removedIndices.add(i);
            }
        }
        
        if(!removedIndices.isEmpty()) {
            sheet.removeMergedRegions(removedIndices);
        }

        //read so it doesn't get shifted again
        for (CellRangeAddress region : shiftedRegions) {
            sheet.addMergedRegion(region);
        }
        return shiftedRegions;
    }

    // Keep in sync with {@link ColumnShifter#removalNeeded}
    private boolean removalNeeded(CellRangeAddress merged, int startRow, int endRow, int n) {
        final int movedRows = endRow - startRow + 1;

        // build a range of the rows that are overwritten, i.e. the target-area, but without
        // rows that are moved along
        final CellRangeAddress overwrite;
        if(n > 0) {
            // area is moved down => overwritten area is [endRow + n - movedRows, endRow + n]
            final int firstRow = Math.max(endRow + 1, endRow + n - movedRows);
            final int lastRow = endRow + n;
            overwrite = new CellRangeAddress(firstRow, lastRow, 0, 0);
        } else {
            // area is moved up => overwritten area is [startRow + n, startRow + n + movedRows]
            final int firstRow = startRow + n;
            final int lastRow = Math.min(startRow - 1, startRow + n + movedRows);
            overwrite = new CellRangeAddress(firstRow, lastRow, 0, 0);
        }

        // if the merged-region and the overwritten area intersect, we need to remove it
        return merged.intersects(overwrite);
    }
}
