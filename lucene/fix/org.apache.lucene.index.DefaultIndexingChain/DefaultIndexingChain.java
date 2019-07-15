

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.index.DocValuesType;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.FieldInvertState;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.IndexableFieldType;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.Counter;
import org.apache.lucene.util.RamUsageEstimator;


final class DefaultIndexingChain {
	final Counter bytesUsed = null;

	private DefaultIndexingChain.PerField[] fieldHash = new DefaultIndexingChain.PerField[2];

	private int hashMask = 1;

	private int totalFieldCount;

	private long nextFieldGen;

	private DefaultIndexingChain.PerField[] fields = new DefaultIndexingChain.PerField[1];

	private final Set<String> finishedDocValues = new HashSet<>();

	public void abort() throws IOException {
	}

	private void rehash() {
		int newHashSize = (fieldHash.length) * 2;
		assert newHashSize > (fieldHash.length);
		DefaultIndexingChain.PerField[] newHashArray = new DefaultIndexingChain.PerField[newHashSize];
		int newHashMask = newHashSize - 1;
		for (int j = 0; j < (fieldHash.length); j++) {
			DefaultIndexingChain.PerField fp0 = fieldHash[j];
			while (fp0 != null) {
				final int hashPos2 = (fp0.fieldInfo.name.hashCode()) & newHashMask;
				DefaultIndexingChain.PerField nextFP0 = fp0.next;
				fp0.next = newHashArray[hashPos2];
				newHashArray[hashPos2] = fp0;
				fp0 = nextFP0;
			} 
		}
		fieldHash = newHashArray;
		hashMask = newHashMask;
	}

	private void startStoredFields(int docID) throws IOException {
		try {
		} catch (Throwable th) {
			throw th;
		}
	}

	private void finishStoredFields() throws IOException {
		try {
		} catch (Throwable th) {
			throw th;
		}
	}

	public void processDocument() throws IOException {
		int fieldCount = 0;
		long fieldGen = (nextFieldGen)++;
		try {
		} finally {
		}
		try {
		} catch (Throwable th) {
			throw th;
		}
	}

	private int processField(IndexableField field, long fieldGen, int fieldCount) throws IOException {
		String fieldName = field.name();
		IndexableFieldType fieldType = field.fieldType();
		DefaultIndexingChain.PerField fp = null;
		if ((fieldType.indexOptions()) == null) {
			throw new NullPointerException((("IndexOptions must not be null (field: \"" + (field.name())) + "\")"));
		}
		if ((fieldType.indexOptions()) != (IndexOptions.NONE)) {
			fp = getOrAddField(fieldName, fieldType, true);
			boolean first = (fp.fieldGen) != fieldGen;
			fp.invert(field, first);
			if (first) {
				fields[(fieldCount++)] = fp;
				fp.fieldGen = fieldGen;
			}
		}else {
			DefaultIndexingChain.verifyUnIndexedFieldType(fieldName, fieldType);
		}
		if (fieldType.stored()) {
			if (fp == null) {
				fp = getOrAddField(fieldName, fieldType, false);
			}
			if (fieldType.stored()) {
				String value = field.stringValue();
				if ((value != null) && ((value.length()) > (IndexWriter.MAX_STORED_STRING_LENGTH))) {
					throw new IllegalArgumentException((((("stored field \"" + (field.name())) + "\" is too large (") + (value.length())) + " characters) to store"));
				}
				try {
				} catch (Throwable th) {
					throw th;
				}
			}
		}
		DocValuesType dvType = fieldType.docValuesType();
		if (dvType == null) {
			throw new NullPointerException((("docValuesType must not be null (field: \"" + fieldName) + "\")"));
		}
		if (dvType != (DocValuesType.NONE)) {
			if (fp == null) {
				fp = getOrAddField(fieldName, fieldType, false);
			}
			indexDocValue(fp, dvType, field);
		}
		if ((fieldType.pointDimensionCount()) != 0) {
			if (fp == null) {
				fp = getOrAddField(fieldName, fieldType, false);
			}
			indexPoint(fp, field);
		}
		return fieldCount;
	}

