

import java.io.IOException;
import org.apache.lucene.geo.GeoEncodingUtils;
import org.apache.lucene.geo.GeoUtils;
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
import org.apache.lucene.search.ScorerSupplier;
import org.apache.lucene.search.Weight;
import org.apache.lucene.util.BitSetIterator;
import org.apache.lucene.util.DocIdSetBuilder;
import org.apache.lucene.util.FixedBitSet;
import org.apache.lucene.util.NumericUtils;
import org.apache.lucene.util.StringHelper;

import static org.apache.lucene.index.PointValues.Relation.CELL_INSIDE_QUERY;
import static org.apache.lucene.index.PointValues.Relation.CELL_OUTSIDE_QUERY;


final class LatLonPointDistanceQuery extends Query {
	final String field;

	final double latitude;

	final double longitude;

	final double radiusMeters;

	public LatLonPointDistanceQuery(String field, double latitude, double longitude, double radiusMeters) {
		if (field == null) {
			throw new IllegalArgumentException("field must not be null");
		}
		if (((Double.isFinite(radiusMeters)) == false) || (radiusMeters < 0)) {
			throw new IllegalArgumentException((("radiusMeters: '" + radiusMeters) + "' is invalid"));
		}
		GeoUtils.checkLatitude(latitude);
		GeoUtils.checkLongitude(longitude);
		this.field = field;
		this.latitude = latitude;
		this.longitude = longitude;
		this.radiusMeters = radiusMeters;
	}

