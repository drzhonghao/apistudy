

import java.io.IOException;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.util.AttributeFactory;
import org.apache.lucene.util.AttributeSource;


public final class ClassicTokenizer extends Tokenizer {
	public static final int ALPHANUM = 0;

	public static final int APOSTROPHE = 1;

	public static final int ACRONYM = 2;

	public static final int COMPANY = 3;

	public static final int EMAIL = 4;

	public static final int HOST = 5;

	public static final int NUM = 6;

	public static final int CJ = 7;

	public static final int ACRONYM_DEP = 8;

	public static final String[] TOKEN_TYPES = new String[]{ "<ALPHANUM>", "<APOSTROPHE>", "<ACRONYM>", "<COMPANY>", "<EMAIL>", "<HOST>", "<NUM>", "<CJ>", "<ACRONYM_DEP>" };

	private int skippedPositions;

	private int maxTokenLength = StandardAnalyzer.DEFAULT_MAX_TOKEN_LENGTH;

	public void setMaxTokenLength(int length) {
		if (length < 1) {
			throw new IllegalArgumentException("maxTokenLength must be greater than zero");
		}
		this.maxTokenLength = length;
	}

	public int getMaxTokenLength() {
		return maxTokenLength;
	}

	public ClassicTokenizer() {
		init();
	}

	public ClassicTokenizer(AttributeFactory factory) {
		super(factory);
		init();
	}

	private void init() {
	}

	private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

	private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);

	private final PositionIncrementAttribute posIncrAtt = addAttribute(PositionIncrementAttribute.class);

	private final TypeAttribute typeAtt = addAttribute(TypeAttribute.class);

	@Override
	public final boolean incrementToken() throws IOException {
		clearAttributes();
		skippedPositions = 0;
		while (true) {
		} 
	}

	@Override
	public final void end() throws IOException {
		super.end();
		posIncrAtt.setPositionIncrement(((posIncrAtt.getPositionIncrement()) + (skippedPositions)));
	}

	@Override
	public void close() throws IOException {
		super.close();
	}

	@Override
	public void reset() throws IOException {
		super.reset();
		skippedPositions = 0;
	}
}

