import org.apache.lucene.search.uhighlight.*;


import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.automaton.Automata;
import org.apache.lucene.util.automaton.CharacterRunAutomaton;

/**
 * Analyzes the text, producing a single {@link OffsetsEnum} wrapping the {@link TokenStream} filtered to terms
 * in the query, including wildcards.  It can't handle position-sensitive queries (phrases). Passage accuracy suffers
 * because the freq() is unknown -- it's always {@link Integer#MAX_VALUE} instead.
 */
public class TokenStreamOffsetStrategy extends AnalysisOffsetStrategy {

  private static final BytesRef[] ZERO_LEN_BYTES_REF_ARRAY = new BytesRef[0];

  public TokenStreamOffsetStrategy(String field, BytesRef[] terms, PhraseHelper phraseHelper, CharacterRunAutomaton[] automata, Analyzer indexAnalyzer) {
    super(field, ZERO_LEN_BYTES_REF_ARRAY, phraseHelper, convertTermsToAutomata(terms, automata), indexAnalyzer);
    assert phraseHelper.hasPositionSensitivity() == false;
  }

  private static CharacterRunAutomaton[] convertTermsToAutomata(BytesRef[] terms, CharacterRunAutomaton[] automata) {
    CharacterRunAutomaton[] newAutomata = new CharacterRunAutomaton[terms.length + automata.length];
    for (int i = 0; i < terms.length; i++) {
      String termString = terms[i].utf8ToString();
      newAutomata[i] = new CharacterRunAutomaton(Automata.makeString(termString)) {
        @Override
        public String toString() {
          return termString;
        }
      };
    }
    // Append existing automata (that which is used for MTQs)
    System.arraycopy(automata, 0, newAutomata, terms.length, automata.length);
    return newAutomata;
  }

  @Override
  public OffsetsEnum getOffsetsEnum(IndexReader reader, int docId, String content) throws IOException {
    return new TokenStreamOffsetsEnum(tokenStream(content), automata);
  }

  private static class TokenStreamOffsetsEnum extends OffsetsEnum {
    TokenStream stream; // becomes null when closed
    final CharacterRunAutomaton[] matchers;
    final CharTermAttribute charTermAtt;
    final OffsetAttribute offsetAtt;

    int currentMatch = -1;

    final BytesRef matchDescriptions[];

    TokenStreamOffsetsEnum(TokenStream ts, CharacterRunAutomaton[] matchers) throws IOException {
      this.stream = ts;
      this.matchers = matchers;
      matchDescriptions = new BytesRef[matchers.length];
      charTermAtt = ts.addAttribute(CharTermAttribute.class);
      offsetAtt = ts.addAttribute(OffsetAttribute.class);
      ts.reset();
    }

    @Override
    public boolean nextPosition() throws IOException {
      if (stream != null) {
        while (stream.incrementToken()) {
          for (int i = 0; i < matchers.length; i++) {
            if (matchers[i].run(charTermAtt.buffer(), 0, charTermAtt.length())) {
              currentMatch = i;
              return true;
            }
          }
        }
        stream.end();
        close();
      }
      // exhausted
      return false;
    }

    @Override
    public int freq() throws IOException {
      return Integer.MAX_VALUE; // lie
    }


    @Override
    public int startOffset() throws IOException {
      return offsetAtt.startOffset();
    }

    @Override
    public int endOffset() throws IOException {
      return offsetAtt.endOffset();
    }

    @Override
    public BytesRef getTerm() throws IOException {
      if (matchDescriptions[currentMatch] == null) {
        // these CharRunAutomata are subclassed so that toString() returns the query
        matchDescriptions[currentMatch] = new BytesRef(matchers[currentMatch].toString());
      }
      return matchDescriptions[currentMatch];
    }

    @Override
    public void close() throws IOException {
      if (stream != null) {
        stream.close();
        stream = null;
      }
    }
  }
}
