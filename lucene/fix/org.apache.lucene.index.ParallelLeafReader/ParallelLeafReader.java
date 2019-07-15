

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.apache.lucene.index.BinaryDocValues;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.FieldInfos;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafMetaData;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.PointValues;
import org.apache.lucene.index.SortedDocValues;
import org.apache.lucene.index.SortedNumericDocValues;
import org.apache.lucene.index.SortedSetDocValues;
import org.apache.lucene.index.StoredFieldVisitor;
import org.apache.lucene.index.Terms;
import org.apache.lucene.search.Sort;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.Version;


public class ParallelLeafReader extends LeafReader {
	private final FieldInfos fieldInfos;

	private final LeafReader[] parallelReaders;

	private final LeafReader[] storedFieldsReaders;

	private final Set<LeafReader> completeReaderSet = Collections.newSetFromMap(new IdentityHashMap<LeafReader, Boolean>());

	private final boolean closeSubReaders;

	private final int maxDoc;

	private final int numDocs;

	private final boolean hasDeletions;

	private final LeafMetaData metaData;

	private final SortedMap<String, LeafReader> tvFieldToReader = new TreeMap<>();

	private final SortedMap<String, LeafReader> fieldToReader = new TreeMap<>();

	private final Map<String, LeafReader> termsFieldToReader = new HashMap<>();

	public ParallelLeafReader(LeafReader... readers) throws IOException {
		this(true, readers);
	}

	public ParallelLeafReader(boolean closeSubReaders, LeafReader... readers) throws IOException {
		this(closeSubReaders, readers, readers);
	}

	public ParallelLeafReader(boolean closeSubReaders, LeafReader[] readers, LeafReader[] storedFieldsReaders) throws IOException {
		this.closeSubReaders = closeSubReaders;
		if (((readers.length) == 0) && ((storedFieldsReaders.length) > 0))
			throw new IllegalArgumentException("There must be at least one main reader if storedFieldsReaders are used.");

		this.parallelReaders = readers.clone();
		this.storedFieldsReaders = storedFieldsReaders.clone();
		if ((parallelReaders.length) > 0) {
			final LeafReader first = parallelReaders[0];
			this.maxDoc = first.maxDoc();
			this.numDocs = first.numDocs();
			this.hasDeletions = first.hasDeletions();
		}else {
			this.maxDoc = this.numDocs = 0;
			this.hasDeletions = false;
		}
		Collections.addAll(completeReaderSet, this.parallelReaders);
		Collections.addAll(completeReaderSet, this.storedFieldsReaders);
		for (LeafReader reader : completeReaderSet) {
			if ((reader.maxDoc()) != (maxDoc)) {
				throw new IllegalArgumentException(((("All readers must have same maxDoc: " + (maxDoc)) + "!=") + (reader.maxDoc())));
			}
		}
		final String softDeletesField = completeReaderSet.stream().map(( r) -> r.getFieldInfos().getSoftDeletesField()).filter(Objects::nonNull).findAny().orElse(null);
		Sort indexSort = null;
		int createdVersionMajor = -1;
		for (final LeafReader reader : this.parallelReaders) {
			LeafMetaData leafMetaData = reader.getMetaData();
			Sort leafIndexSort = leafMetaData.getSort();
			if (indexSort == null) {
				indexSort = leafIndexSort;
			}else
				if ((leafIndexSort != null) && ((indexSort.equals(leafIndexSort)) == false)) {
					throw new IllegalArgumentException(((("cannot combine LeafReaders that have different index sorts: saw both sort=" + indexSort) + " and ") + leafIndexSort));
				}

			if (createdVersionMajor == (-1)) {
				createdVersionMajor = leafMetaData.getCreatedVersionMajor();
			}else
				if (createdVersionMajor != (leafMetaData.getCreatedVersionMajor())) {
					throw new IllegalArgumentException(((("cannot combine LeafReaders that have different creation versions: saw both version=" + createdVersionMajor) + " and ") + (leafMetaData.getCreatedVersionMajor())));
				}

			final FieldInfos readerFieldInfos = reader.getFieldInfos();
			for (FieldInfo fieldInfo : readerFieldInfos) {
				if (!(fieldToReader.containsKey(fieldInfo.name))) {
					fieldToReader.put(fieldInfo.name, reader);
					if (fieldInfo.hasVectors()) {
						tvFieldToReader.put(fieldInfo.name, reader);
					}
					if ((fieldInfo.getIndexOptions()) != (IndexOptions.NONE)) {
						termsFieldToReader.put(fieldInfo.name, reader);
					}
				}
			}
		}
		if (createdVersionMajor == (-1)) {
			createdVersionMajor = Version.LATEST.major;
		}
		Version minVersion = Version.LATEST;
		for (final LeafReader reader : this.parallelReaders) {
			Version leafVersion = reader.getMetaData().getMinVersion();
			if (leafVersion == null) {
				minVersion = null;
				break;
			}else
				if (minVersion.onOrAfter(leafVersion)) {
					minVersion = leafVersion;
				}

		}
		this.metaData = new LeafMetaData(createdVersionMajor, minVersion, indexSort);
		for (LeafReader reader : completeReaderSet) {
			if (!closeSubReaders) {
				reader.incRef();
			}
			reader.registerParentReader(this);
		}
		fieldInfos = null;
	}

