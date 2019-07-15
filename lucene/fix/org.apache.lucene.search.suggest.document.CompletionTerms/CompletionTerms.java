

import java.io.IOException;
import org.apache.lucene.index.FilterLeafReader;
import org.apache.lucene.index.Terms;
import org.apache.lucene.search.suggest.document.CompletionsTermsReader;
import org.apache.lucene.search.suggest.document.NRTSuggester;


public final class CompletionTerms extends FilterLeafReader.FilterTerms {
	private final CompletionsTermsReader reader;

	CompletionTerms(Terms in, CompletionsTermsReader reader) {
		super(in);
		this.reader = reader;
	}

	public byte getType() {
		return 0;
	}

	public long getMinWeight() {
		return (reader) != null ? reader.minWeight : 0;
	}

	public long getMaxWeight() {
		return (reader) != null ? reader.maxWeight : 0;
	}

	public NRTSuggester suggester() throws IOException {
		return (reader) != null ? reader.suggester() : null;
	}
}

