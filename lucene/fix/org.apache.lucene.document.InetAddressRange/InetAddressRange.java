

import java.net.InetAddress;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.InetAddressPoint;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.StringHelper;


public class InetAddressRange extends Field {
	public static final int BYTES = InetAddressPoint.BYTES;

	private static final FieldType TYPE;

	static {
		TYPE = new FieldType();
		InetAddressRange.TYPE.setDimensions(2, InetAddressRange.BYTES);
		InetAddressRange.TYPE.freeze();
	}

	public InetAddressRange(String name, final InetAddress min, final InetAddress max) {
		super(name, InetAddressRange.TYPE);
		setRangeValues(min, max);
	}

	public void setRangeValues(InetAddress min, InetAddress max) {
		final byte[] bytes;
		if ((fieldsData) == null) {
			bytes = new byte[(InetAddressRange.BYTES) * 2];
			fieldsData = new BytesRef(bytes);
		}else {
			bytes = ((BytesRef) (fieldsData)).bytes;
		}
		InetAddressRange.encode(min, max, bytes);
	}

	private static void encode(final InetAddress min, final InetAddress max, final byte[] bytes) {
		final byte[] minEncoded = InetAddressPoint.encode(min);
		final byte[] maxEncoded = InetAddressPoint.encode(max);
		if ((StringHelper.compare(InetAddressRange.BYTES, minEncoded, 0, maxEncoded, 0)) > 0) {
			throw new IllegalArgumentException("min value cannot be greater than max value for InetAddressRange field");
		}
		System.arraycopy(minEncoded, 0, bytes, 0, InetAddressRange.BYTES);
		System.arraycopy(maxEncoded, 0, bytes, InetAddressRange.BYTES, InetAddressRange.BYTES);
	}

	private static byte[] encode(InetAddress min, InetAddress max) {
		byte[] b = new byte[(InetAddressRange.BYTES) * 2];
		InetAddressRange.encode(min, max, b);
		return b;
	}

	public static Query newIntersectsQuery(String field, final InetAddress min, final InetAddress max) {
		return null;
	}

	public static Query newContainsQuery(String field, final InetAddress min, final InetAddress max) {
		return null;
	}

	public static Query newWithinQuery(String field, final InetAddress min, final InetAddress max) {
		return null;
	}

	public static Query newCrossesQuery(String field, final InetAddress min, final InetAddress max) {
		return null;
	}

	private static String toString(byte[] ranges, int dimension) {
		byte[] min = new byte[InetAddressRange.BYTES];
		System.arraycopy(ranges, 0, min, 0, InetAddressRange.BYTES);
		byte[] max = new byte[InetAddressRange.BYTES];
		System.arraycopy(ranges, InetAddressRange.BYTES, max, 0, InetAddressRange.BYTES);
		return ((("[" + (InetAddressPoint.decode(min))) + " : ") + (InetAddressPoint.decode(max))) + "]";
	}
}

