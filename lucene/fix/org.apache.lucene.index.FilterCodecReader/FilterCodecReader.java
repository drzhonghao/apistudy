

import java.io.IOException;
import java.util.Collection;
import java.util.Objects;
import org.apache.lucene.codecs.DocValuesProducer;
import org.apache.lucene.codecs.FieldsProducer;
import org.apache.lucene.codecs.NormsProducer;
import org.apache.lucene.codecs.PointsReader;
import org.apache.lucene.codecs.StoredFieldsReader;
import org.apache.lucene.codecs.TermVectorsReader;
import org.apache.lucene.index.CodecReader;
import org.apache.lucene.index.FieldInfos;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafMetaData;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.util.Accountable;
import org.apache.lucene.util.Bits;


public abstract class FilterCodecReader extends CodecReader {
	protected final CodecReader in;

	public FilterCodecReader(CodecReader in) {
		this.in = Objects.requireNonNull(in);
	}

	@Override
	public StoredFieldsReader getFieldsReader() {
		return in.getFieldsReader();
	}

	@Override
	public TermVectorsReader getTermVectorsReader() {
		return in.getTermVectorsReader();
	}

	@Override
	public NormsProducer getNormsReader() {
		return in.getNormsReader();
	}

	@Override
	public DocValuesProducer getDocValuesReader() {
		return in.getDocValuesReader();
	}

	@Override
	public FieldsProducer getPostingsReader() {
		return in.getPostingsReader();
	}

	@Override
	public Bits getLiveDocs() {
		return in.getLiveDocs();
	}

	@Override
	public FieldInfos getFieldInfos() {
		return in.getFieldInfos();
	}

	@Override
	public PointsReader getPointsReader() {
		return in.getPointsReader();
	}

	@Override
	public int numDocs() {
		return in.numDocs();
	}

	@Override
	public int maxDoc() {
		return in.maxDoc();
	}

	@Override
	public LeafMetaData getMetaData() {
		return in.getMetaData();
	}

	@Override
	protected void doClose() throws IOException {
	}

	@Override
	public long ramBytesUsed() {
		return in.ramBytesUsed();
	}

	@Override
	public Collection<Accountable> getChildResources() {
		return in.getChildResources();
	}

	@Override
	public void checkIntegrity() throws IOException {
		in.checkIntegrity();
	}
}

