

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.lucene.spatial.prefix.tree.Cell;
import org.apache.lucene.spatial.prefix.tree.LegacyCell;
import org.apache.lucene.spatial.prefix.tree.SpatialPrefixTree;
import org.apache.lucene.spatial.prefix.tree.SpatialPrefixTreeFactory;
import org.apache.lucene.util.BytesRef;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.io.GeohashUtils;
import org.locationtech.spatial4j.shape.Point;
import org.locationtech.spatial4j.shape.Rectangle;
import org.locationtech.spatial4j.shape.Shape;


public class GeohashPrefixTree {
	public static class Factory extends SpatialPrefixTreeFactory {
		@Override
		protected int getLevelForDistance(double degrees) {
			GeohashPrefixTree grid = new GeohashPrefixTree(ctx, GeohashPrefixTree.getMaxLevelsPossible());
			return grid.getLevelForDistance(degrees);
		}

		@Override
		protected SpatialPrefixTree newSPT() {
			return null;
		}
	}

	public GeohashPrefixTree(SpatialContext ctx, int maxLevels) {
		Rectangle bounds = ctx.getWorldBounds();
		if ((bounds.getMinX()) != (-180))
			throw new IllegalArgumentException(("Geohash only supports lat-lon world bounds. Got " + bounds));

		int MAXP = GeohashPrefixTree.getMaxLevelsPossible();
		if ((maxLevels <= 0) || (maxLevels > MAXP))
			throw new IllegalArgumentException(((("maxLevels must be [1-" + MAXP) + "] but got ") + maxLevels));

	}

	public static int getMaxLevelsPossible() {
		return GeohashUtils.MAX_PRECISION;
	}

	public Cell getWorldCell() {
		return new GeohashPrefixTree.GhCell(BytesRef.EMPTY_BYTES, 0, 0);
	}

	public int getLevelForDistance(double dist) {
		if (dist == 0) {
		}
		final int level = GeohashUtils.lookupHashLenForWidthHeight(dist, dist);
		return 0;
	}

	protected Cell getCell(Point p, int level) {
		return new GeohashPrefixTree.GhCell(GeohashUtils.encodeLatLon(p.getY(), p.getX(), level));
	}

	private static byte[] stringToBytesPlus1(String token) {
		byte[] bytes = new byte[(token.length()) + 1];
		for (int i = 0; i < (token.length()); i++) {
			bytes[i] = ((byte) (token.charAt(i)));
		}
		return bytes;
	}

	private class GhCell extends LegacyCell {
		private String geohash;

		GhCell(String geohash) {
			super(GeohashPrefixTree.stringToBytesPlus1(geohash), 0, geohash.length());
			this.geohash = geohash;
			if ((isLeaf()) && ((getLevel()) < (getMaxLevels())))
				this.geohash = geohash.substring(0, ((geohash.length()) - 1));

		}

		GhCell(byte[] bytes, int off, int len) {
			super(bytes, off, len);
		}

		@Override
		protected int getMaxLevels() {
			return 0;
		}

		@Override
		protected void readCell(BytesRef bytesRef) {
			super.readCell(bytesRef);
			geohash = null;
		}

		@Override
		public Collection<Cell> getSubCells() {
			String[] hashes = GeohashUtils.getSubGeohashes(getGeohash());
			List<Cell> cells = new ArrayList<>(hashes.length);
			for (String hash : hashes) {
				cells.add(new GeohashPrefixTree.GhCell(hash));
			}
			return cells;
		}

		@Override
		public int getSubCellsSize() {
			return 32;
		}

		@Override
		protected GeohashPrefixTree.GhCell getSubCell(Point p) {
			return null;
		}

		@Override
		public Shape getShape() {
			if ((shape) == null) {
			}
			return shape;
		}

		private String getGeohash() {
			if ((geohash) == null)
				geohash = getTokenBytesNoLeaf(null).utf8ToString();

			return geohash;
		}
	}
}

