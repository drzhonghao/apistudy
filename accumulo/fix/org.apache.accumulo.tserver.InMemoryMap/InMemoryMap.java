

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.accumulo.core.client.SampleNotPresentException;
import org.apache.accumulo.core.client.sample.Sampler;
import org.apache.accumulo.core.client.sample.SamplerConfiguration;
import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.conf.ConfigurationCopy;
import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.core.conf.SiteConfiguration;
import org.apache.accumulo.core.data.ByteSequence;
import org.apache.accumulo.core.data.ColumnUpdate;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Mutation;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.file.FileOperations;
import org.apache.accumulo.core.file.FileSKVIterator;
import org.apache.accumulo.core.file.FileSKVWriter;
import org.apache.accumulo.core.file.rfile.RFile;
import org.apache.accumulo.core.file.rfile.RFileOperations;
import org.apache.accumulo.core.iterators.IteratorEnvironment;
import org.apache.accumulo.core.iterators.SortedKeyValueIterator;
import org.apache.accumulo.core.iterators.WrappingIterator;
import org.apache.accumulo.core.iterators.system.EmptyIterator;
import org.apache.accumulo.core.iterators.system.InterruptibleIterator;
import org.apache.accumulo.core.iterators.system.LocalityGroupIterator;
import org.apache.accumulo.core.iterators.system.SourceSwitchingIterator;
import org.apache.accumulo.core.sample.impl.SamplerConfigurationImpl;
import org.apache.accumulo.core.sample.impl.SamplerFactory;
import org.apache.accumulo.core.util.CachedConfiguration;
import org.apache.accumulo.core.util.LocalityGroupUtil;
import org.apache.accumulo.core.util.Pair;
import org.apache.accumulo.core.util.PreAllocatedArray;
import org.apache.accumulo.fate.util.UtilWaitThread;
import org.apache.accumulo.tserver.MemKey;
import org.apache.accumulo.tserver.MemValue;
import org.apache.accumulo.tserver.NativeMap;
import org.apache.commons.lang.mutable.MutableLong;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocalFileSystem;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class InMemoryMap {
	private InMemoryMap.SimpleMap map = null;

	private static final Logger log = LoggerFactory.getLogger(InMemoryMap.class);

	private volatile String memDumpFile = null;

	private final String memDumpDir;

	private final String mapType;

	private Map<String, Set<ByteSequence>> lggroups;

	private static Pair<SamplerConfigurationImpl, Sampler> getSampler(AccumuloConfiguration config) {
		try {
			SamplerConfigurationImpl sampleConfig = SamplerConfigurationImpl.newSamplerConfig(config);
			if (sampleConfig == null) {
				return new Pair<>(null, null);
			}
			return new Pair<>(sampleConfig, SamplerFactory.newSampler(sampleConfig, config));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static final String TYPE_NATIVE_MAP_WRAPPER = "NativeMapWrapper";

	public static final String TYPE_DEFAULT_MAP = "DefaultMap";

	public static final String TYPE_LOCALITY_GROUP_MAP = "LocalityGroupMap";

	public static final String TYPE_LOCALITY_GROUP_MAP_NATIVE = "LocalityGroupMap with native";

	private AtomicReference<Pair<SamplerConfigurationImpl, Sampler>> samplerRef = new AtomicReference<>(null);

	private AccumuloConfiguration config;

	private Sampler getOrCreateSampler() {
		Pair<SamplerConfigurationImpl, Sampler> pair = samplerRef.get();
		if (pair == null) {
			pair = InMemoryMap.getSampler(config);
			if (!(samplerRef.compareAndSet(null, pair))) {
				pair = samplerRef.get();
			}
		}
		return pair.getSecond();
	}

	public InMemoryMap(AccumuloConfiguration config) throws LocalityGroupUtil.LocalityGroupConfigurationError {
		boolean useNativeMap = config.getBoolean(Property.TSERV_NATIVEMAP_ENABLED);
		this.memDumpDir = config.get(Property.TSERV_MEMDUMP_DIR);
		this.lggroups = LocalityGroupUtil.getLocalityGroups(config);
		this.config = config;
		InMemoryMap.SimpleMap allMap;
		InMemoryMap.SimpleMap sampleMap;
		if ((lggroups.size()) == 0) {
			allMap = InMemoryMap.newMap(useNativeMap);
			sampleMap = InMemoryMap.newMap(useNativeMap);
			mapType = (useNativeMap) ? InMemoryMap.TYPE_NATIVE_MAP_WRAPPER : InMemoryMap.TYPE_DEFAULT_MAP;
		}else {
			allMap = new InMemoryMap.LocalityGroupMap(lggroups, useNativeMap);
			sampleMap = new InMemoryMap.LocalityGroupMap(lggroups, useNativeMap);
			mapType = (useNativeMap) ? InMemoryMap.TYPE_LOCALITY_GROUP_MAP_NATIVE : InMemoryMap.TYPE_LOCALITY_GROUP_MAP;
		}
		map = new InMemoryMap.SampleMap(allMap, sampleMap);
	}

	private static InMemoryMap.SimpleMap newMap(boolean useNativeMap) {
		if (useNativeMap && (NativeMap.isLoaded())) {
			try {
				return new InMemoryMap.NativeMapWrapper();
			} catch (Throwable t) {
				InMemoryMap.log.error("Failed to create native map", t);
			}
		}
		return new InMemoryMap.DefaultMap();
	}

	public String getMapType() {
		return mapType;
	}

	private interface SimpleMap {
		Value get(Key key);

		Iterator<Map.Entry<Key, Value>> iterator(Key startKey);

		int size();

		InterruptibleIterator skvIterator(SamplerConfigurationImpl samplerConfig);

		void delete();

		long getMemoryUsed();

		void mutate(List<Mutation> mutations, int kvCount);
	}

	private class SampleMap implements InMemoryMap.SimpleMap {
		private InMemoryMap.SimpleMap map;

		private InMemoryMap.SimpleMap sample;

		public SampleMap(InMemoryMap.SimpleMap map, InMemoryMap.SimpleMap sampleMap) {
			this.map = map;
			this.sample = sampleMap;
		}

		@Override
		public Value get(Key key) {
			return map.get(key);
		}

		@Override
		public Iterator<Map.Entry<Key, Value>> iterator(Key startKey) {
			throw new UnsupportedOperationException();
		}

		@Override
		public int size() {
			return map.size();
		}

		@Override
		public InterruptibleIterator skvIterator(SamplerConfigurationImpl samplerConfig) {
			if (samplerConfig == null)
				return map.skvIterator(null);
			else {
				Pair<SamplerConfigurationImpl, Sampler> samplerAndConf = samplerRef.get();
				if (samplerAndConf == null) {
					return EmptyIterator.EMPTY_ITERATOR;
				}else
					if (((samplerAndConf.getFirst()) != null) && (samplerAndConf.getFirst().equals(samplerConfig))) {
						return sample.skvIterator(null);
					}else {
						throw new SampleNotPresentException();
					}

			}
		}

		@Override
		public void delete() {
			map.delete();
			sample.delete();
		}

		@Override
		public long getMemoryUsed() {
			return (map.getMemoryUsed()) + (sample.getMemoryUsed());
		}

		@Override
		public void mutate(List<Mutation> mutations, int kvCount) {
			map.mutate(mutations, kvCount);
			Sampler sampler = getOrCreateSampler();
			if (sampler != null) {
				List<Mutation> sampleMutations = null;
				for (Mutation m : mutations) {
					List<ColumnUpdate> colUpdates = m.getUpdates();
					List<ColumnUpdate> sampleColUpdates = null;
					for (ColumnUpdate cvp : colUpdates) {
						Key k = new Key(m.getRow(), cvp.getColumnFamily(), cvp.getColumnQualifier(), cvp.getColumnVisibility(), cvp.getTimestamp(), cvp.isDeleted(), false);
						if (sampler.accept(k)) {
							if (sampleColUpdates == null) {
								sampleColUpdates = new ArrayList<>();
							}
							sampleColUpdates.add(cvp);
						}
					}
					if (sampleColUpdates != null) {
						if (sampleMutations == null) {
							sampleMutations = new ArrayList<>();
						}
						sampleMutations.add(new LocalityGroupUtil.PartitionedMutation(m.getRow(), sampleColUpdates));
					}
				}
				if (sampleMutations != null) {
					sample.mutate(sampleMutations, kvCount);
				}
			}
		}
	}

	private static class LocalityGroupMap implements InMemoryMap.SimpleMap {
		private PreAllocatedArray<Map<ByteSequence, MutableLong>> groupFams;

		private InMemoryMap.SimpleMap[] maps;

		private LocalityGroupUtil.Partitioner partitioner;

		private PreAllocatedArray<List<Mutation>> partitioned;

		LocalityGroupMap(Map<String, Set<ByteSequence>> groups, boolean useNativeMap) {
			this.groupFams = new PreAllocatedArray<>(groups.size());
			this.maps = new InMemoryMap.SimpleMap[(groups.size()) + 1];
			this.partitioned = new PreAllocatedArray<>(((groups.size()) + 1));
			for (int i = 0; i < (maps.length); i++) {
				maps[i] = InMemoryMap.newMap(useNativeMap);
			}
			int count = 0;
			for (Set<ByteSequence> cfset : groups.values()) {
				HashMap<ByteSequence, MutableLong> map = new HashMap<>();
				for (ByteSequence bs : cfset)
					map.put(bs, new MutableLong(1));

				this.groupFams.set((count++), map);
			}
			partitioner = new LocalityGroupUtil.Partitioner(this.groupFams);
			for (int i = 0; i < (partitioned.length); i++) {
				partitioned.set(i, new ArrayList<Mutation>());
			}
		}

		@Override
		public Value get(Key key) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Iterator<Map.Entry<Key, Value>> iterator(Key startKey) {
			throw new UnsupportedOperationException();
		}

		@Override
		public int size() {
			int sum = 0;
			for (InMemoryMap.SimpleMap map : maps)
				sum += map.size();

			return sum;
		}

		@Override
		public InterruptibleIterator skvIterator(SamplerConfigurationImpl samplerConfig) {
			if (samplerConfig != null)
				throw new SampleNotPresentException();

			LocalityGroupIterator.LocalityGroup[] groups = new LocalityGroupIterator.LocalityGroup[maps.length];
			for (int i = 0; i < (groups.length); i++) {
				if (i < (groupFams.length))
					groups[i] = new LocalityGroupIterator.LocalityGroup(maps[i].skvIterator(null), groupFams.get(i), false);
				else
					groups[i] = new LocalityGroupIterator.LocalityGroup(maps[i].skvIterator(null), null, true);

			}
			return new LocalityGroupIterator(groups);
		}

		@Override
		public void delete() {
			for (InMemoryMap.SimpleMap map : maps)
				map.delete();

		}

		@Override
		public long getMemoryUsed() {
			long sum = 0;
			for (InMemoryMap.SimpleMap map : maps)
				sum += map.getMemoryUsed();

			return sum;
		}

		@Override
		public synchronized void mutate(List<Mutation> mutations, int kvCount) {
			try {
				partitioner.partition(mutations, partitioned);
				for (int i = 0; i < (partitioned.length); i++) {
					if ((partitioned.get(i).size()) > 0) {
						maps[i].mutate(partitioned.get(i), kvCount);
						for (Mutation m : partitioned.get(i))
							kvCount += m.getUpdates().size();

					}
				}
			} finally {
				for (List<Mutation> list : partitioned) {
					list.clear();
				}
			}
		}
	}

	private static class DefaultMap implements InMemoryMap.SimpleMap {
		private AtomicLong bytesInMemory = new AtomicLong();

		private AtomicInteger size = new AtomicInteger();

		public void put(Key key, Value value) {
			bytesInMemory.addAndGet(((key.getLength()) + 4));
			bytesInMemory.addAndGet(value.getSize());
		}

		@Override
		public Value get(Key key) {
			return null;
		}

		@Override
		public Iterator<Map.Entry<Key, Value>> iterator(Key startKey) {
			Key lk = new Key(startKey);
			return null;
		}

		@Override
		public int size() {
			return size.get();
		}

		@Override
		public InterruptibleIterator skvIterator(SamplerConfigurationImpl samplerConfig) {
			if (samplerConfig != null)
				throw new SampleNotPresentException();

			return null;
		}

		@Override
		public synchronized void delete() {
		}

		public long getOverheadPerEntry() {
			return 200;
		}

		@Override
		public void mutate(List<Mutation> mutations, int kvCount) {
			for (Mutation m : mutations) {
				for (ColumnUpdate cvp : m.getUpdates()) {
					Key newKey = new MemKey(m.getRow(), cvp.getColumnFamily(), cvp.getColumnQualifier(), cvp.getColumnVisibility(), cvp.getTimestamp(), cvp.isDeleted(), false, (kvCount++));
					Value value = new Value(cvp.getValue());
					put(newKey, value);
				}
			}
		}

		@Override
		public long getMemoryUsed() {
			return (bytesInMemory.get()) + ((size()) * (getOverheadPerEntry()));
		}
	}

	private static class NativeMapWrapper implements InMemoryMap.SimpleMap {
		private NativeMap nativeMap;

		NativeMapWrapper() {
			nativeMap = new NativeMap();
		}

		@Override
		public Value get(Key key) {
			return nativeMap.get(key);
		}

		@Override
		public Iterator<Map.Entry<Key, Value>> iterator(Key startKey) {
			return nativeMap.iterator(startKey);
		}

		@Override
		public int size() {
			return nativeMap.size();
		}

		@Override
		public InterruptibleIterator skvIterator(SamplerConfigurationImpl samplerConfig) {
			if (samplerConfig != null)
				throw new SampleNotPresentException();

			return ((InterruptibleIterator) (nativeMap.skvIterator()));
		}

		@Override
		public void delete() {
			nativeMap.delete();
		}

		@Override
		public long getMemoryUsed() {
			return nativeMap.getMemoryUsed();
		}

		@Override
		public void mutate(List<Mutation> mutations, int kvCount) {
		}
	}

	private AtomicInteger nextKVCount = new AtomicInteger(1);

	private AtomicInteger kvCount = new AtomicInteger(0);

	private Object writeSerializer = new Object();

	public void mutate(List<Mutation> mutations) {
		int numKVs = 0;
		for (int i = 0; i < (mutations.size()); i++)
			numKVs += mutations.get(i).size();

		synchronized(writeSerializer) {
			int kv = nextKVCount.getAndAdd(numKVs);
			try {
				map.mutate(mutations, kv);
			} finally {
				kvCount.set(((kv + numKVs) - 1));
			}
		}
	}

	public synchronized long estimatedSizeInBytes() {
		if ((map) == null)
			return 0;

		return map.getMemoryUsed();
	}

	Iterator<Map.Entry<Key, Value>> iterator(Key startKey) {
		return map.iterator(startKey);
	}

	public synchronized long getNumEntries() {
		if ((map) == null)
			return 0;

		return map.size();
	}

	private final Set<InMemoryMap.MemoryIterator> activeIters = Collections.synchronizedSet(new HashSet<InMemoryMap.MemoryIterator>());

	class MemoryDataSource implements SourceSwitchingIterator.DataSource {
		private boolean switched = false;

		private InterruptibleIterator iter;

		private FileSKVIterator reader;

		private InMemoryMap.MemoryDataSource parent;

		private IteratorEnvironment env;

		private AtomicBoolean iflag;

		private SamplerConfigurationImpl iteratorSamplerConfig;

		private SamplerConfigurationImpl getSamplerConfig() {
			if ((env) != null) {
				if (env.isSamplingEnabled()) {
					return new SamplerConfigurationImpl(env.getSamplerConfiguration());
				}else {
					return null;
				}
			}else {
				return iteratorSamplerConfig;
			}
		}

		MemoryDataSource(SamplerConfigurationImpl samplerConfig) {
			this(null, false, null, null, samplerConfig);
		}

		public MemoryDataSource(InMemoryMap.MemoryDataSource parent, boolean switched, IteratorEnvironment env, AtomicBoolean iflag, SamplerConfigurationImpl samplerConfig) {
			this.parent = parent;
			this.switched = switched;
			this.env = env;
			this.iflag = iflag;
			this.iteratorSamplerConfig = samplerConfig;
		}

		@Override
		public boolean isCurrent() {
			if (switched)
				return true;
			else
				return (memDumpFile) == null;

		}

		@Override
		public SourceSwitchingIterator.DataSource getNewDataSource() {
			if (switched)
				throw new IllegalStateException();

			if (!(isCurrent())) {
				switched = true;
				iter = null;
				try {
					iterator();
				} catch (IOException e) {
					throw new RuntimeException();
				}
			}
			return this;
		}

		private synchronized FileSKVIterator getReader() throws IOException {
			if ((reader) == null) {
				Configuration conf = CachedConfiguration.getInstance();
				FileSystem fs = FileSystem.getLocal(conf);
				reader = new RFileOperations().newReaderBuilder().forFile(memDumpFile, fs, conf).withTableConfiguration(SiteConfiguration.getInstance()).seekToBeginning().build();
				if ((iflag) != null)
					reader.setInterruptFlag(iflag);

				if ((getSamplerConfig()) != null) {
					reader = reader.getSample(getSamplerConfig());
				}
			}
			return reader;
		}

		@Override
		public SortedKeyValueIterator<Key, Value> iterator() throws IOException {
			if ((iter) == null)
				if (!(switched)) {
					iter = map.skvIterator(getSamplerConfig());
					if ((iflag) != null)
						iter.setInterruptFlag(iflag);

				}else {
					if ((parent) == null) {
					}else
						synchronized(parent) {
						}

				}

			return iter;
		}

		@Override
		public SourceSwitchingIterator.DataSource getDeepCopyDataSource(IteratorEnvironment env) {
			return new InMemoryMap.MemoryDataSource(((parent) == null ? this : parent), switched, env, iflag, iteratorSamplerConfig);
		}

		@Override
		public void setInterruptFlag(AtomicBoolean flag) {
			this.iflag = flag;
		}
	}

	public class MemoryIterator extends WrappingIterator implements InterruptibleIterator {
		private AtomicBoolean closed;

		private SourceSwitchingIterator ssi;

		private InMemoryMap.MemoryDataSource mds;

		private MemoryIterator(InterruptibleIterator source) {
			this(source, new AtomicBoolean(false));
		}

		private MemoryIterator(SortedKeyValueIterator<Key, Value> source, AtomicBoolean closed) {
			setSource(source);
			this.closed = closed;
		}

		@Override
		public SortedKeyValueIterator<Key, Value> deepCopy(IteratorEnvironment env) {
			return new InMemoryMap.MemoryIterator(getSource().deepCopy(env), closed);
		}

		public void close() {
			synchronized(this) {
				if (closed.compareAndSet(false, true)) {
					try {
						if ((mds.reader) != null)
							mds.reader.close();

					} catch (IOException e) {
						InMemoryMap.log.warn("{}", e.getMessage(), e);
					}
				}
			}
			activeIters.remove(this);
		}

		private synchronized boolean switchNow() throws IOException {
			if (closed.get())
				return false;

			ssi.switchNow();
			return true;
		}

		@Override
		public void setInterruptFlag(AtomicBoolean flag) {
			((InterruptibleIterator) (getSource())).setInterruptFlag(flag);
		}

		private void setSSI(SourceSwitchingIterator ssi) {
			this.ssi = ssi;
		}

		public void setMDS(InMemoryMap.MemoryDataSource mds) {
			this.mds = mds;
		}
	}

	public synchronized InMemoryMap.MemoryIterator skvIterator(SamplerConfigurationImpl iteratorSamplerConfig) {
		if ((map) == null)
			throw new NullPointerException();

		if (deleted)
			throw new IllegalStateException("Can not obtain iterator after map deleted");

		int mc = kvCount.get();
		InMemoryMap.MemoryDataSource mds = new InMemoryMap.MemoryDataSource(iteratorSamplerConfig);
		SourceSwitchingIterator ssi = new SourceSwitchingIterator(mds);
		return null;
	}

	public SortedKeyValueIterator<Key, Value> compactionIterator() {
		if (((nextKVCount.get()) - 1) != (kvCount.get()))
			throw new IllegalStateException(((("Memory map in unexpected state : nextKVCount = " + (nextKVCount.get())) + " kvCount = ") + (kvCount.get())));

		return map.skvIterator(null);
	}

	private boolean deleted = false;

	public void delete(long waitTime) {
		synchronized(this) {
			if (deleted)
				throw new IllegalStateException("Double delete");

			deleted = true;
		}
		long t1 = System.currentTimeMillis();
		while (((activeIters.size()) > 0) && (((System.currentTimeMillis()) - t1) < waitTime)) {
			UtilWaitThread.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);
		} 
		if ((activeIters.size()) > 0) {
			try {
				Configuration conf = CachedConfiguration.getInstance();
				FileSystem fs = FileSystem.getLocal(conf);
				String tmpFile = ((((memDumpDir) + "/memDump") + (UUID.randomUUID())) + ".") + (RFile.EXTENSION);
				Configuration newConf = new Configuration(conf);
				newConf.setInt("io.seqfile.compress.blocksize", 100000);
				AccumuloConfiguration siteConf = SiteConfiguration.getInstance();
				if ((getOrCreateSampler()) != null) {
					siteConf = createSampleConfig(siteConf);
				}
				FileSKVWriter out = new RFileOperations().newWriterBuilder().forFile(tmpFile, fs, newConf).withTableConfiguration(siteConf).build();
				InterruptibleIterator iter = map.skvIterator(null);
				HashSet<ByteSequence> allfams = new HashSet<>();
				for (Map.Entry<String, Set<ByteSequence>> entry : lggroups.entrySet()) {
					allfams.addAll(entry.getValue());
					out.startNewLocalityGroup(entry.getKey(), entry.getValue());
					iter.seek(new Range(), entry.getValue(), true);
					dumpLocalityGroup(out, iter);
				}
				out.startDefaultLocalityGroup();
				iter.seek(new Range(), allfams, false);
				dumpLocalityGroup(out, iter);
				out.close();
				InMemoryMap.log.debug(("Created mem dump file " + tmpFile));
				memDumpFile = tmpFile;
				synchronized(activeIters) {
					for (InMemoryMap.MemoryIterator mi : activeIters) {
						mi.switchNow();
					}
				}
				fs.delete(new Path(memDumpFile), true);
			} catch (IOException ioe) {
				InMemoryMap.log.error("Failed to create mem dump file ", ioe);
				while ((activeIters.size()) > 0) {
					UtilWaitThread.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
				} 
			}
		}
		InMemoryMap.SimpleMap tmpMap = map;
		synchronized(this) {
			map = null;
		}
		tmpMap.delete();
	}

	private AccumuloConfiguration createSampleConfig(AccumuloConfiguration siteConf) {
		ConfigurationCopy confCopy = new ConfigurationCopy(Iterables.filter(siteConf, new Predicate<Map.Entry<String, String>>() {
			@Override
			public boolean apply(Map.Entry<String, String> input) {
				return !(input.getKey().startsWith(Property.TABLE_SAMPLER.getKey()));
			}
		}));
		for (Map.Entry<String, String> entry : samplerRef.get().getFirst().toTablePropertiesMap().entrySet()) {
			confCopy.set(entry.getKey(), entry.getValue());
		}
		siteConf = confCopy;
		return siteConf;
	}

	private void dumpLocalityGroup(FileSKVWriter out, InterruptibleIterator iter) throws IOException {
		while ((iter.hasTop()) && ((activeIters.size()) > 0)) {
			out.append(iter.getTopKey(), MemValue.encode(iter.getTopValue(), ((MemKey) (iter.getTopKey())).getKVCount()));
			iter.next();
		} 
	}
}

