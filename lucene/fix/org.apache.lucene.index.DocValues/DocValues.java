

import java.io.IOException;
import java.util.Arrays;
import org.apache.lucene.index.BinaryDocValues;
import org.apache.lucene.index.DocValuesType;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.FieldInfos;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.LegacySortedDocValues;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.SortedDocValues;
import org.apache.lucene.index.SortedNumericDocValues;
import org.apache.lucene.index.SortedSetDocValues;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.util.BytesRef;


public final class DocValues {
	private DocValues() {
	}

	public static final BinaryDocValues emptyBinary() {
		return new BinaryDocValues() {
			private int doc = -1;

			@Override
			public int advance(int target) {
				return doc = DocIdSetIterator.NO_MORE_DOCS;
			}

			@Override
			public boolean advanceExact(int target) throws IOException {
				doc = target;
				return false;
			}

			@Override
			public int docID() {
				return doc;
			}

			@Override
			public int nextDoc() {
				return doc = DocIdSetIterator.NO_MORE_DOCS;
			}

			@Override
			public long cost() {
				return 0;
			}

			@Override
			public BytesRef binaryValue() {
				assert false;
				return null;
			}
		};
	}

	public static final NumericDocValues emptyNumeric() {
		return new NumericDocValues() {
			private int doc = -1;

			@Override
			public int advance(int target) {
				return doc = DocIdSetIterator.NO_MORE_DOCS;
			}

			@Override
			public boolean advanceExact(int target) throws IOException {
				doc = target;
				return false;
			}

			@Override
			public int docID() {
				return doc;
			}

			@Override
			public int nextDoc() {
				return doc = DocIdSetIterator.NO_MORE_DOCS;
			}

			@Override
			public long cost() {
				return 0;
			}

			@Override
			public long longValue() {
				assert false;
				return 0;
			}
		};
	}

	public static final LegacySortedDocValues emptyLegacySorted() {
		final BytesRef empty = new BytesRef();
		return new LegacySortedDocValues() {
			@Override
			public int getOrd(int docID) {
				return -1;
			}

			@Override
			public BytesRef lookupOrd(int ord) {
				return empty;
			}

			@Override
			public int getValueCount() {
				return 0;
			}
		};
	}

	public static final SortedDocValues emptySorted() {
		final BytesRef empty = new BytesRef();
		return new SortedDocValues() {
			private int doc = -1;

			@Override
			public int advance(int target) {
				return doc = DocIdSetIterator.NO_MORE_DOCS;
			}

			@Override
			public boolean advanceExact(int target) throws IOException {
				doc = target;
				return false;
			}

			@Override
			public int docID() {
				return doc;
			}

			@Override
			public int nextDoc() {
				return doc = DocIdSetIterator.NO_MORE_DOCS;
			}

			@Override
			public long cost() {
				return 0;
			}

			@Override
			public int ordValue() {
				assert false;
				return -1;
			}

			@Override
			public BytesRef lookupOrd(int ord) {
				return empty;
			}

			@Override
			public int getValueCount() {
				return 0;
			}
		};
	}

	public static final SortedNumericDocValues emptySortedNumeric(int maxDoc) {
		return new SortedNumericDocValues() {
			private int doc = -1;

			@Override
			public int advance(int target) {
				return doc = DocIdSetIterator.NO_MORE_DOCS;
			}

			@Override
			public boolean advanceExact(int target) throws IOException {
				doc = target;
				return false;
			}

			@Override
			public int docID() {
				return doc;
			}

			@Override
			public int nextDoc() {
				return doc = DocIdSetIterator.NO_MORE_DOCS;
			}

			@Override
			public long cost() {
				return 0;
			}

			@Override
			public int docValueCount() {
				throw new IllegalStateException();
			}

			@Override
			public long nextValue() {
				throw new IllegalStateException();
			}
		};
	}

