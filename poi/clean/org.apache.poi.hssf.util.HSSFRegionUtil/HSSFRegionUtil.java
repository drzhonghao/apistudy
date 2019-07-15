import org.apache.poi.hssf.util.*;


import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.RegionUtil;
import org.apache.poi.util.Removal;

/**
 * Various utility functions that make working with a region of cells easier.
 * @deprecated POI 4.0.0
 * @see RegionUtil
 */
@Removal(version="4.2")
public final class HSSFRegionUtil {

	private HSSFRegionUtil() {
		// no instances of this class
	}

	/**
	 * Sets the left border for a region of cells by manipulating the cell style
	 * of the individual cells on the left
	 *
	 * @param border The new border
	 * @param region The region that should have the border
	 * @param workbook The workbook that the region is on.
	 * @param sheet The sheet that the region is on.
	 */
	public static void setBorderLeft(int border, CellRangeAddress region, HSSFSheet sheet,
			HSSFWorkbook workbook) {
		RegionUtil.setBorderLeft(BorderStyle.valueOf((short)border), region, sheet);
	}

	/**
	 * Sets the leftBorderColor attribute of the HSSFRegionUtil object
	 *
	 * @param color The color of the border
	 * @param region The region that should have the border
	 * @param workbook The workbook that the region is on.
	 * @param sheet The sheet that the region is on.
	 */
	public static void setLeftBorderColor(int color, CellRangeAddress region, HSSFSheet sheet,
			HSSFWorkbook workbook) {
		RegionUtil.setLeftBorderColor(color, region, sheet);
	}

	/**
	 * Sets the borderRight attribute of the HSSFRegionUtil object
	 *
	 * @param border The new border
	 * @param region The region that should have the border
	 * @param workbook The workbook that the region is on.
	 * @param sheet The sheet that the region is on.
	 */
	public static void setBorderRight(int border, CellRangeAddress region, HSSFSheet sheet,
			HSSFWorkbook workbook) {
		RegionUtil.setBorderRight(BorderStyle.valueOf((short)border), region, sheet);
	}

	/**
	 * Sets the rightBorderColor attribute of the HSSFRegionUtil object
	 *
	 * @param color The color of the border
	 * @param region The region that should have the border
	 * @param workbook The workbook that the region is on.
	 * @param sheet The sheet that the region is on.
	 */
	public static void setRightBorderColor(int color, CellRangeAddress region, HSSFSheet sheet,
			HSSFWorkbook workbook) {
		RegionUtil.setRightBorderColor(color, region, sheet);
	}

	/**
	 * Sets the borderBottom attribute of the HSSFRegionUtil object
	 *
	 * @param border The new border
	 * @param region The region that should have the border
	 * @param workbook The workbook that the region is on.
	 * @param sheet The sheet that the region is on.
	 */
	public static void setBorderBottom(int border, CellRangeAddress region, HSSFSheet sheet,
			HSSFWorkbook workbook) {
		RegionUtil.setBorderBottom(BorderStyle.valueOf((short)border), region, sheet);
	}

	/**
	 * Sets the bottomBorderColor attribute of the HSSFRegionUtil object
	 *
	 * @param color The color of the border
	 * @param region The region that should have the border
	 * @param workbook The workbook that the region is on.
	 * @param sheet The sheet that the region is on.
	 */
	public static void setBottomBorderColor(int color, CellRangeAddress region, HSSFSheet sheet,
			HSSFWorkbook workbook) {
		RegionUtil.setBottomBorderColor(color, region, sheet);
	}

	/**
	 * Sets the borderBottom attribute of the HSSFRegionUtil object
	 *
	 * @param border The new border
	 * @param region The region that should have the border
	 * @param workbook The workbook that the region is on.
	 * @param sheet The sheet that the region is on.
	 */
	public static void setBorderTop(int border, CellRangeAddress region, HSSFSheet sheet,
			HSSFWorkbook workbook) {
		RegionUtil.setBorderTop(BorderStyle.valueOf((short)border), region, sheet);
	}

	/**
	 * Sets the topBorderColor attribute of the HSSFRegionUtil object
	 *
	 * @param color The color of the border
	 * @param region  The region that should have the border
	 * @param workbook The workbook that the region is on.
	 * @param sheet The sheet that the region is on.
	 */
	public static void setTopBorderColor(int color, CellRangeAddress region, HSSFSheet sheet,
			HSSFWorkbook workbook) {
		RegionUtil.setTopBorderColor(color, region, sheet);
	}
}
