import org.apache.lucene.search.uhighlight.PhraseHelper;
import org.apache.lucene.search.uhighlight.UnifiedHighlighter;
import org.apache.lucene.search.uhighlight.OffsetsEnum;
import org.apache.lucene.search.uhighlight.*;


import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.automaton.CharacterRunAutomaton;

/**
 * Never returns offsets. Used when the query would highlight nothing.
 *
 * @lucene.internal
 */
public class NoOpOffsetStrategy extends FieldOffsetStrategy {

  public static final NoOpOffsetStrategy INSTANCE = new NoOpOffsetStrategy();

  private NoOpOffsetStrategy() {
    super("_ignored_", new BytesRef[0], PhraseHelper.NONE, new CharacterRunAutomaton[0]);
  }

  @Override
  public UnifiedHighlighter.OffsetSource getOffsetSource() {
    return UnifiedHighlighter.OffsetSource.NONE_NEEDED;
  }

  @Override
  public OffsetsEnum getOffsetsEnum(IndexReader reader, int docId, String content) throws IOException {
    return OffsetsEnum.EMPTY;
  }

}
