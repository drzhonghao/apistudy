

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import org.apache.cassandra.index.sasi.utils.trie.Cursor;
import org.apache.cassandra.index.sasi.utils.trie.KeyAnalyzer;


abstract class AbstractPatriciaTrie<K, V> {
	private static final long serialVersionUID = -2303909182832019043L;

	final AbstractPatriciaTrie.TrieEntry<K, V> root = new AbstractPatriciaTrie.TrieEntry<>(null, null, (-1));

	private volatile transient Set<K> keySet;

	private volatile transient Collection<V> values;

	private volatile transient Set<Map.Entry<K, V>> entrySet;

	private int size = 0;

	transient int modCount = 0;

	public AbstractPatriciaTrie(KeyAnalyzer<? super K> keyAnalyzer) {
	}

	public AbstractPatriciaTrie(KeyAnalyzer<? super K> keyAnalyzer, Map<? extends K, ? extends V> m) {
	}

	public void clear() {
		root.bitIndex = -1;
		root.parent = null;
		root.left = root;
		root.right = null;
		root.predecessor = root;
		size = 0;
		incrementModCount();
	}

	public int size() {
		return size;
	}

	void incrementSize() {
		(size)++;
		incrementModCount();
	}

	void decrementSize() {
		(size)--;
		incrementModCount();
	}

	private void incrementModCount() {
		++(modCount);
	}

	public V put(K key, V value) {
		if (key == null)
			throw new NullPointerException("Key cannot be null");

		AbstractPatriciaTrie.TrieEntry<K, V> found = getNearestEntryForKey(key);
		return null;
	}

	AbstractPatriciaTrie.TrieEntry<K, V> addEntry(AbstractPatriciaTrie.TrieEntry<K, V> entry) {
		AbstractPatriciaTrie.TrieEntry<K, V> current = root.left;
		AbstractPatriciaTrie.TrieEntry<K, V> path = root;
		while (true) {
			if (((current.bitIndex) >= (entry.bitIndex)) || ((current.bitIndex) <= (path.bitIndex))) {
				entry.predecessor = entry;
				entry.parent = path;
				if ((current.bitIndex) >= (entry.bitIndex))
					current.parent = entry;

				if ((current.bitIndex) <= (path.bitIndex))
					current.predecessor = entry;

				return entry;
			}
			path = current;
		} 
	}

	public V get(Object k) {
		AbstractPatriciaTrie.TrieEntry<K, V> entry = getEntry(k);
		return null;
	}

	AbstractPatriciaTrie.TrieEntry<K, V> getEntry(Object k) {
		return null;
	}

	public Map.Entry<K, V> select(K key) {
		AbstractPatriciaTrie.Reference<Map.Entry<K, V>> reference = new AbstractPatriciaTrie.Reference<>();
		return !(selectR(root.left, (-1), key, reference)) ? reference.get() : null;
	}

	public Map.Entry<K, V> select(K key, Cursor<? super K, ? super V> cursor) {
		AbstractPatriciaTrie.Reference<Map.Entry<K, V>> reference = new AbstractPatriciaTrie.Reference<>();
		selectR(root.left, (-1), key, cursor, reference);
		return reference.get();
	}

	private boolean selectR(AbstractPatriciaTrie.TrieEntry<K, V> h, int bitIndex, final K key, final AbstractPatriciaTrie.Reference<Map.Entry<K, V>> reference) {
		if ((h.bitIndex) <= bitIndex) {
			if (!(h.isEmpty())) {
				return false;
			}
			return true;
		}
		return false;
	}

	private boolean selectR(AbstractPatriciaTrie.TrieEntry<K, V> h, int bitIndex, final K key, final Cursor<? super K, ? super V> cursor, final AbstractPatriciaTrie.Reference<Map.Entry<K, V>> reference) {
		if ((h.bitIndex) <= bitIndex) {
			if (!(h.isEmpty())) {
			}
			return true;
		}
		return false;
	}

