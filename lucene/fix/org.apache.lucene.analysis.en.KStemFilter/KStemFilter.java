

import java.io.IOException;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.KeywordAttribute;
import org.apache.lucene.util.AttributeSource;


public final class KStemFilter extends TokenFilter {
	private final CharTermAttribute termAttribute = addAttribute(CharTermAttribute.class);

	private final KeywordAttribute keywordAtt = addAttribute(KeywordAttribute.class);

	public KStemFilter(TokenStream in) {
		super(in);
	}

	@Override
	public boolean incrementToken() throws IOException {
		if (!(input.incrementToken()))
			return false;

		char[] term = termAttribute.buffer();
		int len = termAttribute.length();
		return true;
	}
}

