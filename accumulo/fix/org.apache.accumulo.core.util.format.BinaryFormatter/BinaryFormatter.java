

import java.util.Iterator;
import java.util.Map;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.security.ColumnVisibility;
import org.apache.accumulo.core.util.format.DefaultFormatter;
import org.apache.accumulo.core.util.format.FormatterConfig;
import org.apache.hadoop.io.Text;


@Deprecated
public class BinaryFormatter extends DefaultFormatter {
	@Override
	public String next() {
		checkState(true);
		return BinaryFormatter.formatEntry(getScannerIterator().next(), config.willPrintTimestamps(), config.getShownLength());
	}

	public static String formatEntry(Map.Entry<Key, Value> entry, boolean printTimestamps, int shownLength) {
		StringBuilder sb = new StringBuilder();
		Key key = entry.getKey();
		DefaultFormatter.appendText(sb, key.getRow(), shownLength).append(" ");
		DefaultFormatter.appendText(sb, key.getColumnFamily(), shownLength).append(":");
		DefaultFormatter.appendText(sb, key.getColumnQualifier(), shownLength).append(" ");
		sb.append(new ColumnVisibility(key.getColumnVisibility()));
		if (printTimestamps)
			sb.append(" ").append(entry.getKey().getTimestamp());

		Value value = entry.getValue();
		if ((value != null) && ((value.getSize()) > 0)) {
			sb.append("\t");
		}
		return sb.toString();
	}
}

