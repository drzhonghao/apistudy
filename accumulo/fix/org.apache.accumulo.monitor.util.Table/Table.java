

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import javax.servlet.http.HttpServletRequest;
import org.apache.accumulo.monitor.servlets.BasicServlet;
import org.apache.accumulo.monitor.util.TableColumn;
import org.apache.accumulo.monitor.util.TableRow;
import org.apache.accumulo.monitor.util.celltypes.CellType;
import org.apache.accumulo.monitor.util.celltypes.StringType;


public class Table {
	private String tableName;

	private String caption;

	private String captionclass;

	private String subcaption;

	private ArrayList<TableColumn<?>> columns;

	private ArrayList<TableRow> rows;

	private boolean hasBegunAddingRows = false;

	public Table(String tableName, String caption) {
		this(tableName, caption, null);
	}

	public Table(String tableName, String caption, String captionClass) {
		this.tableName = tableName;
		this.caption = caption;
		this.captionclass = captionClass;
		this.subcaption = null;
		this.columns = new ArrayList<>();
		this.rows = new ArrayList<>();
	}

	public synchronized void setSubCaption(String subcaption) {
		this.subcaption = subcaption;
	}

	public synchronized <T> void addColumn(TableColumn<T> column) {
		if (hasBegunAddingRows)
			throw new IllegalStateException("Cannot add more columns newServer rows have been added");

		columns.add(column);
	}

	private synchronized <T> void addColumn(String title, CellType<T> type, String legend, boolean sortable) {
		if (type == null)
			type = new StringType<>();

		type.setSortable(sortable);
		addColumn(new TableColumn<>(title, type, legend));
	}

	public synchronized <T> void addUnsortableColumn(String title, CellType<T> type, String legend) {
		addColumn(title, type, legend, false);
	}

	public synchronized <T> void addSortableColumn(String title, CellType<T> type, String legend) {
		addColumn(title, type, legend, true);
	}

	public synchronized void addUnsortableColumn(String title) {
		addUnsortableColumn(title, null, null);
	}

	public synchronized void addSortableColumn(String title) {
		addSortableColumn(title, null, null);
	}

	public synchronized TableRow prepareRow() {
		hasBegunAddingRows = true;
		return null;
	}

	public synchronized void addRow(TableRow row) {
		hasBegunAddingRows = true;
		rows.add(row);
	}

	public synchronized void addRow(Object... cells) {
		TableRow row = prepareRow();
		if ((cells.length) != (columns.size()))
			throw new IllegalArgumentException("Argument length not equal to the number of columns");

		for (Object cell : cells)
			row.add(cell);

		addRow(row);
	}

