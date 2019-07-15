

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.lucene.index.SegmentInfo;
import org.apache.lucene.store.Directory;


public class SegmentCommitInfo {
	public final SegmentInfo info;

	private int delCount;

	private int softDelCount;

	private long delGen;

	private long nextWriteDelGen;

	private long fieldInfosGen;

	private long nextWriteFieldInfosGen;

	private long docValuesGen;

	private long nextWriteDocValuesGen;

	private final Map<Integer, Set<String>> dvUpdatesFiles = new HashMap<>();

	private final Set<String> fieldInfosFiles = new HashSet<>();

	private volatile long sizeInBytes = -1;

	private long bufferedDeletesGen = -1;

	public SegmentCommitInfo(SegmentInfo info, int delCount, int softDelCount, long delGen, long fieldInfosGen, long docValuesGen) {
		this.info = info;
		this.delCount = delCount;
		this.softDelCount = softDelCount;
		this.delGen = delGen;
		this.nextWriteDelGen = (delGen == (-1)) ? 1 : delGen + 1;
		this.fieldInfosGen = fieldInfosGen;
		this.nextWriteFieldInfosGen = (fieldInfosGen == (-1)) ? 1 : fieldInfosGen + 1;
		this.docValuesGen = docValuesGen;
		this.nextWriteDocValuesGen = (docValuesGen == (-1)) ? 1 : docValuesGen + 1;
	}

	public Map<Integer, Set<String>> getDocValuesUpdatesFiles() {
		return Collections.unmodifiableMap(dvUpdatesFiles);
	}

	public void setDocValuesUpdatesFiles(Map<Integer, Set<String>> dvUpdatesFiles) {
		this.dvUpdatesFiles.clear();
		for (Map.Entry<Integer, Set<String>> kv : dvUpdatesFiles.entrySet()) {
			Set<String> set = new HashSet<>();
			for (String file : kv.getValue()) {
			}
			this.dvUpdatesFiles.put(kv.getKey(), set);
		}
	}

	public Set<String> getFieldInfosFiles() {
		return Collections.unmodifiableSet(fieldInfosFiles);
	}

	public void setFieldInfosFiles(Set<String> fieldInfosFiles) {
		this.fieldInfosFiles.clear();
		for (String file : fieldInfosFiles) {
		}
	}

	void advanceDelGen() {
		delGen = nextWriteDelGen;
		nextWriteDelGen = (delGen) + 1;
		sizeInBytes = -1;
	}

	void advanceNextWriteDelGen() {
		(nextWriteDelGen)++;
	}

	long getNextWriteDelGen() {
		return nextWriteDelGen;
	}

	void setNextWriteDelGen(long v) {
		nextWriteDelGen = v;
	}

	void advanceFieldInfosGen() {
		fieldInfosGen = nextWriteFieldInfosGen;
		nextWriteFieldInfosGen = (fieldInfosGen) + 1;
		sizeInBytes = -1;
	}

	void advanceNextWriteFieldInfosGen() {
		(nextWriteFieldInfosGen)++;
	}

	long getNextWriteFieldInfosGen() {
		return nextWriteFieldInfosGen;
	}

	void setNextWriteFieldInfosGen(long v) {
		nextWriteFieldInfosGen = v;
	}

	void advanceDocValuesGen() {
		docValuesGen = nextWriteDocValuesGen;
		nextWriteDocValuesGen = (docValuesGen) + 1;
		sizeInBytes = -1;
	}

	void advanceNextWriteDocValuesGen() {
		(nextWriteDocValuesGen)++;
	}

	long getNextWriteDocValuesGen() {
		return nextWriteDocValuesGen;
	}

	void setNextWriteDocValuesGen(long v) {
		nextWriteDocValuesGen = v;
	}

	public long sizeInBytes() throws IOException {
		if ((sizeInBytes) == (-1)) {
			long sum = 0;
			for (final String fileName : files()) {
				sum += info.dir.fileLength(fileName);
			}
			sizeInBytes = sum;
		}
		return sizeInBytes;
	}

	public Collection<String> files() throws IOException {
		Collection<String> files = new HashSet<>(info.files());
		for (Set<String> updatefiles : dvUpdatesFiles.values()) {
			files.addAll(updatefiles);
		}
		files.addAll(fieldInfosFiles);
		return files;
	}

	long getBufferedDeletesGen() {
		return bufferedDeletesGen;
	}

	void setBufferedDeletesGen(long v) {
		if ((bufferedDeletesGen) == (-1)) {
			bufferedDeletesGen = v;
			sizeInBytes = -1;
		}else {
			throw new IllegalStateException("buffered deletes gen should only be set once");
		}
	}

	public boolean hasDeletions() {
		return (delGen) != (-1);
	}

	public boolean hasFieldUpdates() {
		return (fieldInfosGen) != (-1);
	}

	public long getNextFieldInfosGen() {
		return nextWriteFieldInfosGen;
	}

	public long getFieldInfosGen() {
		return fieldInfosGen;
	}

	public long getNextDocValuesGen() {
		return nextWriteDocValuesGen;
	}

	public long getDocValuesGen() {
		return docValuesGen;
	}

	public long getNextDelGen() {
		return nextWriteDelGen;
	}

	public long getDelGen() {
		return delGen;
	}

	public int getDelCount() {
		return delCount;
	}

	public int getSoftDelCount() {
		return softDelCount;
	}

	void setDelCount(int delCount) {
		if ((delCount < 0) || (delCount > (info.maxDoc()))) {
			throw new IllegalArgumentException((((("invalid delCount=" + delCount) + " (maxDoc=") + (info.maxDoc())) + ")"));
		}
		assert ((softDelCount) + delCount) <= (info.maxDoc());
		this.delCount = delCount;
	}

	void setSoftDelCount(int softDelCount) {
		if ((softDelCount < 0) || (softDelCount > (info.maxDoc()))) {
			throw new IllegalArgumentException((((("invalid softDelCount=" + softDelCount) + " (maxDoc=") + (info.maxDoc())) + ")"));
		}
		assert (softDelCount + (delCount)) <= (info.maxDoc());
		this.softDelCount = softDelCount;
	}

	public String toString(int pendingDelCount) {
		String s = info.toString(((delCount) + pendingDelCount));
		if ((delGen) != (-1)) {
			s += ":delGen=" + (delGen);
		}
		if ((fieldInfosGen) != (-1)) {
			s += ":fieldInfosGen=" + (fieldInfosGen);
		}
		if ((docValuesGen) != (-1)) {
			s += ":dvGen=" + (docValuesGen);
		}
		if ((softDelCount) > 0) {
			s += " :softDel=" + (softDelCount);
		}
		return s;
	}

	@Override
	public String toString() {
		return toString(0);
	}

	@Override
	public SegmentCommitInfo clone() {
		SegmentCommitInfo other = new SegmentCommitInfo(info, delCount, softDelCount, delGen, fieldInfosGen, docValuesGen);
		other.nextWriteDelGen = nextWriteDelGen;
		other.nextWriteFieldInfosGen = nextWriteFieldInfosGen;
		other.nextWriteDocValuesGen = nextWriteDocValuesGen;
		for (Map.Entry<Integer, Set<String>> e : dvUpdatesFiles.entrySet()) {
			other.dvUpdatesFiles.put(e.getKey(), new HashSet<>(e.getValue()));
		}
		other.fieldInfosFiles.addAll(fieldInfosFiles);
		return other;
	}

	final int getDelCount(boolean includeSoftDeletes) {
		return includeSoftDeletes ? (getDelCount()) + (getSoftDelCount()) : getDelCount();
	}
}

