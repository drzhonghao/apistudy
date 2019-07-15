

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.hunspell.Dictionary;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.KeywordAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.util.AttributeSource;
import org.apache.lucene.util.CharsRef;


public final class HunspellStemFilter extends TokenFilter {
	private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

	private final PositionIncrementAttribute posIncAtt = addAttribute(PositionIncrementAttribute.class);

	private final KeywordAttribute keywordAtt = addAttribute(KeywordAttribute.class);

	private List<CharsRef> buffer;

	private AttributeSource.State savedState;

	private final boolean dedup;

	private final boolean longestOnly;

	public HunspellStemFilter(TokenStream input, Dictionary dictionary) {
		this(input, dictionary, true);
	}

	public HunspellStemFilter(TokenStream input, Dictionary dictionary, boolean dedup) {
		this(input, dictionary, dedup, false);
	}

	public HunspellStemFilter(TokenStream input, Dictionary dictionary, boolean dedup, boolean longestOnly) {
		super(input);
		this.dedup = dedup && (longestOnly == false);
		this.longestOnly = longestOnly;
	}

	@Override
	public boolean incrementToken() throws IOException {
		if (((buffer) != null) && (!(buffer.isEmpty()))) {
			CharsRef nextStem = buffer.remove(0);
			restoreState(savedState);
			posIncAtt.setPositionIncrement(0);
			termAtt.setEmpty().append(nextStem);
			return true;
		}
		if (!(input.incrementToken())) {
			return false;
		}
		if (keywordAtt.isKeyword()) {
			return true;
		}
		if (buffer.isEmpty()) {
			return true;
		}
		if ((longestOnly) && ((buffer.size()) > 1)) {
			Collections.sort(buffer, HunspellStemFilter.lengthComparator);
		}
		CharsRef stem = buffer.remove(0);
		termAtt.setEmpty().append(stem);
		if (longestOnly) {
			buffer.clear();
		}else {
			if (!(buffer.isEmpty())) {
				savedState = captureState();
			}
		}
		return true;
	}

	@Override
	public void reset() throws IOException {
		super.reset();
		buffer = null;
	}

	static final Comparator<CharsRef> lengthComparator = new Comparator<CharsRef>() {
		@Override
		public int compare(CharsRef o1, CharsRef o2) {
			int cmp = Integer.compare(o2.length, o1.length);
			if (cmp == 0) {
				return o2.compareTo(o1);
			}else {
				return cmp;
			}
		}
	};
}

