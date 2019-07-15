

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOError;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.db.BufferDecoratedKey;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.RowIndexEntry;
import org.apache.cassandra.dht.IPartitioner;
import org.apache.cassandra.dht.Token;
import org.apache.cassandra.io.FSWriteError;
import org.apache.cassandra.io.sstable.Component;
import org.apache.cassandra.io.sstable.Descriptor;
import org.apache.cassandra.io.sstable.format.Version;
import org.apache.cassandra.io.util.DiskOptimizationStrategy;
import org.apache.cassandra.io.util.FileUtils;
import org.apache.cassandra.io.util.RandomAccessReader;
import org.apache.cassandra.utils.ByteBufferUtil;
import org.apache.cassandra.utils.Pair;
import org.apache.cassandra.utils.memory.AbstractAllocator;
import org.apache.cassandra.utils.memory.HeapAllocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.cassandra.db.RowIndexEntry.Serializer.skip;


public abstract class SSTable {
	static final Logger logger = LoggerFactory.getLogger(SSTable.class);

	public static final int TOMBSTONE_HISTOGRAM_BIN_SIZE = 100;

	public static final int TOMBSTONE_HISTOGRAM_SPOOL_SIZE = 100000;

	public static final int TOMBSTONE_HISTOGRAM_TTL_ROUND_SECONDS = Integer.valueOf(System.getProperty("cassandra.streaminghistogram.roundseconds", "60"));

	public final Descriptor descriptor;

	protected final Set<Component> components;

	public final CFMetaData metadata;

	public final boolean compression;

	public DecoratedKey first;

	public DecoratedKey last;

	protected final DiskOptimizationStrategy optimizationStrategy;

	protected SSTable(Descriptor descriptor, Set<Component> components, CFMetaData metadata, DiskOptimizationStrategy optimizationStrategy) {
		assert descriptor != null;
		assert components != null;
		assert metadata != null;
		this.descriptor = descriptor;
		Set<Component> dataComponents = new HashSet<>(components);
		this.compression = dataComponents.contains(Component.COMPRESSION_INFO);
		this.components = new CopyOnWriteArraySet<>(dataComponents);
		this.metadata = metadata;
		this.optimizationStrategy = Objects.requireNonNull(optimizationStrategy);
	}

	public static boolean delete(Descriptor desc, Set<Component> components) {
		if (components.contains(Component.DATA))
			FileUtils.deleteWithConfirm(desc.filenameFor(Component.DATA));

		for (Component component : components) {
			if ((component.equals(Component.DATA)) || (component.equals(Component.SUMMARY)))
				continue;

			FileUtils.deleteWithConfirm(desc.filenameFor(component));
		}
		if (components.contains(Component.SUMMARY))
			FileUtils.delete(desc.filenameFor(Component.SUMMARY));

		SSTable.logger.trace("Deleted {}", desc);
		return true;
	}

	public IPartitioner getPartitioner() {
		return metadata.partitioner;
	}

	public DecoratedKey decorateKey(ByteBuffer key) {
		return getPartitioner().decorateKey(key);
	}

	public static DecoratedKey getMinimalKey(DecoratedKey key) {
		return (((key.getKey().position()) > 0) || (key.getKey().hasRemaining())) || (!(key.getKey().hasArray())) ? new BufferDecoratedKey(key.getToken(), HeapAllocator.instance.clone(key.getKey())) : key;
	}

	public String getFilename() {
		return descriptor.filenameFor(Component.DATA);
	}

	public String getIndexFilename() {
		return descriptor.filenameFor(Component.PRIMARY_INDEX);
	}

	public String getColumnFamilyName() {
		return descriptor.cfname;
	}

	public String getKeyspaceName() {
		return descriptor.ksname;
	}

	public List<String> getAllFilePaths() {
		List<String> ret = new ArrayList<>();
		for (Component component : components)
			ret.add(descriptor.filenameFor(component));

		return ret;
	}

	public static Pair<Descriptor, Component> tryComponentFromFilename(File dir, String name) {
		try {
			return Component.fromFilename(dir, name);
		} catch (Throwable e) {
			return null;
		}
	}

	public static Set<Component> componentsFor(final Descriptor desc) {
		try {
			try {
				return SSTable.readTOC(desc);
			} catch (FileNotFoundException e) {
				Set<Component> components = SSTable.discoverComponentsFor(desc);
				if (components.isEmpty())
					return components;

				if (!(components.contains(Component.TOC)))
					components.add(Component.TOC);

				SSTable.appendTOC(desc, components);
				return components;
			}
		} catch (IOException e) {
			throw new IOError(e);
		}
	}

	public static Set<Component> discoverComponentsFor(Descriptor desc) {
		return null;
	}

	protected long estimateRowsFromIndex(RandomAccessReader ifile) throws IOException {
		final int SAMPLES_CAP = 10000;
		final int BYTES_CAP = ((int) (Math.min(10000000, ifile.length())));
		int keys = 0;
		while (((ifile.getFilePointer()) < BYTES_CAP) && (keys < SAMPLES_CAP)) {
			ByteBufferUtil.skipShortLength(ifile);
			skip(ifile, descriptor.version);
			keys++;
		} 
		assert ((keys > 0) && ((ifile.getFilePointer()) > 0)) && ((ifile.length()) > 0) : "Unexpected empty index file: " + ifile;
		long estimatedRows = (ifile.length()) / ((ifile.getFilePointer()) / keys);
		ifile.seek(0);
		return estimatedRows;
	}

	public long bytesOnDisk() {
		long bytes = 0;
		for (Component component : components) {
			bytes += new File(descriptor.filenameFor(component)).length();
		}
		return bytes;
	}

	@Override
	public String toString() {
		return (((((getClass().getSimpleName()) + "(") + "path='") + (getFilename())) + '\'') + ')';
	}

	protected static Set<Component> readTOC(Descriptor descriptor) throws IOException {
		File tocFile = new File(descriptor.filenameFor(Component.TOC));
		List<String> componentNames = Files.readLines(tocFile, Charset.defaultCharset());
		Set<Component> components = Sets.newHashSetWithExpectedSize(componentNames.size());
		for (String componentName : componentNames) {
		}
		return components;
	}

	protected static void appendTOC(Descriptor descriptor, Collection<Component> components) {
		File tocFile = new File(descriptor.filenameFor(Component.TOC));
		try (PrintWriter w = new PrintWriter(new FileWriter(tocFile, true))) {
			for (Component component : components)
				w.println(component.name);

		} catch (IOException e) {
			throw new FSWriteError(e, tocFile);
		}
	}

	public synchronized void addComponents(Collection<Component> newComponents) {
		Collection<Component> componentsToAdd = Collections2.filter(newComponents, Predicates.not(Predicates.in(components)));
		SSTable.appendTOC(descriptor, componentsToAdd);
		components.addAll(componentsToAdd);
	}
}

