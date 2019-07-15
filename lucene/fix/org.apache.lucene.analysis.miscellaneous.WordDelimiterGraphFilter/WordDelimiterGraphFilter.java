

import java.io.IOException;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.miscellaneous.WordDelimiterIterator;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.KeywordAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionLengthAttribute;
import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.AttributeSource;
import org.apache.lucene.util.InPlaceMergeSorter;
import org.apache.lucene.util.RamUsageEstimator;


public final class WordDelimiterGraphFilter extends TokenFilter {
	public static final int GENERATE_WORD_PARTS = 1;

	public static final int GENERATE_NUMBER_PARTS = 2;

	public static final int CATENATE_WORDS = 4;

	public static final int CATENATE_NUMBERS = 8;

	public static final int CATENATE_ALL = 16;

	public static final int PRESERVE_ORIGINAL = 32;

	public static final int SPLIT_ON_CASE_CHANGE = 64;

	public static final int SPLIT_ON_NUMERICS = 128;

	public static final int STEM_ENGLISH_POSSESSIVE = 256;

	public static final int IGNORE_KEYWORDS = 512;

	final CharArraySet protWords;

	private final int flags;

	private int[] bufferedParts = new int[16];

	private int bufferedLen;

	private int bufferedPos;

	private char[][] bufferedTermParts = new char[4][];

	private final CharTermAttribute termAttribute = addAttribute(CharTermAttribute.class);

	private final KeywordAttribute keywordAttribute = addAttribute(KeywordAttribute.class);

	private final OffsetAttribute offsetAttribute = addAttribute(OffsetAttribute.class);

	private final PositionIncrementAttribute posIncAttribute = addAttribute(PositionIncrementAttribute.class);

	private final PositionLengthAttribute posLenAttribute = addAttribute(PositionLengthAttribute.class);

	private final WordDelimiterIterator iterator;

	private final WordDelimiterGraphFilter.WordDelimiterConcatenation concat = new WordDelimiterGraphFilter.WordDelimiterConcatenation();

	private int lastConcatCount;

	private final WordDelimiterGraphFilter.WordDelimiterConcatenation concatAll = new WordDelimiterGraphFilter.WordDelimiterConcatenation();

	private int accumPosInc;

	private char[] savedTermBuffer = new char[16];

	private int savedTermLength;

	private int savedStartOffset;

	private int savedEndOffset;

	private AttributeSource.State savedState;

	private int lastStartOffset;

	private boolean hasIllegalOffsets;

	private int wordPos;

	public WordDelimiterGraphFilter(TokenStream in, byte[] charTypeTable, int configurationFlags, CharArraySet protWords) {
		super(in);
		if ((configurationFlags & (~((((((((((WordDelimiterGraphFilter.GENERATE_WORD_PARTS) | (WordDelimiterGraphFilter.GENERATE_NUMBER_PARTS)) | (WordDelimiterGraphFilter.CATENATE_WORDS)) | (WordDelimiterGraphFilter.CATENATE_NUMBERS)) | (WordDelimiterGraphFilter.CATENATE_ALL)) | (WordDelimiterGraphFilter.PRESERVE_ORIGINAL)) | (WordDelimiterGraphFilter.SPLIT_ON_CASE_CHANGE)) | (WordDelimiterGraphFilter.SPLIT_ON_NUMERICS)) | (WordDelimiterGraphFilter.STEM_ENGLISH_POSSESSIVE)) | (WordDelimiterGraphFilter.IGNORE_KEYWORDS)))) != 0) {
			throw new IllegalArgumentException(("flags contains unrecognized flag: " + configurationFlags));
		}
		this.flags = configurationFlags;
		this.protWords = protWords;
		iterator = null;
	}

	public WordDelimiterGraphFilter(TokenStream in, int configurationFlags, CharArraySet protWords) {
		this(in, WordDelimiterIterator.DEFAULT_WORD_DELIM_TABLE, configurationFlags, protWords);
	}

	private void bufferWordParts() throws IOException {
		saveState();
		hasIllegalOffsets = ((savedEndOffset) - (savedStartOffset)) != (savedTermLength);
		bufferedLen = 0;
		lastConcatCount = 0;
		wordPos = 0;
		if (has(WordDelimiterGraphFilter.PRESERVE_ORIGINAL)) {
			if ((wordPos) == 0) {
				(wordPos)++;
			}
			buffer(0, wordPos, 0, savedTermLength);
		}
		sorter.sort(0, bufferedLen);
		wordPos = 0;
		bufferedPos = 0;
	}

