

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexReaderContext;
import org.apache.lucene.search.DoubleValuesSource;
import org.apache.lucene.spatial.SpatialStrategy;
import org.apache.lucene.spatial.prefix.CellToBytesRefIterator;
import org.apache.lucene.spatial.prefix.HeatmapFacetCounter;
import org.apache.lucene.spatial.prefix.PointPrefixTreeFieldCacheProvider;
import org.apache.lucene.spatial.prefix.tree.Cell;
import org.apache.lucene.spatial.prefix.tree.CellIterator;
import org.apache.lucene.spatial.prefix.tree.SpatialPrefixTree;
import org.apache.lucene.spatial.query.SpatialArgs;
import org.apache.lucene.spatial.util.ShapeFieldCacheDistanceValueSource;
import org.apache.lucene.util.Bits;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.shape.Point;
import org.locationtech.spatial4j.shape.Shape;


public abstract class PrefixTreeStrategy extends SpatialStrategy {
	protected final SpatialPrefixTree grid;

	private final Map<String, PointPrefixTreeFieldCacheProvider> provider = new ConcurrentHashMap<>();

	protected int defaultFieldValuesArrayLen = 2;

	protected double distErrPct = SpatialArgs.DEFAULT_DISTERRPCT;

	protected boolean pointsOnly = false;

	public PrefixTreeStrategy(SpatialPrefixTree grid, String fieldName) {
		super(grid.getSpatialContext(), fieldName);
		this.grid = grid;
	}

	public SpatialPrefixTree getGrid() {
		return grid;
	}

	public void setDefaultFieldValuesArrayLen(int defaultFieldValuesArrayLen) {
		this.defaultFieldValuesArrayLen = defaultFieldValuesArrayLen;
	}

	public double getDistErrPct() {
		return distErrPct;
	}

	public void setDistErrPct(double distErrPct) {
		this.distErrPct = distErrPct;
	}

	public boolean isPointsOnly() {
		return pointsOnly;
	}

	public void setPointsOnly(boolean pointsOnly) {
		this.pointsOnly = pointsOnly;
	}

	@Override
	public Field[] createIndexableFields(Shape shape) {
		double distErr = SpatialArgs.calcDistanceFromErrPct(shape, distErrPct, ctx);
		return createIndexableFields(shape, distErr);
	}

	public Field[] createIndexableFields(Shape shape, double distErr) {
		int detailLevel = grid.getLevelForDistance(distErr);
		return createIndexableFields(shape, detailLevel);
	}

	public Field[] createIndexableFields(Shape shape, int detailLevel) {
		Iterator<Cell> cells = createCellIteratorToIndex(shape, detailLevel, null);
		CellToBytesRefIterator cellToBytesRefIterator = newCellToBytesRefIterator();
		cellToBytesRefIterator.reset(cells);
		return null;
	}

	protected CellToBytesRefIterator newCellToBytesRefIterator() {
		return new CellToBytesRefIterator();
	}

	protected Iterator<Cell> createCellIteratorToIndex(Shape shape, int detailLevel, Iterator<Cell> reuse) {
		if ((pointsOnly) && (!(isPointShape(shape)))) {
			throw new IllegalArgumentException((("pointsOnly is true yet a " + (shape.getClass())) + " is given for indexing"));
		}
		return grid.getTreeCellIterator(shape, detailLevel);
	}

	public static final FieldType FIELD_TYPE = new FieldType();

	static {
		PrefixTreeStrategy.FIELD_TYPE.setTokenized(true);
		PrefixTreeStrategy.FIELD_TYPE.setOmitNorms(true);
		PrefixTreeStrategy.FIELD_TYPE.setIndexOptions(IndexOptions.DOCS);
		PrefixTreeStrategy.FIELD_TYPE.freeze();
	}

	@Override
	public DoubleValuesSource makeDistanceValueSource(Point queryPoint, double multiplier) {
		PointPrefixTreeFieldCacheProvider p = provider.get(getFieldName());
		if (p == null) {
			synchronized(this) {
				p = provider.get(getFieldName());
				if (p == null) {
					p = new PointPrefixTreeFieldCacheProvider(grid, getFieldName(), defaultFieldValuesArrayLen);
					provider.put(getFieldName(), p);
				}
			}
		}
		return new ShapeFieldCacheDistanceValueSource(ctx, p, queryPoint, multiplier);
	}

	public HeatmapFacetCounter.Heatmap calcFacets(IndexReaderContext context, Bits topAcceptDocs, Shape inputShape, final int facetLevel, int maxCells) throws IOException {
		return null;
	}

	protected boolean isPointShape(Shape shape) {
		return shape instanceof Point;
	}
}

