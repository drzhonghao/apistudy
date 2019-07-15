

import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.DocValuesType;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.BytesRef;


public class SortedSetDocValuesField extends Field {
	public static final FieldType TYPE = new FieldType();

	static {
		SortedSetDocValuesField.TYPE.setDocValuesType(DocValuesType.SORTED_SET);
		SortedSetDocValuesField.TYPE.freeze();
	}

	public SortedSetDocValuesField(String name, BytesRef bytes) {
		super(name, SortedSetDocValuesField.TYPE);
		fieldsData = bytes;
	}

	public static Query newSlowRangeQuery(String field, BytesRef lowerValue, BytesRef upperValue, boolean lowerInclusive, boolean upperInclusive) {
		return null;
	}

	public static Query newSlowExactQuery(String field, BytesRef value) {
		return SortedSetDocValuesField.newSlowRangeQuery(field, value, value, true, true);
	}
}

