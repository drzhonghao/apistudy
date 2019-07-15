import org.apache.lucene.search.uhighlight.*;


import java.io.IOException;
import java.util.List;

import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.ReaderUtil;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.automaton.CharacterRunAutomaton;

/**
 * Uses offsets in postings -- {@link IndexOptions#DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS}.  This
 * does not support multi-term queries; the highlighter will fallback on analysis for that.
 *
 * @lucene.internal
 */
public class PostingsOffsetStrategy extends FieldOffsetStrategy {

  public PostingsOffsetStrategy(String field, BytesRef[] queryTerms, PhraseHelper phraseHelper, CharacterRunAutomaton[] automata) {
    super(field, queryTerms, phraseHelper, automata);
  }

  @Override
  public OffsetsEnum getOffsetsEnum(IndexReader reader, int docId, String content) throws IOException {
    final LeafReader leafReader;
    if (reader instanceof LeafReader) {
      leafReader = (LeafReader) reader;
    } else {
      List<LeafReaderContext> leaves = reader.leaves();
      LeafReaderContext leafReaderContext = leaves.get(ReaderUtil.subIndex(docId, leaves));
      leafReader = leafReaderContext.reader();
      docId -= leafReaderContext.docBase; // adjust 'doc' to be within this leaf reader
    }

    return createOffsetsEnumFromReader(leafReader, docId);
  }


  @Override
  public UnifiedHighlighter.OffsetSource getOffsetSource() {
    return UnifiedHighlighter.OffsetSource.POSTINGS;
  }
}
