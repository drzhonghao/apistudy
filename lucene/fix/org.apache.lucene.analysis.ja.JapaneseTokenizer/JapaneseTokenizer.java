

import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.ja.GraphvizFormatter;
import org.apache.lucene.analysis.ja.Token;
import org.apache.lucene.analysis.ja.dict.BinaryDictionary;
import org.apache.lucene.analysis.ja.dict.CharacterDefinition;
import org.apache.lucene.analysis.ja.dict.ConnectionCosts;
import org.apache.lucene.analysis.ja.dict.Dictionary;
import org.apache.lucene.analysis.ja.dict.TokenInfoDictionary;
import org.apache.lucene.analysis.ja.dict.TokenInfoFST;
import org.apache.lucene.analysis.ja.dict.UnknownDictionary;
import org.apache.lucene.analysis.ja.dict.UserDictionary;
import org.apache.lucene.analysis.ja.tokenattributes.BaseFormAttribute;
import org.apache.lucene.analysis.ja.tokenattributes.InflectionAttribute;
import org.apache.lucene.analysis.ja.tokenattributes.PartOfSpeechAttribute;
import org.apache.lucene.analysis.ja.tokenattributes.ReadingAttribute;
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


public final class JapaneseTokenizer extends Tokenizer {
	public static enum Mode {

		NORMAL,
		SEARCH,
		EXTENDED;}

	public static final JapaneseTokenizer.Mode DEFAULT_MODE = JapaneseTokenizer.Mode.SEARCH;

	public enum Type {

		KNOWN,
		UNKNOWN,
		USER;}

	private static final boolean VERBOSE = false;

	private static final int SEARCH_MODE_KANJI_LENGTH = 2;

	private static final int SEARCH_MODE_OTHER_LENGTH = 7;

	private static final int SEARCH_MODE_KANJI_PENALTY = 3000;

	private static final int SEARCH_MODE_OTHER_PENALTY = 1700;

	private static final int MAX_UNKNOWN_WORD_LENGTH = 1024;

	private static final int MAX_BACKTRACE_GAP = 1024;

	private final EnumMap<JapaneseTokenizer.Type, Dictionary> dictionaryMap = new EnumMap<>(JapaneseTokenizer.Type.class);

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

	private final RollingCharBuffer buffer = new RollingCharBuffer();

	private final JapaneseTokenizer.WrappedPositionArray positions = new JapaneseTokenizer.WrappedPositionArray();

	private final boolean discardPunctuation;

	private final boolean searchMode;

	private final boolean extendedMode;

	private final boolean outputCompounds;

	private boolean outputNBest = false;

	private int nBestCost = 0;

	private boolean end;

	private int lastBackTracePos;

	private int lastTokenPos;

	private int pos;

	private final List<Token> pending = new ArrayList<>();

	private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

