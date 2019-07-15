

import java.util.Collection;
import org.apache.lucene.spatial.prefix.tree.Cell;
import org.apache.lucene.spatial.prefix.tree.CellCanPrune;
import org.apache.lucene.spatial.prefix.tree.CellIterator;
import org.apache.lucene.spatial.prefix.tree.SpatialPrefixTree;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.StringHelper;
import org.locationtech.spatial4j.shape.Point;
import org.locationtech.spatial4j.shape.Shape;
import org.locationtech.spatial4j.shape.SpatialRelation;


public abstract class LegacyCell implements CellCanPrune {
	private static final byte LEAF_BYTE = '+';

	protected byte[] bytes;

	protected int b_off;

	protected int b_len;

	protected boolean isLeaf;

	protected SpatialRelation shapeRel;

	protected Shape shape;

	protected LegacyCell(byte[] bytes, int off, int len) {
		this.bytes = bytes;
		this.b_off = off;
		this.b_len = len;
		readLeafAdjust();
	}

	protected void readCell(BytesRef bytes) {
		shapeRel = null;
		shape = null;
		this.bytes = bytes.bytes;
		this.b_off = bytes.offset;
		this.b_len = ((short) (bytes.length));
		readLeafAdjust();
	}

	protected void readLeafAdjust() {
		isLeaf = ((b_len) > 0) && ((bytes[(((b_off) + (b_len)) - 1)]) == (LegacyCell.LEAF_BYTE));
		if (isLeaf)
			(b_len)--;

		if ((getLevel()) == (getMaxLevels()))
			isLeaf = true;

	}

	protected abstract SpatialPrefixTree getGrid();

	protected abstract int getMaxLevels();

	@Override
	public SpatialRelation getShapeRel() {
		return shapeRel;
	}

	@Override
	public void setShapeRel(SpatialRelation rel) {
		this.shapeRel = rel;
	}

	@Override
	public boolean isLeaf() {
		return isLeaf;
	}

	@Override
	public void setLeaf() {
		isLeaf = true;
	}

	@Override
	public BytesRef getTokenBytesWithLeaf(BytesRef result) {
		result = getTokenBytesNoLeaf(result);
		if ((!(isLeaf)) || ((getLevel()) == (getMaxLevels())))
			return result;

		if ((result.bytes.length) < (((result.offset) + (result.length)) + 1)) {
			assert false : "Not supposed to happen; performance bug";
			byte[] copy = new byte[(result.length) + 1];
			System.arraycopy(result.bytes, result.offset, copy, 0, ((result.length) - 1));
			result.bytes = copy;
			result.offset = 0;
		}
		result.bytes[((result.offset) + ((result.length)++))] = LegacyCell.LEAF_BYTE;
		return result;
	}

	@Override
	public BytesRef getTokenBytesNoLeaf(BytesRef result) {
		if (result == null)
			return new BytesRef(bytes, b_off, b_len);

		result.bytes = bytes;
		result.offset = b_off;
		result.length = b_len;
		return result;
	}

	@Override
	public int getLevel() {
		return b_len;
	}

	@Override
	public CellIterator getNextLevelCells(Shape shapeFilter) {
		assert (getLevel()) < (getGrid().getMaxLevels());
		if (shapeFilter instanceof Point) {
			LegacyCell cell = getSubCell(((Point) (shapeFilter)));
			cell.shapeRel = SpatialRelation.CONTAINS;
		}else {
		}
		return null;
	}

	protected abstract LegacyCell getSubCell(Point p);

	protected abstract Collection<Cell> getSubCells();

	@Override
	public boolean isPrefixOf(Cell c) {
		LegacyCell cell = ((LegacyCell) (c));
		boolean result = LegacyCell.sliceEquals(cell.bytes, cell.b_off, cell.b_len, bytes, b_off, b_len);
		assert result == (StringHelper.startsWith(c.getTokenBytesNoLeaf(null), getTokenBytesNoLeaf(null)));
		return result;
	}

	private static boolean sliceEquals(byte[] sliceToTest_bytes, int sliceToTest_offset, int sliceToTest_length, byte[] other_bytes, int other_offset, int other_length) {
		if (sliceToTest_length < other_length) {
			return false;
		}
		int i = sliceToTest_offset;
		int j = other_offset;
		final int k = other_offset + other_length;
		while (j < k) {
			if ((sliceToTest_bytes[(i++)]) != (other_bytes[(j++)])) {
				return false;
			}
		} 
		return true;
	}

	@Override
	public int compareToNoLeaf(Cell fromCell) {
		LegacyCell b = ((LegacyCell) (fromCell));
		return LegacyCell.compare(bytes, b_off, b_len, b.bytes, b.b_off, b.b_len);
	}

	protected static int compare(byte[] aBytes, int aUpto, int a_length, byte[] bBytes, int bUpto, int b_length) {
		final int aStop = aUpto + (Math.min(a_length, b_length));
		while (aUpto < aStop) {
			int aByte = (aBytes[(aUpto++)]) & 255;
			int bByte = (bBytes[(bUpto++)]) & 255;
			int diff = aByte - bByte;
			if (diff != 0) {
				return diff;
			}
		} 
		return a_length - b_length;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Cell) {
			Cell cell = ((Cell) (obj));
			return getTokenBytesWithLeaf(null).equals(cell.getTokenBytesWithLeaf(null));
		}else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return getTokenBytesWithLeaf(null).hashCode();
	}

	@Override
	public String toString() {
		return getTokenBytesWithLeaf(null).utf8ToString();
	}
}