	private static void verifyUnIndexedFieldType(String name, IndexableFieldType ft) {
		if (ft.storeTermVectors()) {
			throw new IllegalArgumentException(((("cannot store term vectors " + "for a field that is not indexed (field=\"") + name) + "\")"));
		}
		if (ft.storeTermVectorPositions()) {
			throw new IllegalArgumentException(((("cannot store term vector positions " + "for a field that is not indexed (field=\"") + name) + "\")"));
		}
		if (ft.storeTermVectorOffsets()) {
			throw new IllegalArgumentException(((("cannot store term vector offsets " + "for a field that is not indexed (field=\"") + name) + "\")"));
		}
		if (ft.storeTermVectorPayloads()) {
			throw new IllegalArgumentException(((("cannot store term vector payloads " + "for a field that is not indexed (field=\"") + name) + "\")"));
		}
	}

	private void indexPoint(DefaultIndexingChain.PerField fp, IndexableField field) throws IOException {
		int pointDimensionCount = field.fieldType().pointDimensionCount();
		int dimensionNumBytes = field.fieldType().pointNumBytes();
		if ((fp.fieldInfo.getPointDimensionCount()) == 0) {
		}
		fp.fieldInfo.setPointDimensions(pointDimensionCount, dimensionNumBytes);
	}

	private void indexDocValue(DefaultIndexingChain.PerField fp, DocValuesType dvType, IndexableField field) throws IOException {
		if ((fp.fieldInfo.getDocValuesType()) == (DocValuesType.NONE)) {
		}
		fp.fieldInfo.setDocValuesType(dvType);
	}

	private DefaultIndexingChain.PerField getPerField(String name) {
		final int hashPos = (name.hashCode()) & (hashMask);
		DefaultIndexingChain.PerField fp = fieldHash[hashPos];
		while ((fp != null) && (!(fp.fieldInfo.name.equals(name)))) {
			fp = fp.next;
		} 
		return fp;
	}

	private DefaultIndexingChain.PerField getOrAddField(String name, IndexableFieldType fieldType, boolean invert) {
		final int hashPos = (name.hashCode()) & (hashMask);
		DefaultIndexingChain.PerField fp = fieldHash[hashPos];
		while ((fp != null) && (!(fp.fieldInfo.name.equals(name)))) {
			fp = fp.next;
		} 
		if (fp == null) {
			fp.next = fieldHash[hashPos];
			fieldHash[hashPos] = fp;
			(totalFieldCount)++;
			if ((totalFieldCount) >= ((fieldHash.length) / 2)) {
				rehash();
			}
			if ((totalFieldCount) > (fields.length)) {
				DefaultIndexingChain.PerField[] newFields = new DefaultIndexingChain.PerField[ArrayUtil.oversize(totalFieldCount, RamUsageEstimator.NUM_BYTES_OBJECT_REF)];
				System.arraycopy(fields, 0, newFields, 0, fields.length);
				fields = newFields;
			}
		}else
			if (invert && ((fp.invertState) == null)) {
				fp.fieldInfo.setIndexOptions(fieldType.indexOptions());
				fp.setInvertState();
			}

		return fp;
	}

	private final class PerField implements Comparable<DefaultIndexingChain.PerField> {
		final int indexCreatedVersionMajor;

		final FieldInfo fieldInfo;

		final Similarity similarity;

		FieldInvertState invertState;

		long fieldGen = -1;

		DefaultIndexingChain.PerField next;

		TokenStream tokenStream;

		public PerField(int indexCreatedVersionMajor, FieldInfo fieldInfo, boolean invert) {
			this.indexCreatedVersionMajor = indexCreatedVersionMajor;
			this.fieldInfo = fieldInfo;
			if (invert) {
				setInvertState();
			}
			similarity = null;
		}

		void setInvertState() {
			invertState = new FieldInvertState(indexCreatedVersionMajor, fieldInfo.name);
			if ((fieldInfo.omitsNorms()) == false) {
			}
		}

		@Override
		public int compareTo(DefaultIndexingChain.PerField other) {
			return this.fieldInfo.name.compareTo(other.fieldInfo.name);
		}

		public void finish() throws IOException {
			if ((fieldInfo.omitsNorms()) == false) {
				long normValue;
			}
		}

		public void invert(IndexableField field, boolean first) throws IOException {
			if (first) {
			}
			IndexableFieldType fieldType = field.fieldType();
			IndexOptions indexOptions = fieldType.indexOptions();
			fieldInfo.setIndexOptions(indexOptions);
			if (fieldType.omitNorms()) {
				fieldInfo.setOmitsNorms();
			}
			boolean succeededInProcessingField = false;
		}
	}

	DocIdSetIterator getHasDocValues(String field) {
		DefaultIndexingChain.PerField perField = getPerField(field);
		if (perField != null) {
		}
		return null;
	}
}

