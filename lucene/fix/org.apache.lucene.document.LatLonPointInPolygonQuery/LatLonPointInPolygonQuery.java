

import java.io.IOException;
import java.util.Arrays;
import org.apache.lucene.geo.GeoEncodingUtils;
import org.apache.lucene.geo.Polygon;
import org.apache.lucene.geo.Polygon2D;
import org.apache.lucene.geo.Rectangle;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.FieldInfos;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.PointValues;
import org.apache.lucene.search.ConstantScoreScorer;
import org.apache.lucene.search.ConstantScoreWeight;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;
import org.apache.lucene.util.DocIdSetBuilder;
import org.apache.lucene.util.NumericUtils;
import org.apache.lucene.util.StringHelper;

import static org.apache.lucene.index.PointValues.Relation.CELL_OUTSIDE_QUERY;


final class LatLonPointInPolygonQuery extends Query {
	final String field;

	final Polygon[] polygons;

	LatLonPointInPolygonQuery(String field, Polygon[] polygons) {
		if (field == null) {
			throw new IllegalArgumentException("field must not be null");
		}
		if (polygons == null) {
			throw new IllegalArgumentException("polygons must not be null");
		}
		if ((polygons.length) == 0) {
			throw new IllegalArgumentException("polygons must not be empty");
		}
		for (int i = 0; i < (polygons.length); i++) {
			if ((polygons[i]) == null) {
				throw new IllegalArgumentException((("polygon[" + i) + "] must not be null"));
			}
		}
		this.field = field;
		this.polygons = polygons.clone();
	}

	@Override
	public Weight createWeight(IndexSearcher searcher, boolean needsScores, float boost) throws IOException {
		final Rectangle box = Rectangle.fromPolygon(polygons);
		final byte[] minLat = new byte[Integer.BYTES];
		final byte[] maxLat = new byte[Integer.BYTES];
		final byte[] minLon = new byte[Integer.BYTES];
		final byte[] maxLon = new byte[Integer.BYTES];
		NumericUtils.intToSortableBytes(GeoEncodingUtils.encodeLatitude(box.minLat), minLat, 0);
		NumericUtils.intToSortableBytes(GeoEncodingUtils.encodeLatitude(box.maxLat), maxLat, 0);
		NumericUtils.intToSortableBytes(GeoEncodingUtils.encodeLongitude(box.minLon), minLon, 0);
		NumericUtils.intToSortableBytes(GeoEncodingUtils.encodeLongitude(box.maxLon), maxLon, 0);
		final Polygon2D tree = Polygon2D.create(polygons);
		final GeoEncodingUtils.PolygonPredicate polygonPredicate = GeoEncodingUtils.createPolygonPredicate(polygons, tree);
		return new ConstantScoreWeight(this, boost) {
			@Override
			public Scorer scorer(LeafReaderContext context) throws IOException {
				LeafReader reader = context.reader();
				PointValues values = reader.getPointValues(field);
				if (values == null) {
					return null;
				}
				FieldInfo fieldInfo = reader.getFieldInfos().fieldInfo(field);
				if (fieldInfo == null) {
					return null;
				}
				DocIdSetBuilder result = new DocIdSetBuilder(reader.maxDoc(), values, field);
				values.intersect(new PointValues.IntersectVisitor() {
					DocIdSetBuilder.BulkAdder adder;

					@Override
					public void grow(int count) {
						adder = result.grow(count);
					}

					@Override
					public void visit(int docID) {
						adder.add(docID);
					}

					@Override
					public void visit(int docID, byte[] packedValue) {
						if (polygonPredicate.test(NumericUtils.sortableBytesToInt(packedValue, 0), NumericUtils.sortableBytesToInt(packedValue, Integer.BYTES))) {
							adder.add(docID);
						}
					}

					@Override
					public PointValues.Relation compare(byte[] minPackedValue, byte[] maxPackedValue) {
						if (((((StringHelper.compare(Integer.BYTES, minPackedValue, 0, maxLat, 0)) > 0) || ((StringHelper.compare(Integer.BYTES, maxPackedValue, 0, minLat, 0)) < 0)) || ((StringHelper.compare(Integer.BYTES, minPackedValue, Integer.BYTES, maxLon, 0)) > 0)) || ((StringHelper.compare(Integer.BYTES, maxPackedValue, Integer.BYTES, minLon, 0)) < 0)) {
							return CELL_OUTSIDE_QUERY;
						}
						double cellMinLat = GeoEncodingUtils.decodeLatitude(minPackedValue, 0);
						double cellMinLon = GeoEncodingUtils.decodeLongitude(minPackedValue, Integer.BYTES);
						double cellMaxLat = GeoEncodingUtils.decodeLatitude(maxPackedValue, 0);
						double cellMaxLon = GeoEncodingUtils.decodeLongitude(maxPackedValue, Integer.BYTES);
						return tree.relate(cellMinLat, cellMaxLat, cellMinLon, cellMaxLon);
					}
				});
				return new ConstantScoreScorer(this, score(), result.build().iterator());
			}

			@Override
			public boolean isCacheable(LeafReaderContext ctx) {
				return true;
			}
		};
	}

	public String getField() {
		return field;
	}

	public Polygon[] getPolygons() {
		return polygons.clone();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = classHash();
		result = (prime * result) + (field.hashCode());
		result = (prime * result) + (Arrays.hashCode(polygons));
		return result;
	}

	@Override
	public boolean equals(Object other) {
		return (sameClassAs(other)) && (equalsTo(getClass().cast(other)));
	}

	private boolean equalsTo(LatLonPointInPolygonQuery other) {
		return (field.equals(other.field)) && (Arrays.equals(polygons, other.polygons));
	}

	@Override
	public String toString(String field) {
		final StringBuilder sb = new StringBuilder();
		sb.append(getClass().getSimpleName());
		sb.append(':');
		if ((this.field.equals(field)) == false) {
			sb.append(" field=");
			sb.append(this.field);
			sb.append(':');
		}
		sb.append(Arrays.toString(polygons));
		return sb.toString();
	}
}

