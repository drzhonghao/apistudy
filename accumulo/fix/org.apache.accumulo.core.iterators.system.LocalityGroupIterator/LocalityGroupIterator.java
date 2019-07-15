

import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.accumulo.core.data.ByteSequence;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.iterators.IteratorEnvironment;
import org.apache.accumulo.core.iterators.SortedKeyValueIterator;
import org.apache.accumulo.core.iterators.system.HeapIterator;
import org.apache.accumulo.core.iterators.system.InterruptibleIterator;
import org.apache.commons.lang.mutable.MutableLong;


public class LocalityGroupIterator extends HeapIterator implements InterruptibleIterator {
	private static final Collection<ByteSequence> EMPTY_CF_SET = Collections.emptySet();

	public static class LocalityGroup {
		private LocalityGroup(LocalityGroupIterator.LocalityGroup localityGroup, IteratorEnvironment env) {
			this(localityGroup.columnFamilies, localityGroup.isDefaultLocalityGroup);
			this.iterator = ((InterruptibleIterator) (localityGroup.iterator.deepCopy(env)));
		}

		public LocalityGroup(InterruptibleIterator iterator, Map<ByteSequence, MutableLong> columnFamilies, boolean isDefaultLocalityGroup) {
			this(columnFamilies, isDefaultLocalityGroup);
			this.iterator = iterator;
		}

		public LocalityGroup(Map<ByteSequence, MutableLong> columnFamilies, boolean isDefaultLocalityGroup) {
			this.isDefaultLocalityGroup = isDefaultLocalityGroup;
			this.columnFamilies = columnFamilies;
		}

		public InterruptibleIterator getIterator() {
			return iterator;
		}

		protected boolean isDefaultLocalityGroup;

		protected Map<ByteSequence, MutableLong> columnFamilies;

		private InterruptibleIterator iterator;
	}

	public static class LocalityGroupContext {
		final List<LocalityGroupIterator.LocalityGroup> groups;

		final LocalityGroupIterator.LocalityGroup defaultGroup;

		final Map<ByteSequence, LocalityGroupIterator.LocalityGroup> groupByCf;

		public LocalityGroupContext(LocalityGroupIterator.LocalityGroup[] groups) {
			this.groups = Collections.unmodifiableList(Arrays.asList(groups));
			this.groupByCf = new HashMap<>();
			LocalityGroupIterator.LocalityGroup foundDefault = null;
			for (LocalityGroupIterator.LocalityGroup group : groups) {
				if ((group.isDefaultLocalityGroup) && ((group.columnFamilies) == null)) {
					if (foundDefault != null) {
						throw new IllegalStateException("Found multiple default locality groups");
					}
					foundDefault = group;
				}else {
					for (Map.Entry<ByteSequence, MutableLong> entry : group.columnFamilies.entrySet()) {
						if ((entry.getValue().longValue()) > 0) {
							if (groupByCf.containsKey(entry.getKey())) {
								throw new IllegalStateException("Found the same cf in multiple locality groups");
							}
							groupByCf.put(entry.getKey(), group);
						}
					}
				}
			}
			defaultGroup = foundDefault;
		}
	}

	public static class LocalityGroupSeekCache {
		private ImmutableSet<ByteSequence> lastColumnFamilies;

		private volatile boolean lastInclusive;

		private Collection<LocalityGroupIterator.LocalityGroup> lastUsed;

		public ImmutableSet<ByteSequence> getLastColumnFamilies() {
			return lastColumnFamilies;
		}

		public boolean isLastInclusive() {
			return lastInclusive;
		}

		public Collection<LocalityGroupIterator.LocalityGroup> getLastUsed() {
			return lastUsed;
		}

		public int getNumLGSeeked() {
			return (lastUsed) == null ? 0 : lastUsed.size();
		}
	}

	private final LocalityGroupIterator.LocalityGroupContext lgContext;

	private LocalityGroupIterator.LocalityGroupSeekCache lgCache;

	private AtomicBoolean interruptFlag;

	public LocalityGroupIterator(LocalityGroupIterator.LocalityGroup[] groups) {
		super(groups.length);
		this.lgContext = new LocalityGroupIterator.LocalityGroupContext(groups);
	}

	@Override
	public void init(SortedKeyValueIterator<Key, Value> source, Map<String, String> options, IteratorEnvironment env) throws IOException {
		throw new UnsupportedOperationException();
	}

