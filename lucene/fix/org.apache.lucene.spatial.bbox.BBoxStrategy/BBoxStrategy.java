

import org.apache.lucene.document.DoubleDocValuesField;
import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DocValuesType;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.DoubleValuesSource;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.spatial.ShapeValuesSource;
import org.apache.lucene.spatial.SpatialStrategy;
import org.apache.lucene.spatial.bbox.BBoxOverlapRatioValueSource;
import org.apache.lucene.spatial.query.SpatialArgs;
import org.apache.lucene.spatial.query.SpatialOperation;
import org.apache.lucene.spatial.query.UnsupportedSpatialOperation;
import org.apache.lucene.spatial.util.DistanceToShapeValueSource;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.shape.Point;
import org.locationtech.spatial4j.shape.Rectangle;
import org.locationtech.spatial4j.shape.Shape;

import static org.apache.lucene.search.BooleanClause.Occur.MUST;
import static org.apache.lucene.search.BooleanClause.Occur.MUST_NOT;
import static org.apache.lucene.search.BooleanClause.Occur.SHOULD;


public class BBoxStrategy extends SpatialStrategy {
	public static FieldType DEFAULT_FIELDTYPE;

	static {
		FieldType type = new FieldType();
		type.setDimensions(1, Double.BYTES);
		type.setDocValuesType(DocValuesType.NUMERIC);
		type.setStored(false);
		type.freeze();
		BBoxStrategy.DEFAULT_FIELDTYPE = type;
	}

	public static final String SUFFIX_MINX = "__minX";

	public static final String SUFFIX_MAXX = "__maxX";

	public static final String SUFFIX_MINY = "__minY";

	public static final String SUFFIX_MAXY = "__maxY";

	public static final String SUFFIX_XDL = "__xdl";

	final String field_bbox;

	final String field_minX;

	final String field_minY;

	final String field_maxX;

	final String field_maxY;

	final String field_xdl;

	private final FieldType optionsFieldType;

	private final int fieldsLen;

	private final boolean hasStored;

	private final boolean hasDocVals;

	private final boolean hasPointVals;

	private final FieldType xdlFieldType;

	public static BBoxStrategy newInstance(SpatialContext ctx, String fieldNamePrefix) {
		return new BBoxStrategy(ctx, fieldNamePrefix, BBoxStrategy.DEFAULT_FIELDTYPE);
	}

	public BBoxStrategy(SpatialContext ctx, String fieldNamePrefix, FieldType fieldType) {
		super(ctx, fieldNamePrefix);
		field_bbox = fieldNamePrefix;
		field_minX = fieldNamePrefix + (BBoxStrategy.SUFFIX_MINX);
		field_maxX = fieldNamePrefix + (BBoxStrategy.SUFFIX_MAXX);
		field_minY = fieldNamePrefix + (BBoxStrategy.SUFFIX_MINY);
		field_maxY = fieldNamePrefix + (BBoxStrategy.SUFFIX_MAXY);
		field_xdl = fieldNamePrefix + (BBoxStrategy.SUFFIX_XDL);
		fieldType.freeze();
		this.optionsFieldType = fieldType;
		int numQuads = 0;
		if (this.hasStored = fieldType.stored()) {
			numQuads++;
		}
		if (this.hasDocVals = (fieldType.docValuesType()) != (DocValuesType.NONE)) {
			numQuads++;
		}
		if (this.hasPointVals = (fieldType.pointDimensionCount()) > 0) {
			numQuads++;
		}
		if (hasPointVals) {
			xdlFieldType = new FieldType(StringField.TYPE_NOT_STORED);
			xdlFieldType.setIndexOptions(IndexOptions.DOCS);
			xdlFieldType.freeze();
		}else {
			xdlFieldType = null;
		}
		this.fieldsLen = (numQuads * 4) + ((xdlFieldType) != null ? 1 : 0);
	}

	public FieldType getFieldType() {
		return optionsFieldType;
	}

	@Override
	public Field[] createIndexableFields(Shape shape) {
		return createIndexableFields(shape.getBoundingBox());
	}

