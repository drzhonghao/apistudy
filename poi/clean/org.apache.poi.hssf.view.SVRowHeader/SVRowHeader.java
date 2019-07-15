import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.view.*;


import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;

import org.apache.poi.hssf.usermodel.*;

/**
 * This class presents the row header to the table.
 *
 *
 * @author Jason Height
 */
public class SVRowHeader extends JList<Object> {
  /** This model simply returns an integer number up to the number of rows
   *  that are present in the sheet.
   *
   */
  private class SVRowHeaderModel extends AbstractListModel<Object> {
    private HSSFSheet sheet;

    public SVRowHeaderModel(HSSFSheet sheet) {
      this.sheet = sheet;
    }

    @Override
    public int getSize() {
    	return sheet.getLastRowNum() + 1;
    }
    @Override
    public Object getElementAt(int index) {
      return Integer.toString(index+1);
    }
  }

  /** Renderes the row number*/
  private class RowHeaderRenderer extends JLabel implements ListCellRenderer<Object> {
    private HSSFSheet sheet;
    private int extraHeight;

    RowHeaderRenderer(HSSFSheet sheet, JTable table, int extraHeight) {
      this.sheet = sheet;
      this.extraHeight = extraHeight;
      JTableHeader header = table.getTableHeader();
      setOpaque(true);
      setBorder(UIManager.getBorder("TableHeader.cellBorder"));
      setHorizontalAlignment(CENTER);
      setForeground(header.getForeground());
      setBackground(header.getBackground());
      setFont(header.getFont());
    }

    @Override
    public Component getListCellRendererComponent( JList list,
           Object value, int index, boolean isSelected, boolean cellHasFocus) {
      Dimension d = getPreferredSize();
      HSSFRow row = sheet.getRow(index);
      int rowHeight;
      if(row == null) {
    	  rowHeight = (int)sheet.getDefaultRowHeightInPoints();
      } else {
    	  rowHeight = (int)row.getHeightInPoints();
      }
      d.height = rowHeight+extraHeight;
      setPreferredSize(d);
      setText((value == null) ? "" : value.toString());
      return this;
    }
  }

  public SVRowHeader(HSSFSheet sheet, JTable table, int extraHeight) {
    ListModel<Object> lm = new SVRowHeaderModel(sheet);
    this.setModel(lm);

    setFixedCellWidth(50);
    setCellRenderer(new RowHeaderRenderer(sheet, table, extraHeight));
  }
}
