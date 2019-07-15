

import java.io.IOException;
import org.apache.lucene.geo.GeoEncodingUtils;
import org.apache.lucene.geo.Rectangle;
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
import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.RamUsageEstimator;
import org.apache.lucene.util.SloppyMath;


class LatLonPointDistanceComparator extends FieldComparator<Double> implements LeafFieldComparator {
	final String field;

	final double latitude;

	final double longitude;

	final double[] values;

	double bottom;

	double topValue;

	SortedNumericDocValues currentDocs;

	int minLon = Integer.MIN_VALUE;

	int maxLon = Integer.MAX_VALUE;

	int minLat = Integer.MIN_VALUE;

	int maxLat = Integer.MAX_VALUE;

	int minLon2 = Integer.MAX_VALUE;

	int setBottomCounter = 0;

	private long[] currentValues = new long[4];

	private int valuesDocID = -1;

	public LatLonPointDistanceComparator(String field, double latitude, double longitude, int numHits) {
		this.field = field;
		this.latitude = latitude;
		this.longitude = longitude;
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
		bottom = values[slot];
		if (((setBottomCounter) < 1024) || (((setBottomCounter) & 63) == 63)) {
			Rectangle box = Rectangle.fromPointDistance(latitude, longitude, LatLonPointDistanceComparator.haversin2(bottom));
			minLat = GeoEncodingUtils.encodeLatitude(box.minLat);
			maxLat = GeoEncodingUtils.encodeLatitude(box.maxLat);
			if (box.crossesDateline()) {
				minLon = Integer.MIN_VALUE;
				maxLon = GeoEncodingUtils.encodeLongitude(box.maxLon);
				minLon2 = GeoEncodingUtils.encodeLongitude(box.minLon);
			}else {
				minLon = GeoEncodingUtils.encodeLongitude(box.minLon);
				maxLon = GeoEncodingUtils.encodeLongitude(box.maxLon);
				minLon2 = Integer.MAX_VALUE;
			}
		}
		(setBottomCounter)++;
	}

	@Override
	public void setTopValue(Double value) {
		topValue = value.doubleValue();
	}

	private void setValues() throws IOException {
		if ((valuesDocID) != (currentDocs.docID())) {
			assert (valuesDocID) < (currentDocs.docID()) : ((" valuesDocID=" + (valuesDocID)) + " vs ") + (currentDocs.docID());
			valuesDocID = currentDocs.docID();
			int count = currentDocs.docValueCount();
			if (count > (currentValues.length)) {
				currentValues = new long[ArrayUtil.oversize(count, RamUsageEstimator.NUM_BYTES_LONG)];
			}
			for (int i = 0; i < count; i++) {
				currentValues[i] = currentDocs.nextValue();
			}
		}
	}

	@Override
	public int compareBottom(int doc) throws IOException {
		if (doc > (currentDocs.docID())) {
			currentDocs.advance(doc);
		}
		if (doc < (currentDocs.docID())) {
			return Double.compare(bottom, Double.POSITIVE_INFINITY);
		}
		setValues();
		int numValues = currentDocs.docValueCount();
		int cmp = -1;
		for (int i = 0; i < numValues; i++) {
			long encoded = currentValues[i];
			int latitudeBits = ((int) (encoded >> 32));
			if ((latitudeBits < (minLat)) || (latitudeBits > (maxLat))) {
				continue;
			}
			int longitudeBits = ((int) (encoded & -1));
			if (((longitudeBits < (minLon)) || (longitudeBits > (maxLon))) && (longitudeBits < (minLon2))) {
				continue;
			}
			double docLatitude = GeoEncodingUtils.decodeLatitude(latitudeBits);
			double docLongitude = GeoEncodingUtils.decodeLongitude(longitudeBits);
			cmp = Math.max(cmp, Double.compare(bottom, SloppyMath.haversinSortKey(latitude, longitude, docLatitude, docLongitude)));
			if (cmp > 0) {
				return cmp;
			}
		}
		return cmp;
	}

	@Override
	public void copy(int slot, int doc) throws IOException {
		values[slot] = sortKey(doc);
	}

	@Override
	public LeafFieldComparator getLeafComparator(LeafReaderContext context) throws IOException {
		LeafReader reader = context.reader();
		FieldInfo info = reader.getFieldInfos().fieldInfo(field);
		if (info != null) {
		}
		currentDocs = DocValues.getSortedNumeric(reader, field);
		valuesDocID = -1;
		return this;
	}

	@Override
	public Double value(int slot) {
		return Double.valueOf(LatLonPointDistanceComparator.haversin2(values[slot]));
	}

	@Override
	public int compareTop(int doc) throws IOException {
		return Double.compare(topValue, LatLonPointDistanceComparator.haversin2(sortKey(doc)));
	}

	double sortKey(int doc) throws IOException {
		if (doc > (currentDocs.docID())) {
			currentDocs.advance(doc);
		}
		double minValue = Double.POSITIVE_INFINITY;
		if (doc == (currentDocs.docID())) {
			setValues();
			int numValues = currentDocs.docValueCount();
			for (int i = 0; i < numValues; i++) {
				long encoded = currentValues[i];
				double docLatitude = GeoEncodingUtils.decodeLatitude(((int) (encoded >> 32)));
				double docLongitude = GeoEncodingUtils.decodeLongitude(((int) (encoded & -1)));
				minValue = Math.min(minValue, SloppyMath.haversinSortKey(latitude, longitude, docLatitude, docLongitude));
			}
		}
		return minValue;
	}

	static double haversin2(double partial) {
		if (Double.isInfinite(partial)) {
			return partial;
		}
		return SloppyMath.haversinMeters(partial);
	}
}

