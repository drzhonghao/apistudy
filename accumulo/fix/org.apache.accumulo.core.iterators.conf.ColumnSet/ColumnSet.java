

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.iterators.conf.ColumnUtil;
import org.apache.accumulo.core.iterators.conf.ColumnUtil.ColFamHashKey;
import org.apache.accumulo.core.util.Pair;
import org.apache.hadoop.io.Text;


public class ColumnSet {
	private Set<ColumnUtil.ColFamHashKey> objectsCF;

	private Set<ColumnUtil.ColHashKey> objectsCol;

	public ColumnSet() {
		objectsCF = new HashSet<>();
		objectsCol = new HashSet<>();
	}

	public ColumnSet(Collection<String> objectStrings) {
		this();
		for (String column : objectStrings) {
			Pair<Text, Text> pcic = ColumnSet.decodeColumns(column);
			if ((pcic.getSecond()) == null) {
				add(pcic.getFirst());
			}else {
				add(pcic.getFirst(), pcic.getSecond());
			}
		}
	}

	protected void add(Text colf) {
	}

	protected void add(Text colf, Text colq) {
	}

	public boolean contains(Key key) {
		if ((objectsCol.size()) > 0) {
		}
		if ((objectsCF.size()) > 0) {
		}
		return false;
	}

	public boolean isEmpty() {
		return ((objectsCol.size()) == 0) && ((objectsCF.size()) == 0);
	}

	public static String encodeColumns(Text columnFamily, Text columnQualifier) {
		StringBuilder sb = new StringBuilder();
		ColumnSet.encode(sb, columnFamily);
		if (columnQualifier != null) {
			sb.append(':');
			ColumnSet.encode(sb, columnQualifier);
		}
		return sb.toString();
	}

	static void encode(StringBuilder sb, Text t) {
		for (int i = 0; i < (t.getLength()); i++) {
			int b = 255 & (t.getBytes()[i]);
			if ((((((b >= 'a') && (b <= 'z')) || ((b >= 'A') && (b <= 'Z'))) || ((b >= '0') && (b <= '9'))) || (b == '_')) || (b == '-')) {
				sb.append(((char) (b)));
			}else {
				sb.append('%');
				sb.append(String.format("%02x", b));
			}
		}
	}

	public static boolean isValidEncoding(String enc) {
		for (char c : enc.toCharArray()) {
			boolean validChar = (((((((c >= 'a') && (c <= 'z')) || ((c >= 'A') && (c <= 'Z'))) || ((c >= '0') && (c <= '9'))) || (c == '_')) || (c == '-')) || (c == ':')) || (c == '%');
			if (!validChar)
				return false;

		}
		return true;
	}

	public static Pair<Text, Text> decodeColumns(String columns) {
		if (!(ColumnSet.isValidEncoding(columns)))
			throw new IllegalArgumentException(("Invalid encoding " + columns));

		String[] cols = columns.split(":");
		if ((cols.length) == 1) {
			return new Pair<>(ColumnSet.decode(cols[0]), null);
		}else
			if ((cols.length) == 2) {
				return new Pair<>(ColumnSet.decode(cols[0]), ColumnSet.decode(cols[1]));
			}else {
				throw new IllegalArgumentException(columns);
			}

	}

	static Text decode(String s) {
		Text t = new Text();
		byte[] sb = s.getBytes(StandardCharsets.UTF_8);
		for (int i = 0; i < (sb.length); i++) {
			if ((sb[i]) != '%') {
				t.append(new byte[]{ sb[i] }, 0, 1);
			}else {
				byte[] hex = new byte[]{ sb[(++i)], sb[(++i)] };
				String hs = new String(hex, StandardCharsets.UTF_8);
				int b = Integer.parseInt(hs, 16);
				t.append(new byte[]{ ((byte) (b)) }, 0, 1);
			}
		}
		return t;
	}
}

