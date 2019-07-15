

import java.util.HashSet;
import java.util.Set;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.db.marshal.SetType;
import org.apache.cassandra.stress.generate.values.Generator;
import org.apache.cassandra.stress.generate.values.GeneratorConfig;


public class Sets<T> extends Generator<Set<T>> {
	final Generator<T> valueType;

	public Sets(String name, Generator<T> valueType, GeneratorConfig config) {
		super(SetType.getInstance(valueType.type, true), config, name, Set.class);
		this.valueType = valueType;
	}

	public void setSeed(long seed) {
		super.setSeed(seed);
		valueType.setSeed((seed * 31));
	}

	@Override
	public Set<T> generate() {
		final Set<T> set = new HashSet<T>();
		return set;
	}
}

