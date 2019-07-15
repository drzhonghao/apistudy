

import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.ko.GraphvizFormatter;
import org.apache.lucene.analysis.ko.POS;
import org.apache.lucene.analysis.ko.Token;
import org.apache.lucene.analysis.ko.dict.BinaryDictionary;
import org.apache.lucene.analysis.ko.dict.CharacterDefinition;
import org.apache.lucene.analysis.ko.dict.ConnectionCosts;
import org.apache.lucene.analysis.ko.dict.Dictionary;
import org.apache.lucene.analysis.ko.dict.TokenInfoDictionary;
import org.apache.lucene.analysis.ko.dict.TokenInfoFST;
import org.apache.lucene.analysis.ko.dict.UnknownDictionary;
import org.apache.lucene.analysis.ko.dict.UserDictionary;
import org.apache.lucene.analysis.ko.tokenattributes.PartOfSpeechAttribute;
import org.apache.lucene.analysis.ko.tokenattributes.ReadingAttribute;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionLengthAttribute;
import org.apache.lucene.analysis.util.RollingCharBuffer;
import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.AttributeFactory;
import org.apache.lucene.util.AttributeSource;
import org.apache.lucene.util.IntsRef;
import org.apache.lucene.util.RamUsageEstimator;
import org.apache.lucene.util.fst.FST;

import static org.apache.lucene.analysis.ko.POS.Tag.E;
import static org.apache.lucene.analysis.ko.POS.Tag.J;
import static org.apache.lucene.analysis.ko.POS.Tag.VCP;
import static org.apache.lucene.analysis.ko.POS.Tag.XSA;
import static org.apache.lucene.analysis.ko.POS.Tag.XSN;
import static org.apache.lucene.analysis.ko.POS.Tag.XSV;


public final class KoreanTokenizer extends Tokenizer {
	public enum Type {

		KNOWN,
		UNKNOWN,
		USER;}

	public enum DecompoundMode {

		NONE,
		DISCARD,
		MIXED;}

	public static final KoreanTokenizer.DecompoundMode DEFAULT_DECOMPOUND = KoreanTokenizer.DecompoundMode.DISCARD;

	private static final boolean VERBOSE = false;

	private static final int MAX_UNKNOWN_WORD_LENGTH = 1024;

	private static final int MAX_BACKTRACE_GAP = 1024;

	private final EnumMap<KoreanTokenizer.Type, Dictionary> dictionaryMap = new EnumMap<>(KoreanTokenizer.Type.class);

	private final TokenInfoFST fst;

	private final TokenInfoDictionary dictionary;

	private final UnknownDictionary unkDictionary;

	private final ConnectionCosts costs;

	private final UserDictionary userDictionary;

	private final CharacterDefinition characterDefinition;

	private final FST.Arc<Long> arc = new FST.Arc<>();

	private final FST.BytesReader fstReader;

	private final IntsRef wordIdRef = new IntsRef();

	private final FST.BytesReader userFSTReader;

	private final TokenInfoFST userFST;

	private final KoreanTokenizer.DecompoundMode mode;

	private final boolean outputUnknownUnigrams;

	private final RollingCharBuffer buffer = new RollingCharBuffer();

	private final KoreanTokenizer.WrappedPositionArray positions = new KoreanTokenizer.WrappedPositionArray();

	private boolean end;

	private int lastBackTracePos;

	private int pos;

	private final List<Token> pending = new ArrayList<>();

	private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