	private Field[] createIndexableFields(Rectangle bbox) {
		Field[] fields = new Field[fieldsLen];
		int idx = -1;
		if (hasStored) {
			fields[(++idx)] = new StoredField(field_minX, bbox.getMinX());
			fields[(++idx)] = new StoredField(field_minY, bbox.getMinY());
			fields[(++idx)] = new StoredField(field_maxX, bbox.getMaxX());
			fields[(++idx)] = new StoredField(field_maxY, bbox.getMaxY());
		}
		if (hasDocVals) {
			fields[(++idx)] = new DoubleDocValuesField(field_minX, bbox.getMinX());
			fields[(++idx)] = new DoubleDocValuesField(field_minY, bbox.getMinY());
			fields[(++idx)] = new DoubleDocValuesField(field_maxX, bbox.getMaxX());
			fields[(++idx)] = new DoubleDocValuesField(field_maxY, bbox.getMaxY());
		}
		if (hasPointVals) {
			fields[(++idx)] = new DoublePoint(field_minX, bbox.getMinX());
			fields[(++idx)] = new DoublePoint(field_minY, bbox.getMinY());
			fields[(++idx)] = new DoublePoint(field_maxX, bbox.getMaxX());
			fields[(++idx)] = new DoublePoint(field_maxY, bbox.getMaxY());
		}
		if ((xdlFieldType) != null) {
			fields[(++idx)] = new Field(field_xdl, (bbox.getCrossesDateLine() ? "T" : "F"), xdlFieldType);
		}
		assert idx == ((fields.length) - 1);
		return fields;
	}

	public ShapeValuesSource makeShapeValueSource() {
		return null;
	}

	@Override
	public DoubleValuesSource makeDistanceValueSource(Point queryPoint, double multiplier) {
		return new DistanceToShapeValueSource(makeShapeValueSource(), queryPoint, multiplier, ctx);
	}

	public DoubleValuesSource makeOverlapRatioValueSource(Rectangle queryBox, double queryTargetProportion) {
		return new BBoxOverlapRatioValueSource(makeShapeValueSource(), ctx.isGeo(), queryBox, queryTargetProportion, 0.0);
	}

	@Override
	public Query makeQuery(SpatialArgs args) {
		Shape shape = args.getShape();
		if (!(shape instanceof Rectangle))
			throw new UnsupportedOperationException(("Can only query by Rectangle, not " + shape));

		Rectangle bbox = ((Rectangle) (shape));
		Query spatial;
		SpatialOperation op = args.getOperation();
		if (op == (SpatialOperation.BBoxIntersects))
			spatial = makeIntersects(bbox);
		else
			if (op == (SpatialOperation.BBoxWithin))
				spatial = makeWithin(bbox);
			else
				if (op == (SpatialOperation.Contains))
					spatial = makeContains(bbox);
				else
					if (op == (SpatialOperation.Intersects))
						spatial = makeIntersects(bbox);
					else
						if (op == (SpatialOperation.IsEqualTo))
							spatial = makeEquals(bbox);
						else
							if (op == (SpatialOperation.IsDisjointTo))
								spatial = makeDisjoint(bbox);
							else
								if (op == (SpatialOperation.IsWithin))
									spatial = makeWithin(bbox);
								else {
									throw new UnsupportedSpatialOperation(op);
								}






		return new ConstantScoreQuery(spatial);
	}

	Query makeContains(Rectangle bbox) {
		Query qMinY = this.makeNumericRangeQuery(field_minY, null, bbox.getMinY(), false, true);
		Query qMaxY = this.makeNumericRangeQuery(field_maxY, bbox.getMaxY(), null, true, false);
		Query yConditions = this.makeQuery(MUST, qMinY, qMaxY);
		Query xConditions;
		if (!(bbox.getCrossesDateLine())) {
			Query qMinX = this.makeNumericRangeQuery(field_minX, null, bbox.getMinX(), false, true);
			Query qMaxX = this.makeNumericRangeQuery(field_maxX, bbox.getMaxX(), null, true, false);
			Query qMinMax = this.makeQuery(MUST, qMinX, qMaxX);
			Query qNonXDL = this.makeXDL(false, qMinMax);
			if (!(ctx.isGeo())) {
				xConditions = qNonXDL;
			}else {
				Query qXDLLeft = this.makeNumericRangeQuery(field_minX, null, bbox.getMinX(), false, true);
				Query qXDLRight = this.makeNumericRangeQuery(field_maxX, bbox.getMaxX(), null, true, false);
				Query qXDLLeftRight = this.makeQuery(SHOULD, qXDLLeft, qXDLRight);
				Query qXDL = this.makeXDL(true, qXDLLeftRight);
				Query qEdgeDL = null;
				if (((bbox.getMinX()) == (bbox.getMaxX())) && ((Math.abs(bbox.getMinX())) == 180)) {
					double edge = (bbox.getMinX()) * (-1);
					qEdgeDL = makeQuery(SHOULD, makeNumberTermQuery(field_minX, edge), makeNumberTermQuery(field_maxX, edge));
				}
				xConditions = this.makeQuery(SHOULD, qNonXDL, qXDL, qEdgeDL);
			}
		}else {
			Query qXDLLeft = this.makeNumericRangeQuery(field_minX, null, bbox.getMinX(), false, true);
			Query qXDLRight = this.makeNumericRangeQuery(field_maxX, bbox.getMaxX(), null, true, false);
			Query qXDLLeftRight = this.makeXDL(true, this.makeQuery(MUST, qXDLLeft, qXDLRight));
			Query qWorld = makeQuery(MUST, makeNumberTermQuery(field_minX, (-180)), makeNumberTermQuery(field_maxX, 180));
			xConditions = makeQuery(SHOULD, qXDLLeftRight, qWorld);
		}
		return this.makeQuery(MUST, xConditions, yConditions);
	}

