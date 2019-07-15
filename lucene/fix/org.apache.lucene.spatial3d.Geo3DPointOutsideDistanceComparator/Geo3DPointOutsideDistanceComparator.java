

import java.io.IOException;
import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.FieldInfos;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.SortedNumericDocValues;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.LeafFieldComparator;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.spatial3d.Geo3DDocValuesField;
import org.apache.lucene.spatial3d.geom.ArcDistance;
import org.apache.lucene.spatial3d.geom.DistanceStyle;
import org.apache.lucene.spatial3d.geom.GeoOutsideDistance;
import org.apache.lucene.spatial3d.geom.PlanetModel;


class Geo3DPointOutsideDistanceComparator extends FieldComparator<Double> implements LeafFieldComparator {
	final String field;

	final GeoOutsideDistance distanceShape;

	final double[] values;

	double bottomDistance;

	double topValue;

	SortedNumericDocValues currentDocs;

	public Geo3DPointOutsideDistanceComparator(String field, final GeoOutsideDistance distanceShape, int numHits) {
		this.field = field;
		this.distanceShape = distanceShape;
		this.values = new double[numHits];
	}

	@Override
	public void setScorer(Scorer scorer) {
	}

	@Override
	public int compare(int slot1, int slot2) {
		return Double.compare(values[slot1], values[slot2]);
	}

	@Override
	public void setBottom(int slot) {
		bottomDistance = values[slot];
	}

	@Override
	public void setTopValue(Double value) {
		topValue = value.doubleValue();
	}

	@Override
	public int compareBottom(int doc) throws IOException {
		if (doc > (currentDocs.docID())) {
			currentDocs.advance(doc);
		}
		if (doc < (currentDocs.docID())) {
			return Double.compare(bottomDistance, Double.POSITIVE_INFINITY);
		}
		int numValues = currentDocs.docValueCount();
		assert numValues > 0;
		int cmp = -1;
		for (int i = 0; i < numValues; i++) {
			long encoded = currentDocs.nextValue();
			final double x = Geo3DDocValuesField.decodeXValue(encoded);
			final double y = Geo3DDocValuesField.decodeYValue(encoded);
			final double z = Geo3DDocValuesField.decodeZValue(encoded);
			cmp = Math.max(cmp, Double.compare(bottomDistance, distanceShape.computeOutsideDistance(DistanceStyle.ARC, x, y, z)));
		}
		return cmp;
	}

	@Override
	public void copy(int slot, int doc) throws IOException {
		values[slot] = computeMinimumDistance(doc);
	}

	@Override
	public LeafFieldComparator getLeafComparator(LeafReaderContext context) throws IOException {
		LeafReader reader = context.reader();
		FieldInfo info = reader.getFieldInfos().fieldInfo(field);
		if (info != null) {
		}
		currentDocs = DocValues.getSortedNumeric(reader, field);
		return this;
	}

	@Override
	public Double value(int slot) {
		return Double.valueOf(((values[slot]) * (PlanetModel.WGS84_MEAN)));
	}

	@Override
	public int compareTop(int doc) throws IOException {
		return Double.compare(topValue, computeMinimumDistance(doc));
	}

	double computeMinimumDistance(final int doc) throws IOException {
		if (doc > (currentDocs.docID())) {
			currentDocs.advance(doc);
		}
		double minValue = Double.POSITIVE_INFINITY;
		if (doc == (currentDocs.docID())) {
			final int numValues = currentDocs.docValueCount();
			for (int i = 0; i < numValues; i++) {
				final long encoded = currentDocs.nextValue();
				final double distance = distanceShape.computeOutsideDistance(DistanceStyle.ARC, Geo3DDocValuesField.decodeXValue(encoded), Geo3DDocValuesField.decodeYValue(encoded), Geo3DDocValuesField.decodeZValue(encoded));
				minValue = Math.min(minValue, distance);
			}
		}
		return minValue;
	}
}
