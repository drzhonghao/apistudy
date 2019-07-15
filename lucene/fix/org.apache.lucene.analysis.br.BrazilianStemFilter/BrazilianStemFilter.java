

import java.io.IOException;
import java.util.Set;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.br.BrazilianStemmer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.KeywordAttribute;
import org.apache.lucene.util.AttributeSource;


public final class BrazilianStemFilter extends TokenFilter {
	private BrazilianStemmer stemmer = new BrazilianStemmer();

	private Set<?> exclusions = null;

	private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

	private final KeywordAttribute keywordAttr = addAttribute(KeywordAttribute.class);

	public BrazilianStemFilter(TokenStream in) {
		super(in);
	}

	@Override
	public boolean incrementToken() throws IOException {
		if (input.incrementToken()) {
			final String term = termAtt.toString();
			if ((!(keywordAttr.isKeyword())) && (((exclusions) == null) || (!(exclusions.contains(term))))) {
			}
			return true;
		}else {
			return false;
		}
	}
}