	public Map.Entry<K, V> traverse(Cursor<? super K, ? super V> cursor) {
		AbstractPatriciaTrie.TrieEntry<K, V> entry = nextEntry(null);
		while (entry != null) {
			AbstractPatriciaTrie.TrieEntry<K, V> current = entry;
			entry = nextEntry(current);
		} 
		return null;
	}

	public boolean containsKey(Object k) {
		if (k == null)
			return false;

		return false;
	}

	public Set<Map.Entry<K, V>> entrySet() {
		if ((entrySet) == null)
			entrySet = new EntrySet();

		return entrySet;
	}

	public Set<K> keySet() {
		if ((keySet) == null)
			keySet = new KeySet();

		return keySet;
	}

	public Collection<V> values() {
		if ((values) == null)
			values = new Values();

		return values;
	}

	public V remove(Object k) {
		if (k == null)
			return null;

		AbstractPatriciaTrie.TrieEntry<K, V> current = root.left;
		AbstractPatriciaTrie.TrieEntry<K, V> path = root;
		while (true) {
			if ((current.bitIndex) <= (path.bitIndex)) {
			}
			path = current;
		} 
	}

	AbstractPatriciaTrie.TrieEntry<K, V> getNearestEntryForKey(K key) {
		AbstractPatriciaTrie.TrieEntry<K, V> current = root.left;
		AbstractPatriciaTrie.TrieEntry<K, V> path = root;
		while (true) {
			if ((current.bitIndex) <= (path.bitIndex))
				return current;

			path = current;
		} 
	}

	V removeEntry(AbstractPatriciaTrie.TrieEntry<K, V> h) {
		if (h != (root)) {
			if (h.isInternalNode()) {
				removeInternalEntry(h);
			}else {
				removeExternalEntry(h);
			}
		}
		decrementSize();
		return null;
	}

	private void removeExternalEntry(AbstractPatriciaTrie.TrieEntry<K, V> h) {
		if (h == (root)) {
			throw new IllegalArgumentException("Cannot delete root Entry!");
		}else
			if (!(h.isExternalNode())) {
				throw new IllegalArgumentException((h + " is not an external Entry!"));
			}

		AbstractPatriciaTrie.TrieEntry<K, V> parent = h.parent;
		AbstractPatriciaTrie.TrieEntry<K, V> child = ((h.left) == h) ? h.right : h.left;
		if ((parent.left) == h) {
			parent.left = child;
		}else {
			parent.right = child;
		}
		if ((child.bitIndex) > (parent.bitIndex)) {
			child.parent = parent;
		}else {
			child.predecessor = parent;
		}
	}

	private void removeInternalEntry(AbstractPatriciaTrie.TrieEntry<K, V> h) {
		if (h == (root)) {
			throw new IllegalArgumentException("Cannot delete root Entry!");
		}else
			if (!(h.isInternalNode())) {
				throw new IllegalArgumentException((h + " is not an internal Entry!"));
			}

		AbstractPatriciaTrie.TrieEntry<K, V> p = h.predecessor;
		p.bitIndex = h.bitIndex;
		{
			AbstractPatriciaTrie.TrieEntry<K, V> parent = p.parent;
			AbstractPatriciaTrie.TrieEntry<K, V> child = ((p.left) == h) ? p.right : p.left;
			if (((p.predecessor) == p) && ((p.parent) != h))
				p.predecessor = p.parent;

			if ((parent.left) == p) {
				parent.left = child;
			}else {
				parent.right = child;
			}
			if ((child.bitIndex) > (parent.bitIndex)) {
				child.parent = parent;
			}
		}
		{
			if ((h.left.parent) == h)
				h.left.parent = p;

			if ((h.right.parent) == h)
				h.right.parent = p;

			if ((h.parent.left) == h) {
				h.parent.left = p;
			}else {
				h.parent.right = p;
			}
		}
		p.parent = h.parent;
		p.left = h.left;
		p.right = h.right;
		if (AbstractPatriciaTrie.isValidUplink(p.left, p))
			p.left.predecessor = p;

		if (AbstractPatriciaTrie.isValidUplink(p.right, p))
			p.right.predecessor = p;

	}

