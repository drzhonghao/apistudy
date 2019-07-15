import org.apache.lucene.search.uhighlight.*;


import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.Terms;
import org.apache.lucene.search.highlight.TermVectorLeafReader;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.automaton.CharacterRunAutomaton;

/**
 * Uses term vectors that contain offsets.
 *
 * @lucene.internal
 */
public class TermVectorOffsetStrategy extends FieldOffsetStrategy {

  public TermVectorOffsetStrategy(String field, BytesRef[] queryTerms, PhraseHelper phraseHelper, CharacterRunAutomaton[] automata) {
    super(field, queryTerms, phraseHelper, automata);
  }

  @Override
  public UnifiedHighlighter.OffsetSource getOffsetSource() {
    return UnifiedHighlighter.OffsetSource.TERM_VECTORS;
  }

  @Override
  public OffsetsEnum getOffsetsEnum(IndexReader reader, int docId, String content) throws IOException {
    Terms tvTerms = reader.getTermVector(docId, field);
    if (tvTerms == null) {
      return OffsetsEnum.EMPTY;
    }

    LeafReader leafReader = new TermVectorLeafReader(field, tvTerms);
    docId = 0;

    return createOffsetsEnumFromReader(leafReader, docId);
  }

}
