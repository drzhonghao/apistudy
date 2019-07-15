

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.cassandra.cql3.Json;
import org.apache.cassandra.cql3.Sets;
import org.apache.cassandra.cql3.Term;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.db.marshal.CollectionType;
import org.apache.cassandra.db.marshal.EmptyType;
import org.apache.cassandra.db.marshal.FrozenType;
import org.apache.cassandra.db.marshal.ListType;
import org.apache.cassandra.db.marshal.TypeParser;
import org.apache.cassandra.db.rows.Cell;
import org.apache.cassandra.db.rows.CellPath;
import org.apache.cassandra.exceptions.ConfigurationException;
import org.apache.cassandra.exceptions.SyntaxException;
import org.apache.cassandra.serializers.MarshalException;
import org.apache.cassandra.serializers.SetSerializer;
import org.apache.cassandra.serializers.TypeSerializer;
import org.apache.cassandra.transport.ProtocolVersion;

import static org.apache.cassandra.db.marshal.AbstractType.ComparisonType.CUSTOM;
import static org.apache.cassandra.db.marshal.CollectionType.Kind.SET;


public class SetType<T> extends CollectionType<Set<T>> {
	private static final Map<AbstractType<?>, SetType> instances = new HashMap<>();

	private static final Map<AbstractType<?>, SetType> frozenInstances = new HashMap<>();

	private final AbstractType<T> elements;

	private final SetSerializer<T> serializer;

	private final boolean isMultiCell;

	public static SetType<?> getInstance(TypeParser parser) throws ConfigurationException, SyntaxException {
		List<AbstractType<?>> l = parser.getTypeParameters();
		if ((l.size()) != 1)
			throw new ConfigurationException("SetType takes exactly 1 type parameter");

		return SetType.getInstance(l.get(0), true);
	}

	public static synchronized <T> SetType<T> getInstance(AbstractType<T> elements, boolean isMultiCell) {
		Map<AbstractType<?>, SetType> internMap = (isMultiCell) ? SetType.instances : SetType.frozenInstances;
		SetType<T> t = internMap.get(elements);
		if (t == null) {
			t = new SetType<T>(elements, isMultiCell);
			internMap.put(elements, t);
		}
		return t;
	}

	public SetType(AbstractType<T> elements, boolean isMultiCell) {
		super(CUSTOM, SET);
		this.elements = elements;
		this.serializer = SetSerializer.getInstance(elements.getSerializer(), elements);
		this.isMultiCell = isMultiCell;
	}

	@Override
	public boolean referencesUserType(String userTypeName) {
		return getElementsType().referencesUserType(userTypeName);
	}

	public AbstractType<T> getElementsType() {
		return elements;
	}

	public AbstractType<T> nameComparator() {
		return elements;
	}

	public AbstractType<?> valueComparator() {
		return EmptyType.instance;
	}

	@Override
	public boolean isMultiCell() {
		return isMultiCell;
	}

	@Override
	public AbstractType<?> freeze() {
		if (isMultiCell)
			return SetType.getInstance(this.elements, false);
		else
			return this;

	}

	@Override
	public AbstractType<?> freezeNestedMulticellTypes() {
		if (!(isMultiCell()))
			return this;

		if ((elements.isFreezable()) && (elements.isMultiCell()))
			return SetType.getInstance(elements.freeze(), isMultiCell);

		return SetType.getInstance(elements.freezeNestedMulticellTypes(), isMultiCell);
	}

	@Override
	public boolean isCompatibleWithFrozen(CollectionType<?> previous) {
		assert !(isMultiCell);
		return this.elements.isCompatibleWith(((SetType) (previous)).elements);
	}

	@Override
	public boolean isValueCompatibleWithFrozen(CollectionType<?> previous) {
		return isCompatibleWithFrozen(previous);
	}

	@Override
	public int compareCustom(ByteBuffer o1, ByteBuffer o2) {
		return 0;
	}

	public SetSerializer<T> getSerializer() {
		return serializer;
	}

	@Override
	public String toString(boolean ignoreFreezing) {
		boolean includeFrozenType = (!ignoreFreezing) && (!(isMultiCell()));
		StringBuilder sb = new StringBuilder();
		if (includeFrozenType)
			sb.append(FrozenType.class.getName()).append("(");

		sb.append(getClass().getName());
		sb.append(TypeParser.stringifyTypeParameters(Collections.<AbstractType<?>>singletonList(elements), (ignoreFreezing || (!(isMultiCell)))));
		if (includeFrozenType)
			sb.append(")");

		return sb.toString();
	}

	public List<ByteBuffer> serializedValues(Iterator<Cell> cells) {
		List<ByteBuffer> bbs = new ArrayList<ByteBuffer>();
		while (cells.hasNext())
			bbs.add(cells.next().path().get(0));

		return bbs;
	}

	@Override
	public Term fromJSONObject(Object parsed) throws MarshalException {
		if (parsed instanceof String)
			parsed = Json.decodeJson(((String) (parsed)));

		if (!(parsed instanceof List))
			throw new MarshalException(String.format("Expected a list (representing a set), but got a %s: %s", parsed.getClass().getSimpleName(), parsed));

		List list = ((List) (parsed));
		Set<Term> terms = new HashSet<>(list.size());
		for (Object element : list) {
			if (element == null)
				throw new MarshalException("Invalid null element in set");

			terms.add(elements.fromJSONObject(element));
		}
		return new Sets.DelayedValue(elements, terms);
	}

	@Override
	public String toJSONString(ByteBuffer buffer, ProtocolVersion protocolVersion) {
		return ListType.setOrListToJsonString(buffer, elements, protocolVersion);
	}
}