	AbstractPatriciaTrie.TrieEntry<K, V> nextEntry(AbstractPatriciaTrie.TrieEntry<K, V> node) {
		return node == null ? firstEntry() : nextEntryImpl(node.predecessor, node, null);
	}

	AbstractPatriciaTrie.TrieEntry<K, V> nextEntryImpl(AbstractPatriciaTrie.TrieEntry<K, V> start, AbstractPatriciaTrie.TrieEntry<K, V> previous, AbstractPatriciaTrie.TrieEntry<K, V> tree) {
		AbstractPatriciaTrie.TrieEntry<K, V> current = start;
		if ((previous == null) || (start != (previous.predecessor))) {
			while (!(current.left.isEmpty())) {
				if (previous == (current.left))
					break;

				if (AbstractPatriciaTrie.isValidUplink(current.left, current))
					return current.left;

				current = current.left;
			} 
		}
		if (current.isEmpty())
			return null;

		if ((current.right) == null)
			return null;

		if (previous != (current.right)) {
			if (AbstractPatriciaTrie.isValidUplink(current.right, current))
				return current.right;

			return nextEntryImpl(current.right, previous, tree);
		}
		while (current == (current.parent.right)) {
			if (current == tree)
				return null;

			current = current.parent;
		} 
		if (current == tree)
			return null;

		if ((current.parent.right) == null)
			return null;

		if ((previous != (current.parent.right)) && (AbstractPatriciaTrie.isValidUplink(current.parent.right, current.parent)))
			return current.parent.right;

		if ((current.parent.right) == (current.parent))
			return null;

		return nextEntryImpl(current.parent.right, previous, tree);
	}

	AbstractPatriciaTrie.TrieEntry<K, V> firstEntry() {
		return null;
	}

	AbstractPatriciaTrie.TrieEntry<K, V> followLeft(AbstractPatriciaTrie.TrieEntry<K, V> node) {
		while (true) {
			AbstractPatriciaTrie.TrieEntry<K, V> child = node.left;
			if (child.isEmpty())
				child = node.right;

			if ((child.bitIndex) <= (node.bitIndex))
				return child;

			node = child;
		} 
	}

	static boolean isValidUplink(AbstractPatriciaTrie.TrieEntry<?, ?> next, AbstractPatriciaTrie.TrieEntry<?, ?> from) {
		return ((next != null) && ((next.bitIndex) <= (from.bitIndex))) && (!(next.isEmpty()));
	}

	private static class Reference<E> {
		private E item;

		public void set(E item) {
			this.item = item;
		}

		public E get() {
			return item;
		}
	}

	static class TrieEntry<K, V> {
		private static final long serialVersionUID = 4596023148184140013L;

		protected int bitIndex;

		protected AbstractPatriciaTrie.TrieEntry<K, V> parent;

		protected AbstractPatriciaTrie.TrieEntry<K, V> left;

		protected AbstractPatriciaTrie.TrieEntry<K, V> right;

		protected AbstractPatriciaTrie.TrieEntry<K, V> predecessor;

		public TrieEntry(K key, V value, int bitIndex) {
			this.bitIndex = bitIndex;
			this.parent = null;
			this.left = this;
			this.right = null;
			this.predecessor = this;
		}

		public boolean isEmpty() {
			return false;
		}

		public boolean isInternalNode() {
			return ((left) != (this)) && ((right) != (this));
		}

		public boolean isExternalNode() {
			return !(isInternalNode());
		}
	}

	private class EntrySet extends AbstractSet<Map.Entry<K, V>> {
		@Override
		public Iterator<Map.Entry<K, V>> iterator() {
			return new EntryIterator();
		}

