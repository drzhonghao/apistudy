

import org.apache.accumulo.core.iterators.conf.ColumnSet;
import org.apache.hadoop.io.Text;


@Deprecated
public class PerColumnIteratorConfig {
	private String parameter;

	private Text colq;

	private Text colf;

	public PerColumnIteratorConfig(Text columnFamily, String parameter) {
		this.colf = columnFamily;
		this.colq = null;
		this.parameter = parameter;
	}

	public PerColumnIteratorConfig(Text columnFamily, Text columnQualifier, String parameter) {
		this.colf = columnFamily;
		this.colq = columnQualifier;
		this.parameter = parameter;
	}

	public Text getColumnFamily() {
		return colf;
	}

	public Text getColumnQualifier() {
		return colq;
	}

	public String encodeColumns() {
		return PerColumnIteratorConfig.encodeColumns(this);
	}

	public String getClassName() {
		return parameter;
	}

	private static String encodeColumns(PerColumnIteratorConfig pcic) {
		return ColumnSet.encodeColumns(pcic.colf, pcic.colq);
	}

	public static String encodeColumns(Text columnFamily, Text columnQualifier) {
		return ColumnSet.encodeColumns(columnFamily, columnQualifier);
	}

	public static PerColumnIteratorConfig decodeColumns(String columns, String className) {
		String[] cols = columns.split(":");
		if ((cols.length) == 1) {
		}else
			if ((cols.length) == 2) {
			}else {
				throw new IllegalArgumentException(columns);
			}

		return null;
	}
}

