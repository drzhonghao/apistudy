

import java.io.IOException;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.DoubleValues;
import org.apache.lucene.search.DoubleValuesSource;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.spatial.SpatialStrategy;
import org.apache.lucene.spatial.vector.PointVectorStrategy;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.distance.DistanceCalculator;
import org.locationtech.spatial4j.shape.Point;
import org.locationtech.spatial4j.shape.Shape;


public class DistanceValueSource extends DoubleValuesSource {
	private PointVectorStrategy strategy;

	private final Point from;

	private final double multiplier;

	private final double nullValue;

	public DistanceValueSource(PointVectorStrategy strategy, Point from, double multiplier) {
		this.strategy = strategy;
		this.from = from;
		this.multiplier = multiplier;
		this.nullValue = 180 * multiplier;
	}

	@Override
	public String toString() {
		return ((("DistanceValueSource(" + (strategy)) + ", ") + (from)) + ")";
	}

	@Override
	public DoubleValues getValues(LeafReaderContext readerContext, DoubleValues scores) throws IOException {
		LeafReader reader = readerContext.reader();
		return DoubleValues.withDefault(new DoubleValues() {
			private final Point from = DistanceValueSource.this.from;

			private final DistanceCalculator calculator = strategy.getSpatialContext().getDistCalc();

			@Override
			public double doubleValue() throws IOException {
				return 0d;
			}

			@Override
			public boolean advanceExact(int doc) throws IOException {
				return false;
			}
		}, nullValue);
	}

	@Override
	public boolean needsScores() {
		return false;
	}

	@Override
	public boolean isCacheable(LeafReaderContext ctx) {
		return false;
	}

	@Override
	public DoubleValuesSource rewrite(IndexSearcher searcher) throws IOException {
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if ((this) == o)
			return true;

		if ((o == null) || ((getClass()) != (o.getClass())))
			return false;

		DistanceValueSource that = ((DistanceValueSource) (o));
		if (!(from.equals(that.from)))
			return false;

		if (!(strategy.equals(that.strategy)))
			return false;

		if ((multiplier) != (that.multiplier))
			return false;

		return true;
	}

	@Override
	public int hashCode() {
		return from.hashCode();
	}
}