		@Override
		public boolean contains(Object o) {
			if (!(o instanceof Map.Entry))
				return false;

			AbstractPatriciaTrie.TrieEntry<K, V> candidate = getEntry(((Map.Entry<?, ?>) (o)).getKey());
			return (candidate != null) && (candidate.equals(o));
		}

		@Override
		public boolean remove(Object o) {
			int size = size();
			AbstractPatriciaTrie.this.remove(o);
			return size != (size());
		}

		@Override
		public int size() {
			return AbstractPatriciaTrie.this.size();
		}

		@Override
		public void clear() {
			AbstractPatriciaTrie.this.clear();
		}

		private class EntryIterator extends AbstractPatriciaTrie<K, V>.TrieIterator<Map.Entry<K, V>> {
			@Override
			public Map.Entry<K, V> next() {
				return null;
			}
		}
	}

	private class KeySet extends AbstractSet<K> {
		@Override
		public Iterator<K> iterator() {
			return new KeyIterator();
		}

		@Override
		public int size() {
			return AbstractPatriciaTrie.this.size();
		}

		@Override
		public boolean contains(Object o) {
			return containsKey(o);
		}

		@Override
		public boolean remove(Object o) {
			int size = size();
			AbstractPatriciaTrie.this.remove(o);
			return size != (size());
		}

		@Override
		public void clear() {
			AbstractPatriciaTrie.this.clear();
		}

		private class KeyIterator extends AbstractPatriciaTrie<K, V>.TrieIterator<K> {
			@Override
			public K next() {
				return null;
			}
		}
	}

	private class Values extends AbstractCollection<V> {
		@Override
		public Iterator<V> iterator() {
			return new ValueIterator();
		}

		@Override
		public int size() {
			return AbstractPatriciaTrie.this.size();
		}

		@Override
		public boolean contains(Object o) {
			return false;
		}

		@Override
		public void clear() {
			AbstractPatriciaTrie.this.clear();
		}

		@Override
		public boolean remove(Object o) {
			for (Iterator<V> it = iterator(); it.hasNext();) {
				V value = it.next();
			}
			return false;
		}

		private class ValueIterator extends AbstractPatriciaTrie<K, V>.TrieIterator<V> {
			@Override
			public V next() {
				return null;
			}
		}
	}

	abstract class TrieIterator<E> implements Iterator<E> {
		protected int expectedModCount = AbstractPatriciaTrie.this.modCount;

		protected AbstractPatriciaTrie.TrieEntry<K, V> next;

		protected AbstractPatriciaTrie.TrieEntry<K, V> current;

		protected TrieIterator() {
			next = AbstractPatriciaTrie.this.nextEntry(null);
		}

		protected TrieIterator(AbstractPatriciaTrie.TrieEntry<K, V> firstEntry) {
			next = firstEntry;
		}

		protected AbstractPatriciaTrie.TrieEntry<K, V> nextEntry() {
			if ((expectedModCount) != (AbstractPatriciaTrie.this.modCount))
				throw new ConcurrentModificationException();

			AbstractPatriciaTrie.TrieEntry<K, V> e = next;
			if (e == null)
				throw new NoSuchElementException();

			next = findNext(e);
			current = e;
			return e;
		}

		protected AbstractPatriciaTrie.TrieEntry<K, V> findNext(AbstractPatriciaTrie.TrieEntry<K, V> prior) {
			return AbstractPatriciaTrie.this.nextEntry(prior);
		}

		@Override
		public boolean hasNext() {
			return (next) != null;
		}

		@Override
		public void remove() {
			if ((current) == null)
				throw new IllegalStateException();

			if ((expectedModCount) != (AbstractPatriciaTrie.this.modCount))
				throw new ConcurrentModificationException();

			AbstractPatriciaTrie.TrieEntry<K, V> node = current;
			current = null;
			AbstractPatriciaTrie.this.removeEntry(node);
			expectedModCount = AbstractPatriciaTrie.this.modCount;
		}
	}
}

