

import java.io.IOException;
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
import org.apache.lucene.spatial3d.geom.BasePlanetObject;
import org.apache.lucene.spatial3d.geom.Bounded;
import org.apache.lucene.spatial3d.geom.GeoShape;
import org.apache.lucene.spatial3d.geom.PlanetModel;
import org.apache.lucene.spatial3d.geom.XYZBounds;
import org.apache.lucene.util.DocIdSetBuilder;


final class PointInGeo3DShapeQuery extends Query {
	final String field;

	final GeoShape shape;

	final XYZBounds shapeBounds;

	public PointInGeo3DShapeQuery(String field, GeoShape shape) {
		this.field = field;
		this.shape = shape;
		this.shapeBounds = new XYZBounds();
		shape.getBounds(shapeBounds);
		if (shape instanceof BasePlanetObject) {
			BasePlanetObject planetObject = ((BasePlanetObject) (shape));
			if ((planetObject.getPlanetModel().equals(PlanetModel.WGS84)) == false) {
				throw new IllegalArgumentException(("this qurey requires PlanetModel.WGS84, but got: " + (planetObject.getPlanetModel())));
			}
		}
	}

	@Override
	public Weight createWeight(IndexSearcher searcher, boolean needsScores, float boost) throws IOException {
		return new ConstantScoreWeight(this, boost) {
			@Override
			public Scorer scorer(LeafReaderContext context) throws IOException {
				LeafReader reader = context.reader();
				PointValues values = reader.getPointValues(field);
				if (values == null) {
					return null;
				}
				DocIdSetBuilder result = new DocIdSetBuilder(reader.maxDoc(), values, field);
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

	public GeoShape getShape() {
		return shape;
	}

	@Override
	public boolean equals(Object other) {
		return (sameClassAs(other)) && (equalsTo(getClass().cast(other)));
	}

	private boolean equalsTo(PointInGeo3DShapeQuery other) {
		return (field.equals(other.field)) && (shape.equals(other.shape));
	}

	@Override
	public int hashCode() {
		int result = classHash();
		result = (31 * result) + (field.hashCode());
		result = (31 * result) + (shape.hashCode());
		return result;
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
		sb.append(" Shape: ");
		sb.append(shape);
		return sb.toString();
	}
}

