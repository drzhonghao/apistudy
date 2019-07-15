

import java.io.IOException;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.search.uhighlight.FieldOffsetStrategy;
import org.apache.lucene.search.uhighlight.PhraseHelper;
import org.apache.lucene.search.uhighlight.UnifiedHighlighter;
import org.apache.lucene.util.AttributeSource;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.automaton.CharacterRunAutomaton;

import static org.apache.lucene.search.uhighlight.UnifiedHighlighter.OffsetSource.ANALYSIS;


public abstract class AnalysisOffsetStrategy extends FieldOffsetStrategy {
	protected final Analyzer analyzer;

	public AnalysisOffsetStrategy(String field, BytesRef[] queryTerms, PhraseHelper phraseHelper, CharacterRunAutomaton[] automata, Analyzer analyzer) {
		super(field, queryTerms, phraseHelper, automata);
		this.analyzer = analyzer;
		if ((analyzer.getOffsetGap(field)) != 1) {
			throw new IllegalArgumentException((("offset gap of the provided analyzer should be 1 (field " + field) + ")"));
		}
	}

	@Override
	public final UnifiedHighlighter.OffsetSource getOffsetSource() {
		return ANALYSIS;
	}

	protected TokenStream tokenStream(String content) throws IOException {
		return null;
	}

	private static final class MultiValueTokenStream extends TokenFilter {
		private final String fieldName;

		private final Analyzer indexAnalyzer;

		private final String content;

		private final char splitChar;

		private final PositionIncrementAttribute posIncAtt = addAttribute(PositionIncrementAttribute.class);

		private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);

		private int startValIdx = 0;

		private int endValIdx;

		private int remainingPosInc = 0;

		private MultiValueTokenStream(TokenStream subTokenStream, String fieldName, Analyzer indexAnalyzer, String content, char splitChar, int splitCharIdx) {
			super(subTokenStream);
			this.fieldName = fieldName;
			this.indexAnalyzer = indexAnalyzer;
			this.content = content;
			this.splitChar = splitChar;
			this.endValIdx = splitCharIdx;
		}

		@Override
		public void reset() throws IOException {
			if ((startValIdx) != 0) {
				throw new IllegalStateException("This TokenStream wasn't developed to be re-used.");
			}
			super.reset();
		}

		@Override
		public boolean incrementToken() throws IOException {
			while (true) {
				if (input.incrementToken()) {
					if ((remainingPosInc) > 0) {
						posIncAtt.setPositionIncrement(((remainingPosInc) + (posIncAtt.getPositionIncrement())));
						remainingPosInc = 0;
					}
					offsetAtt.setOffset(((startValIdx) + (offsetAtt.startOffset())), ((startValIdx) + (offsetAtt.endOffset())));
					return true;
				}
				if ((endValIdx) == (content.length())) {
					return false;
				}
				input.end();
				remainingPosInc += posIncAtt.getPositionIncrement();
				input.close();
				remainingPosInc += indexAnalyzer.getPositionIncrementGap(fieldName);
				startValIdx = (endValIdx) + 1;
				endValIdx = content.indexOf(splitChar, startValIdx);
				if ((endValIdx) == (-1)) {
					endValIdx = content.length();
				}
				TokenStream tokenStream = indexAnalyzer.tokenStream(fieldName, content.substring(startValIdx, endValIdx));
				if (tokenStream != (input)) {
					throw new IllegalStateException(("Require TokenStream re-use.  Unsupported re-use strategy?: " + (indexAnalyzer.getReuseStrategy())));
				}
				tokenStream.reset();
			} 
		}

		@Override
		public void end() throws IOException {
			super.end();
			offsetAtt.setOffset(((startValIdx) + (offsetAtt.startOffset())), ((startValIdx) + (offsetAtt.endOffset())));
		}
	}
}

