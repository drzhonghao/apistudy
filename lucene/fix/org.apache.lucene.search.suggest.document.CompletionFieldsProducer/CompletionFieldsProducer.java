

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.lucene.codecs.FieldsProducer;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.SegmentReadState;
import org.apache.lucene.index.Terms;
import org.apache.lucene.search.suggest.document.CompletionsTermsReader;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.util.Accountable;
import org.apache.lucene.util.Accountables;
import org.apache.lucene.util.IOUtils;


final class CompletionFieldsProducer extends FieldsProducer {
	private FieldsProducer delegateFieldsProducer;

	private Map<String, CompletionsTermsReader> readers;

	private IndexInput dictIn;

	private CompletionFieldsProducer(FieldsProducer delegateFieldsProducer, Map<String, CompletionsTermsReader> readers) {
		this.delegateFieldsProducer = delegateFieldsProducer;
		this.readers = readers;
	}

	CompletionFieldsProducer(SegmentReadState state) throws IOException {
		delegateFieldsProducer = null;
		boolean success = false;
	}

	@Override
	public void close() throws IOException {
		boolean success = false;
		try {
			delegateFieldsProducer.close();
			IOUtils.close(dictIn);
			success = true;
		} finally {
			if (success == false) {
				IOUtils.closeWhileHandlingException(delegateFieldsProducer, dictIn);
			}
		}
	}

	@Override
	public void checkIntegrity() throws IOException {
		delegateFieldsProducer.checkIntegrity();
	}

	@Override
	public FieldsProducer getMergeInstance() throws IOException {
		return new CompletionFieldsProducer(delegateFieldsProducer, readers);
	}

	@Override
	public long ramBytesUsed() {
		long ramBytesUsed = delegateFieldsProducer.ramBytesUsed();
		for (CompletionsTermsReader reader : readers.values()) {
			ramBytesUsed += reader.ramBytesUsed();
		}
		return ramBytesUsed;
	}

	@Override
	public Collection<Accountable> getChildResources() {
		List<Accountable> accountableList = new ArrayList<>();
		for (Map.Entry<String, CompletionsTermsReader> readerEntry : readers.entrySet()) {
			accountableList.add(Accountables.namedAccountable(readerEntry.getKey(), readerEntry.getValue()));
		}
		return Collections.unmodifiableCollection(accountableList);
	}

	@Override
	public Iterator<String> iterator() {
		return readers.keySet().iterator();
	}

	@Override
	public Terms terms(String field) throws IOException {
		Terms terms = delegateFieldsProducer.terms(field);
		if (terms == null) {
			return null;
		}
		return null;
	}

	@Override
	public int size() {
		return readers.size();
	}
}