	@Override
	public boolean incrementToken() throws IOException {
		while (true) {
			if ((savedState) == null) {
				if ((input.incrementToken()) == false) {
					return false;
				}
				if ((has(WordDelimiterGraphFilter.IGNORE_KEYWORDS)) && (keywordAttribute.isKeyword())) {
					return true;
				}
				int termLength = termAttribute.length();
				char[] termBuffer = termAttribute.buffer();
				accumPosInc += posIncAttribute.getPositionIncrement();
				bufferWordParts();
			}
			if ((bufferedPos) < (bufferedLen)) {
				clearAttributes();
				restoreState(savedState);
				char[] termPart = bufferedTermParts[bufferedPos];
				int startPos = bufferedParts[(4 * (bufferedPos))];
				int endPos = bufferedParts[((4 * (bufferedPos)) + 1)];
				int startPart = bufferedParts[((4 * (bufferedPos)) + 2)];
				int endPart = bufferedParts[((4 * (bufferedPos)) + 3)];
				(bufferedPos)++;
				int startOffset;
				int endOffset;
				if (hasIllegalOffsets) {
					startOffset = savedStartOffset;
					endOffset = savedEndOffset;
				}else {
					startOffset = (savedStartOffset) + startPart;
					endOffset = (savedStartOffset) + endPart;
				}
				startOffset = Math.max(startOffset, lastStartOffset);
				endOffset = Math.max(endOffset, lastStartOffset);
				offsetAttribute.setOffset(startOffset, endOffset);
				lastStartOffset = startOffset;
				if (termPart == null) {
					termAttribute.copyBuffer(savedTermBuffer, startPart, (endPart - startPart));
				}else {
					termAttribute.copyBuffer(termPart, 0, termPart.length);
				}
				posIncAttribute.setPositionIncrement((((accumPosInc) + startPos) - (wordPos)));
				accumPosInc = 0;
				posLenAttribute.setPositionLength((endPos - startPos));
				wordPos = startPos;
				return true;
			}
			savedState = null;
		} 
	}

	@Override
	public void reset() throws IOException {
		super.reset();
		accumPosInc = 0;
		savedState = null;
		lastStartOffset = 0;
		concat.clear();
		concatAll.clear();
	}

	private class PositionSorter extends InPlaceMergeSorter {
		@Override
		protected int compare(int i, int j) {
			int iPosStart = bufferedParts[(4 * i)];
			int jPosStart = bufferedParts[(4 * j)];
			int cmp = Integer.compare(iPosStart, jPosStart);
			if (cmp != 0) {
				return cmp;
			}
			int iPosEnd = bufferedParts[((4 * i) + 1)];
			int jPosEnd = bufferedParts[((4 * j) + 1)];
			return Integer.compare(jPosEnd, iPosEnd);
		}

		@Override
		protected void swap(int i, int j) {
			int iOffset = 4 * i;
			int jOffset = 4 * j;
			for (int x = 0; x < 4; x++) {
				int tmp = bufferedParts[(iOffset + x)];
				bufferedParts[(iOffset + x)] = bufferedParts[(jOffset + x)];
				bufferedParts[(jOffset + x)] = tmp;
			}
			char[] tmp2 = bufferedTermParts[i];
			bufferedTermParts[i] = bufferedTermParts[j];
			bufferedTermParts[j] = tmp2;
		}
	}

	final WordDelimiterGraphFilter.PositionSorter sorter = new WordDelimiterGraphFilter.PositionSorter();

	void buffer(int startPos, int endPos, int startPart, int endPart) {
		buffer(null, startPos, endPos, startPart, endPart);
	}

	void buffer(char[] termPart, int startPos, int endPos, int startPart, int endPart) {
		assert endPos > startPos : (("startPos=" + startPos) + " endPos=") + endPos;
		assert (endPart > startPart) || (((endPart == 0) && (startPart == 0)) && ((savedTermLength) == 0)) : (("startPart=" + startPart) + " endPart=") + endPart;
		if ((((bufferedLen) + 1) * 4) > (bufferedParts.length)) {
			bufferedParts = ArrayUtil.grow(bufferedParts, (((bufferedLen) + 1) * 4));
		}
		if ((bufferedTermParts.length) == (bufferedLen)) {
			int newSize = ArrayUtil.oversize(((bufferedLen) + 1), RamUsageEstimator.NUM_BYTES_OBJECT_REF);
			char[][] newArray = new char[newSize][];
			System.arraycopy(bufferedTermParts, 0, newArray, 0, bufferedTermParts.length);
			bufferedTermParts = newArray;
		}
		bufferedTermParts[bufferedLen] = termPart;
		bufferedParts[((bufferedLen) * 4)] = startPos;
		bufferedParts[(((bufferedLen) * 4) + 1)] = endPos;
		bufferedParts[(((bufferedLen) * 4) + 2)] = startPart;
		bufferedParts[(((bufferedLen) * 4) + 3)] = endPart;
		(bufferedLen)++;
	}

