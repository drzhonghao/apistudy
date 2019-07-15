

import java.io.PrintStream;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import org.apache.lucene.spatial.prefix.tree.Cell;
import org.apache.lucene.spatial.prefix.tree.LegacyCell;
import org.apache.lucene.spatial.prefix.tree.SpatialPrefixTree;
import org.apache.lucene.spatial.prefix.tree.SpatialPrefixTreeFactory;
import org.apache.lucene.util.BytesRef;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.shape.Point;
import org.locationtech.spatial4j.shape.Rectangle;
import org.locationtech.spatial4j.shape.Shape;
import org.locationtech.spatial4j.shape.SpatialRelation;


public class QuadPrefixTree {
	public static class Factory extends SpatialPrefixTreeFactory {
		@Override
		protected int getLevelForDistance(double degrees) {
			QuadPrefixTree grid = new QuadPrefixTree(ctx, QuadPrefixTree.MAX_LEVELS_POSSIBLE);
			return grid.getLevelForDistance(degrees);
		}

		@Override
		protected SpatialPrefixTree newSPT() {
			return null;
		}
	}

	public static final int MAX_LEVELS_POSSIBLE = 50;

	public static final int DEFAULT_MAX_LEVELS = 12;

	protected final double xmin;

	protected final double xmax;

	protected final double ymin;

	protected final double ymax;

	protected final double xmid;

	protected final double ymid;

	protected final double gridW;

	public final double gridH;

	final double[] levelW;

	final double[] levelH;

	final int[] levelS;

	final int[] levelN;

	public QuadPrefixTree(SpatialContext ctx, Rectangle bounds, int maxLevels) {
		this.xmin = bounds.getMinX();
		this.xmax = bounds.getMaxX();
		this.ymin = bounds.getMinY();
		this.ymax = bounds.getMaxY();
		levelW = new double[maxLevels];
		levelH = new double[maxLevels];
		levelS = new int[maxLevels];
		levelN = new int[maxLevels];
		gridW = (xmax) - (xmin);
		gridH = (ymax) - (ymin);
		this.xmid = (xmin) + ((gridW) / 2.0);
		this.ymid = (ymin) + ((gridH) / 2.0);
		levelW[0] = (gridW) / 2.0;
		levelH[0] = (gridH) / 2.0;
		levelS[0] = 2;
		levelN[0] = 4;
		for (int i = 1; i < (levelW.length); i++) {
			levelW[i] = (levelW[(i - 1)]) / 2.0;
			levelH[i] = (levelH[(i - 1)]) / 2.0;
			levelS[i] = (levelS[(i - 1)]) * 2;
			levelN[i] = (levelN[(i - 1)]) * 4;
		}
	}

	public QuadPrefixTree(SpatialContext ctx) {
		this(ctx, QuadPrefixTree.DEFAULT_MAX_LEVELS);
	}

	public QuadPrefixTree(SpatialContext ctx, int maxLevels) {
		this(ctx, ctx.getWorldBounds(), maxLevels);
	}

	public Cell getWorldCell() {
		return new QuadPrefixTree.QuadCell(BytesRef.EMPTY_BYTES, 0, 0);
	}

	public void printInfo(PrintStream out) {
		NumberFormat nf = NumberFormat.getNumberInstance(Locale.ROOT);
		nf.setMaximumFractionDigits(5);
		nf.setMinimumFractionDigits(5);
		nf.setMinimumIntegerDigits(3);
	}

	public int getLevelForDistance(double dist) {
		if (dist == 0) {
		}
		return 0;
	}

	public Cell getCell(Point p, int level) {
		List<Cell> cells = new ArrayList<>(1);
		return cells.get(0);
	}

	private void build(double x, double y, int level, List<Cell> matches, BytesRef str, Shape shape, int maxLevel) {
		assert (str.length) == level;
		double w = (levelW[level]) / 2;
		double h = (levelH[level]) / 2;
		checkBattenberg('A', (x - w), (y + h), level, matches, str, shape, maxLevel);
		checkBattenberg('B', (x + w), (y + h), level, matches, str, shape, maxLevel);
		checkBattenberg('C', (x - w), (y - h), level, matches, str, shape, maxLevel);
		checkBattenberg('D', (x + w), (y - h), level, matches, str, shape, maxLevel);
	}

	protected void checkBattenberg(char c, double cx, double cy, int level, List<Cell> matches, BytesRef str, Shape shape, int maxLevel) {
		assert (str.length) == level;
		assert (str.offset) == 0;
		double w = (levelW[level]) / 2;
		double h = (levelH[level]) / 2;
		int strlen = str.length;
		str.length = strlen;
	}

	protected class QuadCell extends LegacyCell {
		QuadCell(byte[] bytes, int off, int len) {
			super(bytes, off, len);
		}

		QuadCell(BytesRef str, SpatialRelation shapeRel) {
			this(str.bytes, str.offset, str.length);
			this.shapeRel = shapeRel;
		}

		@Override
		protected int getMaxLevels() {
			return 0;
		}

		@Override
		protected Collection<Cell> getSubCells() {
			BytesRef source = getTokenBytesNoLeaf(null);
			List<Cell> cells = new ArrayList<>(4);
			cells.add(new QuadPrefixTree.QuadCell(concat(source, ((byte) ('A'))), null));
			cells.add(new QuadPrefixTree.QuadCell(concat(source, ((byte) ('B'))), null));
			cells.add(new QuadPrefixTree.QuadCell(concat(source, ((byte) ('C'))), null));
			cells.add(new QuadPrefixTree.QuadCell(concat(source, ((byte) ('D'))), null));
			return cells;
		}

		protected BytesRef concat(BytesRef source, byte b) {
			final byte[] buffer = Arrays.copyOfRange(source.bytes, source.offset, (((source.offset) + (source.length)) + 2));
			BytesRef target = new BytesRef(buffer);
			target.length = source.length;
			target.bytes[((target.length)++)] = b;
			return target;
		}

		@Override
		public int getSubCellsSize() {
			return 4;
		}

		@Override
		protected QuadPrefixTree.QuadCell getSubCell(Point p) {
			return ((QuadPrefixTree.QuadCell) (QuadPrefixTree.this.getCell(p, ((getLevel()) + 1))));
		}

		@Override
		public Shape getShape() {
			if ((shape) == null)
				shape = makeShape();

			return shape;
		}

		protected Rectangle makeShape() {
			BytesRef token = getTokenBytesNoLeaf(null);
			double xmin = QuadPrefixTree.this.xmin;
			double ymin = QuadPrefixTree.this.ymin;
			for (int i = 0; i < (token.length); i++) {
				byte c = token.bytes[((token.offset) + i)];
				switch (c) {
					case 'A' :
						ymin += levelH[i];
						break;
					case 'B' :
						xmin += levelW[i];
						ymin += levelH[i];
						break;
					case 'C' :
						break;
					case 'D' :
						xmin += levelW[i];
						break;
					default :
						throw new RuntimeException(("unexpected char: " + c));
				}
			}
			int len = token.length;
			double width;
			double height;
			if (len > 0) {
				width = levelW[(len - 1)];
				height = levelH[(len - 1)];
			}else {
				width = gridW;
				height = gridH;
			}
			return null;
		}
	}
}