	public synchronized void generate(HttpServletRequest req, StringBuilder sb) {
		String page = req.getRequestURI();
		if (columns.isEmpty())
			throw new IllegalStateException("No columns in table");

		for (TableRow row : rows) {
		}
		boolean sortAscending = !("false".equals(BasicServlet.getCookieValue(req, ((((("tableSort." + (BasicServlet.encode(page))) + ".") + (BasicServlet.encode(tableName))) + ".") + "sortAsc"))));
		int sortCol = -1;
		int numLegends = 0;
		for (int i = 0; i < (columns.size()); ++i) {
			TableColumn<?> col = columns.get(i);
			if ((sortCol < 0) && (col.getCellType().isSortable()))
				sortCol = i;

			if (((col.getLegend()) != null) && (!(col.getLegend().isEmpty())))
				++numLegends;

		}
		if (sortCol >= 0) {
			String sortColStr = BasicServlet.getCookieValue(req, ((((("tableSort." + (BasicServlet.encode(page))) + ".") + (BasicServlet.encode(tableName))) + ".") + "sortCol"));
			if (sortColStr != null) {
				try {
					int col = Integer.parseInt(sortColStr);
					if ((!((col < 0) || (sortCol >= (columns.size())))) && (columns.get(col).getCellType().isSortable()))
						sortCol = col;

				} catch (NumberFormatException e) {
				}
			}
		}
		boolean showLegend = false;
		if (numLegends > 0) {
			String showStr = BasicServlet.getCookieValue(req, ((((("tableLegend." + (BasicServlet.encode(page))) + ".") + (BasicServlet.encode(tableName))) + ".") + "show"));
			showLegend = (showStr != null) && (Boolean.parseBoolean(showStr));
		}
		sb.append("<div>\n");
		sb.append("<a name='").append(tableName).append("\'>&nbsp;</a>\n");
		sb.append("<table id='").append(tableName).append("\' class=\'sortable\'>\n");
		sb.append("<caption");
		if (((captionclass) != null) && (!(captionclass.isEmpty())))
			sb.append(" class='").append(captionclass).append("'");

		sb.append(">\n");
		if (((caption) != null) && (!(caption.isEmpty())))
			sb.append("<span class='table-caption'>").append(caption).append("</span><br />\n");

		if (((subcaption) != null) && (!(subcaption.isEmpty())))
			sb.append("<span class='table-subcaption'>").append(subcaption).append("</span><br />\n");

		String redir = BasicServlet.currentPage(req);
		if (numLegends > 0) {
			String legendUrl = String.format("/op?action=toggleLegend&redir=%s&page=%s&table=%s&show=%s", redir, page, tableName, (!showLegend));
			sb.append("<a href='").append(legendUrl).append("'>").append((showLegend ? "Hide" : "Show")).append("&nbsp;Legend</a>\n");
			if (showLegend)
				sb.append("<div class=\'left show\'><dl>\n");

		}
		for (int i = 0; i < (columns.size()); ++i) {
			TableColumn<?> col = columns.get(i);
			String title = col.getTitle();
			if (((rows.size()) > 1) && (col.getCellType().isSortable())) {
				String url = String.format("/op?action=sortTable&redir=%s&page=%s&table=%s&%s=%s", redir, page, tableName, (sortCol == i ? "asc" : "col"), (sortCol == i ? !sortAscending : i));
				String img = "";
				if (sortCol == i)
					img = String.format("&nbsp;<img width='10px' height='10px' src='/web/%s.gif' alt='%s' />", (sortAscending ? "up" : "down"), (!sortAscending ? "^" : "v"));

				col.setTitle(String.format("<a href='%s'>%s%s</a>", url, title, img));
			}
			String legend = col.getLegend();
			if ((showLegend && (legend != null)) && (!(legend.isEmpty())))
				sb.append("<dt class='smalltext'><b>").append(title.replace("<br />", "&nbsp;")).append("</b><dd>").append(legend).append("</dd></dt>\n");

		}
		if (showLegend && (numLegends > 0))
			sb.append("</dl></div>\n");

		sb.append("</caption>\n");
		sb.append("<tr>");
		boolean first = true;
		for (TableColumn<?> col : columns) {
			String cellValue = ((col.getTitle()) == null) ? "" : String.valueOf(col.getTitle()).trim();
			sb.append("<th").append((first ? " class='firstcell'" : "")).append(">").append((cellValue.isEmpty() ? "-" : cellValue)).append("</th>");
			first = false;
		}
		sb.append("</tr>\n");
		if (((rows.size()) > 1) && (sortCol > (-1))) {
			Collections.sort(rows, TableRow.getComparator(sortCol, columns.get(sortCol).getCellType()));
			if (!sortAscending)
				Collections.reverse(rows);

		}
		boolean highlight = true;
		for (TableRow row : rows) {
			Table.row(sb, highlight, columns, row);
			highlight = !highlight;
		}
		if (rows.isEmpty())
			sb.append("<tr><td class='center' colspan='").append(columns.size()).append("\'><i>Empty</i></td></tr>\n");

		sb.append("</table>\n</div>\n\n");
	}

	private static void row(StringBuilder sb, boolean highlight, ArrayList<TableColumn<?>> columns, TableRow row) {
		sb.append((highlight ? "<tr class='highlight'>" : "<tr>"));
		boolean first = true;
		sb.append("</tr>\n");
	}
}

