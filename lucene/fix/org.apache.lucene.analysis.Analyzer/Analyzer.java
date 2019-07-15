

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.TermToBytesRefAttribute;
import org.apache.lucene.store.AlreadyClosedException;
import org.apache.lucene.util.AttributeFactory;
import org.apache.lucene.util.AttributeSource;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.CloseableThreadLocal;
import org.apache.lucene.util.Version;


public abstract class Analyzer implements Closeable {
	private final Analyzer.ReuseStrategy reuseStrategy;

	private Version version = Version.LATEST;

	CloseableThreadLocal<Object> storedValue = new CloseableThreadLocal<>();

	public Analyzer() {
		this(Analyzer.GLOBAL_REUSE_STRATEGY);
	}

	public Analyzer(Analyzer.ReuseStrategy reuseStrategy) {
		this.reuseStrategy = reuseStrategy;
	}

	protected abstract Analyzer.TokenStreamComponents createComponents(String fieldName);

	protected TokenStream normalize(String fieldName, TokenStream in) {
		return in;
	}

	public final TokenStream tokenStream(final String fieldName, final Reader reader) {
		Analyzer.TokenStreamComponents components = reuseStrategy.getReusableComponents(this, fieldName);
		final Reader r = initReader(fieldName, reader);
		if (components == null) {
			components = createComponents(fieldName);
			reuseStrategy.setReusableComponents(this, fieldName, components);
		}
		components.setReader(r);
		return components.getTokenStream();
	}

	public final TokenStream tokenStream(final String fieldName, final String text) {
		Analyzer.TokenStreamComponents components = reuseStrategy.getReusableComponents(this, fieldName);
		if (components == null) {
			components = createComponents(fieldName);
			reuseStrategy.setReusableComponents(this, fieldName, components);
		}
		return components.getTokenStream();
	}

	public final BytesRef normalize(final String fieldName, final String text) {
		try {
			final String filteredText;
			try (Reader reader = new StringReader(text)) {
				Reader filterReader = initReaderForNormalization(fieldName, reader);
				char[] buffer = new char[64];
				StringBuilder builder = new StringBuilder();
				for (; ;) {
					final int read = filterReader.read(buffer, 0, buffer.length);
					if (read == (-1)) {
						break;
					}
					builder.append(buffer, 0, read);
				}
				filteredText = builder.toString();
			} catch (IOException e) {
				throw new IllegalStateException("Normalization threw an unexpected exception", e);
			}
			final AttributeFactory attributeFactory = attributeFactory(fieldName);
			try (TokenStream ts = normalize(fieldName, new Analyzer.StringTokenStream(attributeFactory, filteredText, text.length()))) {
				final TermToBytesRefAttribute termAtt = ts.addAttribute(TermToBytesRefAttribute.class);
				ts.reset();
				if ((ts.incrementToken()) == false) {
					throw new IllegalStateException(((((("The normalization token stream is " + "expected to produce exactly 1 token, but got 0 for analyzer ") + (this)) + " and input \"") + text) + "\""));
				}
				final BytesRef term = BytesRef.deepCopyOf(termAtt.getBytesRef());
				if (ts.incrementToken()) {
					throw new IllegalStateException(((((("The normalization token stream is " + "expected to produce exactly 1 token, but got 2+ for analyzer ") + (this)) + " and input \"") + text) + "\""));
				}
				ts.end();
				return term;
			}
		} catch (IOException e) {
			throw new IllegalStateException("Normalization threw an unexpected exception", e);
		}
	}

	protected Reader initReader(String fieldName, Reader reader) {
		return reader;
	}

	protected Reader initReaderForNormalization(String fieldName, Reader reader) {
		return reader;
	}

	protected AttributeFactory attributeFactory(String fieldName) {
		return TokenStream.DEFAULT_TOKEN_ATTRIBUTE_FACTORY;
	}

	public int getPositionIncrementGap(String fieldName) {
		return 0;
	}

	public int getOffsetGap(String fieldName) {
		return 1;
	}

	public final Analyzer.ReuseStrategy getReuseStrategy() {
		return reuseStrategy;
	}

	public void setVersion(Version v) {
		version = v;
	}

	public Version getVersion() {
		return version;
	}

	@Override
	public void close() {
		if ((storedValue) != null) {
			storedValue.close();
			storedValue = null;
		}
	}

	public static class TokenStreamComponents {
		protected final Tokenizer source;

		protected final TokenStream sink;

		public TokenStreamComponents(final Tokenizer source, final TokenStream result) {
			this.source = source;
			this.sink = result;
		}

		public TokenStreamComponents(final Tokenizer source) {
			this.source = source;
			this.sink = source;
		}

		protected void setReader(final Reader reader) {
			source.setReader(reader);
		}

		public TokenStream getTokenStream() {
			return sink;
		}

		public Tokenizer getTokenizer() {
			return source;
		}
	}

	public static abstract class ReuseStrategy {
		public ReuseStrategy() {
		}

		public abstract Analyzer.TokenStreamComponents getReusableComponents(Analyzer analyzer, String fieldName);

		public abstract void setReusableComponents(Analyzer analyzer, String fieldName, Analyzer.TokenStreamComponents components);

		protected final Object getStoredValue(Analyzer analyzer) {
			if ((analyzer.storedValue) == null) {
				throw new AlreadyClosedException("this Analyzer is closed");
			}
			return analyzer.storedValue.get();
		}

		protected final void setStoredValue(Analyzer analyzer, Object storedValue) {
			if ((analyzer.storedValue) == null) {
				throw new AlreadyClosedException("this Analyzer is closed");
			}
			analyzer.storedValue.set(storedValue);
		}
	}

	public static final Analyzer.ReuseStrategy GLOBAL_REUSE_STRATEGY = null;

	public static final Analyzer.ReuseStrategy PER_FIELD_REUSE_STRATEGY = null;

	private static final class StringTokenStream extends TokenStream {
		private final String value;

		private final int length;

		private boolean used = true;

		private final CharTermAttribute termAttribute = addAttribute(CharTermAttribute.class);

		private final OffsetAttribute offsetAttribute = addAttribute(OffsetAttribute.class);

		StringTokenStream(AttributeFactory attributeFactory, String value, int length) {
			super(attributeFactory);
			this.value = value;
			this.length = length;
		}

		@Override
		public void reset() {
			used = false;
		}

		@Override
		public boolean incrementToken() {
			if (used) {
				return false;
			}
			clearAttributes();
			termAttribute.append(value);
			offsetAttribute.setOffset(0, length);
			used = true;
			return true;
		}

		@Override
		public void end() throws IOException {
			super.end();
			offsetAttribute.setOffset(length, length);
		}
	}
}

