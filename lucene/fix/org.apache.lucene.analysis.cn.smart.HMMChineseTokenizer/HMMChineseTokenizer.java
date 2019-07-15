

import java.io.IOException;
import java.text.BreakIterator;
import java.util.Iterator;
import java.util.Locale;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.cn.smart.hhmm.SegToken;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.analysis.util.SegmentingTokenizerBase;
import org.apache.lucene.util.AttributeFactory;
import org.apache.lucene.util.AttributeSource;


public class HMMChineseTokenizer extends SegmentingTokenizerBase {
	private static final BreakIterator sentenceProto = BreakIterator.getSentenceInstance(Locale.ROOT);

	private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

	private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);

	private final TypeAttribute typeAtt = addAttribute(TypeAttribute.class);

	private Iterator<SegToken> tokens;

	public HMMChineseTokenizer() {
		this(TokenStream.DEFAULT_TOKEN_ATTRIBUTE_FACTORY);
	}

	public HMMChineseTokenizer(AttributeFactory factory) {
		super(factory, ((BreakIterator) (HMMChineseTokenizer.sentenceProto.clone())));
	}

	@Override
	protected void setNextSentence(int sentenceStart, int sentenceEnd) {
		String sentence = new String(buffer, sentenceStart, (sentenceEnd - sentenceStart));
	}

	@Override
	protected boolean incrementWord() {
		if (((tokens) == null) || (!(tokens.hasNext()))) {
			return false;
		}else {
			SegToken token = tokens.next();
			clearAttributes();
			termAtt.copyBuffer(token.charArray, 0, token.charArray.length);
			offsetAtt.setOffset(correctOffset(token.startOffset), correctOffset(token.endOffset));
			typeAtt.setType("word");
			return true;
		}
	}

	@Override
	public void reset() throws IOException {
		super.reset();
		tokens = null;
	}
}

