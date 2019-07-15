

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.cassandra.cql3.CQL3Type;
import org.apache.cassandra.cql3.Term;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.db.marshal.TypeParser;
import org.apache.cassandra.exceptions.ConfigurationException;
import org.apache.cassandra.exceptions.SyntaxException;
import org.apache.cassandra.serializers.MarshalException;
import org.apache.cassandra.serializers.TypeSerializer;
import org.apache.cassandra.transport.ProtocolVersion;

import static org.apache.cassandra.db.marshal.AbstractType.ComparisonType.CUSTOM;


public class ReversedType<T> extends AbstractType<T> {
	private static final Map<AbstractType<?>, ReversedType> instances = new HashMap<AbstractType<?>, ReversedType>();

	public final AbstractType<T> baseType;

	public static <T> ReversedType<T> getInstance(TypeParser parser) throws ConfigurationException, SyntaxException {
		List<AbstractType<?>> types = parser.getTypeParameters();
		if ((types.size()) != 1)
			throw new ConfigurationException((("ReversedType takes exactly one argument, " + (types.size())) + " given"));

		return ReversedType.getInstance(((AbstractType<T>) (types.get(0))));
	}

	public static synchronized <T> ReversedType<T> getInstance(AbstractType<T> baseType) {
		ReversedType<T> type = ReversedType.instances.get(baseType);
		if (type == null) {
			type = new ReversedType<T>(baseType);
			ReversedType.instances.put(baseType, type);
		}
		return type;
	}

	private ReversedType(AbstractType<T> baseType) {
		super(CUSTOM);
		this.baseType = baseType;
	}

	public boolean isEmptyValueMeaningless() {
		return baseType.isEmptyValueMeaningless();
	}

	public int compareCustom(ByteBuffer o1, ByteBuffer o2) {
		return baseType.compare(o2, o1);
	}

	@Override
	public int compareForCQL(ByteBuffer v1, ByteBuffer v2) {
		return baseType.compare(v1, v2);
	}

	public String getString(ByteBuffer bytes) {
		return baseType.getString(bytes);
	}

	public ByteBuffer fromString(String source) {
		return baseType.fromString(source);
	}

	@Override
	public Term fromJSONObject(Object parsed) throws MarshalException {
		return baseType.fromJSONObject(parsed);
	}

	@Override
	public String toJSONString(ByteBuffer buffer, ProtocolVersion protocolVersion) {
		return baseType.toJSONString(buffer, protocolVersion);
	}

	@Override
	public boolean isCompatibleWith(AbstractType<?> otherType) {
		if (!(otherType instanceof ReversedType))
			return false;

		return this.baseType.isCompatibleWith(((ReversedType) (otherType)).baseType);
	}

	@Override
	public boolean isValueCompatibleWith(AbstractType<?> otherType) {
		return this.baseType.isValueCompatibleWith(otherType);
	}

	@Override
	public CQL3Type asCQL3Type() {
		return baseType.asCQL3Type();
	}

	public TypeSerializer<T> getSerializer() {
		return baseType.getSerializer();
	}

	public boolean referencesUserType(String userTypeName) {
		return baseType.referencesUserType(userTypeName);
	}

	@Override
	protected int valueLengthIfFixed() {
		return 0;
	}

	@Override
	public boolean isReversed() {
		return true;
	}

	@Override
	public String toString() {
		return (((getClass().getName()) + "(") + (baseType)) + ")";
	}
}

