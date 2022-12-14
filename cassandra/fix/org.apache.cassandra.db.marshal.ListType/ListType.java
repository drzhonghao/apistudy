

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.cassandra.cql3.Json;
import org.apache.cassandra.cql3.Lists;
import org.apache.cassandra.cql3.Term;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.db.marshal.CollectionType;
import org.apache.cassandra.db.marshal.FrozenType;
import org.apache.cassandra.db.marshal.TimeUUIDType;
import org.apache.cassandra.db.marshal.TypeParser;
import org.apache.cassandra.db.rows.Cell;
import org.apache.cassandra.exceptions.ConfigurationException;
import org.apache.cassandra.exceptions.SyntaxException;
import org.apache.cassandra.serializers.CollectionSerializer;
import org.apache.cassandra.serializers.ListSerializer;
import org.apache.cassandra.serializers.MarshalException;
import org.apache.cassandra.serializers.TypeSerializer;
import org.apache.cassandra.transport.ProtocolVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.cassandra.db.marshal.AbstractType.ComparisonType.CUSTOM;
import static org.apache.cassandra.db.marshal.CollectionType.Kind.LIST;


public class ListType<T> extends CollectionType<List<T>> {
	private static final Logger logger = LoggerFactory.getLogger(ListType.class);

	private static final Map<AbstractType<?>, ListType> instances = new HashMap<>();

	private static final Map<AbstractType<?>, ListType> frozenInstances = new HashMap<>();

	private final AbstractType<T> elements;

	public final ListSerializer<T> serializer;

	private final boolean isMultiCell;

	public static ListType<?> getInstance(TypeParser parser) throws ConfigurationException, SyntaxException {
		List<AbstractType<?>> l = parser.getTypeParameters();
		if ((l.size()) != 1)
			throw new ConfigurationException("ListType takes exactly 1 type parameter");

		return ListType.getInstance(l.get(0), true);
	}

	public static synchronized <T> ListType<T> getInstance(AbstractType<T> elements, boolean isMultiCell) {
		Map<AbstractType<?>, ListType> internMap = (isMultiCell) ? ListType.instances : ListType.frozenInstances;
		ListType<T> t = internMap.get(elements);
		if (t == null) {
			t = new ListType<T>(elements, isMultiCell);
			internMap.put(elements, t);
		}
		return t;
	}

	private ListType(AbstractType<T> elements, boolean isMultiCell) {
		super(CUSTOM, LIST);
		this.elements = elements;
		this.serializer = ListSerializer.getInstance(elements.getSerializer());
		this.isMultiCell = isMultiCell;
	}

	@Override
	public boolean referencesUserType(String userTypeName) {
		return getElementsType().referencesUserType(userTypeName);
	}

	@Override
	public boolean referencesDuration() {
		return getElementsType().referencesDuration();
	}

	public AbstractType<T> getElementsType() {
		return elements;
	}

	public AbstractType<UUID> nameComparator() {
		return TimeUUIDType.instance;
	}

	public AbstractType<T> valueComparator() {
		return elements;
	}

	public ListSerializer<T> getSerializer() {
		return serializer;
	}

	@Override
	public AbstractType<?> freeze() {
		if (isMultiCell)
			return ListType.getInstance(this.elements, false);
		else
			return this;

	}

	@Override
	public AbstractType<?> freezeNestedMulticellTypes() {
		if (!(isMultiCell()))
			return this;

		if ((elements.isFreezable()) && (elements.isMultiCell()))
			return ListType.getInstance(elements.freeze(), isMultiCell);

		return ListType.getInstance(elements.freezeNestedMulticellTypes(), isMultiCell);
	}

	@Override
	public boolean isMultiCell() {
		return isMultiCell;
	}

	@Override
	public boolean isCompatibleWithFrozen(CollectionType<?> previous) {
		assert !(isMultiCell);
		return this.elements.isCompatibleWith(((ListType) (previous)).elements);
	}

	@Override
	public boolean isValueCompatibleWithFrozen(CollectionType<?> previous) {
		assert !(isMultiCell);
		return false;
	}

	@Override
	public int compareCustom(ByteBuffer o1, ByteBuffer o2) {
		return ListType.compareListOrSet(elements, o1, o2);
	}

	static int compareListOrSet(AbstractType<?> elementsComparator, ByteBuffer o1, ByteBuffer o2) {
		if ((!(o1.hasRemaining())) || (!(o2.hasRemaining())))
			return o1.hasRemaining() ? 1 : o2.hasRemaining() ? -1 : 0;

		ByteBuffer bb1 = o1.duplicate();
		ByteBuffer bb2 = o2.duplicate();
		int size1 = CollectionSerializer.readCollectionSize(bb1, ProtocolVersion.V3);
		int size2 = CollectionSerializer.readCollectionSize(bb2, ProtocolVersion.V3);
		for (int i = 0; i < (Math.min(size1, size2)); i++) {
			ByteBuffer v1 = CollectionSerializer.readValue(bb1, ProtocolVersion.V3);
			ByteBuffer v2 = CollectionSerializer.readValue(bb2, ProtocolVersion.V3);
			int cmp = elementsComparator.compare(v1, v2);
			if (cmp != 0)
				return cmp;

		}
		return size1 == size2 ? 0 : size1 < size2 ? -1 : 1;
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
		assert isMultiCell;
		List<ByteBuffer> bbs = new ArrayList<ByteBuffer>();
		while (cells.hasNext())
			bbs.add(cells.next().value());

		return bbs;
	}

	@Override
	public Term fromJSONObject(Object parsed) throws MarshalException {
		if (parsed instanceof String)
			parsed = Json.decodeJson(((String) (parsed)));

		if (!(parsed instanceof List))
			throw new MarshalException(String.format("Expected a list, but got a %s: %s", parsed.getClass().getSimpleName(), parsed));

		List list = ((List) (parsed));
		List<Term> terms = new ArrayList<>(list.size());
		for (Object element : list) {
			if (element == null)
				throw new MarshalException("Invalid null element in list");

			terms.add(elements.fromJSONObject(element));
		}
		return new Lists.DelayedValue(terms);
	}

	public static String setOrListToJsonString(ByteBuffer buffer, AbstractType elementsType, ProtocolVersion protocolVersion) {
		ByteBuffer value = buffer.duplicate();
		StringBuilder sb = new StringBuilder("[");
		int size = CollectionSerializer.readCollectionSize(value, protocolVersion);
		for (int i = 0; i < size; i++) {
			if (i > 0)
				sb.append(", ");

			sb.append(elementsType.toJSONString(CollectionSerializer.readValue(value, protocolVersion), protocolVersion));
		}
		return sb.append("]").toString();
	}

	@Override
	public String toJSONString(ByteBuffer buffer, ProtocolVersion protocolVersion) {
		return ListType.setOrListToJsonString(buffer, elements, protocolVersion);
	}
}

