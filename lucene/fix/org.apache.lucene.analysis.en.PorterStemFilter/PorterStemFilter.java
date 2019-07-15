

import java.io.IOException;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.KeywordAttribute;
import org.apache.lucene.util.AttributeSource;


public final class PorterStemFilter extends TokenFilter {
	private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

	private final KeywordAttribute keywordAttr = addAttribute(KeywordAttribute.class);

	public PorterStemFilter(TokenStream in) {
		super(in);
	}

	@Override
	public final boolean incrementToken() throws IOException {
		if (!(input.incrementToken()))
			return false;

		return true;
	}
}