	private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);

	private final PositionIncrementAttribute posIncAtt = addAttribute(PositionIncrementAttribute.class);

	private final PositionLengthAttribute posLengthAtt = addAttribute(PositionLengthAttribute.class);

	private final PartOfSpeechAttribute posAtt = addAttribute(PartOfSpeechAttribute.class);

	private final ReadingAttribute readingAtt = addAttribute(ReadingAttribute.class);

	public KoreanTokenizer() {
		this(TokenStream.DEFAULT_TOKEN_ATTRIBUTE_FACTORY, null, KoreanTokenizer.DEFAULT_DECOMPOUND, false);
	}

	public KoreanTokenizer(AttributeFactory factory, UserDictionary userDictionary, KoreanTokenizer.DecompoundMode mode, boolean outputUnknownUnigrams) {
		super(factory);
		this.mode = mode;
		this.outputUnknownUnigrams = outputUnknownUnigrams;
		dictionary = TokenInfoDictionary.getInstance();
		fst = dictionary.getFST();
		unkDictionary = UnknownDictionary.getInstance();
		characterDefinition = unkDictionary.getCharacterDefinition();
		this.userDictionary = userDictionary;
		costs = ConnectionCosts.getInstance();
		fstReader = fst.getBytesReader();
		if (userDictionary != null) {
			userFST = userDictionary.getFST();
			userFSTReader = userFST.getBytesReader();
		}else {
			userFST = null;
			userFSTReader = null;
		}
		buffer.reset(this.input);
		resetState();
		dictionaryMap.put(KoreanTokenizer.Type.KNOWN, dictionary);
		dictionaryMap.put(KoreanTokenizer.Type.UNKNOWN, unkDictionary);
		dictionaryMap.put(KoreanTokenizer.Type.USER, userDictionary);
	}

	private GraphvizFormatter dotOut;

	public void setGraphvizFormatter(GraphvizFormatter dotOut) {
		this.dotOut = dotOut;
	}

	@Override
	public void close() throws IOException {
		super.close();
		buffer.reset(input);
	}

	@Override
	public void reset() throws IOException {
		super.reset();
		buffer.reset(input);
		resetState();
	}

	private void resetState() {
		positions.reset();
		pos = 0;
		end = false;
		lastBackTracePos = 0;
		pending.clear();
		positions.get(0).add(0, 0, (-1), (-1), (-1), (-1), KoreanTokenizer.Type.KNOWN);
	}

	@Override
	public void end() throws IOException {
		super.end();
		int finalOffset = correctOffset(pos);
		offsetAtt.setOffset(finalOffset, finalOffset);
	}

	static final class Position {
		int pos;

		int count;

		int[] costs = new int[8];

		int[] lastRightID = new int[8];

		int[] backPos = new int[8];

		int[] backWordPos = new int[8];

		int[] backIndex = new int[8];

		int[] backID = new int[8];

		KoreanTokenizer.Type[] backType = new KoreanTokenizer.Type[8];

		public void grow() {
			costs = ArrayUtil.grow(costs, (1 + (count)));
			lastRightID = ArrayUtil.grow(lastRightID, (1 + (count)));
			backPos = ArrayUtil.grow(backPos, (1 + (count)));
			backWordPos = ArrayUtil.grow(backWordPos, (1 + (count)));
			backIndex = ArrayUtil.grow(backIndex, (1 + (count)));
			backID = ArrayUtil.grow(backID, (1 + (count)));
			final KoreanTokenizer.Type[] newBackType = new KoreanTokenizer.Type[backID.length];
			System.arraycopy(backType, 0, newBackType, 0, backType.length);
			backType = newBackType;
		}

		public void add(int cost, int lastRightID, int backPos, int backRPos, int backIndex, int backID, KoreanTokenizer.Type backType) {
			if ((count) == (costs.length)) {
				grow();
			}
			this.costs[count] = cost;
			this.lastRightID[count] = lastRightID;
			this.backPos[count] = backPos;
			this.backWordPos[count] = backRPos;
			this.backIndex[count] = backIndex;
			this.backID[count] = backID;
			this.backType[count] = backType;
			(count)++;
		}

		public void reset() {
			count = 0;
		}
	}

	private int computeSpacePenalty(POS.Tag leftPOS, int numSpaces) {
		int spacePenalty = 0;
		if (numSpaces > 0) {
			switch (leftPOS) {
				case E :
				case J :
				case VCP :
				case XSA :
				case XSN :
				case XSV :
					spacePenalty = 3000;
					break;
				default :
					break;
			}
		}
		return spacePenalty;
	}

	private void add(Dictionary dict, KoreanTokenizer.Position fromPosData, int wordPos, int endPos, int wordID, KoreanTokenizer.Type type) throws IOException {
		final POS.Tag leftPOS = dict.getLeftPOS(wordID);
		final int wordCost = dict.getWordCost(wordID);
		final int leftID = dict.getLeftId(wordID);
		int leastCost = Integer.MAX_VALUE;
		int leastIDX = -1;
		assert (fromPosData.count) > 0;
		for (int idx = 0; idx < (fromPosData.count); idx++) {
			int numSpaces = wordPos - (fromPosData.pos);
			final int cost = ((fromPosData.costs[idx]) + (costs.get(fromPosData.lastRightID[idx], leftID))) + (computeSpacePenalty(leftPOS, numSpaces));
			if (KoreanTokenizer.VERBOSE) {
				System.out.println((((((((((((((((("      fromIDX=" + idx) + ": cost=") + cost) + " (prevCost=") + (fromPosData.costs[idx])) + " wordCost=") + wordCost) + " bgCost=") + (costs.get(fromPosData.lastRightID[idx], leftID))) + " spacePenalty=") + (computeSpacePenalty(leftPOS, numSpaces))) + ") leftID=") + leftID) + " leftPOS=") + (leftPOS.name())) + ")"));
			}
			if (cost < leastCost) {
				leastCost = cost;
				leastIDX = idx;
				if (KoreanTokenizer.VERBOSE) {
					System.out.println("        **");
				}
			}
		}
		leastCost += wordCost;
		if (KoreanTokenizer.VERBOSE) {
			System.out.println(((((((((((("      + cost=" + leastCost) + " wordID=") + wordID) + " leftID=") + leftID) + " leastIDX=") + leastIDX) + " toPos=") + endPos) + " toPos.idx=") + (positions.get(endPos).count)));
		}
		positions.get(endPos).add(leastCost, dict.getRightId(wordID), fromPosData.pos, wordPos, leastIDX, wordID, type);
	}

	@Override
	public boolean incrementToken() throws IOException {
		while ((pending.size()) == 0) {
			if (end) {
				return false;
			}
			parse();
		} 
		final Token token = pending.remove(((pending.size()) - 1));
		int length = token.getLength();
		clearAttributes();
		assert length > 0;
		termAtt.copyBuffer(token.getSurfaceForm(), token.getOffset(), length);
		offsetAtt.setOffset(correctOffset(token.getStartOffset()), correctOffset(token.getEndOffset()));
		posAtt.setToken(token);
		readingAtt.setToken(token);
		posIncAtt.setPositionIncrement(token.getPositionIncrement());
		posLengthAtt.setPositionLength(token.getPositionLength());
		if (KoreanTokenizer.VERBOSE) {
			System.out.println((((Thread.currentThread().getName()) + ":    incToken: return token=") + token));
		}
		return true;
	}

	static final class WrappedPositionArray {
		private KoreanTokenizer.Position[] positions = new KoreanTokenizer.Position[8];

		public WrappedPositionArray() {
			for (int i = 0; i < (positions.length); i++) {
				positions[i] = new KoreanTokenizer.Position();
			}
		}

		private int nextWrite;

		private int nextPos;

		private int count;

		public void reset() {
			(nextWrite)--;
			while ((count) > 0) {
				if ((nextWrite) == (-1)) {
					nextWrite = (positions.length) - 1;
				}
				positions[((nextWrite)--)].reset();
				(count)--;
			} 
			nextWrite = 0;
			nextPos = 0;
			count = 0;
		}

		public KoreanTokenizer.Position get(int pos) {
			while (pos >= (nextPos)) {
				if ((count) == (positions.length)) {
					KoreanTokenizer.Position[] newPositions = new KoreanTokenizer.Position[ArrayUtil.oversize((1 + (count)), RamUsageEstimator.NUM_BYTES_OBJECT_REF)];
					System.arraycopy(positions, nextWrite, newPositions, 0, ((positions.length) - (nextWrite)));
					System.arraycopy(positions, 0, newPositions, ((positions.length) - (nextWrite)), nextWrite);
					for (int i = positions.length; i < (newPositions.length); i++) {
						newPositions[i] = new KoreanTokenizer.Position();
					}
					nextWrite = positions.length;
					positions = newPositions;
				}
				if ((nextWrite) == (positions.length)) {
					nextWrite = 0;
				}
				assert (positions[nextWrite].count) == 0;
				positions[((nextWrite)++)].pos = (nextPos)++;
				(count)++;
			} 
			assert inBounds(pos);
			final int index = getIndex(pos);
			assert (positions[index].pos) == pos;
			return positions[index];
		}

		public int getNextPos() {
			return nextPos;
		}

		private boolean inBounds(int pos) {
			return (pos < (nextPos)) && (pos >= ((nextPos) - (count)));
		}

		private int getIndex(int pos) {
			int index = (nextWrite) - ((nextPos) - pos);
			if (index < 0) {
				index += positions.length;
			}
			return index;
		}

		public void freeBefore(int pos) {
			final int toFree = (count) - ((nextPos) - pos);
			assert toFree >= 0;
			assert toFree <= (count);
			int index = (nextWrite) - (count);
			if (index < 0) {
				index += positions.length;
			}
			for (int i = 0; i < toFree; i++) {
				if (index == (positions.length)) {
					index = 0;
				}
				positions[index].reset();
				index++;
			}
			count -= toFree;
		}
	}

	private void parse() throws IOException {
		if (KoreanTokenizer.VERBOSE) {
			System.out.println("\nPARSE");
		}
		int unknownWordEndIndex = -1;
		while (true) {
			if ((buffer.get(pos)) == (-1)) {
				break;
			}
			final KoreanTokenizer.Position posData = positions.get(pos);
			final boolean isFrontier = (positions.getNextPos()) == ((pos) + 1);
			if ((posData.count) == 0) {
				if (KoreanTokenizer.VERBOSE) {
					System.out.println(("    no arcs in; skip pos=" + (pos)));
				}
				(pos)++;
				continue;
			}
			if ((((pos) > (lastBackTracePos)) && ((posData.count) == 1)) && isFrontier) {
				backtrace(posData, 0);
				posData.costs[0] = 0;
				if ((pending.size()) > 0) {
					return;
				}else {
				}
			}
			if (((pos) - (lastBackTracePos)) >= (KoreanTokenizer.MAX_BACKTRACE_GAP)) {
				int leastIDX = -1;
				int leastCost = Integer.MAX_VALUE;
				KoreanTokenizer.Position leastPosData = null;
				for (int pos2 = pos; pos2 < (positions.getNextPos()); pos2++) {
					final KoreanTokenizer.Position posData2 = positions.get(pos2);
					for (int idx = 0; idx < (posData2.count); idx++) {
						final int cost = posData2.costs[idx];
						if (cost < leastCost) {
							leastCost = cost;
							leastIDX = idx;
							leastPosData = posData2;
						}
					}
				}
				assert leastIDX != (-1);
				for (int pos2 = pos; pos2 < (positions.getNextPos()); pos2++) {
					final KoreanTokenizer.Position posData2 = positions.get(pos2);
					if (posData2 != leastPosData) {
						posData2.reset();
					}else {
						if (leastIDX != 0) {
							posData2.costs[0] = posData2.costs[leastIDX];
							posData2.lastRightID[0] = posData2.lastRightID[leastIDX];
							posData2.backPos[0] = posData2.backPos[leastIDX];
							posData2.backWordPos[0] = posData2.backWordPos[leastIDX];
							posData2.backIndex[0] = posData2.backIndex[leastIDX];
							posData2.backID[0] = posData2.backID[leastIDX];
							posData2.backType[0] = posData2.backType[leastIDX];
						}
						posData2.count = 1;
					}
				}
				backtrace(leastPosData, 0);
				Arrays.fill(leastPosData.costs, 0, leastPosData.count, 0);
				if ((pos) != (leastPosData.pos)) {
					assert (pos) < (leastPosData.pos);
					pos = leastPosData.pos;
				}
				if ((pending.size()) > 0) {
					return;
				}else {
				}
			}
			if (KoreanTokenizer.VERBOSE) {
				System.out.println(((((("\n  extend @ pos=" + (pos)) + " char=") + ((char) (buffer.get(pos)))) + " hex=") + (Integer.toHexString(buffer.get(pos)))));
			}
			if (KoreanTokenizer.VERBOSE) {
				System.out.println((("    " + (posData.count)) + " arcs in"));
			}
			if ((Character.getType(buffer.get(pos))) == (Character.SPACE_SEPARATOR)) {
				int nextChar = buffer.get((++(pos)));
				while ((nextChar != (-1)) && ((Character.getType(nextChar)) == (Character.SPACE_SEPARATOR))) {
					(pos)++;
					nextChar = buffer.get(pos);
				} 
			}
			if ((buffer.get(pos)) == (-1)) {
				pos = posData.pos;
			}
			boolean anyMatches = false;
			if ((userFST) != null) {
				userFST.getFirstArc(arc);
				int output = 0;
				for (int posAhead = pos; ; posAhead++) {
					final int ch = buffer.get(posAhead);
					if (ch == (-1)) {
						break;
					}
					if ((userFST.findTargetArc(ch, arc, arc, (posAhead == (pos)), userFSTReader)) == null) {
						break;
					}
					output += arc.output.intValue();
					if (arc.isFinal()) {
						if (KoreanTokenizer.VERBOSE) {
							System.out.println(((("    USER word " + (new String(buffer.get(pos, ((posAhead - (pos)) + 1))))) + " toPos=") + (posAhead + 1)));
						}
						add(userDictionary, posData, pos, (posAhead + 1), (output + (arc.nextFinalOutput.intValue())), KoreanTokenizer.Type.USER);
						anyMatches = true;
					}
				}
			}
			if (!anyMatches) {
				fst.getFirstArc(arc);
				int output = 0;
				for (int posAhead = pos; ; posAhead++) {
					final int ch = buffer.get(posAhead);
					if (ch == (-1)) {
						break;
					}
					if ((fst.findTargetArc(ch, arc, arc, (posAhead == (pos)), fstReader)) == null) {
						break;
					}
					output += arc.output.intValue();
					if (arc.isFinal()) {
						dictionary.lookupWordIds((output + (arc.nextFinalOutput.intValue())), wordIdRef);
						if (KoreanTokenizer.VERBOSE) {
							System.out.println((((((("    KNOWN word " + (new String(buffer.get(pos, ((posAhead - (pos)) + 1))))) + " toPos=") + (posAhead + 1)) + " ") + (wordIdRef.length)) + " wordIDs"));
						}
						for (int ofs = 0; ofs < (wordIdRef.length); ofs++) {
							add(dictionary, posData, pos, (posAhead + 1), wordIdRef.ints[((wordIdRef.offset) + ofs)], KoreanTokenizer.Type.KNOWN);
							anyMatches = true;
						}
					}
				}
			}
			if (unknownWordEndIndex > (posData.pos)) {
				(pos)++;
				continue;
			}
			final char firstCharacter = ((char) (buffer.get(pos)));
			if ((!anyMatches) || (characterDefinition.isInvoke(firstCharacter))) {
				final int characterId = characterDefinition.getCharacterClass(firstCharacter);
				final boolean isPunct = KoreanTokenizer.isPunctuation(firstCharacter);
				int unknownWordLength;
				if (!(characterDefinition.isGroup(firstCharacter))) {
					unknownWordLength = 1;
				}else {
					unknownWordLength = 1;
					for (int posAhead = (pos) + 1; unknownWordLength < (KoreanTokenizer.MAX_UNKNOWN_WORD_LENGTH); posAhead++) {
						final int ch = buffer.get(posAhead);
						if (ch == (-1)) {
							break;
						}
						if ((characterId == (characterDefinition.getCharacterClass(((char) (ch))))) && ((KoreanTokenizer.isPunctuation(((char) (ch)))) == isPunct)) {
							unknownWordLength++;
						}else {
							break;
						}
					}
				}
				unkDictionary.lookupWordIds(characterId, wordIdRef);
				if (KoreanTokenizer.VERBOSE) {
					System.out.println((((("    UNKNOWN word len=" + unknownWordLength) + " ") + (wordIdRef.length)) + " wordIDs"));
				}
				for (int ofs = 0; ofs < (wordIdRef.length); ofs++) {
					add(unkDictionary, posData, pos, ((pos) + unknownWordLength), wordIdRef.ints[((wordIdRef.offset) + ofs)], KoreanTokenizer.Type.UNKNOWN);
				}
			}
			(pos)++;
		} 
		end = true;
		if ((pos) > 0) {
			final KoreanTokenizer.Position endPosData = positions.get(pos);
			int leastCost = Integer.MAX_VALUE;
			int leastIDX = -1;
			if (KoreanTokenizer.VERBOSE) {
				System.out.println((("  end: " + (endPosData.count)) + " nodes"));
			}
			for (int idx = 0; idx < (endPosData.count); idx++) {
				final int cost = (endPosData.costs[idx]) + (costs.get(endPosData.lastRightID[idx], 0));
				if (cost < leastCost) {
					leastCost = cost;
					leastIDX = idx;
				}
			}
			backtrace(endPosData, leastIDX);
		}else {
		}
	}

	private void backtrace(final KoreanTokenizer.Position endPosData, final int fromIDX) {
		final int endPos = endPosData.pos;
		if (KoreanTokenizer.VERBOSE) {
			System.out.println(((((((((("\n  backtrace: endPos=" + endPos) + " pos=") + (pos)) + "; ") + ((pos) - (lastBackTracePos))) + " characters; last=") + (lastBackTracePos)) + " cost=") + (endPosData.costs[fromIDX])));
		}
		final char[] fragment = buffer.get(lastBackTracePos, (endPos - (lastBackTracePos)));
		if ((dotOut) != null) {
		}
		int pos = endPos;
		int bestIDX = fromIDX;
		while (pos > (lastBackTracePos)) {
			final KoreanTokenizer.Position posData = positions.get(pos);
			assert bestIDX < (posData.count);
			int backPos = posData.backPos[bestIDX];
			int backWordPos = posData.backWordPos[bestIDX];
			assert backPos >= (lastBackTracePos) : (("backPos=" + backPos) + " vs lastBackTracePos=") + (lastBackTracePos);
			int length = pos - backWordPos;
			KoreanTokenizer.Type backType = posData.backType[bestIDX];
			int backID = posData.backID[bestIDX];
			int nextBestIDX = posData.backIndex[bestIDX];
			final int fragmentOffset = backWordPos - (lastBackTracePos);
			assert fragmentOffset >= 0;
			final Dictionary dict = getDict(backType);
			if ((outputUnknownUnigrams) && (backType == (KoreanTokenizer.Type.UNKNOWN))) {
				for (int i = length - 1; i >= 0; i--) {
					int charLen = 1;
					if ((i > 0) && (Character.isLowSurrogate(fragment[(fragmentOffset + i)]))) {
						i--;
						charLen = 2;
					}
				}
			}else {
			}
			pos = backPos;
			bestIDX = nextBestIDX;
		} 
		lastBackTracePos = endPos;
		if (KoreanTokenizer.VERBOSE) {
			System.out.println(("  freeBefore pos=" + endPos));
		}
		buffer.freeBefore(endPos);
		positions.freeBefore(endPos);
	}

	Dictionary getDict(KoreanTokenizer.Type type) {
		return dictionaryMap.get(type);
	}

	private boolean shouldFilterToken(Token token) {
		return KoreanTokenizer.isPunctuation(token.getSurfaceForm()[token.getOffset()]);
	}

	private static boolean isPunctuation(char ch) {
		switch (Character.getType(ch)) {
			case Character.SPACE_SEPARATOR :
			case Character.LINE_SEPARATOR :
			case Character.PARAGRAPH_SEPARATOR :
			case Character.CONTROL :
			case Character.FORMAT :
			case Character.DASH_PUNCTUATION :
			case Character.START_PUNCTUATION :
			case Character.END_PUNCTUATION :
			case Character.CONNECTOR_PUNCTUATION :
			case Character.OTHER_PUNCTUATION :
			case Character.MATH_SYMBOL :
			case Character.CURRENCY_SYMBOL :
			case Character.MODIFIER_SYMBOL :
			case Character.OTHER_SYMBOL :
			case Character.INITIAL_QUOTE_PUNCTUATION :
			case Character.FINAL_QUOTE_PUNCTUATION :
				return true;
			default :
				return false;
		}
	}
}

