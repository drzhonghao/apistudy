

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.lucene.index.DocValuesType;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.PointValues;
import org.apache.lucene.util.ArrayUtil;


public class FieldInfos implements Iterable<FieldInfo> {
	private final boolean hasFreq;

	private final boolean hasProx;

	private final boolean hasPayloads;

	private final boolean hasOffsets;

	private final boolean hasVectors;

	private final boolean hasNorms;

	private final boolean hasDocValues;

	private final boolean hasPointValues;

	private final String softDeletesField;

	private final FieldInfo[] byNumber;

	private final HashMap<String, FieldInfo> byName = new HashMap<>();

	private final Collection<FieldInfo> values;

	public FieldInfos(FieldInfo[] infos) {
		boolean hasVectors = false;
		boolean hasProx = false;
		boolean hasPayloads = false;
		boolean hasOffsets = false;
		boolean hasFreq = false;
		boolean hasNorms = false;
		boolean hasDocValues = false;
		boolean hasPointValues = false;
		String softDeletesField = null;
		int size = 0;
		FieldInfo[] byNumberTemp = new FieldInfo[10];
		for (FieldInfo info : infos) {
			if ((info.number) < 0) {
				throw new IllegalArgumentException(((("illegal field number: " + (info.number)) + " for field ") + (info.name)));
			}
			size = ((info.number) >= size) ? (info.number) + 1 : size;
			if ((info.number) >= (byNumberTemp.length)) {
				byNumberTemp = ArrayUtil.grow(byNumberTemp, ((info.number) + 1));
			}
			FieldInfo previous = byNumberTemp[info.number];
			if (previous != null) {
				throw new IllegalArgumentException(((((("duplicate field numbers: " + (previous.name)) + " and ") + (info.name)) + " have: ") + (info.number)));
			}
			byNumberTemp[info.number] = info;
			previous = byName.put(info.name, info);
			if (previous != null) {
				throw new IllegalArgumentException(((((("duplicate field names: " + (previous.number)) + " and ") + (info.number)) + " have: ") + (info.name)));
			}
			hasVectors |= info.hasVectors();
			hasProx |= (info.getIndexOptions().compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS)) >= 0;
			hasFreq |= (info.getIndexOptions()) != (IndexOptions.DOCS);
			hasOffsets |= (info.getIndexOptions().compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS)) >= 0;
			hasNorms |= info.hasNorms();
			hasDocValues |= (info.getDocValuesType()) != (DocValuesType.NONE);
			hasPayloads |= info.hasPayloads();
			hasPointValues |= (info.getPointDimensionCount()) != 0;
			if (info.isSoftDeletesField()) {
				if ((softDeletesField != null) && ((softDeletesField.equals(info.name)) == false)) {
					throw new IllegalArgumentException((((("multiple soft-deletes fields [" + (info.name)) + ", ") + softDeletesField) + "]"));
				}
				softDeletesField = info.name;
			}
		}
		this.hasVectors = hasVectors;
		this.hasProx = hasProx;
		this.hasPayloads = hasPayloads;
		this.hasOffsets = hasOffsets;
		this.hasFreq = hasFreq;
		this.hasNorms = hasNorms;
		this.hasDocValues = hasDocValues;
		this.hasPointValues = hasPointValues;
		this.softDeletesField = softDeletesField;
		List<FieldInfo> valuesTemp = new ArrayList<>();
		byNumber = new FieldInfo[size];
		for (int i = 0; i < size; i++) {
			byNumber[i] = byNumberTemp[i];
			if ((byNumberTemp[i]) != null) {
				valuesTemp.add(byNumberTemp[i]);
			}
		}
		values = Collections.unmodifiableCollection(Arrays.asList(valuesTemp.toArray(new FieldInfo[0])));
	}

	public boolean hasFreq() {
		return hasFreq;
	}

	public boolean hasProx() {
		return hasProx;
	}

	public boolean hasPayloads() {
		return hasPayloads;
	}

	public boolean hasOffsets() {
		return hasOffsets;
	}

	public boolean hasVectors() {
		return hasVectors;
	}

	public boolean hasNorms() {
		return hasNorms;
	}

	public boolean hasDocValues() {
		return hasDocValues;
	}

	public boolean hasPointValues() {
		return hasPointValues;
	}

	public String getSoftDeletesField() {
		return softDeletesField;
	}

	public int size() {
		return byName.size();
	}

	@Override
	public Iterator<FieldInfo> iterator() {
		return values.iterator();
	}

	public FieldInfo fieldInfo(String fieldName) {
		return byName.get(fieldName);
	}

	public FieldInfo fieldInfo(int fieldNumber) {
		if (fieldNumber < 0) {
			throw new IllegalArgumentException(("Illegal field number: " + fieldNumber));
		}
		if (fieldNumber >= (byNumber.length)) {
			return null;
		}
		return byNumber[fieldNumber];
	}

	static final class FieldDimensions {
		public final int dimensionCount;

		public final int dimensionNumBytes;

		public FieldDimensions(int dimensionCount, int dimensionNumBytes) {
			this.dimensionCount = dimensionCount;
			this.dimensionNumBytes = dimensionNumBytes;
		}
	}

	static final class FieldNumbers {
		private final Map<Integer, String> numberToName;

		private final Map<String, Integer> nameToNumber;

		private final Map<String, DocValuesType> docValuesType;

		private final Map<String, FieldInfos.FieldDimensions> dimensions;

		private int lowestUnassignedFieldNumber = -1;

		private final String softDeletesFieldName;

		FieldNumbers(String softDeletesFieldName) {
			this.nameToNumber = new HashMap<>();
			this.numberToName = new HashMap<>();
			this.docValuesType = new HashMap<>();
			this.dimensions = new HashMap<>();
			this.softDeletesFieldName = softDeletesFieldName;
		}

		synchronized int addOrGet(String fieldName, int preferredFieldNumber, DocValuesType dvType, int dimensionCount, int dimensionNumBytes, boolean isSoftDeletesField) {
			if (dvType != (DocValuesType.NONE)) {
				DocValuesType currentDVType = docValuesType.get(fieldName);
				if (currentDVType == null) {
					docValuesType.put(fieldName, dvType);
				}else
					if ((currentDVType != (DocValuesType.NONE)) && (currentDVType != dvType)) {
						throw new IllegalArgumentException((((((("cannot change DocValues type from " + currentDVType) + " to ") + dvType) + " for field \"") + fieldName) + "\""));
					}

			}
			if (dimensionCount != 0) {
				FieldInfos.FieldDimensions dims = dimensions.get(fieldName);
				if (dims != null) {
					if ((dims.dimensionCount) != dimensionCount) {
						throw new IllegalArgumentException((((((("cannot change point dimension count from " + (dims.dimensionCount)) + " to ") + dimensionCount) + " for field=\"") + fieldName) + "\""));
					}
					if ((dims.dimensionNumBytes) != dimensionNumBytes) {
						throw new IllegalArgumentException((((((("cannot change point numBytes from " + (dims.dimensionNumBytes)) + " to ") + dimensionNumBytes) + " for field=\"") + fieldName) + "\""));
					}
				}else {
					dimensions.put(fieldName, new FieldInfos.FieldDimensions(dimensionCount, dimensionNumBytes));
				}
			}
			Integer fieldNumber = nameToNumber.get(fieldName);
			if (fieldNumber == null) {
				final Integer preferredBoxed = Integer.valueOf(preferredFieldNumber);
				if ((preferredFieldNumber != (-1)) && (!(numberToName.containsKey(preferredBoxed)))) {
					fieldNumber = preferredBoxed;
				}else {
					while (numberToName.containsKey((++(lowestUnassignedFieldNumber)))) {
					} 
					fieldNumber = lowestUnassignedFieldNumber;
				}
				assert fieldNumber >= 0;
				numberToName.put(fieldNumber, fieldName);
				nameToNumber.put(fieldName, fieldNumber);
			}
			if (isSoftDeletesField) {
				if ((softDeletesFieldName) == null) {
					throw new IllegalArgumentException((("this index has [" + fieldName) + "] as soft-deletes already but soft-deletes field is not configured in IWC"));
				}else
					if ((fieldName.equals(softDeletesFieldName)) == false) {
						throw new IllegalArgumentException((((("cannot configure [" + (softDeletesFieldName)) + "] as soft-deletes; this index uses [") + fieldName) + "] as soft-deletes already"));
					}

			}else
				if (fieldName.equals(softDeletesFieldName)) {
					throw new IllegalArgumentException((((("cannot configure [" + (softDeletesFieldName)) + "] as soft-deletes; this index uses [") + fieldName) + "] as non-soft-deletes already"));
				}

			return fieldNumber.intValue();
		}

		synchronized void verifyConsistent(Integer number, String name, DocValuesType dvType) {
			if ((name.equals(numberToName.get(number))) == false) {
				throw new IllegalArgumentException((((((("field number " + number) + " is already mapped to field name \"") + (numberToName.get(number))) + "\", not \"") + name) + "\""));
			}
			if ((number.equals(nameToNumber.get(name))) == false) {
				throw new IllegalArgumentException((((((("field name \"" + name) + "\" is already mapped to field number \"") + (nameToNumber.get(name))) + "\", not \"") + number) + "\""));
			}
			DocValuesType currentDVType = docValuesType.get(name);
			if ((((dvType != (DocValuesType.NONE)) && (currentDVType != null)) && (currentDVType != (DocValuesType.NONE))) && (dvType != currentDVType)) {
				throw new IllegalArgumentException((((((("cannot change DocValues type from " + currentDVType) + " to ") + dvType) + " for field \"") + name) + "\""));
			}
		}

		synchronized void verifyConsistentDimensions(Integer number, String name, int dimensionCount, int dimensionNumBytes) {
			if ((name.equals(numberToName.get(number))) == false) {
				throw new IllegalArgumentException((((((("field number " + number) + " is already mapped to field name \"") + (numberToName.get(number))) + "\", not \"") + name) + "\""));
			}
			if ((number.equals(nameToNumber.get(name))) == false) {
				throw new IllegalArgumentException((((((("field name \"" + name) + "\" is already mapped to field number \"") + (nameToNumber.get(name))) + "\", not \"") + number) + "\""));
			}
			FieldInfos.FieldDimensions dim = dimensions.get(name);
			if (dim != null) {
				if ((dim.dimensionCount) != dimensionCount) {
					throw new IllegalArgumentException((((((("cannot change point dimension count from " + (dim.dimensionCount)) + " to ") + dimensionCount) + " for field=\"") + name) + "\""));
				}
				if ((dim.dimensionNumBytes) != dimensionNumBytes) {
					throw new IllegalArgumentException((((((("cannot change point numBytes from " + (dim.dimensionNumBytes)) + " to ") + dimensionNumBytes) + " for field=\"") + name) + "\""));
				}
			}
		}

		synchronized boolean contains(String fieldName, DocValuesType dvType) {
			if (!(nameToNumber.containsKey(fieldName))) {
				return false;
			}else {
				return dvType == (docValuesType.get(fieldName));
			}
		}

		synchronized Set<String> getFieldNames() {
			return Collections.unmodifiableSet(new HashSet<>(nameToNumber.keySet()));
		}

		synchronized void clear() {
			numberToName.clear();
			nameToNumber.clear();
			docValuesType.clear();
			dimensions.clear();
		}

		synchronized void setDocValuesType(int number, String name, DocValuesType dvType) {
			verifyConsistent(number, name, dvType);
			docValuesType.put(name, dvType);
		}

		synchronized void setDimensions(int number, String name, int dimensionCount, int dimensionNumBytes) {
			if (dimensionNumBytes > (PointValues.MAX_NUM_BYTES)) {
				throw new IllegalArgumentException((((((("dimension numBytes must be <= PointValues.MAX_NUM_BYTES (= " + (PointValues.MAX_NUM_BYTES)) + "); got ") + dimensionNumBytes) + " for field=\"") + name) + "\""));
			}
			if (dimensionCount > (PointValues.MAX_DIMENSIONS)) {
				throw new IllegalArgumentException((((((("pointDimensionCount must be <= PointValues.MAX_DIMENSIONS (= " + (PointValues.MAX_DIMENSIONS)) + "); got ") + dimensionCount) + " for field=\"") + name) + "\""));
			}
			verifyConsistentDimensions(number, name, dimensionCount, dimensionNumBytes);
			dimensions.put(name, new FieldInfos.FieldDimensions(dimensionCount, dimensionNumBytes));
		}
	}

	static final class Builder {
		private final HashMap<String, FieldInfo> byName = new HashMap<>();

		final FieldInfos.FieldNumbers globalFieldNumbers;

		private boolean finished;

		Builder(FieldInfos.FieldNumbers globalFieldNumbers) {
			assert globalFieldNumbers != null;
			this.globalFieldNumbers = globalFieldNumbers;
		}

		public void add(FieldInfos other) {
			assert assertNotFinished();
			for (FieldInfo fieldInfo : other) {
				add(fieldInfo);
			}
		}

		public FieldInfo getOrAdd(String name) {
			FieldInfo fi = fieldInfo(name);
			if (fi == null) {
				assert assertNotFinished();
				final boolean isSoftDeletesField = name.equals(globalFieldNumbers.softDeletesFieldName);
				final int fieldNumber = globalFieldNumbers.addOrGet(name, (-1), DocValuesType.NONE, 0, 0, isSoftDeletesField);
				fi = new FieldInfo(name, fieldNumber, false, false, false, IndexOptions.NONE, DocValuesType.NONE, (-1), new HashMap<>(), 0, 0, isSoftDeletesField);
				assert !(byName.containsKey(fi.name));
				globalFieldNumbers.verifyConsistent(Integer.valueOf(fi.number), fi.name, DocValuesType.NONE);
				byName.put(fi.name, fi);
			}
			return fi;
		}

		private FieldInfo addOrUpdateInternal(String name, int preferredFieldNumber, boolean storeTermVector, boolean omitNorms, boolean storePayloads, IndexOptions indexOptions, DocValuesType docValues, long dvGen, int dimensionCount, int dimensionNumBytes, boolean isSoftDeletesField) {
			assert assertNotFinished();
			if (docValues == null) {
				throw new NullPointerException("DocValuesType must not be null");
			}
			FieldInfo fi = fieldInfo(name);
			if (fi == null) {
				final int fieldNumber = globalFieldNumbers.addOrGet(name, preferredFieldNumber, docValues, dimensionCount, dimensionNumBytes, isSoftDeletesField);
				fi = new FieldInfo(name, fieldNumber, storeTermVector, omitNorms, storePayloads, indexOptions, docValues, dvGen, new HashMap<>(), dimensionCount, dimensionNumBytes, isSoftDeletesField);
				assert !(byName.containsKey(fi.name));
				globalFieldNumbers.verifyConsistent(Integer.valueOf(fi.number), fi.name, fi.getDocValuesType());
				byName.put(fi.name, fi);
			}else {
				if (docValues != (DocValuesType.NONE)) {
					boolean updateGlobal = (fi.getDocValuesType()) == (DocValuesType.NONE);
					if (updateGlobal) {
						globalFieldNumbers.setDocValuesType(fi.number, name, docValues);
					}
					fi.setDocValuesType(docValues);
				}
			}
			return fi;
		}

		public FieldInfo add(FieldInfo fi) {
			return add(fi, (-1));
		}

		public FieldInfo add(FieldInfo fi, long dvGen) {
			return addOrUpdateInternal(fi.name, fi.number, fi.hasVectors(), fi.omitsNorms(), fi.hasPayloads(), fi.getIndexOptions(), fi.getDocValuesType(), dvGen, fi.getPointDimensionCount(), fi.getPointNumBytes(), fi.isSoftDeletesField());
		}

		public FieldInfo fieldInfo(String fieldName) {
			return byName.get(fieldName);
		}

		private boolean assertNotFinished() {
			if (finished) {
				throw new IllegalStateException("FieldInfos.Builder was already finished; cannot add new fields");
			}
			return true;
		}

		FieldInfos finish() {
			finished = true;
			return new FieldInfos(byName.values().toArray(new FieldInfo[byName.size()]));
		}
	}
}

