

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.UUID;
import org.apache.cassandra.cql3.CQL3Type;
import org.apache.cassandra.cql3.Constants;
import org.apache.cassandra.cql3.Term;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.serializers.MarshalException;
import org.apache.cassandra.serializers.TimeUUIDSerializer;
import org.apache.cassandra.serializers.TypeSerializer;

import static org.apache.cassandra.cql3.CQL3Type.Native.TIMEUUID;
import static org.apache.cassandra.db.marshal.AbstractType.ComparisonType.CUSTOM;


public class TimeUUIDType extends AbstractType<UUID> {
	public static final TimeUUIDType instance = new TimeUUIDType();

	TimeUUIDType() {
		super(CUSTOM);
	}

	public boolean isEmptyValueMeaningless() {
		return true;
	}

	public int compareCustom(ByteBuffer b1, ByteBuffer b2) {
		int s1 = b1.position();
		int s2 = b2.position();
		int l1 = b1.limit();
		int l2 = b2.limit();
		boolean p1 = (l1 - s1) == 16;
		boolean p2 = (l2 - s2) == 16;
		if (!(p1 & p2)) {
			assert p1 | (l1 == s1);
			assert p2 | (l2 == s2);
			return p1 ? 1 : p2 ? -1 : 0;
		}
		long msb1 = b1.getLong(s1);
		long msb2 = b2.getLong(s2);
		msb1 = TimeUUIDType.reorderTimestampBytes(msb1);
		msb2 = TimeUUIDType.reorderTimestampBytes(msb2);
		assert (msb1 & (TimeUUIDType.topbyte(240L))) == (TimeUUIDType.topbyte(16L));
		assert (msb2 & (TimeUUIDType.topbyte(240L))) == (TimeUUIDType.topbyte(16L));
		int c = Long.compare(msb1, msb2);
		if (c != 0)
			return c;

		long lsb1 = TimeUUIDType.signedBytesToNativeLong(b1.getLong((s1 + 8)));
		long lsb2 = TimeUUIDType.signedBytesToNativeLong(b2.getLong((s2 + 8)));
		return Long.compare(lsb1, lsb2);
	}

	private static long signedBytesToNativeLong(long signedBytes) {
		return signedBytes ^ 36170086419038336L;
	}

	private static long topbyte(long topbyte) {
		return topbyte << 56;
	}

	protected static long reorderTimestampBytes(long input) {
		return ((input << 48) | ((input << 16) & 281470681743360L)) | (input >>> 32);
	}

	public ByteBuffer fromString(String source) throws MarshalException {
		return null;
	}

	@Override
	public Term fromJSONObject(Object parsed) throws MarshalException {
		try {
			return new Constants.Value(fromString(((String) (parsed))));
		} catch (ClassCastException exc) {
			throw new MarshalException(String.format("Expected a string representation of a timeuuid, but got a %s: %s", parsed.getClass().getSimpleName(), parsed));
		}
	}

	public CQL3Type asCQL3Type() {
		return TIMEUUID;
	}

	public TypeSerializer<UUID> getSerializer() {
		return TimeUUIDSerializer.instance;
	}

	@Override
	protected int valueLengthIfFixed() {
		return 16;
	}
}

