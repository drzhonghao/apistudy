

import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.DocValuesType;
import org.apache.lucene.search.Query;


public class NumericDocValuesField extends Field {
	public static final FieldType TYPE = new FieldType();

	static {
		NumericDocValuesField.TYPE.setDocValuesType(DocValuesType.NUMERIC);
		NumericDocValuesField.TYPE.freeze();
	}

	public NumericDocValuesField(String name, long value) {
		this(name, Long.valueOf(value));
	}

	public NumericDocValuesField(String name, Long value) {
		super(name, NumericDocValuesField.TYPE);
		fieldsData = value;
	}

	public static Query newSlowRangeQuery(String field, long lowerValue, long upperValue) {
		return null;
	}

	public static Query newSlowExactQuery(String field, long value) {
		return NumericDocValuesField.newSlowRangeQuery(field, value, value);
	}
}

