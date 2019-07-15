

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import org.apache.poi.hslf.record.ParentAwareRecord;
import org.apache.poi.hslf.record.Record;
import org.apache.poi.hslf.util.MutableByteArrayOutputStream;
import org.apache.poi.util.ArrayUtil;
import org.apache.poi.util.LittleEndian;


public abstract class RecordContainer extends Record {
	protected Record[] _children;

	@Override
	public Record[] getChildRecords() {
		return _children;
	}

	@Override
	public boolean isAnAtom() {
		return false;
	}

	private int findChildLocation(Record child) {
		int i = 0;
		for (Record r : _children) {
			if (r.equals(child)) {
				return i;
			}
			i++;
		}
		return -1;
	}

	private int appendChild(Record newChild) {
		Record[] nc = new Record[(_children.length) + 1];
		System.arraycopy(_children, 0, nc, 0, _children.length);
		nc[_children.length] = newChild;
		_children = nc;
		return _children.length;
	}

	private void addChildAt(Record newChild, int position) {
		appendChild(newChild);
		moveChildRecords(((_children.length) - 1), position, 1);
	}

	private void moveChildRecords(int oldLoc, int newLoc, int number) {
		if (oldLoc == newLoc) {
			return;
		}
		if (number == 0) {
			return;
		}
		if ((oldLoc + number) > (_children.length)) {
			throw new IllegalArgumentException("Asked to move more records than there are!");
		}
		ArrayUtil.arrayMoveWithin(_children, oldLoc, newLoc, number);
	}

	public Record findFirstOfType(long type) {
		for (Record r : _children) {
			if ((r.getRecordType()) == type) {
				return r;
			}
		}
		return null;
	}

	public Record removeChild(Record ch) {
		Record rm = null;
		ArrayList<Record> lst = new ArrayList<>();
		for (Record r : _children) {
			if (r != ch) {
				lst.add(r);
			}else {
				rm = r;
			}
		}
		_children = lst.toArray(new Record[lst.size()]);
		return rm;
	}

	public int appendChildRecord(Record newChild) {
		return appendChild(newChild);
	}

	public int addChildAfter(Record newChild, Record after) {
		int loc = findChildLocation(after);
		if (loc == (-1)) {
			throw new IllegalArgumentException("Asked to add a new child after another record, but that record wasn't one of our children!");
		}
		addChildAt(newChild, (loc + 1));
		return loc + 1;
	}

	public int addChildBefore(Record newChild, Record before) {
		int loc = findChildLocation(before);
		if (loc == (-1)) {
			throw new IllegalArgumentException("Asked to add a new child before another record, but that record wasn't one of our children!");
		}
		addChildAt(newChild, loc);
		return loc;
	}

	@org.apache.poi.util.Removal(version = "3.19")
	@Deprecated
	public void moveChildBefore(Record child, Record before) {
		moveChildrenBefore(child, 1, before);
	}

	@org.apache.poi.util.Removal(version = "3.19")
	@Deprecated
	public void moveChildrenBefore(Record firstChild, int number, Record before) {
		if (number < 1) {
			return;
		}
		int newLoc = findChildLocation(before);
		if (newLoc == (-1)) {
			throw new IllegalArgumentException("Asked to move children before another record, but that record wasn't one of our children!");
		}
		int oldLoc = findChildLocation(firstChild);
		if (oldLoc == (-1)) {
			throw new IllegalArgumentException("Asked to move a record that wasn't a child!");
		}
		moveChildRecords(oldLoc, newLoc, number);
	}

	@org.apache.poi.util.Removal(version = "3.19")
	@Deprecated
	public void moveChildrenAfter(Record firstChild, int number, Record after) {
		if (number < 1) {
			return;
		}
		int newLoc = findChildLocation(after);
		if (newLoc == (-1)) {
			throw new IllegalArgumentException("Asked to move children before another record, but that record wasn't one of our children!");
		}
		newLoc++;
		int oldLoc = findChildLocation(firstChild);
		if (oldLoc == (-1)) {
			throw new IllegalArgumentException("Asked to move a record that wasn't a child!");
		}
		moveChildRecords(oldLoc, newLoc, number);
	}

	public void setChildRecord(Record[] records) {
		this._children = records.clone();
	}

	public void writeOut(byte headerA, byte headerB, long type, Record[] children, OutputStream out) throws IOException {
		if (out instanceof MutableByteArrayOutputStream) {
			MutableByteArrayOutputStream mout = ((MutableByteArrayOutputStream) (out));
			int oldSize = mout.getBytesWritten();
			mout.write(new byte[]{ headerA, headerB });
			byte[] typeB = new byte[2];
			LittleEndian.putShort(typeB, 0, ((short) (type)));
			mout.write(typeB);
			mout.write(new byte[4]);
			for (Record aChildren : children) {
				aChildren.writeOut(mout);
			}
			int length = ((mout.getBytesWritten()) - oldSize) - 8;
			byte[] size = new byte[4];
			LittleEndian.putInt(size, 0, length);
			mout.overwrite(size, (oldSize + 4));
		}else {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			baos.write(new byte[]{ headerA, headerB });
			byte[] typeB = new byte[2];
			LittleEndian.putShort(typeB, 0, ((short) (type)));
			baos.write(typeB);
			baos.write(new byte[]{ 0, 0, 0, 0 });
			for (Record aChildren : children) {
				aChildren.writeOut(baos);
			}
			byte[] toWrite = baos.toByteArray();
			LittleEndian.putInt(toWrite, 4, ((toWrite.length) - 8));
			out.write(toWrite);
		}
	}

	public static void handleParentAwareRecords(RecordContainer br) {
		for (Record record : br.getChildRecords()) {
			if (record instanceof ParentAwareRecord) {
			}
			if (record instanceof RecordContainer) {
				RecordContainer.handleParentAwareRecords(((RecordContainer) (record)));
			}
		}
	}
}

