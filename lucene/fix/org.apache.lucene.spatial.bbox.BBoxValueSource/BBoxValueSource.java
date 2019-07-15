

import java.io.IOException;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.spatial.ShapeValues;
import org.apache.lucene.spatial.ShapeValuesSource;
import org.apache.lucene.spatial.SpatialStrategy;
import org.apache.lucene.spatial.bbox.BBoxStrategy;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.shape.Rectangle;
import org.locationtech.spatial4j.shape.Shape;


class BBoxValueSource extends ShapeValuesSource {
	private final BBoxStrategy strategy;

	public BBoxValueSource(BBoxStrategy strategy) {
		this.strategy = strategy;
	}

	@Override
	public String toString() {
		return ("bboxShape(" + (strategy.getFieldName())) + ")";
	}

	@Override
	public ShapeValues getValues(LeafReaderContext readerContext) throws IOException {
		LeafReader reader = readerContext.reader();
		final Rectangle rect = strategy.getSpatialContext().makeRectangle(0, 0, 0, 0);
		return new ShapeValues() {
			@Override
			public boolean advanceExact(int doc) throws IOException {
				return false;
			}

			@Override
			public Shape value() throws IOException {
				return rect;
			}
		};
	}

	@Override
	public boolean isCacheable(LeafReaderContext ctx) {
		return false;
	}

	@Override
	public boolean equals(Object o) {
		if ((this) == o)
			return true;

		if ((o == null) || ((getClass()) != (o.getClass())))
			return false;

		BBoxValueSource that = ((BBoxValueSource) (o));
		if (!(strategy.equals(that.strategy)))
			return false;

		return true;
	}

	@Override
	public int hashCode() {
		return strategy.hashCode();
	}
}

