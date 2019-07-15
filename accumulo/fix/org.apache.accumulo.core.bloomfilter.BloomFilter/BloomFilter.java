

import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.BitSet;
import org.apache.accumulo.core.bloomfilter.Filter;
import org.apache.hadoop.util.bloom.HashFunction;
import org.apache.hadoop.util.bloom.Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class BloomFilter extends Filter {
	private static final Logger log = LoggerFactory.getLogger(BloomFilter.class);

	private static final byte[] bitvalues = new byte[]{ ((byte) (1)), ((byte) (2)), ((byte) (4)), ((byte) (8)), ((byte) (16)), ((byte) (32)), ((byte) (64)), ((byte) (128)) };

	BitSet bits;

	public BloomFilter() {
		super();
	}

	public BloomFilter(final int vectorSize, final int nbHash, final int hashType) {
		super(vectorSize, nbHash, hashType);
		bits = new BitSet(this.vectorSize);
	}

	@Override
	public boolean add(final Key key) {
		if (key == null) {
			throw new NullPointerException("key cannot be null");
		}
		int[] h = hash.hash(key);
		hash.clear();
		boolean bitsSet = false;
		for (int i = 0; i < (nbHash); i++) {
			bitsSet |= !(bits.get(h[i]));
			bits.set(h[i]);
		}
		return bitsSet;
	}

	@Override
	public void and(final Filter filter) {
		this.bits.and(((BloomFilter) (filter)).bits);
	}

	@Override
	public boolean membershipTest(final Key key) {
		if (key == null) {
			throw new NullPointerException("key cannot be null");
		}
		int[] h = hash.hash(key);
		hash.clear();
		for (int i = 0; i < (nbHash); i++) {
			if (!(bits.get(h[i]))) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void not() {
		bits.flip(0, ((vectorSize) - 1));
	}

	@Override
	public void or(final Filter filter) {
		bits.or(((BloomFilter) (filter)).bits);
	}

	@Override
	public void xor(final Filter filter) {
		bits.xor(((BloomFilter) (filter)).bits);
	}

	@Override
	public String toString() {
		return bits.toString();
	}

	public int getVectorSize() {
		return this.vectorSize;
	}

	@Override
	public void write(final DataOutput out) throws IOException {
		super.write(out);
		ByteArrayOutputStream boas = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(boas);
		oos.writeObject(bits);
		oos.flush();
		oos.close();
		out.write(boas.toByteArray());
	}

	@Override
	public void readFields(final DataInput in) throws IOException {
		super.readFields(in);
		bits = new BitSet(this.vectorSize);
		byte[] bytes = null;
		if ((super.getSerialVersion()) != (super.getVersion())) {
			bytes = new byte[getNBytes()];
			in.readFully(bytes);
		}
		if ((super.getSerialVersion()) == (super.getVersion())) {
			ObjectInputStream ois = new ObjectInputStream(((DataInputStream) (in)));
			try {
				bits = ((BitSet) (ois.readObject()));
			} catch (ClassNotFoundException e) {
				BloomFilter.log.error("BloomFilter tried to deserialize as bitset", e);
				throw new IOException(("BloomFilter tried to deserialize as bitset: " + e));
			}
		}else {
			for (int i = 0, byteIndex = 0, bitIndex = 0; i < (vectorSize); i++ , bitIndex++) {
				if (bitIndex == 8) {
					bitIndex = 0;
					byteIndex++;
				}
				if (((bytes[byteIndex]) & (BloomFilter.bitvalues[bitIndex])) != 0) {
					bits.set(i);
				}
			}
		}
	}

	private int getNBytes() {
		return ((vectorSize) + 7) / 8;
	}
}