	private void saveState() {
		savedTermLength = termAttribute.length();
		savedStartOffset = offsetAttribute.startOffset();
		savedEndOffset = offsetAttribute.endOffset();
		savedState = captureState();
		if ((savedTermBuffer.length) < (savedTermLength)) {
			savedTermBuffer = new char[ArrayUtil.oversize(savedTermLength, Character.BYTES)];
		}
		System.arraycopy(termAttribute.buffer(), 0, savedTermBuffer, 0, savedTermLength);
	}

	private void flushConcatenation(WordDelimiterGraphFilter.WordDelimiterConcatenation concat) {
		if ((wordPos) == (concat.startPos)) {
			(wordPos)++;
		}
		lastConcatCount = concat.subwordCount;
		if (((concat.subwordCount) != 1) || ((shouldGenerateParts(concat.type)) == false)) {
			concat.write();
		}
		concat.clear();
	}

	private boolean shouldConcatenate(int wordType) {
		return false;
	}

	private boolean shouldGenerateParts(int wordType) {
		return false;
	}

	private void concatenate(WordDelimiterGraphFilter.WordDelimiterConcatenation concatenation) {
		if (concatenation.isEmpty()) {
			concatenation.startPos = wordPos;
		}
	}

	private boolean has(int flag) {
		return ((flags) & flag) != 0;
	}

	final class WordDelimiterConcatenation {
		final StringBuilder buffer = new StringBuilder();

		int startPart;

		int endPart;

		int startPos;

		int type;

		int subwordCount;

		void append(char[] text, int offset, int length) {
			buffer.append(text, offset, length);
			(subwordCount)++;
		}

		void write() {
			char[] termPart = new char[buffer.length()];
			buffer.getChars(0, buffer.length(), termPart, 0);
			buffer(termPart, startPos, wordPos, startPart, endPart);
		}

		boolean isEmpty() {
			return (buffer.length()) == 0;
		}

		boolean isNotEmpty() {
			return (isEmpty()) == false;
		}

		void clear() {
			buffer.setLength(0);
			startPart = endPart = type = subwordCount = 0;
		}
	}

	public static String flagsToString(int flags) {
		StringBuilder b = new StringBuilder();
		if ((flags & (WordDelimiterGraphFilter.GENERATE_WORD_PARTS)) != 0) {
			b.append("GENERATE_WORD_PARTS");
		}
		if ((flags & (WordDelimiterGraphFilter.GENERATE_NUMBER_PARTS)) != 0) {
			if ((b.length()) > 0) {
				b.append(" | ");
			}
			b.append("GENERATE_NUMBER_PARTS");
		}
		if ((flags & (WordDelimiterGraphFilter.CATENATE_WORDS)) != 0) {
			if ((b.length()) > 0) {
				b.append(" | ");
			}
			b.append("CATENATE_WORDS");
		}
		if ((flags & (WordDelimiterGraphFilter.CATENATE_NUMBERS)) != 0) {
			if ((b.length()) > 0) {
				b.append(" | ");
			}
			b.append("CATENATE_NUMBERS");
		}
		if ((flags & (WordDelimiterGraphFilter.CATENATE_ALL)) != 0) {
			if ((b.length()) > 0) {
				b.append(" | ");
			}
			b.append("CATENATE_ALL");
		}
		if ((flags & (WordDelimiterGraphFilter.PRESERVE_ORIGINAL)) != 0) {
			if ((b.length()) > 0) {
				b.append(" | ");
			}
			b.append("PRESERVE_ORIGINAL");
		}
		if ((flags & (WordDelimiterGraphFilter.SPLIT_ON_CASE_CHANGE)) != 0) {
			if ((b.length()) > 0) {
				b.append(" | ");
			}
			b.append("SPLIT_ON_CASE_CHANGE");
		}
		if ((flags & (WordDelimiterGraphFilter.SPLIT_ON_NUMERICS)) != 0) {
			if ((b.length()) > 0) {
				b.append(" | ");
			}
			b.append("SPLIT_ON_NUMERICS");
		}
		if ((flags & (WordDelimiterGraphFilter.STEM_ENGLISH_POSSESSIVE)) != 0) {
			if ((b.length()) > 0) {
				b.append(" | ");
			}
			b.append("STEM_ENGLISH_POSSESSIVE");
		}
		return b.toString();
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("WordDelimiterGraphFilter(flags=");
		b.append(WordDelimiterGraphFilter.flagsToString(flags));
		b.append(')');
		return b.toString();
	}
}

