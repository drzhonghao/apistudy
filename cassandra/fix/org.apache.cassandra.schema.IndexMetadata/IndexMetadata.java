

import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.config.ColumnDefinition;
import org.apache.cassandra.cql3.ColumnIdentifier;
import org.apache.cassandra.cql3.ColumnSpecification;
import org.apache.cassandra.cql3.statements.IndexTarget;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.exceptions.ConfigurationException;
import org.apache.cassandra.index.Index;
import org.apache.cassandra.io.util.DataInputPlus;
import org.apache.cassandra.io.util.DataOutputPlus;
import org.apache.cassandra.utils.FBUtilities;
import org.apache.cassandra.utils.UUIDSerializer;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.cassandra.cql3.statements.IndexTarget.Type.FULL;
import static org.apache.cassandra.cql3.statements.IndexTarget.Type.KEYS;
import static org.apache.cassandra.cql3.statements.IndexTarget.Type.KEYS_AND_VALUES;
import static org.apache.cassandra.cql3.statements.IndexTarget.Type.VALUES;


public final class IndexMetadata {
	private static final Logger logger = LoggerFactory.getLogger(IndexMetadata.class);

	private static final Pattern PATTERN_NON_WORD_CHAR = Pattern.compile("\\W");

	private static final Pattern PATTERN_WORD_CHARS = Pattern.compile("\\w+");

	public static final IndexMetadata.Serializer serializer = new IndexMetadata.Serializer();

	public enum Kind {

		KEYS,
		CUSTOM,
		COMPOSITES;}

	public final UUID id;

	public final String name;

	public final IndexMetadata.Kind kind;

	public final Map<String, String> options;

	private IndexMetadata(String name, Map<String, String> options, IndexMetadata.Kind kind) {
		this.id = UUID.nameUUIDFromBytes(name.getBytes());
		this.name = name;
		this.options = (options == null) ? ImmutableMap.of() : ImmutableMap.copyOf(options);
		this.kind = kind;
	}

	public static IndexMetadata fromLegacyMetadata(CFMetaData cfm, ColumnDefinition column, String name, IndexMetadata.Kind kind, Map<String, String> options) {
		Map<String, String> newOptions = new HashMap<>();
		if (options != null)
			newOptions.putAll(options);

		IndexTarget target;
		if (newOptions.containsKey(IndexTarget.INDEX_KEYS_OPTION_NAME)) {
			newOptions.remove(IndexTarget.INDEX_KEYS_OPTION_NAME);
			target = new IndexTarget(column.name, KEYS);
		}else
			if (newOptions.containsKey(IndexTarget.INDEX_ENTRIES_OPTION_NAME)) {
				newOptions.remove(IndexTarget.INDEX_KEYS_OPTION_NAME);
				target = new IndexTarget(column.name, KEYS_AND_VALUES);
			}else {
				if ((column.type.isCollection()) && (!(column.type.isMultiCell()))) {
					target = new IndexTarget(column.name, FULL);
				}else {
					target = new IndexTarget(column.name, VALUES);
				}
			}

		newOptions.put(IndexTarget.TARGET_OPTION_NAME, target.asCqlString(cfm));
		return new IndexMetadata(name, newOptions, kind);
	}

	public static IndexMetadata fromSchemaMetadata(String name, IndexMetadata.Kind kind, Map<String, String> options) {
		return new IndexMetadata(name, options, kind);
	}

	public static IndexMetadata fromIndexTargets(CFMetaData cfm, List<IndexTarget> targets, String name, IndexMetadata.Kind kind, Map<String, String> options) {
		Map<String, String> newOptions = new HashMap<>(options);
		newOptions.put(IndexTarget.TARGET_OPTION_NAME, targets.stream().map(( target) -> target.asCqlString(cfm)).collect(Collectors.joining(", ")));
		return new IndexMetadata(name, newOptions, kind);
	}

	public static boolean isNameValid(String name) {
		return ((name != null) && (!(name.isEmpty()))) && (IndexMetadata.PATTERN_WORD_CHARS.matcher(name).matches());
	}

	public static String getDefaultIndexName(String cfName, String root) {
		if (root == null)
			return IndexMetadata.PATTERN_NON_WORD_CHAR.matcher(((cfName + "_") + "idx")).replaceAll("");
		else
			return IndexMetadata.PATTERN_NON_WORD_CHAR.matcher((((cfName + "_") + root) + "_idx")).replaceAll("");

	}

