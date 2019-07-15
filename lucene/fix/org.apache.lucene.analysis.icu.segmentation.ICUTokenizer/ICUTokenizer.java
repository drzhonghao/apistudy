

import com.ibm.icu.lang.UCharacter;
import java.io.IOException;
import java.io.Reader;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.icu.segmentation.DefaultICUTokenizerConfig;
import org.apache.lucene.analysis.icu.segmentation.ICUTokenizerConfig;
import org.apache.lucene.analysis.icu.tokenattributes.ScriptAttribute;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.util.AttributeFactory;
import org.apache.lucene.util.AttributeSource;


public final class ICUTokenizer extends Tokenizer {
	private static final int IOBUFFER = 4096;

	private final char[] buffer = new char[ICUTokenizer.IOBUFFER];

	private int length = 0;

	private int usableLength = 0;

	private int offset = 0;

	private final ICUTokenizerConfig config;

	private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);

	private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

	private final TypeAttribute typeAtt = addAttribute(TypeAttribute.class);

	private final ScriptAttribute scriptAtt = addAttribute(ScriptAttribute.class);

	public ICUTokenizer() {
		this(new DefaultICUTokenizerConfig(true, true));
	}

	public ICUTokenizer(ICUTokenizerConfig config) {
		this(TokenStream.DEFAULT_TOKEN_ATTRIBUTE_FACTORY, config);
	}

	public ICUTokenizer(AttributeFactory factory, ICUTokenizerConfig config) {
		super(factory);
		this.config = config;
	}

	@Override
	public boolean incrementToken() throws IOException {
		clearAttributes();
		if ((length) == 0)
			refill();

		while (!(incrementTokenBuffer())) {
			refill();
			if ((length) <= 0)
				return false;

		} 
		return true;
	}

	@Override
	public void reset() throws IOException {
		super.reset();
		length = usableLength = offset = 0;
	}

	@Override
	public void end() throws IOException {
		super.end();
		final int finalOffset = ((length) < 0) ? offset : (offset) + (length);
		offsetAtt.setOffset(correctOffset(finalOffset), correctOffset(finalOffset));
	}

	private int findSafeEnd() {
		for (int i = (length) - 1; i >= 0; i--)
			if (UCharacter.isWhitespace(buffer[i]))
				return i + 1;


		return -1;
	}

	private void refill() throws IOException {
		offset += usableLength;
		int leftover = (length) - (usableLength);
		System.arraycopy(buffer, usableLength, buffer, 0, leftover);
		int requested = (buffer.length) - leftover;
		int returned = ICUTokenizer.read(input, buffer, leftover, requested);
		length = returned + leftover;
		if (returned < requested)
			usableLength = length;
		else {
			usableLength = findSafeEnd();
			if ((usableLength) < 0)
				usableLength = length;

		}
	}

	private static int read(Reader input, char[] buffer, int offset, int length) throws IOException {
		assert length >= 0 : "length must not be negative: " + length;
		int remaining = length;
		while (remaining > 0) {
			int location = length - remaining;
			int count = input.read(buffer, (offset + location), remaining);
			if ((-1) == count) {
				break;
			}
			remaining -= count;
		} 
		return length - remaining;
	}

	private boolean incrementTokenBuffer() {
		return true;
	}
}

