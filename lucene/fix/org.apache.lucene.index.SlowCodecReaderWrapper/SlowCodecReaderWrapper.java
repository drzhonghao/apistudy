

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import org.apache.lucene.codecs.DocValuesProducer;
import org.apache.lucene.codecs.FieldsProducer;
import org.apache.lucene.codecs.NormsProducer;
import org.apache.lucene.codecs.PointsReader;
import org.apache.lucene.codecs.StoredFieldsReader;
import org.apache.lucene.codecs.TermVectorsReader;
import org.apache.lucene.index.BinaryDocValues;
import org.apache.lucene.index.CodecReader;
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
import org.apache.lucene.util.Bits;


public final class SlowCodecReaderWrapper {
	private SlowCodecReaderWrapper() {
	}

	public static CodecReader wrap(final LeafReader reader) throws IOException {
		if (reader instanceof CodecReader) {
			return ((CodecReader) (reader));
		}else {
			reader.checkIntegrity();
			return new CodecReader() {
				@Override
				public TermVectorsReader getTermVectorsReader() {
					return SlowCodecReaderWrapper.readerToTermVectorsReader(reader);
				}

				@Override
				public StoredFieldsReader getFieldsReader() {
					return SlowCodecReaderWrapper.readerToStoredFieldsReader(reader);
				}

				@Override
				public NormsProducer getNormsReader() {
					return SlowCodecReaderWrapper.readerToNormsProducer(reader);
				}

				@Override
				public DocValuesProducer getDocValuesReader() {
					return SlowCodecReaderWrapper.readerToDocValuesProducer(reader);
				}

				@Override
				public FieldsProducer getPostingsReader() {
					try {
						return SlowCodecReaderWrapper.readerToFieldsProducer(reader);
					} catch (IOException bogus) {
						throw new AssertionError(bogus);
					}
				}

				@Override
				public FieldInfos getFieldInfos() {
					return reader.getFieldInfos();
				}

				@Override
				public PointsReader getPointsReader() {
					return SlowCodecReaderWrapper.pointValuesToReader(reader);
				}

				@Override
				public Bits getLiveDocs() {
					return reader.getLiveDocs();
				}

				@Override
				public int numDocs() {
					return reader.numDocs();
				}

				@Override
				public int maxDoc() {
					return reader.maxDoc();
				}

				@Override
				public IndexReader.CacheHelper getCoreCacheHelper() {
					return reader.getCoreCacheHelper();
				}

				@Override
				public IndexReader.CacheHelper getReaderCacheHelper() {
					return reader.getReaderCacheHelper();
				}

				@Override
				public String toString() {
					return ("SlowCodecReaderWrapper(" + reader) + ")";
				}

				@Override
				public LeafMetaData getMetaData() {
					return reader.getMetaData();
				}
			};
		}
	}

	private static PointsReader pointValuesToReader(LeafReader reader) {
		return new PointsReader() {
			@Override
			public PointValues getValues(String field) throws IOException {
				return reader.getPointValues(field);
			}

			@Override
			public void checkIntegrity() throws IOException {
			}

			@Override
			public void close() {
			}

			@Override
			public long ramBytesUsed() {
				return 0;
			}
		};
	}

	private static NormsProducer readerToNormsProducer(final LeafReader reader) {
		return new NormsProducer() {
			@Override
			public NumericDocValues getNorms(FieldInfo field) throws IOException {
				return reader.getNormValues(field.name);
			}

			@Override
			public void checkIntegrity() throws IOException {
			}

			@Override
			public void close() {
			}

			@Override
			public long ramBytesUsed() {
				return 0;
			}
		};
	}

	private static DocValuesProducer readerToDocValuesProducer(final LeafReader reader) {
		return new DocValuesProducer() {
			@Override
			public NumericDocValues getNumeric(FieldInfo field) throws IOException {
				return reader.getNumericDocValues(field.name);
			}

			@Override
			public BinaryDocValues getBinary(FieldInfo field) throws IOException {
				return reader.getBinaryDocValues(field.name);
			}

			@Override
			public SortedDocValues getSorted(FieldInfo field) throws IOException {
				return reader.getSortedDocValues(field.name);
			}

			@Override
			public SortedNumericDocValues getSortedNumeric(FieldInfo field) throws IOException {
				return reader.getSortedNumericDocValues(field.name);
			}

			@Override
			public SortedSetDocValues getSortedSet(FieldInfo field) throws IOException {
				return reader.getSortedSetDocValues(field.name);
			}

			@Override
			public void checkIntegrity() throws IOException {
			}

			@Override
			public void close() {
			}

			@Override
			public long ramBytesUsed() {
				return 0;
			}
		};
	}

	private static StoredFieldsReader readerToStoredFieldsReader(final LeafReader reader) {
		return new StoredFieldsReader() {
			@Override
			public void visitDocument(int docID, StoredFieldVisitor visitor) throws IOException {
				reader.document(docID, visitor);
			}

			@Override
			public StoredFieldsReader clone() {
				return SlowCodecReaderWrapper.readerToStoredFieldsReader(reader);
			}

			@Override
			public void checkIntegrity() throws IOException {
			}

			@Override
			public void close() {
			}

			@Override
			public long ramBytesUsed() {
				return 0;
			}
		};
	}

	private static TermVectorsReader readerToTermVectorsReader(final LeafReader reader) {
		return new TermVectorsReader() {
			@Override
			public Fields get(int docID) throws IOException {
				return reader.getTermVectors(docID);
			}

			@Override
			public TermVectorsReader clone() {
				return SlowCodecReaderWrapper.readerToTermVectorsReader(reader);
			}

			@Override
			public void checkIntegrity() throws IOException {
			}

			@Override
			public void close() {
			}

			@Override
			public long ramBytesUsed() {
				return 0;
			}
		};
	}

	private static FieldsProducer readerToFieldsProducer(final LeafReader reader) throws IOException {
		ArrayList<String> indexedFields = new ArrayList<>();
		for (FieldInfo fieldInfo : reader.getFieldInfos()) {
			if ((fieldInfo.getIndexOptions()) != (IndexOptions.NONE)) {
				indexedFields.add(fieldInfo.name);
			}
		}
		Collections.sort(indexedFields);
		return new FieldsProducer() {
			@Override
			public Iterator<String> iterator() {
				return indexedFields.iterator();
			}

			@Override
			public Terms terms(String field) throws IOException {
				return reader.terms(field);
			}

			@Override
			public int size() {
				return indexedFields.size();
			}

			@Override
			public void checkIntegrity() throws IOException {
			}

			@Override
			public void close() {
			}

			@Override
			public long ramBytesUsed() {
				return 0;
			}
		};
	}
}

