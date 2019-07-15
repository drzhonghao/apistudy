

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import org.apache.lucene.analysis.CharArrayMap;


public class CharArraySet extends AbstractSet<Object> {
	public static final CharArraySet EMPTY_SET = new CharArraySet(CharArrayMap.<Object>emptyMap());

	private static final Object PLACEHOLDER = new Object();

	private final CharArrayMap<Object> map;

	public CharArraySet(int startSize, boolean ignoreCase) {
		this(new CharArrayMap<>(startSize, ignoreCase));
	}

	public CharArraySet(Collection<?> c, boolean ignoreCase) {
		this(c.size(), ignoreCase);
		addAll(c);
	}

	CharArraySet(final CharArrayMap<Object> map) {
		this.map = map;
	}

	@Override
	public void clear() {
		map.clear();
	}

	public boolean contains(char[] text, int off, int len) {
		return map.containsKey(text, off, len);
	}

	public boolean contains(CharSequence cs) {
		return map.containsKey(cs);
	}

	@Override
	public boolean contains(Object o) {
		return map.containsKey(o);
	}

	@Override
	public boolean add(Object o) {
		return (map.put(o, CharArraySet.PLACEHOLDER)) == null;
	}

	public boolean add(CharSequence text) {
		return (map.put(text, CharArraySet.PLACEHOLDER)) == null;
	}

	public boolean add(String text) {
		return (map.put(text, CharArraySet.PLACEHOLDER)) == null;
	}

	public boolean add(char[] text) {
		return (map.put(text, CharArraySet.PLACEHOLDER)) == null;
	}

	@Override
	public int size() {
		return map.size();
	}

	public static CharArraySet unmodifiableSet(CharArraySet set) {
		if (set == null)
			throw new NullPointerException("Given set is null");

		if (set == (CharArraySet.EMPTY_SET))
			return CharArraySet.EMPTY_SET;

		return new CharArraySet(CharArrayMap.unmodifiableMap(set.map));
	}

	public static CharArraySet copy(final Set<?> set) {
		if (set == (CharArraySet.EMPTY_SET))
			return CharArraySet.EMPTY_SET;

		if (set instanceof CharArraySet) {
			final CharArraySet source = ((CharArraySet) (set));
			return new CharArraySet(CharArrayMap.copy(source.map));
		}
		return new CharArraySet(set, false);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Iterator<Object> iterator() {
		return null;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("[");
		for (Object item : this) {
			if ((sb.length()) > 1)
				sb.append(", ");

			if (item instanceof char[]) {
				sb.append(((char[]) (item)));
			}else {
				sb.append(item);
			}
		}
		return sb.append(']').toString();
	}
}

