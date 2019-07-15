

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.search.suggest.document.CompletionTokenStream;
import org.apache.lucene.search.suggest.document.SuggestField;
import org.apache.lucene.util.AttributeSource;


public class ContextSuggestField extends SuggestField {
	public static final int CONTEXT_SEPARATOR = '\u001d';

	static final byte TYPE = 1;

	private final Set<CharSequence> contexts;

	public ContextSuggestField(String name, String value, int weight, CharSequence... contexts) {
		super(name, value, weight);
		validate(value);
		this.contexts = new HashSet<>((contexts != null ? contexts.length : 0));
		if (contexts != null) {
			Collections.addAll(this.contexts, contexts);
		}
	}

	protected Iterable<CharSequence> contexts() {
		return contexts;
	}

	@Override
	protected CompletionTokenStream wrapTokenStream(TokenStream stream) {
		final Iterable<CharSequence> contexts = contexts();
		for (CharSequence context : contexts) {
			validate(context);
		}
		CompletionTokenStream completionTokenStream;
		if (stream instanceof CompletionTokenStream) {
			completionTokenStream = ((CompletionTokenStream) (stream));
		}else {
		}
		completionTokenStream = null;
		return completionTokenStream;
	}

	@Override
	protected byte type() {
		return ContextSuggestField.TYPE;
	}

	private static final class PrefixTokenFilter extends TokenFilter {
		private final char separator;

		private final CharTermAttribute termAttr = addAttribute(CharTermAttribute.class);

		private final PositionIncrementAttribute posAttr = addAttribute(PositionIncrementAttribute.class);

		private final Iterable<CharSequence> prefixes;

		private Iterator<CharSequence> currentPrefix;

		public PrefixTokenFilter(TokenStream input, char separator, Iterable<CharSequence> prefixes) {
			super(input);
			this.prefixes = prefixes;
			this.currentPrefix = null;
			this.separator = separator;
		}

		@Override
		public boolean incrementToken() throws IOException {
			if ((currentPrefix) != null) {
				if (!(currentPrefix.hasNext())) {
					return input.incrementToken();
				}else {
					posAttr.setPositionIncrement(0);
				}
			}else {
				currentPrefix = prefixes.iterator();
				termAttr.setEmpty();
				posAttr.setPositionIncrement(1);
			}
			termAttr.setEmpty();
			if (currentPrefix.hasNext()) {
				termAttr.append(currentPrefix.next());
			}
			termAttr.append(separator);
			return true;
		}

		@Override
		public void reset() throws IOException {
			super.reset();
			currentPrefix = null;
		}
	}

	private void validate(final CharSequence value) {
		for (int i = 0; i < (value.length()); i++) {
			if ((ContextSuggestField.CONTEXT_SEPARATOR) == (value.charAt(i))) {
				throw new IllegalArgumentException((((((("Illegal value [" + value) + "] UTF-16 codepoint [0x") + (Integer.toHexString(((int) (value.charAt(i)))))) + "] at position ") + i) + " is a reserved character"));
			}
		}
	}
}