	public void validate(CFMetaData cfm) {
		if (!(IndexMetadata.isNameValid(name)))
			throw new ConfigurationException(("Illegal index name " + (name)));

		if ((kind) == null)
			throw new ConfigurationException(("Index kind is null for index " + (name)));

		if ((kind) == (IndexMetadata.Kind.CUSTOM)) {
			if (((options) == null) || (!(options.containsKey(IndexTarget.CUSTOM_INDEX_OPTION_NAME))))
				throw new ConfigurationException(String.format("Required option missing for index %s : %s", name, IndexTarget.CUSTOM_INDEX_OPTION_NAME));

			String className = options.get(IndexTarget.CUSTOM_INDEX_OPTION_NAME);
			Class<Index> indexerClass = FBUtilities.classForName(className, "custom indexer");
			if (!(Index.class.isAssignableFrom(indexerClass)))
				throw new ConfigurationException(String.format("Specified Indexer class (%s) does not implement the Indexer interface", className));

			validateCustomIndexOptions(cfm, indexerClass, options);
		}
	}

	private void validateCustomIndexOptions(CFMetaData cfm, Class<? extends Index> indexerClass, Map<String, String> options) throws ConfigurationException {
		try {
			Map<String, String> filteredOptions = Maps.filterKeys(options, ( key) -> !(key.equals(IndexTarget.CUSTOM_INDEX_OPTION_NAME)));
			if (filteredOptions.isEmpty())
				return;

			Map<?, ?> unknownOptions;
			try {
				unknownOptions = ((Map) (indexerClass.getMethod("validateOptions", Map.class, CFMetaData.class).invoke(null, filteredOptions, cfm)));
			} catch (NoSuchMethodException e) {
				unknownOptions = ((Map) (indexerClass.getMethod("validateOptions", Map.class).invoke(null, filteredOptions)));
			}
			if (!(unknownOptions.isEmpty()))
				throw new ConfigurationException(String.format("Properties specified %s are not understood by %s", unknownOptions.keySet(), indexerClass.getSimpleName()));

		} catch (NoSuchMethodException e) {
			IndexMetadata.logger.info("Indexer {} does not have a static validateOptions method. Validation ignored", indexerClass.getName());
		} catch (InvocationTargetException e) {
			if ((e.getTargetException()) instanceof ConfigurationException)
				throw ((ConfigurationException) (e.getTargetException()));

			throw new ConfigurationException(("Failed to validate custom indexer options: " + options));
		} catch (ConfigurationException e) {
			throw e;
		} catch (Exception e) {
			throw new ConfigurationException(("Failed to validate custom indexer options: " + options));
		}
	}

	public boolean isCustom() {
		return (kind) == (IndexMetadata.Kind.CUSTOM);
	}

	public boolean isKeys() {
		return (kind) == (IndexMetadata.Kind.KEYS);
	}

	public boolean isComposites() {
		return (kind) == (IndexMetadata.Kind.COMPOSITES);
	}

	public int hashCode() {
		return Objects.hashCode(id, name, kind, options);
	}

	public boolean equalsWithoutName(IndexMetadata other) {
		return (Objects.equal(kind, other.kind)) && (Objects.equal(options, other.options));
	}

	public boolean equals(Object obj) {
		if (obj == (this))
			return true;

		if (!(obj instanceof IndexMetadata))
			return false;

		IndexMetadata other = ((IndexMetadata) (obj));
		return ((Objects.equal(id, other.id)) && (Objects.equal(name, other.name))) && (equalsWithoutName(other));
	}

	public String toString() {
		return new ToStringBuilder(this).append("id", id.toString()).append("name", name).append("kind", kind).append("options", options).build();
	}

	public static class Serializer {
		public void serialize(IndexMetadata metadata, DataOutputPlus out, int version) throws IOException {
			UUIDSerializer.serializer.serialize(metadata.id, out, version);
		}

		public IndexMetadata deserialize(DataInputPlus in, int version, CFMetaData cfm) throws IOException {
			UUID id = UUIDSerializer.serializer.deserialize(in, version);
			return null;
		}

		public long serializedSize(IndexMetadata metadata, int version) {
			return UUIDSerializer.serializer.serializedSize(metadata.id, version);
		}
	}
}

