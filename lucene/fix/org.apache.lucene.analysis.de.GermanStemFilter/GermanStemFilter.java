

import java.io.IOException;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.de.GermanStemmer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.KeywordAttribute;
import org.apache.lucene.util.AttributeSource;


public final class GermanStemFilter extends TokenFilter {
	private GermanStemmer stemmer = new GermanStemmer();

	private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

	private final KeywordAttribute keywordAttr = addAttribute(KeywordAttribute.class);

	public GermanStemFilter(TokenStream in) {
		super(in);
	}

	@Override
	public boolean incrementToken() throws IOException {
		if (input.incrementToken()) {
			String term = termAtt.toString();
			if (!(keywordAttr.isKeyword())) {
			}
			return true;
		}else {
			return false;
		}
	}

	public void setStemmer(GermanStemmer stemmer) {
		if (stemmer != null) {
			this.stemmer = stemmer;
		}
	}
}

