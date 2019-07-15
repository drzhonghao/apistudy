

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.karaf.shell.table.Col;


public class Row {
	private List<Object> data;

	private List<String> content;

	Row() {
		data = new ArrayList<>();
		content = new ArrayList<>();
	}

	Row(List<Col> cols) {
		this();
		for (Col col : cols) {
		}
	}

	public void addContent(Object... cellDataAr) {
		data.addAll(Arrays.asList(cellDataAr));
	}

	void formatContent(List<Col> cols) {
		content.clear();
		int c = 0;
		for (Col col : cols) {
			c++;
		}
	}

	String getContent(List<Col> cols, String separator) {
		StringBuilder st = new StringBuilder();
		int c = 0;
		if ((cols.size()) != (content.size())) {
			throw new RuntimeException("Number of columns and number of content elements do not match");
		}
		for (Col col : cols) {
			if ((c + 1) < (cols.size())) {
				st.append(separator);
			}
			c++;
		}
		return st.toString();
	}
}