	@Override
	public Weight createWeight(IndexSearcher searcher, boolean needsScores, float boost) throws IOException {
		Rectangle box = Rectangle.fromPointDistance(latitude, longitude, radiusMeters);
		final byte[] minLat = new byte[Integer.BYTES];
		final byte[] maxLat = new byte[Integer.BYTES];
		final byte[] minLon = new byte[Integer.BYTES];
		final byte[] maxLon = new byte[Integer.BYTES];
		final byte[] minLon2 = new byte[Integer.BYTES];
		NumericUtils.intToSortableBytes(GeoEncodingUtils.encodeLatitude(box.minLat), minLat, 0);
		NumericUtils.intToSortableBytes(GeoEncodingUtils.encodeLatitude(box.maxLat), maxLat, 0);
		if (box.crossesDateline()) {
			NumericUtils.intToSortableBytes(Integer.MIN_VALUE, minLon, 0);
			NumericUtils.intToSortableBytes(GeoEncodingUtils.encodeLongitude(box.maxLon), maxLon, 0);
			NumericUtils.intToSortableBytes(GeoEncodingUtils.encodeLongitude(box.minLon), minLon2, 0);
		}else {
			NumericUtils.intToSortableBytes(GeoEncodingUtils.encodeLongitude(box.minLon), minLon, 0);
			NumericUtils.intToSortableBytes(GeoEncodingUtils.encodeLongitude(box.maxLon), maxLon, 0);
			NumericUtils.intToSortableBytes(Integer.MAX_VALUE, minLon2, 0);
		}
		final double sortKey = GeoUtils.distanceQuerySortKey(radiusMeters);
		final double axisLat = Rectangle.axisLat(latitude, radiusMeters);
		return new ConstantScoreWeight(this, boost) {
			final GeoEncodingUtils.DistancePredicate distancePredicate = GeoEncodingUtils.createDistancePredicate(latitude, longitude, radiusMeters);

			@Override
			public Scorer scorer(LeafReaderContext context) throws IOException {
				ScorerSupplier scorerSupplier = scorerSupplier(context);
				if (scorerSupplier == null) {
					return null;
				}
				return scorerSupplier.get(Long.MAX_VALUE);
			}

			@Override
			public boolean isCacheable(LeafReaderContext ctx) {
				return true;
			}

			@Override
			public ScorerSupplier scorerSupplier(LeafReaderContext context) throws IOException {
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
				final PointValues.IntersectVisitor visitor = getIntersectVisitor(result);
				final Weight weight = this;
				return new ScorerSupplier() {
					long cost = -1;

					@Override
					public Scorer get(long leadCost) throws IOException {
						if ((((values.getDocCount()) == (reader.maxDoc())) && ((values.getDocCount()) == (values.size()))) && ((cost()) > ((reader.maxDoc()) / 2))) {
							final FixedBitSet result = new FixedBitSet(reader.maxDoc());
							result.set(0, reader.maxDoc());
							int[] cost = new int[]{ reader.maxDoc() };
							values.intersect(getInverseIntersectVisitor(result, cost));
							final DocIdSetIterator iterator = new BitSetIterator(result, cost[0]);
							return new ConstantScoreScorer(weight, score(), iterator);
						}
						values.intersect(visitor);
						return new ConstantScoreScorer(weight, score(), result.build().iterator());
					}

					@Override
					public long cost() {
						if ((cost) == (-1)) {
							cost = values.estimatePointCount(visitor);
						}
						assert (cost) >= 0;
						return cost;
					}
				};
			}

			private PointValues.IntersectVisitor getIntersectVisitor(DocIdSetBuilder result) {
				return new PointValues.IntersectVisitor() {
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
						if (((StringHelper.compare(Integer.BYTES, packedValue, 0, maxLat, 0)) > 0) || ((StringHelper.compare(Integer.BYTES, packedValue, 0, minLat, 0)) < 0)) {
							return;
						}
						if ((((StringHelper.compare(Integer.BYTES, packedValue, Integer.BYTES, maxLon, 0)) > 0) || ((StringHelper.compare(Integer.BYTES, packedValue, Integer.BYTES, minLon, 0)) < 0)) && ((StringHelper.compare(Integer.BYTES, packedValue, Integer.BYTES, minLon2, 0)) < 0)) {
							return;
						}
						int docLatitude = NumericUtils.sortableBytesToInt(packedValue, 0);
						int docLongitude = NumericUtils.sortableBytesToInt(packedValue, Integer.BYTES);
						if (distancePredicate.test(docLatitude, docLongitude)) {
							adder.add(docID);
						}
					}

					@Override
					public PointValues.Relation compare(byte[] minPackedValue, byte[] maxPackedValue) {
						if (((StringHelper.compare(Integer.BYTES, minPackedValue, 0, maxLat, 0)) > 0) || ((StringHelper.compare(Integer.BYTES, maxPackedValue, 0, minLat, 0)) < 0)) {
							return CELL_OUTSIDE_QUERY;
						}
						if ((((StringHelper.compare(Integer.BYTES, minPackedValue, Integer.BYTES, maxLon, 0)) > 0) || ((StringHelper.compare(Integer.BYTES, maxPackedValue, Integer.BYTES, minLon, 0)) < 0)) && ((StringHelper.compare(Integer.BYTES, maxPackedValue, Integer.BYTES, minLon2, 0)) < 0)) {
							return CELL_OUTSIDE_QUERY;
						}
						double latMin = GeoEncodingUtils.decodeLatitude(minPackedValue, 0);
						double lonMin = GeoEncodingUtils.decodeLongitude(minPackedValue, Integer.BYTES);
						double latMax = GeoEncodingUtils.decodeLatitude(maxPackedValue, 0);
						double lonMax = GeoEncodingUtils.decodeLongitude(maxPackedValue, Integer.BYTES);
						return GeoUtils.relate(latMin, latMax, lonMin, lonMax, latitude, longitude, sortKey, axisLat);
					}
				};
			}

			private PointValues.IntersectVisitor getInverseIntersectVisitor(FixedBitSet result, int[] cost) {
				return new PointValues.IntersectVisitor() {
					@Override
					public void visit(int docID) {
						result.clear(docID);
						(cost[0])--;
					}

					@Override
					public void visit(int docID, byte[] packedValue) {
						if (((StringHelper.compare(Integer.BYTES, packedValue, 0, maxLat, 0)) > 0) || ((StringHelper.compare(Integer.BYTES, packedValue, 0, minLat, 0)) < 0)) {
							result.clear(docID);
							(cost[0])--;
							return;
						}
						if ((((StringHelper.compare(Integer.BYTES, packedValue, Integer.BYTES, maxLon, 0)) > 0) || ((StringHelper.compare(Integer.BYTES, packedValue, Integer.BYTES, minLon, 0)) < 0)) && ((StringHelper.compare(Integer.BYTES, packedValue, Integer.BYTES, minLon2, 0)) < 0)) {
							result.clear(docID);
							(cost[0])--;
							return;
						}
						int docLatitude = NumericUtils.sortableBytesToInt(packedValue, 0);
						int docLongitude = NumericUtils.sortableBytesToInt(packedValue, Integer.BYTES);
						if (!(distancePredicate.test(docLatitude, docLongitude))) {
							result.clear(docID);
							(cost[0])--;
						}
					}

					@Override
					public PointValues.Relation compare(byte[] minPackedValue, byte[] maxPackedValue) {
						if (((StringHelper.compare(Integer.BYTES, minPackedValue, 0, maxLat, 0)) > 0) || ((StringHelper.compare(Integer.BYTES, maxPackedValue, 0, minLat, 0)) < 0)) {
							return CELL_INSIDE_QUERY;
						}
						if ((((StringHelper.compare(Integer.BYTES, minPackedValue, Integer.BYTES, maxLon, 0)) > 0) || ((StringHelper.compare(Integer.BYTES, maxPackedValue, Integer.BYTES, minLon, 0)) < 0)) && ((StringHelper.compare(Integer.BYTES, maxPackedValue, Integer.BYTES, minLon2, 0)) < 0)) {
							return CELL_INSIDE_QUERY;
						}
						double latMin = GeoEncodingUtils.decodeLatitude(minPackedValue, 0);
						double lonMin = GeoEncodingUtils.decodeLongitude(minPackedValue, Integer.BYTES);
						double latMax = GeoEncodingUtils.decodeLatitude(maxPackedValue, 0);
						double lonMax = GeoEncodingUtils.decodeLongitude(maxPackedValue, Integer.BYTES);
						PointValues.Relation relation = GeoUtils.relate(latMin, latMax, lonMin, lonMax, latitude, longitude, sortKey, axisLat);
						switch (relation) {
							case CELL_INSIDE_QUERY :
								return CELL_OUTSIDE_QUERY;
							case CELL_OUTSIDE_QUERY :
								return CELL_INSIDE_QUERY;
							default :
								return relation;
						}
					}
				};
			}
		};
	}

	public String getField() {
		return field;
	}

	public double getLatitude() {
		return latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public double getRadiusMeters() {
		return radiusMeters;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = classHash();
		result = (prime * result) + (field.hashCode());
		long temp;
		temp = Double.doubleToLongBits(latitude);
		result = (prime * result) + ((int) (temp ^ (temp >>> 32)));
		temp = Double.doubleToLongBits(longitude);
		result = (prime * result) + ((int) (temp ^ (temp >>> 32)));
		temp = Double.doubleToLongBits(radiusMeters);
		result = (prime * result) + ((int) (temp ^ (temp >>> 32)));
		return result;
	}

	@Override
	public boolean equals(Object other) {
		return (sameClassAs(other)) && (equalsTo(getClass().cast(other)));
	}

	private boolean equalsTo(LatLonPointDistanceQuery other) {
		return (((field.equals(other.field)) && ((Double.doubleToLongBits(latitude)) == (Double.doubleToLongBits(other.latitude)))) && ((Double.doubleToLongBits(longitude)) == (Double.doubleToLongBits(other.longitude)))) && ((Double.doubleToLongBits(radiusMeters)) == (Double.doubleToLongBits(other.radiusMeters)));
	}

	@Override
	public String toString(String field) {
		StringBuilder sb = new StringBuilder();
		if (!(this.field.equals(field))) {
			sb.append(this.field);
			sb.append(':');
		}
		sb.append(latitude);
		sb.append(",");
		sb.append(longitude);
		sb.append(" +/- ");
		sb.append(radiusMeters);
		sb.append(" meters");
		return sb.toString();
	}
}