	static final Collection<LocalityGroupIterator.LocalityGroup> _seek(HeapIterator hiter, LocalityGroupIterator.LocalityGroupContext lgContext, Range range, Collection<ByteSequence> columnFamilies, boolean inclusive) throws IOException {
		Set<ByteSequence> cfSet;
		if ((columnFamilies.size()) > 0)
			if (columnFamilies instanceof Set<?>) {
				cfSet = ((Set<ByteSequence>) (columnFamilies));
			}else {
				cfSet = new HashSet<>();
				cfSet.addAll(columnFamilies);
			}
		else
			cfSet = Collections.emptySet();

		Collection<LocalityGroupIterator.LocalityGroup> groups = Collections.emptyList();
		if ((cfSet.size()) == 0) {
			if (!inclusive) {
				groups = lgContext.groups;
			}
		}else {
			groups = new HashSet<>();
			if ((lgContext.defaultGroup) != null) {
				if (inclusive) {
					if (!(lgContext.groupByCf.keySet().containsAll(cfSet))) {
						groups.add(lgContext.defaultGroup);
					}
				}else {
					groups.add(lgContext.defaultGroup);
				}
			}
			if (!inclusive) {
				for (Map.Entry<ByteSequence, LocalityGroupIterator.LocalityGroup> entry : lgContext.groupByCf.entrySet()) {
					if (!(cfSet.contains(entry.getKey()))) {
						groups.add(entry.getValue());
					}
				}
			}else
				if ((lgContext.groupByCf.size()) <= (cfSet.size())) {
					for (Map.Entry<ByteSequence, LocalityGroupIterator.LocalityGroup> entry : lgContext.groupByCf.entrySet()) {
						if (cfSet.contains(entry.getKey())) {
							groups.add(entry.getValue());
						}
					}
				}else {
					for (ByteSequence cf : cfSet) {
						LocalityGroupIterator.LocalityGroup group = lgContext.groupByCf.get(cf);
						if (group != null) {
							groups.add(group);
						}
					}
				}

		}
		for (LocalityGroupIterator.LocalityGroup lgr : groups) {
			lgr.getIterator().seek(range, LocalityGroupIterator.EMPTY_CF_SET, false);
		}
		return groups;
	}

	public static LocalityGroupIterator.LocalityGroupSeekCache seek(HeapIterator hiter, LocalityGroupIterator.LocalityGroupContext lgContext, Range range, Collection<ByteSequence> columnFamilies, boolean inclusive, LocalityGroupIterator.LocalityGroupSeekCache lgSeekCache) throws IOException {
		if (lgSeekCache == null)
			lgSeekCache = new LocalityGroupIterator.LocalityGroupSeekCache();

		boolean sameArgs = false;
		ImmutableSet<ByteSequence> cfSet = null;
		if (((lgSeekCache.lastUsed) != null) && (inclusive == (lgSeekCache.lastInclusive))) {
			if (columnFamilies instanceof Set) {
				sameArgs = lgSeekCache.lastColumnFamilies.equals(columnFamilies);
			}else {
				cfSet = ImmutableSet.copyOf(columnFamilies);
				sameArgs = lgSeekCache.lastColumnFamilies.equals(cfSet);
			}
		}
		if (sameArgs) {
			for (LocalityGroupIterator.LocalityGroup lgr : lgSeekCache.lastUsed) {
				lgr.getIterator().seek(range, LocalityGroupIterator.EMPTY_CF_SET, false);
			}
		}else {
			lgSeekCache.lastColumnFamilies = (cfSet == null) ? ImmutableSet.copyOf(columnFamilies) : cfSet;
			lgSeekCache.lastInclusive = inclusive;
			lgSeekCache.lastUsed = LocalityGroupIterator._seek(hiter, lgContext, range, columnFamilies, inclusive);
		}
		return lgSeekCache;
	}

	@Override
	public void seek(Range range, Collection<ByteSequence> columnFamilies, boolean inclusive) throws IOException {
		lgCache = LocalityGroupIterator.seek(this, lgContext, range, columnFamilies, inclusive, lgCache);
	}

	@Override
	public SortedKeyValueIterator<Key, Value> deepCopy(IteratorEnvironment env) {
		LocalityGroupIterator.LocalityGroup[] groupsCopy = new LocalityGroupIterator.LocalityGroup[lgContext.groups.size()];
		for (int i = 0; i < (lgContext.groups.size()); i++) {
			groupsCopy[i] = new LocalityGroupIterator.LocalityGroup(lgContext.groups.get(i), env);
			if ((interruptFlag) != null)
				groupsCopy[i].getIterator().setInterruptFlag(interruptFlag);

		}
		return new LocalityGroupIterator(groupsCopy);
	}

	@Override
	public void setInterruptFlag(AtomicBoolean flag) {
		this.interruptFlag = flag;
		for (LocalityGroupIterator.LocalityGroup lgr : lgContext.groups) {
			lgr.getIterator().setInterruptFlag(flag);
		}
	}
}

