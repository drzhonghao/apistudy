

import org.apache.lucene.spatial.prefix.tree.Cell;
import org.apache.lucene.spatial.prefix.tree.CellIterator;
import org.apache.lucene.spatial.prefix.tree.LegacyCell;
import org.apache.lucene.spatial.prefix.tree.SpatialPrefixTree;
import org.apache.lucene.util.BytesRef;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.shape.Point;
import org.locationtech.spatial4j.shape.Rectangle;
import org.locationtech.spatial4j.shape.Shape;


abstract class LegacyPrefixTree extends SpatialPrefixTree {
	public LegacyPrefixTree(SpatialContext ctx, int maxLevels) {
		super(ctx, maxLevels);
	}

	public double getDistanceForLevel(int level) {
		if ((level < 1) || (level > (getMaxLevels())))
			throw new IllegalArgumentException("Level must be in 1 to maxLevels range");

		Cell cell = getCell(ctx.getWorldBounds().getCenter(), level);
		Rectangle bbox = cell.getShape().getBoundingBox();
		double width = bbox.getWidth();
		double height = bbox.getHeight();
		return Math.sqrt(((width * width) + (height * height)));
	}

	protected abstract Cell getCell(Point p, int level);

	@Override
	public Cell readCell(BytesRef term, Cell scratch) {
		LegacyCell cell = ((LegacyCell) (scratch));
		if (cell == null)
			cell = ((LegacyCell) (getWorldCell()));

		return cell;
	}

	@Override
	public CellIterator getTreeCellIterator(Shape shape, int detailLevel) {
		if (!(shape instanceof Point))
			return super.getTreeCellIterator(shape, detailLevel);

		Cell cell = getCell(((Point) (shape)), detailLevel);
		assert cell instanceof LegacyCell;
		BytesRef fullBytes = cell.getTokenBytesNoLeaf(null);
		Cell[] cells = new Cell[detailLevel];
		for (int i = 1; i < detailLevel; i++) {
			fullBytes.length = i;
			Cell parentCell = readCell(fullBytes, null);
			cells[(i - 1)] = parentCell;
		}
		cells[(detailLevel - 1)] = cell;
		return null;
	}
}

