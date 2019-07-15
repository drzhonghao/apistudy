

import com.ibm.icu.text.Normalizer2;

import static com.ibm.icu.text.Normalizer2.Mode.COMPOSE;


public class TokenInfoDictionaryBuilder {
	private int offset = 0;

	private String encoding = "utf-8";

	private boolean normalizeEntries = false;

	private Normalizer2 normalizer;

	public TokenInfoDictionaryBuilder(String encoding, boolean normalizeEntries) {
		this.encoding = encoding;
		this.normalizeEntries = normalizeEntries;
		this.normalizer = (normalizeEntries) ? Normalizer2.getInstance(null, "nfkc", COMPOSE) : null;
	}
}

