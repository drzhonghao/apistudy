

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import org.apache.accumulo.core.bloomfilter.BloomFilter;
import org.apache.accumulo.core.bloomfilter.Filter;
import org.apache.hadoop.util.bloom.Key;


public class DynamicBloomFilter extends Filter {
	private int nr;

	private int currentNbRecord;

	private BloomFilter[] matrix;

	public DynamicBloomFilter() {
	}

	public DynamicBloomFilter(final int vectorSize, final int nbHash, final int hashType, final int nr) {
		super(vectorSize, nbHash, hashType);
		this.nr = nr;
		this.currentNbRecord = 0;
		matrix = new BloomFilter[1];
		matrix[0] = new BloomFilter(this.vectorSize, this.nbHash, this.hashType);
	}

	@Override
	public boolean add(final Key key) {
		if (key == null) {
			throw new NullPointerException("Key can not be null");
		}
		BloomFilter bf = getActiveStandardBF();
		if (bf == null) {
			addRow();
			bf = matrix[((matrix.length) - 1)];
			currentNbRecord = 0;
		}
		boolean added = bf.add(key);
		if (added)
			(currentNbRecord)++;

		return added;
	}

	@Override
	public void and(final Filter filter) {
		DynamicBloomFilter dbf = ((DynamicBloomFilter) (filter));
		if (((dbf.matrix.length) != (this.matrix.length)) || ((dbf.nr) != (this.nr))) {
			throw new IllegalArgumentException("filters cannot be and-ed");
		}
		for (int i = 0; i < (matrix.length); i++) {
			matrix[i].and(dbf.matrix[i]);
		}
	}

	@Override
	public boolean membershipTest(final Key key) {
		if (key == null) {
			return true;
		}
		for (int i = 0; i < (matrix.length); i++) {
			if (matrix[i].membershipTest(key)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void not() {
		for (int i = 0; i < (matrix.length); i++) {
			matrix[i].not();
		}
	}

	@Override
	public void or(final Filter filter) {
		DynamicBloomFilter dbf = ((DynamicBloomFilter) (filter));
		if (((dbf.matrix.length) != (this.matrix.length)) || ((dbf.nr) != (this.nr))) {
			throw new IllegalArgumentException("filters cannot be or-ed");
		}
		for (int i = 0; i < (matrix.length); i++) {
			matrix[i].or(dbf.matrix[i]);
		}
	}

	@Override
	public void xor(final Filter filter) {
		DynamicBloomFilter dbf = ((DynamicBloomFilter) (filter));
		if (((dbf.matrix.length) != (this.matrix.length)) || ((dbf.nr) != (this.nr))) {
			throw new IllegalArgumentException("filters cannot be xor-ed");
		}
		for (int i = 0; i < (matrix.length); i++) {
			matrix[i].xor(dbf.matrix[i]);
		}
	}

	@Override
	public String toString() {
		StringBuilder res = new StringBuilder();
		for (int i = 0; i < (matrix.length); i++) {
			res.append(matrix[i]);
			res.append(Character.LINE_SEPARATOR);
		}
		return res.toString();
	}

	@Override
	public void write(final DataOutput out) throws IOException {
		super.write(out);
		out.writeInt(nr);
		out.writeInt(currentNbRecord);
		out.writeInt(matrix.length);
		for (int i = 0; i < (matrix.length); i++) {
			matrix[i].write(out);
		}
	}

	@Override
	public void readFields(final DataInput in) throws IOException {
		super.readFields(in);
		nr = in.readInt();
		currentNbRecord = in.readInt();
		int len = in.readInt();
		matrix = new BloomFilter[len];
		for (int i = 0; i < (matrix.length); i++) {
			matrix[i] = new BloomFilter();
			matrix[i].readFields(in);
		}
	}

	private void addRow() {
		BloomFilter[] tmp = new BloomFilter[(matrix.length) + 1];
		for (int i = 0; i < (matrix.length); i++) {
			tmp[i] = matrix[i];
		}
		tmp[((tmp.length) - 1)] = new BloomFilter(vectorSize, nbHash, hashType);
		matrix = tmp;
	}

	private BloomFilter getActiveStandardBF() {
		if ((currentNbRecord) >= (nr)) {
			return null;
		}
		return matrix[((matrix.length) - 1)];
	}
}