	@Override
	public String toString() {
		final StringBuilder buffer = new StringBuilder("ParallelLeafReader(");
		for (final Iterator<LeafReader> iter = completeReaderSet.iterator(); iter.hasNext();) {
			buffer.append(iter.next());
			if (iter.hasNext())
				buffer.append(", ");

		}
		return buffer.append(')').toString();
	}

	private static final class ParallelFields extends Fields {
		final Map<String, Terms> fields = new TreeMap<>();

		ParallelFields() {
		}

		void addField(String fieldName, Terms terms) {
			fields.put(fieldName, terms);
		}

		@Override
		public Iterator<String> iterator() {
			return Collections.unmodifiableSet(fields.keySet()).iterator();
		}

		@Override
		public Terms terms(String field) {
			return fields.get(field);
		}

		@Override
		public int size() {
			return fields.size();
		}
	}

	@Override
	public FieldInfos getFieldInfos() {
		return fieldInfos;
	}

	@Override
	public Bits getLiveDocs() {
		ensureOpen();
		return hasDeletions ? parallelReaders[0].getLiveDocs() : null;
	}

	@Override
	public Terms terms(String field) throws IOException {
		ensureOpen();
		LeafReader leafReader = termsFieldToReader.get(field);
		return leafReader == null ? null : leafReader.terms(field);
	}

	@Override
	public int numDocs() {
		return numDocs;
	}

	@Override
	public int maxDoc() {
		return maxDoc;
	}

	@Override
	public void document(int docID, StoredFieldVisitor visitor) throws IOException {
		ensureOpen();
		for (final LeafReader reader : storedFieldsReaders) {
			reader.document(docID, visitor);
		}
	}

	@Override
	public IndexReader.CacheHelper getCoreCacheHelper() {
		if ((((parallelReaders.length) == 1) && ((storedFieldsReaders.length) == 1)) && ((parallelReaders[0]) == (storedFieldsReaders[0]))) {
			return parallelReaders[0].getCoreCacheHelper();
		}
		return null;
	}

	@Override
	public IndexReader.CacheHelper getReaderCacheHelper() {
		if ((((parallelReaders.length) == 1) && ((storedFieldsReaders.length) == 1)) && ((parallelReaders[0]) == (storedFieldsReaders[0]))) {
			return parallelReaders[0].getReaderCacheHelper();
		}
		return null;
	}

	@Override
	public Fields getTermVectors(int docID) throws IOException {
		ensureOpen();
		ParallelLeafReader.ParallelFields fields = null;
		for (Map.Entry<String, LeafReader> ent : tvFieldToReader.entrySet()) {
			String fieldName = ent.getKey();
			Terms vector = ent.getValue().getTermVector(docID, fieldName);
			if (vector != null) {
				if (fields == null) {
					fields = new ParallelLeafReader.ParallelFields();
				}
				fields.addField(fieldName, vector);
			}
		}
		return fields;
	}

	@Override
	protected synchronized void doClose() throws IOException {
		IOException ioe = null;
		for (LeafReader reader : completeReaderSet) {
			try {
				if (closeSubReaders) {
					reader.close();
				}else {
					reader.decRef();
				}
			} catch (IOException e) {
				if (ioe == null)
					ioe = e;

			}
		}
		if (ioe != null)
			throw ioe;

	}

	@Override
	public NumericDocValues getNumericDocValues(String field) throws IOException {
		ensureOpen();
		LeafReader reader = fieldToReader.get(field);
		return reader == null ? null : reader.getNumericDocValues(field);
	}

	@Override
	public BinaryDocValues getBinaryDocValues(String field) throws IOException {
		ensureOpen();
		LeafReader reader = fieldToReader.get(field);
		return reader == null ? null : reader.getBinaryDocValues(field);
	}

	@Override
	public SortedDocValues getSortedDocValues(String field) throws IOException {
		ensureOpen();
		LeafReader reader = fieldToReader.get(field);
		return reader == null ? null : reader.getSortedDocValues(field);
	}

	@Override
	public SortedNumericDocValues getSortedNumericDocValues(String field) throws IOException {
		ensureOpen();
		LeafReader reader = fieldToReader.get(field);
		return reader == null ? null : reader.getSortedNumericDocValues(field);
	}

	@Override
	public SortedSetDocValues getSortedSetDocValues(String field) throws IOException {
		ensureOpen();
		LeafReader reader = fieldToReader.get(field);
		return reader == null ? null : reader.getSortedSetDocValues(field);
	}

	@Override
	public NumericDocValues getNormValues(String field) throws IOException {
		ensureOpen();
		LeafReader reader = fieldToReader.get(field);
		NumericDocValues values = (reader == null) ? null : reader.getNormValues(field);
		return values;
	}

	@Override
	public PointValues getPointValues(String fieldName) throws IOException {
		ensureOpen();
		LeafReader reader = fieldToReader.get(fieldName);
		return reader == null ? null : reader.getPointValues(fieldName);
	}

	@Override
	public void checkIntegrity() throws IOException {
		ensureOpen();
		for (LeafReader reader : completeReaderSet) {
			reader.checkIntegrity();
		}
	}

	public LeafReader[] getParallelReaders() {
		ensureOpen();
		return parallelReaders;
	}

	@Override
	public LeafMetaData getMetaData() {
		return metaData;
	}
}

