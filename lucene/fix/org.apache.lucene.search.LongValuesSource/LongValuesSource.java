

import java.io.IOException;
import java.util.Objects;
import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.search.DoubleValues;
import org.apache.lucene.search.DoubleValuesSource;
import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.FieldComparatorSource;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.LongValues;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.SegmentCacheable;
import org.apache.lucene.search.SortField;


public abstract class LongValuesSource implements SegmentCacheable {
	public abstract LongValues getValues(LeafReaderContext ctx, DoubleValues scores) throws IOException;

	public abstract boolean needsScores();

	@Override
	public abstract int hashCode();

	@Override
	public abstract boolean equals(Object obj);

	@Override
	public abstract String toString();

	public abstract LongValuesSource rewrite(IndexSearcher searcher) throws IOException;

	public SortField getSortField(boolean reverse) {
		return new LongValuesSource.LongValuesSortField(this, reverse);
	}

	public DoubleValuesSource toDoubleValuesSource() {
		return new LongValuesSource.DoubleLongValuesSource(this);
	}

	private static class DoubleLongValuesSource extends DoubleValuesSource {
		private final LongValuesSource inner;

		private DoubleLongValuesSource(LongValuesSource inner) {
			this.inner = inner;
		}

		@Override
		public DoubleValues getValues(LeafReaderContext ctx, DoubleValues scores) throws IOException {
			LongValues v = inner.getValues(ctx, scores);
			return new DoubleValues() {
				@Override
				public double doubleValue() throws IOException {
					return ((double) (v.longValue()));
				}

				@Override
				public boolean advanceExact(int doc) throws IOException {
					return v.advanceExact(doc);
				}
			};
		}

		@Override
		public DoubleValuesSource rewrite(IndexSearcher searcher) throws IOException {
			return inner.rewrite(searcher).toDoubleValuesSource();
		}

		@Override
		public boolean isCacheable(LeafReaderContext ctx) {
			return inner.isCacheable(ctx);
		}

		@Override
		public String toString() {
			return ("double(" + (inner.toString())) + ")";
		}

		@Override
		public boolean needsScores() {
			return inner.needsScores();
		}

		@Override
		public boolean equals(Object o) {
			if ((this) == o)
				return true;

			if ((o == null) || ((getClass()) != (o.getClass())))
				return false;

			LongValuesSource.DoubleLongValuesSource that = ((LongValuesSource.DoubleLongValuesSource) (o));
			return Objects.equals(inner, that.inner);
		}

		@Override
		public int hashCode() {
			return Objects.hash(inner);
		}
	}

	public static LongValuesSource fromLongField(String field) {
		return new LongValuesSource.FieldValuesSource(field);
	}

	public static LongValuesSource fromIntField(String field) {
		return LongValuesSource.fromLongField(field);
	}

	public static LongValuesSource constant(long value) {
		return new LongValuesSource.ConstantLongValuesSource(value);
	}

	private static class ConstantLongValuesSource extends LongValuesSource {
		private final long value;

		private ConstantLongValuesSource(long value) {
			this.value = value;
		}

		@Override
		public LongValues getValues(LeafReaderContext ctx, DoubleValues scores) throws IOException {
			return new LongValues() {
				@Override
				public long longValue() throws IOException {
					return value;
				}

				@Override
				public boolean advanceExact(int doc) throws IOException {
					return true;
				}
			};
		}

		@Override
		public boolean isCacheable(LeafReaderContext ctx) {
			return true;
		}

		@Override
		public boolean needsScores() {
			return false;
		}

		@Override
		public int hashCode() {
			return Objects.hash(value);
		}

		@Override
		public boolean equals(Object o) {
			if ((this) == o)
				return true;

			if ((o == null) || ((getClass()) != (o.getClass())))
				return false;

			LongValuesSource.ConstantLongValuesSource that = ((LongValuesSource.ConstantLongValuesSource) (o));
			return (value) == (that.value);
		}