	Query makeDisjoint(Rectangle bbox) {
		Query qMinY = this.makeNumericRangeQuery(field_minY, bbox.getMaxY(), null, false, false);
		Query qMaxY = this.makeNumericRangeQuery(field_maxY, null, bbox.getMinY(), false, false);
		Query yConditions = this.makeQuery(SHOULD, qMinY, qMaxY);
		Query xConditions;
		if (!(bbox.getCrossesDateLine())) {
			Query qMinX = this.makeNumericRangeQuery(field_minX, bbox.getMaxX(), null, false, false);
			if (((bbox.getMinX()) == (-180.0)) && (ctx.isGeo())) {
				BooleanQuery.Builder bq = new BooleanQuery.Builder();
				bq.add(qMinX, MUST);
				bq.add(makeNumberTermQuery(field_maxX, 180.0), MUST_NOT);
				qMinX = bq.build();
			}
			Query qMaxX = this.makeNumericRangeQuery(field_maxX, null, bbox.getMinX(), false, false);
			if (((bbox.getMaxX()) == 180.0) && (ctx.isGeo())) {
				BooleanQuery.Builder bq = new BooleanQuery.Builder();
				bq.add(qMaxX, MUST);
				bq.add(makeNumberTermQuery(field_minX, (-180.0)), MUST_NOT);
				qMaxX = bq.build();
			}
			Query qMinMax = this.makeQuery(SHOULD, qMinX, qMaxX);
			Query qNonXDL = this.makeXDL(false, qMinMax);
			if (!(ctx.isGeo())) {
				xConditions = qNonXDL;
			}else {
				Query qMinXLeft = this.makeNumericRangeQuery(field_minX, bbox.getMaxX(), null, false, false);
				Query qMaxXRight = this.makeNumericRangeQuery(field_maxX, null, bbox.getMinX(), false, false);
				Query qLeftRight = this.makeQuery(MUST, qMinXLeft, qMaxXRight);
				Query qXDL = this.makeXDL(true, qLeftRight);
				xConditions = this.makeQuery(SHOULD, qNonXDL, qXDL);
			}
		}else {
			Query qMinXLeft = this.makeNumericRangeQuery(field_minX, 180.0, null, false, false);
			Query qMaxXLeft = this.makeNumericRangeQuery(field_maxX, null, bbox.getMinX(), false, false);
			Query qMinXRight = this.makeNumericRangeQuery(field_minX, bbox.getMaxX(), null, false, false);
			Query qMaxXRight = this.makeNumericRangeQuery(field_maxX, null, (-180.0), false, false);
			Query qLeft = this.makeQuery(SHOULD, qMinXLeft, qMaxXLeft);
			Query qRight = this.makeQuery(SHOULD, qMinXRight, qMaxXRight);
			Query qLeftRight = this.makeQuery(MUST, qLeft, qRight);
			xConditions = this.makeXDL(false, qLeftRight);
		}
		return this.makeQuery(SHOULD, xConditions, yConditions);
	}

	Query makeEquals(Rectangle bbox) {
		Query qMinX = makeNumberTermQuery(field_minX, bbox.getMinX());
		Query qMinY = makeNumberTermQuery(field_minY, bbox.getMinY());
		Query qMaxX = makeNumberTermQuery(field_maxX, bbox.getMaxX());
		Query qMaxY = makeNumberTermQuery(field_maxY, bbox.getMaxY());
		return makeQuery(MUST, qMinX, qMinY, qMaxX, qMaxY);
	}

	Query makeIntersects(Rectangle bbox) {
		Query qHasEnv;
		if (ctx.isGeo()) {
			Query qIsNonXDL = this.makeXDL(false);
			Query qIsXDL = (ctx.isGeo()) ? this.makeXDL(true) : null;
			qHasEnv = this.makeQuery(SHOULD, qIsNonXDL, qIsXDL);
		}else {
			qHasEnv = this.makeXDL(false);
		}
		BooleanQuery.Builder qNotDisjoint = new BooleanQuery.Builder();
		qNotDisjoint.add(qHasEnv, MUST);
		Query qDisjoint = makeDisjoint(bbox);
		qNotDisjoint.add(qDisjoint, MUST_NOT);
		return qNotDisjoint.build();
	}

