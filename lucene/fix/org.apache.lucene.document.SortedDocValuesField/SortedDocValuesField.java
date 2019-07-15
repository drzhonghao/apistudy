

import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.DocValuesType;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.BytesRef;


public class SortedDocValuesField extends Field {
	public static final FieldType TYPE = new FieldType();

	static {
		SortedDocValuesField.TYPE.setDocValuesType(DocValuesType.SORTED);
		SortedDocValuesField.TYPE.freeze();
	}

	public SortedDocValuesField(String name, BytesRef bytes) {
		super(name, SortedDocValuesField.TYPE);
		fieldsData = bytes;
	}

	public static Query newSlowRangeQuery(String field, BytesRef lowerValue, BytesRef upperValue, boolean lowerInclusive, boolean upperInclusive) {
		return null;
	}

	public static Query newSlowExactQuery(String field, BytesRef value) {
		return SortedDocValuesField.newSlowRangeQuery(field, value, value, true, true);
	}
}

