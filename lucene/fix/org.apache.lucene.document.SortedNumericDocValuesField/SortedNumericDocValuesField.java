

import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.DocValuesType;
import org.apache.lucene.search.Query;


public class SortedNumericDocValuesField extends Field {
	public static final FieldType TYPE = new FieldType();

	static {
		SortedNumericDocValuesField.TYPE.setDocValuesType(DocValuesType.SORTED_NUMERIC);
		SortedNumericDocValuesField.TYPE.freeze();
	}

	public SortedNumericDocValuesField(String name, long value) {
		super(name, SortedNumericDocValuesField.TYPE);
		fieldsData = Long.valueOf(value);
	}

	public static Query newSlowRangeQuery(String field, long lowerValue, long upperValue) {
		return null;
	}

	public static Query newSlowExactQuery(String field, long value) {
		return SortedNumericDocValuesField.newSlowRangeQuery(field, value, value);
	}
}