	BooleanQuery makeQuery(BooleanClause.Occur occur, Query... queries) {
		BooleanQuery.Builder bq = new BooleanQuery.Builder();
		for (Query query : queries) {
			if (query != null)
				bq.add(query, occur);

		}
		return bq.build();
	}

	Query makeWithin(Rectangle bbox) {
		Query qMinY = this.makeNumericRangeQuery(field_minY, bbox.getMinY(), null, true, false);
		Query qMaxY = this.makeNumericRangeQuery(field_maxY, null, bbox.getMaxY(), false, true);
		Query yConditions = this.makeQuery(MUST, qMinY, qMaxY);
		Query xConditions;
		if (((ctx.isGeo()) && ((bbox.getMinX()) == (-180.0))) && ((bbox.getMaxX()) == 180.0)) {
			return yConditions;
		}else
			if (!(bbox.getCrossesDateLine())) {
				Query qMinX = this.makeNumericRangeQuery(field_minX, bbox.getMinX(), null, true, false);
				Query qMaxX = this.makeNumericRangeQuery(field_maxX, null, bbox.getMaxX(), false, true);
				Query qMinMax = this.makeQuery(MUST, qMinX, qMaxX);
				double edge = 0;
				if ((bbox.getMinX()) == (-180.0))
					edge = 180;
				else
					if ((bbox.getMaxX()) == 180.0)
						edge = -180;


				if ((edge != 0) && (ctx.isGeo())) {
					Query edgeQ = makeQuery(MUST, makeNumberTermQuery(field_minX, edge), makeNumberTermQuery(field_maxX, edge));
					qMinMax = makeQuery(SHOULD, qMinMax, edgeQ);
				}
				xConditions = this.makeXDL(false, qMinMax);
			}else {
				Query qMinXLeft = this.makeNumericRangeQuery(field_minX, bbox.getMinX(), null, true, false);
				Query qMaxXLeft = this.makeNumericRangeQuery(field_maxX, null, 180.0, false, true);
				Query qLeft = this.makeQuery(MUST, qMinXLeft, qMaxXLeft);
				Query qMinXRight = this.makeNumericRangeQuery(field_minX, (-180.0), null, true, false);
				Query qMaxXRight = this.makeNumericRangeQuery(field_maxX, null, bbox.getMaxX(), false, true);
				Query qRight = this.makeQuery(MUST, qMinXRight, qMaxXRight);
				Query qLeftRight = this.makeQuery(SHOULD, qLeft, qRight);
				Query qNonXDL = this.makeXDL(false, qLeftRight);
				Query qXDLLeft = this.makeNumericRangeQuery(field_minX, bbox.getMinX(), null, true, false);
				Query qXDLRight = this.makeNumericRangeQuery(field_maxX, null, bbox.getMaxX(), false, true);
				Query qXDLLeftRight = this.makeQuery(MUST, qXDLLeft, qXDLRight);
				Query qXDL = this.makeXDL(true, qXDLLeftRight);
				xConditions = this.makeQuery(SHOULD, qNonXDL, qXDL);
			}

		return this.makeQuery(MUST, xConditions, yConditions);
	}

	private Query makeXDL(boolean crossedDateLine) {
		return new TermQuery(new Term(field_xdl, (crossedDateLine ? "T" : "F")));
	}

	private Query makeXDL(boolean crossedDateLine, Query query) {
		if (!(ctx.isGeo())) {
			assert !crossedDateLine;
			return query;
		}
		BooleanQuery.Builder bq = new BooleanQuery.Builder();
		bq.add(this.makeXDL(crossedDateLine), MUST);
		bq.add(query, MUST);
		return bq.build();
	}

	private Query makeNumberTermQuery(String field, double number) {
		if (hasPointVals) {
			return DoublePoint.newExactQuery(field, number);
		}
		throw new UnsupportedOperationException("An index is required for this operation.");
	}

	private Query makeNumericRangeQuery(String fieldname, Double min, Double max, boolean minInclusive, boolean maxInclusive) {
		if (hasPointVals) {
			if (min == null) {
				min = Double.NEGATIVE_INFINITY;
			}
			if (max == null) {
				max = Double.POSITIVE_INFINITY;
			}
			if (minInclusive == false) {
				min = Math.nextUp(min);
			}
			if (maxInclusive == false) {
				max = Math.nextDown(max);
			}
			return DoublePoint.newRangeQuery(fieldname, min, max);
		}
		throw new UnsupportedOperationException("An index is required for this operation.");
	}
}

