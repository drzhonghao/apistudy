

import com.google.common.primitives.UnsignedLongs;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.cassandra.cql3.CQL3Type;
import org.apache.cassandra.cql3.Constants;
import org.apache.cassandra.cql3.Term;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.db.marshal.TimeUUIDType;
import org.apache.cassandra.serializers.MarshalException;
import org.apache.cassandra.serializers.TypeSerializer;
import org.apache.cassandra.serializers.UUIDSerializer;
import org.apache.cassandra.utils.ByteBufferUtil;

import static org.apache.cassandra.cql3.CQL3Type.Native.UUID;
import static org.apache.cassandra.db.marshal.AbstractType.ComparisonType.CUSTOM;


public class UUIDType extends AbstractType<UUID> {
	public static final UUIDType instance = new UUIDType();

	UUIDType() {
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
		int version1 = ((int) ((msb1 >>> 12) & 15));
		int version2 = ((int) ((msb2 >>> 12) & 15));
		if (version1 != version2)
			return version1 - version2;

		if (version1 == 1) {
		}else {
			int c = UnsignedLongs.compare(msb1, msb2);
			if (c != 0)
				return c;

		}
		return UnsignedLongs.compare(b1.getLong((s1 + 8)), b2.getLong((s2 + 8)));
	}

	@Override
	public boolean isValueCompatibleWithInternal(AbstractType<?> otherType) {
		return (otherType instanceof UUIDType) || (otherType instanceof TimeUUIDType);
	}

	@Override
	public ByteBuffer fromString(String source) throws MarshalException {
		ByteBuffer parsed = UUIDType.parse(source);
		if (parsed != null)
			return parsed;

		throw new MarshalException(String.format("Unable to make UUID from '%s'", source));
	}

	@Override
	public CQL3Type asCQL3Type() {
		return UUID;
	}

	public TypeSerializer<UUID> getSerializer() {
		return UUIDSerializer.instance;
	}

	static final Pattern regexPattern = Pattern.compile("[A-Fa-f0-9]{8}\\-[A-Fa-f0-9]{4}\\-[A-Fa-f0-9]{4}\\-[A-Fa-f0-9]{4}\\-[A-Fa-f0-9]{12}");

	static ByteBuffer parse(String source) {
		if (source.isEmpty())
			return ByteBufferUtil.EMPTY_BYTE_BUFFER;

		if (UUIDType.regexPattern.matcher(source).matches()) {
			try {
			} catch (IllegalArgumentException e) {
				throw new MarshalException(String.format("Unable to make UUID from '%s'", source), e);
			}
		}
		return null;
	}

	@Override
	public Term fromJSONObject(Object parsed) throws MarshalException {
		try {
			return new Constants.Value(fromString(((String) (parsed))));
		} catch (ClassCastException exc) {
			throw new MarshalException(String.format("Expected a string representation of a uuid, but got a %s: %s", parsed.getClass().getSimpleName(), parsed));
		}
	}

	static int version(ByteBuffer uuid) {
		return ((uuid.get(6)) & 240) >> 4;
	}

	@Override
	protected int valueLengthIfFixed() {
		return 16;
	}
}

