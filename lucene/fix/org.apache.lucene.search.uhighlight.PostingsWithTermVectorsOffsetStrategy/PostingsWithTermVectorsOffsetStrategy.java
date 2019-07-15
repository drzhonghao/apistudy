

import java.io.IOException;
import java.util.List;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.ReaderUtil;
import org.apache.lucene.index.Terms;
import org.apache.lucene.search.uhighlight.FieldOffsetStrategy;
import org.apache.lucene.search.uhighlight.OffsetsEnum;
import org.apache.lucene.search.uhighlight.PhraseHelper;
import org.apache.lucene.search.uhighlight.UnifiedHighlighter;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.automaton.CharacterRunAutomaton;

import static org.apache.lucene.search.uhighlight.UnifiedHighlighter.OffsetSource.POSTINGS_WITH_TERM_VECTORS;


public class PostingsWithTermVectorsOffsetStrategy extends FieldOffsetStrategy {
	public PostingsWithTermVectorsOffsetStrategy(String field, BytesRef[] queryTerms, PhraseHelper phraseHelper, CharacterRunAutomaton[] automata) {
		super(field, queryTerms, phraseHelper, automata);
	}

	@Override
	public OffsetsEnum getOffsetsEnum(IndexReader reader, int docId, String content) throws IOException {
		LeafReader leafReader;
		if (reader instanceof LeafReader) {
			leafReader = ((LeafReader) (reader));
		}else {
			List<LeafReaderContext> leaves = reader.leaves();
			LeafReaderContext LeafReaderContext = leaves.get(ReaderUtil.subIndex(docId, leaves));
			leafReader = LeafReaderContext.reader();
			docId -= LeafReaderContext.docBase;
		}
		Terms docTerms = leafReader.getTermVector(docId, field);
		if (docTerms == null) {
			return OffsetsEnum.EMPTY;
		}
		return createOffsetsEnumFromReader(leafReader, docId);
	}

	@Override
	public UnifiedHighlighter.OffsetSource getOffsetSource() {
		return POSTINGS_WITH_TERM_VECTORS;
	}
}