	private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);

	private final PositionIncrementAttribute posIncAtt = addAttribute(PositionIncrementAttribute.class);

	private final PositionLengthAttribute posLengthAtt = addAttribute(PositionLengthAttribute.class);

	private final BaseFormAttribute basicFormAtt = addAttribute(BaseFormAttribute.class);

	private final PartOfSpeechAttribute posAtt = addAttribute(PartOfSpeechAttribute.class);

	private final ReadingAttribute readingAtt = addAttribute(ReadingAttribute.class);

	private final InflectionAttribute inflectionAtt = addAttribute(InflectionAttribute.class);

	public JapaneseTokenizer(UserDictionary userDictionary, boolean discardPunctuation, JapaneseTokenizer.Mode mode) {
		this(TokenStream.DEFAULT_TOKEN_ATTRIBUTE_FACTORY, userDictionary, discardPunctuation, mode);
	}

	public JapaneseTokenizer(AttributeFactory factory, UserDictionary userDictionary, boolean discardPunctuation, JapaneseTokenizer.Mode mode) {
		super(factory);
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
		this.discardPunctuation = discardPunctuation;
		switch (mode) {
			case SEARCH :
				searchMode = true;
				extendedMode = false;
				outputCompounds = true;
				break;
			case EXTENDED :
				searchMode = true;
				extendedMode = true;
				outputCompounds = false;
				break;
			default :
				searchMode = false;
				extendedMode = false;
				outputCompounds = false;
				break;
		}
		buffer.reset(this.input);
		resetState();
		dictionaryMap.put(JapaneseTokenizer.Type.KNOWN, dictionary);
		dictionaryMap.put(JapaneseTokenizer.Type.UNKNOWN, unkDictionary);
		dictionaryMap.put(JapaneseTokenizer.Type.USER, userDictionary);
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
		lastTokenPos = -1;
		pending.clear();
		positions.get(0).add(0, 0, (-1), (-1), (-1), JapaneseTokenizer.Type.KNOWN);
	}

	@Override
	public void end() throws IOException {
		super.end();
		int finalOffset = correctOffset(pos);
		offsetAtt.setOffset(finalOffset, finalOffset);
	}

	private int computeSecondBestThreshold(int pos, int length) throws IOException {
		return computePenalty(pos, length);
	}

	private int computePenalty(int pos, int length) throws IOException {
		if (length > (JapaneseTokenizer.SEARCH_MODE_KANJI_LENGTH)) {
			boolean allKanji = true;
			final int endPos = pos + length;
			for (int pos2 = pos; pos2 < endPos; pos2++) {
				if (!(characterDefinition.isKanji(((char) (buffer.get(pos2)))))) {
					allKanji = false;
					break;
				}
			}
			if (allKanji) {
				return (length - (JapaneseTokenizer.SEARCH_MODE_KANJI_LENGTH)) * (JapaneseTokenizer.SEARCH_MODE_KANJI_PENALTY);
			}else
				if (length > (JapaneseTokenizer.SEARCH_MODE_OTHER_LENGTH)) {
					return (length - (JapaneseTokenizer.SEARCH_MODE_OTHER_LENGTH)) * (JapaneseTokenizer.SEARCH_MODE_OTHER_PENALTY);
				}

		}
		return 0;
	}

	static final class Position {
		int pos;

		int count;

		int[] costs = new int[8];

		int[] lastRightID = new int[8];

		int[] backPos = new int[8];

		int[] backIndex = new int[8];

		int[] backID = new int[8];

		JapaneseTokenizer.Type[] backType = new JapaneseTokenizer.Type[8];

		int forwardCount;

		int[] forwardPos = new int[8];

		int[] forwardID = new int[8];

		int[] forwardIndex = new int[8];

		JapaneseTokenizer.Type[] forwardType = new JapaneseTokenizer.Type[8];

		public void grow() {
			costs = ArrayUtil.grow(costs, (1 + (count)));
			lastRightID = ArrayUtil.grow(lastRightID, (1 + (count)));
			backPos = ArrayUtil.grow(backPos, (1 + (count)));
			backIndex = ArrayUtil.grow(backIndex, (1 + (count)));
			backID = ArrayUtil.grow(backID, (1 + (count)));
			final JapaneseTokenizer.Type[] newBackType = new JapaneseTokenizer.Type[backID.length];
			System.arraycopy(backType, 0, newBackType, 0, backType.length);
			backType = newBackType;
		}

		public void growForward() {
			forwardPos = ArrayUtil.grow(forwardPos, (1 + (forwardCount)));
			forwardID = ArrayUtil.grow(forwardID, (1 + (forwardCount)));
			forwardIndex = ArrayUtil.grow(forwardIndex, (1 + (forwardCount)));
			final JapaneseTokenizer.Type[] newForwardType = new JapaneseTokenizer.Type[forwardPos.length];
			System.arraycopy(forwardType, 0, newForwardType, 0, forwardType.length);
			forwardType = newForwardType;
		}

		public void add(int cost, int lastRightID, int backPos, int backIndex, int backID, JapaneseTokenizer.Type backType) {
			if ((count) == (costs.length)) {
				grow();
			}
			this.costs[count] = cost;
			this.lastRightID[count] = lastRightID;
			this.backPos[count] = backPos;
			this.backIndex[count] = backIndex;
			this.backID[count] = backID;
			this.backType[count] = backType;
			(count)++;
		}

		public void addForward(int forwardPos, int forwardIndex, int forwardID, JapaneseTokenizer.Type forwardType) {
			if ((forwardCount) == (this.forwardID.length)) {
				growForward();
			}
			this.forwardPos[forwardCount] = forwardPos;
			this.forwardIndex[forwardCount] = forwardIndex;
			this.forwardID[forwardCount] = forwardID;
			this.forwardType[forwardCount] = forwardType;
			(forwardCount)++;
		}

		public void reset() {
			count = 0;
			assert (forwardCount) == 0 : (("pos=" + (pos)) + " forwardCount=") + (forwardCount);
		}
	}

	private void add(Dictionary dict, JapaneseTokenizer.Position fromPosData, int endPos, int wordID, JapaneseTokenizer.Type type, boolean addPenalty) throws IOException {
		final int wordCost = dict.getWordCost(wordID);
		final int leftID = dict.getLeftId(wordID);
		int leastCost = Integer.MAX_VALUE;
		int leastIDX = -1;
		assert (fromPosData.count) > 0;
		for (int idx = 0; idx < (fromPosData.count); idx++) {
			final int cost = (fromPosData.costs[idx]) + (costs.get(fromPosData.lastRightID[idx], leftID));
			if (JapaneseTokenizer.VERBOSE) {
				System.out.println((((((((((((("      fromIDX=" + idx) + ": cost=") + cost) + " (prevCost=") + (fromPosData.costs[idx])) + " wordCost=") + wordCost) + " bgCost=") + (costs.get(fromPosData.lastRightID[idx], leftID))) + " leftID=") + leftID) + ")"));
			}
			if (cost < leastCost) {
				leastCost = cost;
				leastIDX = idx;
				if (JapaneseTokenizer.VERBOSE) {
					System.out.println("        **");
				}
			}
		}
		leastCost += wordCost;
		if (JapaneseTokenizer.VERBOSE) {
			System.out.println(((((((((((("      + cost=" + leastCost) + " wordID=") + wordID) + " leftID=") + leftID) + " leastIDX=") + leastIDX) + " toPos=") + endPos) + " toPos.idx=") + (positions.get(endPos).count)));
		}
		if ((addPenalty || ((!(outputCompounds)) && (searchMode))) && (type != (JapaneseTokenizer.Type.USER))) {
			final int penalty = computePenalty(fromPosData.pos, (endPos - (fromPosData.pos)));
			if (JapaneseTokenizer.VERBOSE) {
				if (penalty > 0) {
					System.out.println(((("        + penalty=" + penalty) + " cost=") + (leastCost + penalty)));
				}
			}
			leastCost += penalty;
		}
		assert leftID == (dict.getRightId(wordID));
		positions.get(endPos).add(leastCost, leftID, fromPosData.pos, leastIDX, wordID, type);
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
		int position = token.getPosition();
		int length = token.getLength();
		clearAttributes();
		assert length > 0;
		termAtt.copyBuffer(token.getSurfaceForm(), token.getOffset(), length);
		offsetAtt.setOffset(correctOffset(position), correctOffset((position + length)));
		basicFormAtt.setToken(token);
		posAtt.setToken(token);
		readingAtt.setToken(token);
		inflectionAtt.setToken(token);
		if ((token.getPosition()) == (lastTokenPos)) {
			posIncAtt.setPositionIncrement(0);
			posLengthAtt.setPositionLength(token.getPositionLength());
		}else
			if (outputNBest) {
				assert (token.getPosition()) > (lastTokenPos);
				posIncAtt.setPositionIncrement(1);
				posLengthAtt.setPositionLength(token.getPositionLength());
			}else {
				assert (token.getPosition()) > (lastTokenPos);
				posIncAtt.setPositionIncrement(1);
				posLengthAtt.setPositionLength(1);
			}

		if (JapaneseTokenizer.VERBOSE) {
			System.out.println((((Thread.currentThread().getName()) + ":    incToken: return token=") + token));
		}
		lastTokenPos = token.getPosition();
		return true;
	}

	static final class WrappedPositionArray {
		private JapaneseTokenizer.Position[] positions = new JapaneseTokenizer.Position[8];

		public WrappedPositionArray() {
			for (int i = 0; i < (positions.length); i++) {
				positions[i] = new JapaneseTokenizer.Position();
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

		public JapaneseTokenizer.Position get(int pos) {
			while (pos >= (nextPos)) {
				if ((count) == (positions.length)) {
					JapaneseTokenizer.Position[] newPositions = new JapaneseTokenizer.Position[ArrayUtil.oversize((1 + (count)), RamUsageEstimator.NUM_BYTES_OBJECT_REF)];
					System.arraycopy(positions, nextWrite, newPositions, 0, ((positions.length) - (nextWrite)));
					System.arraycopy(positions, 0, newPositions, ((positions.length) - (nextWrite)), nextWrite);
					for (int i = positions.length; i < (newPositions.length); i++) {
						newPositions[i] = new JapaneseTokenizer.Position();
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
		if (JapaneseTokenizer.VERBOSE) {
			System.out.println("\nPARSE");
		}
		int unknownWordEndIndex = -1;
		while (true) {
			if ((buffer.get(pos)) == (-1)) {
				break;
			}
			final JapaneseTokenizer.Position posData = positions.get(pos);
			final boolean isFrontier = (positions.getNextPos()) == ((pos) + 1);
			if ((posData.count) == 0) {
				if (JapaneseTokenizer.VERBOSE) {
					System.out.println(("    no arcs in; skip pos=" + (pos)));
				}
				(pos)++;
				continue;
			}
			if ((((pos) > (lastBackTracePos)) && ((posData.count) == 1)) && isFrontier) {
				if (outputNBest) {
					backtraceNBest(posData, false);
				}
				backtrace(posData, 0);
				if (outputNBest) {
					fixupPendingList();
				}
				posData.costs[0] = 0;
				if ((pending.size()) != 0) {
					return;
				}else {
				}
			}
			if (((pos) - (lastBackTracePos)) >= (JapaneseTokenizer.MAX_BACKTRACE_GAP)) {
				int leastIDX = -1;
				int leastCost = Integer.MAX_VALUE;
				JapaneseTokenizer.Position leastPosData = null;
				for (int pos2 = pos; pos2 < (positions.getNextPos()); pos2++) {
					final JapaneseTokenizer.Position posData2 = positions.get(pos2);
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
				if (outputNBest) {
					backtraceNBest(leastPosData, false);
				}
				for (int pos2 = pos; pos2 < (positions.getNextPos()); pos2++) {
					final JapaneseTokenizer.Position posData2 = positions.get(pos2);
					if (posData2 != leastPosData) {
						posData2.reset();
					}else {
						if (leastIDX != 0) {
							posData2.costs[0] = posData2.costs[leastIDX];
							posData2.lastRightID[0] = posData2.lastRightID[leastIDX];
							posData2.backPos[0] = posData2.backPos[leastIDX];
							posData2.backIndex[0] = posData2.backIndex[leastIDX];
							posData2.backID[0] = posData2.backID[leastIDX];
							posData2.backType[0] = posData2.backType[leastIDX];
						}
						posData2.count = 1;
					}
				}
				backtrace(leastPosData, 0);
				if (outputNBest) {
					fixupPendingList();
				}
				Arrays.fill(leastPosData.costs, 0, leastPosData.count, 0);
				if ((pos) != (leastPosData.pos)) {
					assert (pos) < (leastPosData.pos);
					pos = leastPosData.pos;
				}
				if ((pending.size()) != 0) {
					return;
				}else {
					continue;
				}
			}
			if (JapaneseTokenizer.VERBOSE) {
				System.out.println(((((("\n  extend @ pos=" + (pos)) + " char=") + ((char) (buffer.get(pos)))) + " hex=") + (Integer.toHexString(buffer.get(pos)))));
			}
			if (JapaneseTokenizer.VERBOSE) {
				System.out.println((("    " + (posData.count)) + " arcs in"));
			}
			boolean anyMatches = false;
			if ((userFST) != null) {
				userFST.getFirstArc(arc);
				int output = 0;
				for (int posAhead = posData.pos; ; posAhead++) {
					final int ch = buffer.get(posAhead);
					if (ch == (-1)) {
						break;
					}
					if ((userFST.findTargetArc(ch, arc, arc, (posAhead == (posData.pos)), userFSTReader)) == null) {
						break;
					}
					output += arc.output.intValue();
					if (arc.isFinal()) {
						if (JapaneseTokenizer.VERBOSE) {
							System.out.println(((("    USER word " + (new String(buffer.get(pos, ((posAhead - (pos)) + 1))))) + " toPos=") + (posAhead + 1)));
						}
						add(userDictionary, posData, (posAhead + 1), (output + (arc.nextFinalOutput.intValue())), JapaneseTokenizer.Type.USER, false);
						anyMatches = true;
					}
				}
			}
			if (!anyMatches) {
				fst.getFirstArc(arc);
				int output = 0;
				for (int posAhead = posData.pos; ; posAhead++) {
					final int ch = buffer.get(posAhead);
					if (ch == (-1)) {
						break;
					}
					if ((fst.findTargetArc(ch, arc, arc, (posAhead == (posData.pos)), fstReader)) == null) {
						break;
					}
					output += arc.output.intValue();
					if (arc.isFinal()) {
						dictionary.lookupWordIds((output + (arc.nextFinalOutput.intValue())), wordIdRef);
						if (JapaneseTokenizer.VERBOSE) {
							System.out.println((((((("    KNOWN word " + (new String(buffer.get(pos, ((posAhead - (pos)) + 1))))) + " toPos=") + (posAhead + 1)) + " ") + (wordIdRef.length)) + " wordIDs"));
						}
						for (int ofs = 0; ofs < (wordIdRef.length); ofs++) {
							add(dictionary, posData, (posAhead + 1), wordIdRef.ints[((wordIdRef.offset) + ofs)], JapaneseTokenizer.Type.KNOWN, false);
							anyMatches = true;
						}
					}
				}
			}
			if ((!(searchMode)) && (unknownWordEndIndex > (posData.pos))) {
				(pos)++;
				continue;
			}
			final char firstCharacter = ((char) (buffer.get(pos)));
			if ((!anyMatches) || (characterDefinition.isInvoke(firstCharacter))) {
				final int characterId = characterDefinition.getCharacterClass(firstCharacter);
				final boolean isPunct = JapaneseTokenizer.isPunctuation(firstCharacter);
				int unknownWordLength;
				if (!(characterDefinition.isGroup(firstCharacter))) {
					unknownWordLength = 1;
				}else {
					unknownWordLength = 1;
					for (int posAhead = (pos) + 1; unknownWordLength < (JapaneseTokenizer.MAX_UNKNOWN_WORD_LENGTH); posAhead++) {
						final int ch = buffer.get(posAhead);
						if (ch == (-1)) {
							break;
						}
						if ((characterId == (characterDefinition.getCharacterClass(((char) (ch))))) && ((JapaneseTokenizer.isPunctuation(((char) (ch)))) == isPunct)) {
							unknownWordLength++;
						}else {
							break;
						}
					}
				}
				unkDictionary.lookupWordIds(characterId, wordIdRef);
				if (JapaneseTokenizer.VERBOSE) {
					System.out.println((((("    UNKNOWN word len=" + unknownWordLength) + " ") + (wordIdRef.length)) + " wordIDs"));
				}
				for (int ofs = 0; ofs < (wordIdRef.length); ofs++) {
					add(unkDictionary, posData, ((posData.pos) + unknownWordLength), wordIdRef.ints[((wordIdRef.offset) + ofs)], JapaneseTokenizer.Type.UNKNOWN, false);
				}
				unknownWordEndIndex = (posData.pos) + unknownWordLength;
			}
			(pos)++;
		} 
		end = true;
		if ((pos) > 0) {
			final JapaneseTokenizer.Position endPosData = positions.get(pos);
			int leastCost = Integer.MAX_VALUE;
			int leastIDX = -1;
			if (JapaneseTokenizer.VERBOSE) {
				System.out.println((("  end: " + (endPosData.count)) + " nodes"));
			}
			for (int idx = 0; idx < (endPosData.count); idx++) {
				final int cost = (endPosData.costs[idx]) + (costs.get(endPosData.lastRightID[idx], 0));
				if (cost < leastCost) {
					leastCost = cost;
					leastIDX = idx;
				}
			}
			if (outputNBest) {
				backtraceNBest(endPosData, true);
			}
			backtrace(endPosData, leastIDX);
			if (outputNBest) {
				fixupPendingList();
			}
		}else {
		}
	}

	private void pruneAndRescore(int startPos, int endPos, int bestStartIDX) throws IOException {
		if (JapaneseTokenizer.VERBOSE) {
			System.out.println(((((("  pruneAndRescore startPos=" + startPos) + " endPos=") + endPos) + " bestStartIDX=") + bestStartIDX));
		}
		for (int pos = endPos; pos > startPos; pos--) {
			final JapaneseTokenizer.Position posData = positions.get(pos);
			if (JapaneseTokenizer.VERBOSE) {
				System.out.println(("    back pos=" + pos));
			}
			for (int arcIDX = 0; arcIDX < (posData.count); arcIDX++) {
				final int backPos = posData.backPos[arcIDX];
				if (backPos >= startPos) {
					positions.get(backPos).addForward(pos, arcIDX, posData.backID[arcIDX], posData.backType[arcIDX]);
				}else {
					if (JapaneseTokenizer.VERBOSE) {
						System.out.println("      prune");
					}
				}
			}
			if (pos != startPos) {
				posData.count = 0;
			}
		}
		for (int pos = startPos; pos < endPos; pos++) {
			final JapaneseTokenizer.Position posData = positions.get(pos);
			if (JapaneseTokenizer.VERBOSE) {
				System.out.println(((("    forward pos=" + pos) + " count=") + (posData.forwardCount)));
			}
			if ((posData.count) == 0) {
				if (JapaneseTokenizer.VERBOSE) {
					System.out.println("      skip");
				}
				posData.forwardCount = 0;
				continue;
			}
			if (pos == startPos) {
				final int rightID;
				if (startPos == 0) {
					rightID = 0;
				}else {
					rightID = getDict(posData.backType[bestStartIDX]).getRightId(posData.backID[bestStartIDX]);
				}
				final int pathCost = posData.costs[bestStartIDX];
				for (int forwardArcIDX = 0; forwardArcIDX < (posData.forwardCount); forwardArcIDX++) {
					final JapaneseTokenizer.Type forwardType = posData.forwardType[forwardArcIDX];
					final Dictionary dict2 = getDict(forwardType);
					final int wordID = posData.forwardID[forwardArcIDX];
					final int toPos = posData.forwardPos[forwardArcIDX];
					final int newCost = ((pathCost + (dict2.getWordCost(wordID))) + (costs.get(rightID, dict2.getLeftId(wordID)))) + (computePenalty(pos, (toPos - pos)));
					if (JapaneseTokenizer.VERBOSE) {
						System.out.println(((((((((((("      + " + forwardType) + " word ") + (new String(buffer.get(pos, (toPos - pos))))) + " toPos=") + toPos) + " cost=") + newCost) + " penalty=") + (computePenalty(pos, (toPos - pos)))) + " toPos.idx=") + (positions.get(toPos).count)));
					}
					positions.get(toPos).add(newCost, dict2.getRightId(wordID), pos, bestStartIDX, wordID, forwardType);
				}
			}else {
				for (int forwardArcIDX = 0; forwardArcIDX < (posData.forwardCount); forwardArcIDX++) {
					final JapaneseTokenizer.Type forwardType = posData.forwardType[forwardArcIDX];
					final int toPos = posData.forwardPos[forwardArcIDX];
					if (JapaneseTokenizer.VERBOSE) {
						System.out.println(((((("      + " + forwardType) + " word ") + (new String(buffer.get(pos, (toPos - pos))))) + " toPos=") + toPos));
					}
					add(getDict(forwardType), posData, toPos, posData.forwardID[forwardArcIDX], forwardType, true);
				}
			}
			posData.forwardCount = 0;
		}
	}

	private static final class Lattice {
		char[] fragment;

		EnumMap<JapaneseTokenizer.Type, Dictionary> dictionaryMap;

		boolean useEOS;

		int rootCapacity = 0;

		int rootSize = 0;

		int rootBase = 0;

		int[] lRoot;

		int[] rRoot;

		int capacity = 0;

		int nodeCount = 0;

		JapaneseTokenizer.Type[] nodeDicType;

		int[] nodeWordID;

		int[] nodeMark;

		int[] nodeLeftID;

		int[] nodeRightID;

		int[] nodeWordCost;

		int[] nodeLeftCost;

		int[] nodeRightCost;

		int[] nodeLeftNode;

		int[] nodeRightNode;

		int[] nodeLeft;

		int[] nodeRight;

		int[] nodeLeftChain;

		int[] nodeRightChain;

		private void setupRoot(int baseOffset, int lastOffset) {
			assert baseOffset <= lastOffset;
			int size = (lastOffset - baseOffset) + 1;
			if ((rootCapacity) < size) {
				int oversize = ArrayUtil.oversize(size, Integer.BYTES);
				lRoot = new int[oversize];
				rRoot = new int[oversize];
				rootCapacity = oversize;
			}
			Arrays.fill(lRoot, 0, size, (-1));
			Arrays.fill(rRoot, 0, size, (-1));
			rootSize = size;
			rootBase = baseOffset;
		}

		private void reserve(int n) {
			if ((capacity) < n) {
				int oversize = ArrayUtil.oversize(n, Integer.BYTES);
				nodeDicType = new JapaneseTokenizer.Type[oversize];
				nodeWordID = new int[oversize];
				nodeMark = new int[oversize];
				nodeLeftID = new int[oversize];
				nodeRightID = new int[oversize];
				nodeWordCost = new int[oversize];
				nodeLeftCost = new int[oversize];
				nodeRightCost = new int[oversize];
				nodeLeftNode = new int[oversize];
				nodeRightNode = new int[oversize];
				nodeLeft = new int[oversize];
				nodeRight = new int[oversize];
				nodeLeftChain = new int[oversize];
				nodeRightChain = new int[oversize];
				capacity = oversize;
			}
		}

		private void setupNodePool(int n) {
			reserve(n);
			nodeCount = 0;
			if (JapaneseTokenizer.VERBOSE) {
				System.out.printf("DEBUG: setupNodePool: n = %d\n", n);
				System.out.printf("DEBUG: setupNodePool: lattice.capacity = %d\n", capacity);
			}
		}

		private int addNode(JapaneseTokenizer.Type dicType, int wordID, int left, int right) {
			if (JapaneseTokenizer.VERBOSE) {
				System.out.printf("DEBUG: addNode: dicType=%s, wordID=%d, left=%d, right=%d, str=%s\n", dicType.toString(), wordID, left, right, (left == (-1) ? "BOS" : right == (-1) ? "EOS" : new String(fragment, left, (right - left))));
			}
			assert (nodeCount) < (capacity);
			assert ((left == (-1)) || (right == (-1))) || (left < right);
			assert (left == (-1)) || ((0 <= left) && (left < (rootSize)));
			assert (right == (-1)) || ((0 <= right) && (right < (rootSize)));
			int node = (nodeCount)++;
			if (JapaneseTokenizer.VERBOSE) {
				System.out.printf("DEBUG: addNode: node=%d\n", node);
			}
			nodeDicType[node] = dicType;
			nodeWordID[node] = wordID;
			nodeMark[node] = 0;
			if (wordID < 0) {
				nodeWordCost[node] = 0;
				nodeLeftCost[node] = 0;
				nodeRightCost[node] = 0;
				nodeLeftID[node] = 0;
				nodeRightID[node] = 0;
			}else {
				Dictionary dic = dictionaryMap.get(dicType);
				nodeWordCost[node] = dic.getWordCost(wordID);
				nodeLeftID[node] = dic.getLeftId(wordID);
				nodeRightID[node] = dic.getRightId(wordID);
			}
			if (JapaneseTokenizer.VERBOSE) {
				System.out.printf("DEBUG: addNode: wordCost=%d, leftID=%d, rightID=%d\n", nodeWordCost[node], nodeLeftID[node], nodeRightID[node]);
			}
			nodeLeft[node] = left;
			nodeRight[node] = right;
			if (0 <= left) {
				nodeLeftChain[node] = lRoot[left];
				lRoot[left] = node;
			}else {
				nodeLeftChain[node] = -1;
			}
			if (0 <= right) {
				nodeRightChain[node] = rRoot[right];
				rRoot[right] = node;
			}else {
				nodeRightChain[node] = -1;
			}
			return node;
		}

		private int positionCount(JapaneseTokenizer.WrappedPositionArray positions, int beg, int end) {
			int count = 0;
			for (int i = beg; i < end; ++i) {
				count += positions.get(i).count;
			}
			return count;
		}

		void setup(char[] fragment, EnumMap<JapaneseTokenizer.Type, Dictionary> dictionaryMap, JapaneseTokenizer.WrappedPositionArray positions, int prevOffset, int endOffset, boolean useEOS) {
			assert (positions.get(prevOffset).count) == 1;
			if (JapaneseTokenizer.VERBOSE) {
				System.out.printf("DEBUG: setup: prevOffset=%d, endOffset=%d\n", prevOffset, endOffset);
			}
			this.fragment = fragment;
			this.dictionaryMap = dictionaryMap;
			this.useEOS = useEOS;
			setupRoot(prevOffset, endOffset);
			setupNodePool(((positionCount(positions, (prevOffset + 1), (endOffset + 1))) + 2));
			JapaneseTokenizer.Position first = positions.get(prevOffset);
			if ((addNode(first.backType[0], first.backID[0], (-1), 0)) != 0) {
				assert false;
			}
			if ((addNode(JapaneseTokenizer.Type.KNOWN, (-1), (endOffset - (rootBase)), (-1))) != 1) {
				assert false;
			}
			for (int offset = endOffset; prevOffset < offset; --offset) {
				int right = offset - (rootBase);
				if (0 <= (lRoot[right])) {
					JapaneseTokenizer.Position pos = positions.get(offset);
					for (int i = 0; i < (pos.count); ++i) {
						addNode(pos.backType[i], pos.backID[i], ((pos.backPos[i]) - (rootBase)), right);
					}
				}
			}
		}

		void markUnreachable() {
			for (int index = 1; index < ((rootSize) - 1); ++index) {
				if ((rRoot[index]) < 0) {
					for (int node = lRoot[index]; 0 <= node; node = nodeLeftChain[node]) {
						if (JapaneseTokenizer.VERBOSE) {
							System.out.printf("DEBUG: markUnreachable: node=%d\n", node);
						}
						nodeMark[node] = -1;
					}
				}
			}
		}

		int connectionCost(ConnectionCosts costs, int left, int right) {
			int leftID = nodeLeftID[right];
			return (leftID == 0) && (!(useEOS)) ? 0 : costs.get(nodeRightID[left], leftID);
		}

		void calcLeftCost(ConnectionCosts costs) {
			for (int index = 0; index < (rootSize); ++index) {
				for (int node = lRoot[index]; 0 <= node; node = nodeLeftChain[node]) {
					if (0 <= (nodeMark[node])) {
						int leastNode = -1;
						int leastCost = Integer.MAX_VALUE;
						for (int leftNode = rRoot[index]; 0 <= leftNode; leftNode = nodeRightChain[leftNode]) {
							if (0 <= (nodeMark[leftNode])) {
								int cost = ((nodeLeftCost[leftNode]) + (nodeWordCost[leftNode])) + (connectionCost(costs, leftNode, node));
								if (cost < leastCost) {
									leastCost = cost;
									leastNode = leftNode;
								}
							}
						}
						assert 0 <= leastNode;
						nodeLeftNode[node] = leastNode;
						nodeLeftCost[node] = leastCost;
						if (JapaneseTokenizer.VERBOSE) {
							System.out.printf("DEBUG: calcLeftCost: node=%d, leftNode=%d, leftCost=%d\n", node, nodeLeftNode[node], nodeLeftCost[node]);
						}
					}
				}
			}
		}

		void calcRightCost(ConnectionCosts costs) {
			for (int index = (rootSize) - 1; 0 <= index; --index) {
				for (int node = rRoot[index]; 0 <= node; node = nodeRightChain[node]) {
					if (0 <= (nodeMark[node])) {
						int leastNode = -1;
						int leastCost = Integer.MAX_VALUE;
						for (int rightNode = lRoot[index]; 0 <= rightNode; rightNode = nodeLeftChain[rightNode]) {
							if (0 <= (nodeMark[rightNode])) {
								int cost = ((nodeRightCost[rightNode]) + (nodeWordCost[rightNode])) + (connectionCost(costs, node, rightNode));
								if (cost < leastCost) {
									leastCost = cost;
									leastNode = rightNode;
								}
							}
						}
						assert 0 <= leastNode;
						nodeRightNode[node] = leastNode;
						nodeRightCost[node] = leastCost;
						if (JapaneseTokenizer.VERBOSE) {
							System.out.printf("DEBUG: calcRightCost: node=%d, rightNode=%d, rightCost=%d\n", node, nodeRightNode[node], nodeRightCost[node]);
						}
					}
				}
			}
		}

		void markSameSpanNode(int refNode, int value) {
			int left = nodeLeft[refNode];
			int right = nodeRight[refNode];
			for (int node = lRoot[left]; 0 <= node; node = nodeLeftChain[node]) {
				if ((nodeRight[node]) == right) {
					nodeMark[node] = value;
				}
			}
		}

		List<Integer> bestPathNodeList() {
			List<Integer> list = new ArrayList<>();
			for (int node = nodeRightNode[0]; node != 1; node = nodeRightNode[node]) {
				list.add(node);
				markSameSpanNode(node, 1);
			}
			return list;
		}

		private int cost(int node) {
			return ((nodeLeftCost[node]) + (nodeWordCost[node])) + (nodeRightCost[node]);
		}

		List<Integer> nBestNodeList(int N) {
			List<Integer> list = new ArrayList<>();
			int leastCost = Integer.MAX_VALUE;
			int leastLeft = -1;
			int leastRight = -1;
			for (int node = 2; node < (nodeCount); ++node) {
				if ((nodeMark[node]) == 0) {
					int cost = cost(node);
					if (cost < leastCost) {
						leastCost = cost;
						leastLeft = nodeLeft[node];
						leastRight = nodeRight[node];
						list.clear();
						list.add(node);
					}else
						if ((cost == leastCost) && (((nodeLeft[node]) != leastLeft) || ((nodeRight[node]) != leastRight))) {
							list.add(node);
						}

				}
			}
			for (int node : list) {
				markSameSpanNode(node, N);
			}
			return list;
		}

		int bestCost() {
			return nodeLeftCost[1];
		}

		int probeDelta(int start, int end) {
			int left = start - (rootBase);
			int right = end - (rootBase);
			if ((left < 0) || ((rootSize) < right)) {
				return Integer.MAX_VALUE;
			}
			int probedCost = Integer.MAX_VALUE;
			for (int node = lRoot[left]; 0 <= node; node = nodeLeftChain[node]) {
				if ((nodeRight[node]) == right) {
					probedCost = Math.min(probedCost, cost(node));
				}
			}
			return probedCost - (bestCost());
		}

		void debugPrint() {
			if (JapaneseTokenizer.VERBOSE) {
				for (int node = 0; node < (nodeCount); ++node) {
					System.out.printf("DEBUG NODE: node=%d, mark=%d, cost=%d, left=%d, right=%d\n", node, nodeMark[node], cost(node), nodeLeft[node], nodeRight[node]);
				}
			}
		}
	}

	private JapaneseTokenizer.Lattice lattice = null;

	private void registerNode(int node, char[] fragment) {
		int left = lattice.nodeLeft[node];
		int right = lattice.nodeRight[node];
		JapaneseTokenizer.Type type = lattice.nodeDicType[node];
		if ((!(discardPunctuation)) || (!(JapaneseTokenizer.isPunctuation(fragment[left])))) {
			if (type == (JapaneseTokenizer.Type.USER)) {
				final int[] wordIDAndLength = userDictionary.lookupSegmentation(lattice.nodeWordID[node]);
				int wordID = wordIDAndLength[0];
				int current = 0;
				for (int j = 1; j < (wordIDAndLength.length); j++) {
					final int len = wordIDAndLength[j];
					if (len < (right - left)) {
					}
					current += len;
				}
			}else {
			}
		}
	}

	private void fixupPendingList() {
		Collections.sort(pending, new Comparator<Token>() {
			@Override
			public int compare(Token a, Token b) {
				int aOff = a.getOffset();
				int bOff = b.getOffset();
				if (aOff != bOff) {
					return aOff - bOff;
				}
				int aLen = a.getLength();
				int bLen = b.getLength();
				if (aLen != bLen) {
					return aLen - bLen;
				}
				return (b.getType().ordinal()) - (a.getType().ordinal());
			}
		});
		for (int i = 1; i < (pending.size()); ++i) {
			Token a = pending.get((i - 1));
			Token b = pending.get(i);
			if (((a.getOffset()) == (b.getOffset())) && ((a.getLength()) == (b.getLength()))) {
				pending.remove(i);
				--i;
			}
		}
		HashMap<Integer, Integer> map = new HashMap<>();
		for (Token t : pending) {
			map.put(t.getOffset(), 0);
			map.put(((t.getOffset()) + (t.getLength())), 0);
		}
		Integer[] offsets = map.keySet().toArray(new Integer[0]);
		Arrays.sort(offsets);
		for (int i = 0; i < (offsets.length); ++i) {
			map.put(offsets[i], i);
		}
		for (Token t : pending) {
			t.setPositionLength(((map.get(((t.getOffset()) + (t.getLength())))) - (map.get(t.getOffset()))));
		}
		Collections.reverse(pending);
	}

	private int probeDelta(String inText, String requiredToken) throws IOException {
		int start = inText.indexOf(requiredToken);
		if (start < 0) {
			return -1;
		}
		int delta = Integer.MAX_VALUE;
		int saveNBestCost = nBestCost;
		setReader(new StringReader(inText));
		reset();
		try {
			setNBestCost(1);
			int prevRootBase = -1;
			while (incrementToken()) {
				if ((lattice.rootBase) != prevRootBase) {
					prevRootBase = lattice.rootBase;
					delta = Math.min(delta, lattice.probeDelta(start, (start + (requiredToken.length()))));
				}
			} 
		} finally {
			end();
			close();
			setNBestCost(saveNBestCost);
		}
		if (JapaneseTokenizer.VERBOSE) {
			System.out.printf("JapaneseTokenizer: delta = %d: %s-%s\n", delta, inText, requiredToken);
		}
		return delta == (Integer.MAX_VALUE) ? -1 : delta;
	}

	public int calcNBestCost(String examples) {
		int maxDelta = 0;
		for (String example : examples.split("/")) {
			if (!(example.isEmpty())) {
				String[] pair = example.split("-");
				if ((pair.length) != 2) {
					throw new RuntimeException((("Unexpected example form: " + example) + " (expected two '-')"));
				}else {
					try {
						maxDelta = Math.max(maxDelta, probeDelta(pair[0], pair[1]));
					} catch (IOException e) {
						throw new RuntimeException("Internal error calculating best costs from examples. Got ", e);
					}
				}
			}
		}
		return maxDelta;
	}

	public void setNBestCost(int value) {
		nBestCost = value;
		outputNBest = 0 < (nBestCost);
	}

	private void backtraceNBest(final JapaneseTokenizer.Position endPosData, final boolean useEOS) throws IOException {
		if ((lattice) == null) {
			lattice = new JapaneseTokenizer.Lattice();
		}
		final int endPos = endPosData.pos;
		char[] fragment = buffer.get(lastBackTracePos, (endPos - (lastBackTracePos)));
		lattice.setup(fragment, dictionaryMap, positions, lastBackTracePos, endPos, useEOS);
		lattice.markUnreachable();
		lattice.calcLeftCost(costs);
		lattice.calcRightCost(costs);
		int bestCost = lattice.bestCost();
		if (JapaneseTokenizer.VERBOSE) {
			System.out.printf("DEBUG: 1-BEST COST: %d\n", bestCost);
		}
		for (int node : lattice.bestPathNodeList()) {
			registerNode(node, fragment);
		}
		for (int n = 2; ; ++n) {
			List<Integer> nbest = lattice.nBestNodeList(n);
			if (nbest.isEmpty()) {
				break;
			}
			int cost = lattice.cost(nbest.get(0));
			if (JapaneseTokenizer.VERBOSE) {
				System.out.printf("DEBUG: %d-BEST COST: %d\n", n, cost);
			}
			if ((bestCost + (nBestCost)) < cost) {
				break;
			}
			for (int node : nbest) {
				registerNode(node, fragment);
			}
		}
		if (JapaneseTokenizer.VERBOSE) {
			lattice.debugPrint();
		}
	}

	private void backtrace(final JapaneseTokenizer.Position endPosData, final int fromIDX) throws IOException {
		final int endPos = endPosData.pos;
		if (JapaneseTokenizer.VERBOSE) {
			System.out.println(((((((((("\n  backtrace: endPos=" + endPos) + " pos=") + (pos)) + "; ") + ((pos) - (lastBackTracePos))) + " characters; last=") + (lastBackTracePos)) + " cost=") + (endPosData.costs[fromIDX])));
		}
		final char[] fragment = buffer.get(lastBackTracePos, (endPos - (lastBackTracePos)));
		if ((dotOut) != null) {
		}
		int pos = endPos;
		int bestIDX = fromIDX;
		Token altToken = null;
		int lastLeftWordID = -1;
		int backCount = 0;
		while (pos > (lastBackTracePos)) {
			final JapaneseTokenizer.Position posData = positions.get(pos);
			assert bestIDX < (posData.count);
			int backPos = posData.backPos[bestIDX];
			assert backPos >= (lastBackTracePos) : (("backPos=" + backPos) + " vs lastBackTracePos=") + (lastBackTracePos);
			int length = pos - backPos;
			JapaneseTokenizer.Type backType = posData.backType[bestIDX];
			int backID = posData.backID[bestIDX];
			int nextBestIDX = posData.backIndex[bestIDX];
			if ((((outputCompounds) && (searchMode)) && (altToken == null)) && (backType != (JapaneseTokenizer.Type.USER))) {
				final int penalty = computeSecondBestThreshold(backPos, (pos - backPos));
				if (penalty > 0) {
					if (JapaneseTokenizer.VERBOSE) {
						System.out.println(((((((((((((("  compound=" + (new String(buffer.get(backPos, (pos - backPos))))) + " backPos=") + backPos) + " pos=") + pos) + " penalty=") + penalty) + " cost=") + (posData.costs[bestIDX])) + " bestIDX=") + bestIDX) + " lastLeftID=") + lastLeftWordID));
					}
					int maxCost = (posData.costs[bestIDX]) + penalty;
					if (lastLeftWordID != (-1)) {
						maxCost += costs.get(getDict(backType).getRightId(backID), lastLeftWordID);
					}
					pruneAndRescore(backPos, pos, posData.backIndex[bestIDX]);
					int leastCost = Integer.MAX_VALUE;
					int leastIDX = -1;
					for (int idx = 0; idx < (posData.count); idx++) {
						int cost = posData.costs[idx];
						if (lastLeftWordID != (-1)) {
							cost += costs.get(getDict(posData.backType[idx]).getRightId(posData.backID[idx]), lastLeftWordID);
						}
						if (cost < leastCost) {
							leastCost = cost;
							leastIDX = idx;
						}
					}
					if (JapaneseTokenizer.VERBOSE) {
						System.out.println(((((((("  afterPrune: " + (posData.count)) + " arcs arriving; leastCost=") + leastCost) + " vs threshold=") + maxCost) + " lastLeftWordID=") + lastLeftWordID));
					}
					if (((leastIDX != (-1)) && (leastCost <= maxCost)) && ((posData.backPos[leastIDX]) != backPos)) {
						assert (posData.backPos[leastIDX]) != backPos;
						bestIDX = leastIDX;
						nextBestIDX = posData.backIndex[bestIDX];
						backPos = posData.backPos[bestIDX];
						length = pos - backPos;
						backType = posData.backType[bestIDX];
						backID = posData.backID[bestIDX];
						backCount = 0;
					}else {
					}
				}
			}
			final int offset = backPos - (lastBackTracePos);
			assert offset >= 0;
			if ((altToken != null) && ((altToken.getPosition()) >= backPos)) {
				assert (altToken.getPosition()) == backPos : ((altToken.getPosition()) + " vs ") + backPos;
				if (backCount > 0) {
					backCount++;
					altToken.setPositionLength(backCount);
					if (JapaneseTokenizer.VERBOSE) {
						System.out.println(("    add altToken=" + altToken));
					}
					pending.add(altToken);
				}else {
					if (JapaneseTokenizer.VERBOSE) {
						System.out.println(("    discard all-punctuation altToken=" + altToken));
					}
					assert discardPunctuation;
				}
				altToken = null;
			}
			final Dictionary dict = getDict(backType);
			if (backType == (JapaneseTokenizer.Type.USER)) {
				final int[] wordIDAndLength = userDictionary.lookupSegmentation(backID);
				int wordID = wordIDAndLength[0];
				int current = 0;
				for (int j = 1; j < (wordIDAndLength.length); j++) {
					final int len = wordIDAndLength[j];
					if (JapaneseTokenizer.VERBOSE) {
						System.out.println(("    add USER token=" + (pending.get(((pending.size()) - 1)))));
					}
					current += len;
				}
				Collections.reverse(pending.subList(((pending.size()) - ((wordIDAndLength.length) - 1)), pending.size()));
				backCount += (wordIDAndLength.length) - 1;
			}else {
				if ((extendedMode) && (backType == (JapaneseTokenizer.Type.UNKNOWN))) {
					int unigramTokenCount = 0;
					for (int i = length - 1; i >= 0; i--) {
						int charLen = 1;
						if ((i > 0) && (Character.isLowSurrogate(fragment[(offset + i)]))) {
							i--;
							charLen = 2;
						}
						if ((!(discardPunctuation)) || (!(JapaneseTokenizer.isPunctuation(fragment[(offset + i)])))) {
							unigramTokenCount++;
						}
					}
					backCount += unigramTokenCount;
				}else
					if (((!(discardPunctuation)) || (length == 0)) || (!(JapaneseTokenizer.isPunctuation(fragment[offset])))) {
						if (JapaneseTokenizer.VERBOSE) {
							System.out.println(("    add token=" + (pending.get(((pending.size()) - 1)))));
						}
						backCount++;
					}else {
						if (JapaneseTokenizer.VERBOSE) {
							System.out.println(("    skip punctuation token=" + (new String(fragment, offset, length))));
						}
					}

			}
			lastLeftWordID = dict.getLeftId(backID);
			pos = backPos;
			bestIDX = nextBestIDX;
		} 
		lastBackTracePos = endPos;
		if (JapaneseTokenizer.VERBOSE) {
			System.out.println(("  freeBefore pos=" + endPos));
		}
		buffer.freeBefore(endPos);
		positions.freeBefore(endPos);
	}

	Dictionary getDict(JapaneseTokenizer.Type type) {
		return dictionaryMap.get(type);
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