	public static final SortedSetDocValues emptySortedSet() {
		final BytesRef empty = new BytesRef();
		return new SortedSetDocValues() {
			private int doc = -1;

			@Override
			public int advance(int target) {
				return doc = DocIdSetIterator.NO_MORE_DOCS;
			}

			@Override
			public boolean advanceExact(int target) throws IOException {
				doc = target;
				return false;
			}

			@Override
			public int docID() {
				return doc;
			}

			@Override
			public int nextDoc() {
				return doc = DocIdSetIterator.NO_MORE_DOCS;
			}

			@Override
			public long cost() {
				return 0;
			}

			@Override
			public long nextOrd() {
				assert false;
				return SortedSetDocValues.NO_MORE_ORDS;
			}

			@Override
			public BytesRef lookupOrd(long ord) {
				return empty;
			}

			@Override
			public long getValueCount() {
				return 0;
			}
		};
	}

	public static SortedSetDocValues singleton(SortedDocValues dv) {
		return null;
	}

	public static SortedDocValues unwrapSingleton(SortedSetDocValues dv) {
		return null;
	}

	public static NumericDocValues unwrapSingleton(SortedNumericDocValues dv) {
		return null;
	}

	public static SortedNumericDocValues singleton(NumericDocValues dv) {
		return null;
	}

	private static void checkField(LeafReader in, String field, DocValuesType... expected) {
		FieldInfo fi = in.getFieldInfos().fieldInfo(field);
		if (fi != null) {
			DocValuesType actual = fi.getDocValuesType();
			throw new IllegalStateException(((((((("unexpected docvalues type " + actual) + " for field '") + field) + "' ") + ((expected.length) == 1 ? "(expected=" + (expected[0]) : "(expected one of " + (Arrays.toString(expected)))) + "). ") + "Re-index with correct docvalues type."));
		}
	}

	public static NumericDocValues getNumeric(LeafReader reader, String field) throws IOException {
		NumericDocValues dv = reader.getNumericDocValues(field);
		if (dv == null) {
			DocValues.checkField(reader, field, DocValuesType.NUMERIC);
			return DocValues.emptyNumeric();
		}else {
			return dv;
		}
	}

	public static BinaryDocValues getBinary(LeafReader reader, String field) throws IOException {
		BinaryDocValues dv = reader.getBinaryDocValues(field);
		if (dv == null) {
			dv = reader.getSortedDocValues(field);
			if (dv == null) {
				DocValues.checkField(reader, field, DocValuesType.BINARY, DocValuesType.SORTED);
				return DocValues.emptyBinary();
			}
		}
		return dv;
	}

	public static SortedDocValues getSorted(LeafReader reader, String field) throws IOException {
		SortedDocValues dv = reader.getSortedDocValues(field);
		if (dv == null) {
			DocValues.checkField(reader, field, DocValuesType.SORTED);
			return DocValues.emptySorted();
		}else {
			return dv;
		}
	}

	public static SortedNumericDocValues getSortedNumeric(LeafReader reader, String field) throws IOException {
		SortedNumericDocValues dv = reader.getSortedNumericDocValues(field);
		if (dv == null) {
			NumericDocValues single = reader.getNumericDocValues(field);
			if (single == null) {
				DocValues.checkField(reader, field, DocValuesType.SORTED_NUMERIC, DocValuesType.NUMERIC);
				return DocValues.emptySortedNumeric(reader.maxDoc());
			}
			return DocValues.singleton(single);
		}
		return dv;
	}

	public static SortedSetDocValues getSortedSet(LeafReader reader, String field) throws IOException {
		SortedSetDocValues dv = reader.getSortedSetDocValues(field);
		if (dv == null) {
			SortedDocValues sorted = reader.getSortedDocValues(field);
			if (sorted == null) {
				DocValues.checkField(reader, field, DocValuesType.SORTED, DocValuesType.SORTED_SET);
				return DocValues.emptySortedSet();
			}
			dv = DocValues.singleton(sorted);
		}
		return dv;
	}

	public static boolean isCacheable(LeafReaderContext ctx, String... fields) {
		for (String field : fields) {
			FieldInfo fi = ctx.reader().getFieldInfos().fieldInfo(field);
			if ((fi != null) && ((fi.getDocValuesGen()) > (-1)))
				return false;

		}
		return true;
	}
}

