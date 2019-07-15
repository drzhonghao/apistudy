

import com.google.common.base.Predicate;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.cassandra.cql3.CQL3Type;
import org.apache.cassandra.cql3.FieldIdentifier;
import org.apache.cassandra.db.marshal.UserType;
import org.apache.cassandra.exceptions.ConfigurationException;


public final class Types implements Iterable<UserType> {
	private static final Types NONE = new Types(ImmutableMap.of());

	private final Map<ByteBuffer, UserType> types;

	private Types(Types.Builder builder) {
		types = builder.types.build();
	}

	private Types(Map<ByteBuffer, UserType> types) {
		this.types = types;
	}

	public static Types.Builder builder() {
		return new Types.Builder();
	}

	public static Types.RawBuilder rawBuilder(String keyspace) {
		return new Types.RawBuilder(keyspace);
	}

	public static Types none() {
		return Types.NONE;
	}

	public static Types of(UserType... types) {
		return Types.builder().add(types).build();
	}

	public Iterator<UserType> iterator() {
		return types.values().iterator();
	}

	public Optional<UserType> get(ByteBuffer name) {
		return Optional.ofNullable(types.get(name));
	}

	@javax.annotation.Nullable
	public UserType getNullable(ByteBuffer name) {
		return types.get(name);
	}

	public Types with(UserType type) {
		if (get(type.name).isPresent())
			throw new IllegalStateException(String.format("Type %s already exists", type.name));

		return Types.builder().add(this).add(type).build();
	}

	public Types without(ByteBuffer name) {
		UserType type = get(name).orElseThrow(() -> new IllegalStateException(String.format("Type %s doesn't exists", name)));
		return Types.builder().add(Iterables.filter(this, ( t) -> t != type)).build();
	}

	MapDifference<ByteBuffer, UserType> diff(Types other) {
		return Maps.difference(types, other.types);
	}

	@Override
	public boolean equals(Object o) {
		if ((this) == o)
			return true;

		if (!(o instanceof Types))
			return false;

		Types other = ((Types) (o));
		if ((types.size()) != (other.types.size()))
			return false;

		Iterator<Map.Entry<ByteBuffer, UserType>> thisIter = this.types.entrySet().iterator();
		Iterator<Map.Entry<ByteBuffer, UserType>> otherIter = other.types.entrySet().iterator();
		while (thisIter.hasNext()) {
			Map.Entry<ByteBuffer, UserType> thisNext = thisIter.next();
			Map.Entry<ByteBuffer, UserType> otherNext = otherIter.next();
			if (!(thisNext.getKey().equals(otherNext.getKey())))
				return false;

			if (!(thisNext.getValue().equals(otherNext.getValue(), true)))
				return false;

		} 
		return true;
	}

	@Override
	public int hashCode() {
		return types.hashCode();
	}

	@Override
	public String toString() {
		return types.values().toString();
	}

	public static final class Builder {
		final ImmutableSortedMap.Builder<ByteBuffer, UserType> types = ImmutableSortedMap.naturalOrder();

		private Builder() {
		}

		public Types build() {
			return new Types(this);
		}

		public Types.Builder add(UserType type) {
			assert type.isMultiCell();
			types.put(type.name, type);
			return this;
		}

		public Types.Builder add(UserType... types) {
			for (UserType type : types)
				add(type);

			return this;
		}

		public Types.Builder add(Iterable<UserType> types) {
			types.forEach(this::add);
			return this;
		}
	}

	public static final class RawBuilder {
		final String keyspace;

		final List<Types.RawBuilder.RawUDT> definitions;

		private RawBuilder(String keyspace) {
			this.keyspace = keyspace;
			this.definitions = new ArrayList<>();
		}

		public Types build() {
			if (definitions.isEmpty())
				return Types.none();

			Map<Types.RawBuilder.RawUDT, Integer> vertices = new HashMap<>();
			for (Types.RawBuilder.RawUDT udt : definitions)
				vertices.put(udt, 0);

			Multimap<Types.RawBuilder.RawUDT, Types.RawBuilder.RawUDT> adjacencyList = HashMultimap.create();
			for (Types.RawBuilder.RawUDT udt1 : definitions)
				for (Types.RawBuilder.RawUDT udt2 : definitions)
					if ((udt1 != udt2) && (udt1.referencesUserType(udt2)))
						adjacencyList.put(udt2, udt1);



			adjacencyList.values().forEach(( vertex) -> vertices.put(vertex, ((vertices.get(vertex)) + 1)));
			Queue<Types.RawBuilder.RawUDT> resolvableTypes = new LinkedList<>();
			for (Map.Entry<Types.RawBuilder.RawUDT, Integer> entry : vertices.entrySet())
				if ((entry.getValue()) == 0)
					resolvableTypes.add(entry.getKey());


			Types types = new Types(new HashMap<>());
			while (!(resolvableTypes.isEmpty())) {
				Types.RawBuilder.RawUDT vertex = resolvableTypes.remove();
				for (Types.RawBuilder.RawUDT dependentType : adjacencyList.get(vertex))
					if ((vertices.replace(dependentType, ((vertices.get(dependentType)) - 1))) == 1)
						resolvableTypes.add(dependentType);


				UserType udt = vertex.prepare(keyspace, types);
				types.types.put(udt.name, udt);
			} 
			if ((types.types.size()) != (definitions.size()))
				throw new ConfigurationException(String.format("Cannot resolve UDTs for keyspace %s: some types are missing", keyspace));

			return Types.builder().add(types).build();
		}

		public void add(String name, List<String> fieldNames, List<String> fieldTypes) {
		}

		private static final class RawUDT {
			final String name;

			final List<String> fieldNames;

			final List<CQL3Type.Raw> fieldTypes;

			RawUDT(String name, List<String> fieldNames, List<CQL3Type.Raw> fieldTypes) {
				this.name = name;
				this.fieldNames = fieldNames;
				this.fieldTypes = fieldTypes;
			}

			boolean referencesUserType(Types.RawBuilder.RawUDT other) {
				return fieldTypes.stream().anyMatch(( t) -> t.referencesUserType(other.name));
			}

			UserType prepare(String keyspace, Types types) {
				List<FieldIdentifier> preparedFieldNames = fieldNames.stream().map(( t) -> FieldIdentifier.forInternalString(t)).collect(Collectors.toList());
				return null;
			}

			@Override
			public int hashCode() {
				return name.hashCode();
			}

			@Override
			public boolean equals(Object other) {
				return name.equals(((Types.RawBuilder.RawUDT) (other)).name);
			}
		}
	}
}

