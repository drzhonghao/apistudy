

import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.felix.gogo.runtime.threadio.ThreadPrintStream;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Job;
import org.apache.karaf.shell.support.table.Col;
import org.apache.karaf.shell.support.table.Row;
import org.jline.terminal.Terminal;

import static org.apache.felix.service.command.Job.Utils.current;


public class ShellTable {
	private static final char SEP_HORIZONTAL = '1';

	private static final char SEP_VERTICAL = '2';

	private static final char SEP_CROSS = '3';

	private static final char SEP_HORIZONTAL_ASCII = '-';

	private static final char SEP_VERTICAL_ASCII = '|';

	private static final char SEP_CROSS_ASCII = '+';

	private static final String DEFAULT_SEPARATOR = (" " + (ShellTable.SEP_VERTICAL)) + " ";

	private static final String DEFAULT_SEPARATOR_ASCII = (" " + (ShellTable.SEP_VERTICAL_ASCII)) + " ";

	private static final String DEFAULT_SEPARATOR_NO_FORMAT = "\t";

	private List<Col> cols = new ArrayList<>();

	private List<Row> rows = new ArrayList<>();

	private boolean showHeaders = true;

	private String separator = ShellTable.DEFAULT_SEPARATOR;

	private int size;

	private String emptyTableText;

	private boolean forceAscii;

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

	public ShellTable forceAscii() {
		forceAscii = true;
		return this;
	}

	public ShellTable emptyTableText(String text) {
		this.emptyTableText = text;
		return this;
	}

	public void print(PrintStream out) {
		print(out, true);
	}

	public void print(PrintStream out, boolean format) {
		print(out, null, format);
	}

	public void print(PrintStream out, Charset charset, boolean format) {
		boolean unicode = supportsUnicode(out, charset);
		String separator = (unicode) ? this.separator : ShellTable.DEFAULT_SEPARATOR_ASCII;
		for (Row row : rows) {
		}
		if ((size) > 0) {
			adjustSize();
		}
		if (format && (showHeaders)) {
			int iCol = 0;
			for (Col col : cols) {
				if ((iCol++) == 0) {
					out.print(underline(col.getSize(), false, unicode));
				}else {
					out.print(underline(((col.getSize()) + 3), true, unicode));
				}
				iCol++;
			}
			out.println();
		}
		for (Row row : rows) {
			if (!format) {
				if ((separator == null) || (separator.equals(ShellTable.DEFAULT_SEPARATOR))) {
				}else {
				}
			}else {
			}
		}
		if ((format && ((rows.size()) == 0)) && ((emptyTableText) != null)) {
			out.println(emptyTableText);
		}
	}

	private boolean supportsUnicode(PrintStream out, Charset charset) {
		if (forceAscii) {
			return false;
		}
		if (charset == null) {
			charset = getEncoding(out);
		}
		if (charset == null) {
			return false;
		}
		CharsetEncoder encoder = charset.newEncoder();
		return ((encoder.canEncode(separator)) && (encoder.canEncode(ShellTable.SEP_HORIZONTAL))) && (encoder.canEncode(ShellTable.SEP_CROSS));
	}

	private Charset getEncoding(PrintStream ps) {
		if ((ps.getClass()) == (ThreadPrintStream.class)) {
			try {
				return ((Terminal) (current().session().get(".jline.terminal"))).encoding();
			} catch (Throwable t) {
			}
			try {
				ps = ((PrintStream) (ps.getClass().getMethod("getCurrent").invoke(ps)));
			} catch (Throwable t) {
			}
		}
		try {
			Field f = ps.getClass().getDeclaredField("charOut");
			f.setAccessible(true);
			OutputStreamWriter osw = ((OutputStreamWriter) (f.get(ps)));
			return Charset.forName(osw.getEncoding());
		} catch (Throwable t) {
		}
		return null;
	}

	private void adjustSize() {
		int currentSize = 0;
		for (Col col : cols) {
		}
		currentSize -= separator.length();
		int sizeToGrow = (size) - currentSize;
		for (int i = (cols.size()) - 1; i >= 0; i--) {
			Col col = cols.get(i);
		}
	}

	private String underline(int length, boolean crossAtBeg, boolean supported) {
		char[] exmarks = new char[length];
		Arrays.fill(exmarks, (supported ? ShellTable.SEP_HORIZONTAL : ShellTable.SEP_HORIZONTAL_ASCII));
		if (crossAtBeg) {
			exmarks[1] = (supported) ? ShellTable.SEP_CROSS : ShellTable.SEP_CROSS_ASCII;
		}
		return new String(exmarks);
	}
}

