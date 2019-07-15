

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.karaf.shell.table.Col;
import org.apache.karaf.shell.table.Row;


@Deprecated
public class ShellTable {
	private List<Col> cols = new ArrayList<>();

	private List<Row> rows = new ArrayList<>();

	boolean showHeaders = true;

	private String separator = " | ";

	private int size;

	private String emptyTableText;

	public ShellTable() {
	}

	public ShellTable noHeaders() {
		this.showHeaders = false;
		return this;
	}

	public ShellTable separator(String separator) {
		this.separator = separator;
		return this;
	}

	public ShellTable size(int size) {
		this.size = size;
		return this;
	}

	public ShellTable column(Col colunmn) {
		cols.add(colunmn);
		return this;
	}

	public Col column(String header) {
		Col col = new Col(header);
		cols.add(col);
		return col;
	}

	public Row addRow() {
		return null;
	}

	public ShellTable emptyTableText(String text) {
		this.emptyTableText = text;
		return this;
	}

	public void print(PrintStream out) {
		print(out, true);
	}

	public void print(PrintStream out, boolean format) {
		for (Row row : rows) {
		}
		if ((size) > 0) {
			tryGrowToMaxSize();
		}
		if (format && (showHeaders)) {
			for (Col col : cols) {
				out.print(underline(col.getSize()));
			}
			out.println(underline((((cols.size()) - 1) * 3)));
		}
		for (Row row : rows) {
			if (!format) {
				if (((separator) == null) || (separator.equals(" | "))) {
				}else {
				}
			}else {
			}
		}
		if ((format && ((rows.size()) == 0)) && ((emptyTableText) != null)) {
			out.println(emptyTableText);
		}
	}

	private void tryGrowToMaxSize() {
		int currentSize = 0;
		for (Col col : cols) {
		}
		currentSize -= separator.length();
		int sizeToGrow = (size) - currentSize;
		for (Col col : cols) {
		}
	}

	private String underline(int length) {
		char[] exmarks = new char[length];
		Arrays.fill(exmarks, '-');
		return new String(exmarks);
	}
}

