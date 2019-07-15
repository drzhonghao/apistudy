

import org.apache.lucene.analysis.icu.segmentation.ICUTokenizerConfig;


final class CompositeBreakIterator {
	private final ICUTokenizerConfig config;

	private char[] text;

	CompositeBreakIterator(ICUTokenizerConfig config) {
		this.config = config;
	}

	int next() {
		return 0;
	}

	int current() {
		return 0;
	}

	int getRuleStatus() {
		return 0;
	}

	int getScriptCode() {
		return 0;
	}

	void setText(final char[] text, int start, int length) {
		this.text = text;
	}
}

