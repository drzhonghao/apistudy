

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.cassandra.io.sstable.format.SSTableReader;
import org.apache.cassandra.utils.Throwables;


class Helpers {
	static <T> Set<T> replace(Set<T> original, Set<T> remove, Iterable<T> add) {
		return ImmutableSet.copyOf(Helpers.replace(Helpers.identityMap(original), remove, add).keySet());
	}

	static <T> Map<T, T> replace(Map<T, T> original, Set<T> remove, Iterable<T> add) {
		for (T reader : remove)
			assert (original.get(reader)) == reader;

		assert !(Iterables.any(add, Predicates.and(Predicates.not(Predicates.in(remove)), Predicates.in(original.keySet())))) : String.format("original:%s remove:%s add:%s", original.keySet(), remove, add);
		Map<T, T> result = Helpers.identityMap(Iterables.concat(add, Iterables.filter(original.keySet(), Predicates.not(Predicates.in(remove)))));
		assert (result.size()) == (((original.size()) - (remove.size())) + (Iterables.size(add))) : String.format("Expecting new size of %d, got %d while replacing %s by %s in %s", (((original.size()) - (remove.size())) + (Iterables.size(add))), result.size(), remove, add, original.keySet());
		return result;
	}

	static void setupOnline(Iterable<SSTableReader> readers) {
		for (SSTableReader reader : readers)
			reader.setupOnline();

	}

	static Throwable setReplaced(Iterable<SSTableReader> readers, Throwable accumulate) {
		for (SSTableReader reader : readers) {
			try {
				reader.setReplaced();
			} catch (Throwable t) {
				accumulate = Throwables.merge(accumulate, t);
			}
		}
		return accumulate;
	}

	static void checkNotReplaced(Iterable<SSTableReader> readers) {
		for (SSTableReader reader : readers)
			assert !(reader.isReplaced());

	}

	static <T> Map<T, T> identityMap(Iterable<T> values) {
		ImmutableMap.Builder<T, T> builder = ImmutableMap.<T, T>builder();
		for (T t : values)
			builder.put(t, t);

		return builder.build();
	}

	static <T> Iterable<T> concatUniq(Set<T>... sets) {
		List<Predicate<T>> notIn = new ArrayList<>(sets.length);
		for (Set<T> set : sets)
			notIn.add(Predicates.not(Predicates.in(set)));

		List<Iterable<T>> results = new ArrayList<>(sets.length);
		for (int i = 0; i < (sets.length); i++)
			results.add(Iterables.filter(sets[i], Predicates.and(notIn.subList(0, i))));

		return Iterables.concat(results);
	}

	static <T> Predicate<T> notIn(Set<T>... sets) {
		return Predicates.not(Helpers.orIn(sets));
	}

	static <T> Predicate<T> orIn(Collection<T>... sets) {
		Predicate<T>[] orIn = new Predicate[sets.length];
		for (int i = 0; i < (orIn.length); i++)
			orIn[i] = Predicates.in(sets[i]);

		return Predicates.or(orIn);
	}

	static <T> Iterable<T> filterOut(Iterable<T> filter, Set<T>... inNone) {
		return Iterables.filter(filter, Helpers.notIn(inNone));
	}

	static <T> Iterable<T> filterIn(Iterable<T> filter, Set<T>... inAny) {
		return Iterables.filter(filter, Helpers.orIn(inAny));
	}

	static Set<SSTableReader> emptySet() {
		return Collections.emptySet();
	}

	static <T> T select(T t, Collection<T> col) {
		if ((col instanceof Set) && (!(col.contains(t))))
			return null;

		return Iterables.getFirst(Iterables.filter(col, Predicates.equalTo(t)), null);
	}

	static <T> T selectFirst(T t, Collection<T>... sets) {
		for (Collection<T> set : sets) {
			T select = Helpers.select(t, set);
			if (select != null)
				return select;

		}
		return null;
	}

	static <T> Predicate<T> idIn(Set<T> set) {
		return Helpers.idIn(Helpers.identityMap(set));
	}

	static <T> Predicate<T> idIn(final Map<T, T> identityMap) {
		return new Predicate<T>() {
			public boolean apply(T t) {
				return (identityMap.get(t)) == t;
			}
		};
	}
}