		@Override
		public String toString() {
			return ("constant(" + (value)) + ")";
		}

		@Override
		public LongValuesSource rewrite(IndexSearcher searcher) throws IOException {
			return this;
		}
	}

	private static class FieldValuesSource extends LongValuesSource {
		final String field;

		private FieldValuesSource(String field) {
			this.field = field;
		}

		@Override
		public boolean equals(Object o) {
			if ((this) == o)
				return true;

			if ((o == null) || ((getClass()) != (o.getClass())))
				return false;

			LongValuesSource.FieldValuesSource that = ((LongValuesSource.FieldValuesSource) (o));
			return Objects.equals(field, that.field);
		}

		@Override
		public String toString() {
			return ("long(" + (field)) + ")";
		}

		@Override
		public int hashCode() {
			return Objects.hash(field);
		}

		@Override
		public LongValues getValues(LeafReaderContext ctx, DoubleValues scores) throws IOException {
			final NumericDocValues values = DocValues.getNumeric(ctx.reader(), field);
			return LongValuesSource.toLongValues(values);
		}

		@Override
		public boolean isCacheable(LeafReaderContext ctx) {
			return DocValues.isCacheable(ctx, field);
		}

		@Override
		public boolean needsScores() {
			return false;
		}

		@Override
		public LongValuesSource rewrite(IndexSearcher searcher) throws IOException {
			return this;
		}
	}

	private static class LongValuesSortField extends SortField {
		final LongValuesSource producer;

		public LongValuesSortField(LongValuesSource producer, boolean reverse) {
			super(producer.toString(), new LongValuesSource.LongValuesComparatorSource(producer), reverse);
			this.producer = producer;
		}

		@Override
		public boolean needsScores() {
			return producer.needsScores();
		}

		@Override
		public String toString() {
			StringBuilder buffer = new StringBuilder("<");
			buffer.append(getField()).append(">");
			return buffer.toString();
		}

		@Override
		public SortField rewrite(IndexSearcher searcher) throws IOException {
			return null;
		}
	}

	private static class LongValuesHolder {
		LongValues values;
	}

	private static class LongValuesComparatorSource extends FieldComparatorSource {
		private final LongValuesSource producer;

		public LongValuesComparatorSource(LongValuesSource producer) {
			this.producer = producer;
		}

		@Override
		public FieldComparator<Long> newComparator(String fieldname, int numHits, int sortPos, boolean reversed) {
			return new FieldComparator.LongComparator(numHits, fieldname, 0L) {
				LeafReaderContext ctx;

				LongValuesSource.LongValuesHolder holder = new LongValuesSource.LongValuesHolder();

				@Override
				protected NumericDocValues getNumericDocValues(LeafReaderContext context, String field) throws IOException {
					ctx = context;
					return LongValuesSource.asNumericDocValues(holder);
				}

				@Override
				public void setScorer(Scorer scorer) throws IOException {
					holder.values = producer.getValues(ctx, DoubleValuesSource.fromScorer(scorer));
				}
			};
		}
	}

	private static LongValues toLongValues(NumericDocValues in) {
		return new LongValues() {
			@Override
			public long longValue() throws IOException {
				return in.longValue();
			}

			@Override
			public boolean advanceExact(int target) throws IOException {
				return in.advanceExact(target);
			}
		};
	}

	private static NumericDocValues asNumericDocValues(LongValuesSource.LongValuesHolder in) {
		return new NumericDocValues() {
			@Override
			public long longValue() throws IOException {
				return in.values.longValue();
			}

			@Override
			public boolean advanceExact(int target) throws IOException {
				return in.values.advanceExact(target);
			}

			@Override
			public int docID() {
				throw new UnsupportedOperationException();
			}

			@Override
			public int nextDoc() throws IOException {
				throw new UnsupportedOperationException();
			}

			@Override
			public int advance(int target) throws IOException {
				throw new UnsupportedOperationException();
			}

			@Override
			public long cost() {
				throw new UnsupportedOperationException();
			}
		};
	}
}

