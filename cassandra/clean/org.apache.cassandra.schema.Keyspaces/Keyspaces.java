import org.apache.cassandra.schema.KeyspaceMetadata;
import org.apache.cassandra.schema.*;


import java.util.Iterator;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;

public final class Keyspaces implements Iterable<KeyspaceMetadata>
{
    private final ImmutableMap<String, KeyspaceMetadata> keyspaces;

    private Keyspaces(Builder builder)
    {
        keyspaces = builder.keyspaces.build();
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static Keyspaces none()
    {
        return builder().build();
    }

    public static Keyspaces of(KeyspaceMetadata... keyspaces)
    {
        return builder().add(keyspaces).build();
    }

    public Iterator<KeyspaceMetadata> iterator()
    {
        return keyspaces.values().iterator();
    }

    public Stream<KeyspaceMetadata> stream()
    {
        return keyspaces.values().stream();
    }

    public Keyspaces filter(Predicate<KeyspaceMetadata> predicate)
    {
        Builder builder = builder();
        stream().filter(predicate).forEach(builder::add);
        return builder.build();
    }

    MapDifference<String, KeyspaceMetadata> diff(Keyspaces other)
    {
        return Maps.difference(keyspaces, other.keyspaces);
    }

    @Override
    public boolean equals(Object o)
    {
        return this == o || (o instanceof Keyspaces && keyspaces.equals(((Keyspaces) o).keyspaces));
    }

    @Override
    public int hashCode()
    {
        return keyspaces.hashCode();
    }

    @Override
    public String toString()
    {
        return keyspaces.values().toString();
    }

    public static final class Builder
    {
        private final ImmutableMap.Builder<String, KeyspaceMetadata> keyspaces = new ImmutableMap.Builder<>();

        private Builder()
        {
        }

        public Keyspaces build()
        {
            return new Keyspaces(this);
        }

        public Builder add(KeyspaceMetadata keyspace)
        {
            keyspaces.put(keyspace.name, keyspace);
            return this;
        }

        public Builder add(KeyspaceMetadata... keyspaces)
        {
            for (KeyspaceMetadata keyspace : keyspaces)
                add(keyspace);
            return this;
        }

        public Builder add(Iterable<KeyspaceMetadata> keyspaces)
        {
            keyspaces.forEach(this::add);
            return this;
        }
    }
}
