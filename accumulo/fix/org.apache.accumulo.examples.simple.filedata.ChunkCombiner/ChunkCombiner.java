

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.apache.accumulo.core.data.ByteSequence;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.PartialKey;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.iterators.IteratorEnvironment;
import org.apache.accumulo.core.iterators.SortedKeyValueIterator;
import org.apache.accumulo.examples.simple.filedata.FileDataIngest;
import org.apache.accumulo.examples.simple.filedata.VisibilityCombiner;
import org.apache.hadoop.io.Text;


public class ChunkCombiner implements SortedKeyValueIterator<Key, Value> {
	private SortedKeyValueIterator<Key, Value> source;

	private SortedKeyValueIterator<Key, Value> refsSource;

	private static final Collection<ByteSequence> refsColf = Collections.singleton(FileDataIngest.REFS_CF_BS);

	private Map<Text, byte[]> lastRowVC = Collections.emptyMap();

	private Key topKey = null;

	private Value topValue = null;

	public ChunkCombiner() {
	}

	@Override
	public void init(SortedKeyValueIterator<Key, Value> source, Map<String, String> options, IteratorEnvironment env) throws IOException {
		this.source = source;
		this.refsSource = source.deepCopy(env);
	}

	@Override
	public boolean hasTop() {
		return (topKey) != null;
	}

	@Override
	public void next() throws IOException {
		findTop();
	}

	@Override
	public void seek(Range range, Collection<ByteSequence> columnFamilies, boolean inclusive) throws IOException {
		source.seek(range, columnFamilies, inclusive);
		findTop();
	}

	private void findTop() throws IOException {
		do {
			topKey = null;
			topValue = null;
		} while ((source.hasTop()) && ((_findTop()) == null) );
	}

	private byte[] _findTop() throws IOException {
		long maxTS;
		topKey = new Key(source.getTopKey());
		topValue = new Value(source.getTopValue());
		source.next();
		if (!(topKey.getColumnFamilyData().equals(FileDataIngest.CHUNK_CF_BS)))
			return topKey.getColumnVisibility().getBytes();

		maxTS = topKey.getTimestamp();
		while ((source.hasTop()) && (source.getTopKey().equals(topKey, PartialKey.ROW_COLFAM_COLQUAL))) {
			if ((source.getTopKey().getTimestamp()) > maxTS)
				maxTS = source.getTopKey().getTimestamp();

			if (!(topValue.equals(source.getTopValue())))
				throw new RuntimeException(((((("values not equals " + (topKey)) + " ") + (source.getTopKey())) + " : ") + (diffInfo(topValue, source.getTopValue()))));

			source.next();
		} 
		byte[] vis = getVisFromRefs();
		if (vis != null) {
			topKey = new Key(topKey.getRowData().toArray(), topKey.getColumnFamilyData().toArray(), topKey.getColumnQualifierData().toArray(), vis, maxTS);
		}
		return vis;
	}

	private byte[] getVisFromRefs() throws IOException {
		Text row = topKey.getRow();
		if (lastRowVC.containsKey(row))
			return lastRowVC.get(row);

		Range range = new Range(row);
		refsSource.seek(range, ChunkCombiner.refsColf, true);
		VisibilityCombiner vc = null;
		while (refsSource.hasTop()) {
			if (vc == null)
				vc = new VisibilityCombiner();

			refsSource.next();
		} 
		if (vc == null) {
			lastRowVC = Collections.singletonMap(row, null);
			return null;
		}
		return null;
	}

	private String diffInfo(Value v1, Value v2) {
		if ((v1.getSize()) != (v2.getSize())) {
			return (("val len not equal " + (v1.getSize())) + "!=") + (v2.getSize());
		}
		byte[] vb1 = v1.get();
		byte[] vb2 = v2.get();
		for (int i = 0; i < (vb1.length); i++) {
			if ((vb1[i]) != (vb2[i])) {
				return String.format("first diff at offset %,d 0x%02x != 0x%02x", i, (255 & (vb1[i])), (255 & (vb2[i])));
			}
		}
		return null;
	}

	@Override
	public Key getTopKey() {
		return topKey;
	}

	@Override
	public Value getTopValue() {
		return topValue;
	}

	@Override
	public SortedKeyValueIterator<Key, Value> deepCopy(IteratorEnvironment env) {
		ChunkCombiner cc = new ChunkCombiner();
		try {
			cc.init(source.deepCopy(env), null, env);
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		}
		return cc;
	}
}

