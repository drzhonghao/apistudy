

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.FlagsAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.util.AttributeFactory;
import org.apache.lucene.util.AttributeSource;
import org.apache.lucene.util.AttributeSource.State;


public final class WikipediaTokenizer extends Tokenizer {
	public static final String INTERNAL_LINK = "il";

	public static final String EXTERNAL_LINK = "el";

	public static final String EXTERNAL_LINK_URL = "elu";

	public static final String CITATION = "ci";

	public static final String CATEGORY = "c";

	public static final String BOLD = "b";

	public static final String ITALICS = "i";

	public static final String BOLD_ITALICS = "bi";

	public static final String HEADING = "h";

	public static final String SUB_HEADING = "sh";

	public static final int ALPHANUM_ID = 0;

	public static final int APOSTROPHE_ID = 1;

	public static final int ACRONYM_ID = 2;

	public static final int COMPANY_ID = 3;

	public static final int EMAIL_ID = 4;

	public static final int HOST_ID = 5;

	public static final int NUM_ID = 6;

	public static final int CJ_ID = 7;

	public static final int INTERNAL_LINK_ID = 8;

	public static final int EXTERNAL_LINK_ID = 9;

	public static final int CITATION_ID = 10;

	public static final int CATEGORY_ID = 11;

	public static final int BOLD_ID = 12;

	public static final int ITALICS_ID = 13;

	public static final int BOLD_ITALICS_ID = 14;

	public static final int HEADING_ID = 15;

	public static final int SUB_HEADING_ID = 16;

	public static final int EXTERNAL_LINK_URL_ID = 17;

	public static final String[] TOKEN_TYPES = new String[]{ "<ALPHANUM>", "<APOSTROPHE>", "<ACRONYM>", "<COMPANY>", "<EMAIL>", "<HOST>", "<NUM>", "<CJ>", WikipediaTokenizer.INTERNAL_LINK, WikipediaTokenizer.EXTERNAL_LINK, WikipediaTokenizer.CITATION, WikipediaTokenizer.CATEGORY, WikipediaTokenizer.BOLD, WikipediaTokenizer.ITALICS, WikipediaTokenizer.BOLD_ITALICS, WikipediaTokenizer.HEADING, WikipediaTokenizer.SUB_HEADING, WikipediaTokenizer.EXTERNAL_LINK_URL };

	public static final int TOKENS_ONLY = 0;

	public static final int UNTOKENIZED_ONLY = 1;

	public static final int BOTH = 2;

	public static final int UNTOKENIZED_TOKEN_FLAG = 1;

	private int tokenOutput = WikipediaTokenizer.TOKENS_ONLY;

	private Set<String> untokenizedTypes = Collections.emptySet();

	private Iterator<AttributeSource.State> tokens = null;

	private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);

	private final TypeAttribute typeAtt = addAttribute(TypeAttribute.class);

	private final PositionIncrementAttribute posIncrAtt = addAttribute(PositionIncrementAttribute.class);

	private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

	private final FlagsAttribute flagsAtt = addAttribute(FlagsAttribute.class);

	private boolean first;

	public WikipediaTokenizer() {
		this(WikipediaTokenizer.TOKENS_ONLY, Collections.<String>emptySet());
	}

	public WikipediaTokenizer(int tokenOutput, Set<String> untokenizedTypes) {
		init(tokenOutput, untokenizedTypes);
	}

	public WikipediaTokenizer(AttributeFactory factory, int tokenOutput, Set<String> untokenizedTypes) {
		super(factory);
		init(tokenOutput, untokenizedTypes);
	}

	private void init(int tokenOutput, Set<String> untokenizedTypes) {
		if (((tokenOutput != (WikipediaTokenizer.TOKENS_ONLY)) && (tokenOutput != (WikipediaTokenizer.UNTOKENIZED_ONLY))) && (tokenOutput != (WikipediaTokenizer.BOTH))) {
			throw new IllegalArgumentException("tokenOutput must be TOKENS_ONLY, UNTOKENIZED_ONLY or BOTH");
		}
		this.tokenOutput = tokenOutput;
		this.untokenizedTypes = untokenizedTypes;
	}

	@Override
	public final boolean incrementToken() throws IOException {
		if (((tokens) != null) && (tokens.hasNext())) {
			AttributeSource.State state = tokens.next();
			restoreState(state);
			return true;
		}
		clearAttributes();
		first = false;
		return true;
	}

	private void collapseAndSaveTokens(int tokenType, String type) throws IOException {
		StringBuilder buffer = new StringBuilder(32);
		int tmpTokType;
		int numSeen = 0;
		List<AttributeSource.State> tmp = new ArrayList<>();
		setupSavedToken(0, type);
		tmp.add(captureState());
		String s = buffer.toString().trim();
		termAtt.setEmpty().append(s);
		flagsAtt.setFlags(WikipediaTokenizer.UNTOKENIZED_TOKEN_FLAG);
		tokens = tmp.iterator();
	}

	private void setupSavedToken(int positionInc, String type) {
		setupToken();
		posIncrAtt.setPositionIncrement(positionInc);
		typeAtt.setType(type);
	}

	private void collapseTokens(int tokenType) throws IOException {
		StringBuilder buffer = new StringBuilder(32);
		int tmpTokType;
		int numSeen = 0;
		String s = buffer.toString().trim();
		termAtt.setEmpty().append(s);
		flagsAtt.setFlags(WikipediaTokenizer.UNTOKENIZED_TOKEN_FLAG);
	}

	private void setupToken() {
	}

	@Override
	public void close() throws IOException {
		super.close();
	}

	@Override
	public void reset() throws IOException {
		super.reset();
		tokens = null;
		first = true;
	}

	@Override
	public void end() throws IOException {
		super.end();
	}
}

