

import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.apache.lucene.util.UnicodeUtil;
import org.apache.lucene.util.automaton.Automata;
import org.apache.lucene.util.automaton.Automaton;


public class LevenshteinAutomata {
	public static final int MAXIMUM_SUPPORTED_DISTANCE = 2;

	final int[] word;

	final int[] alphabet;

	final int alphaMax;

	final int[] rangeLower;

	final int[] rangeUpper;

	int numRanges = 0;

	LevenshteinAutomata.ParametricDescription[] descriptions;

	public LevenshteinAutomata(String input, boolean withTranspositions) {
		this(LevenshteinAutomata.codePoints(input), Character.MAX_CODE_POINT, withTranspositions);
	}

	public LevenshteinAutomata(int[] word, int alphaMax, boolean withTranspositions) {
		this.word = word;
		this.alphaMax = alphaMax;
		SortedSet<Integer> set = new TreeSet<>();
		for (int i = 0; i < (word.length); i++) {
			int v = word[i];
			if (v > alphaMax) {
				throw new IllegalArgumentException((("alphaMax exceeded by symbol " + v) + " in word"));
			}
			set.add(v);
		}
		alphabet = new int[set.size()];
		Iterator<Integer> iterator = set.iterator();
		for (int i = 0; i < (alphabet.length); i++)
			alphabet[i] = iterator.next();

		rangeLower = new int[(alphabet.length) + 2];
		rangeUpper = new int[(alphabet.length) + 2];
		int lower = 0;
		for (int i = 0; i < (alphabet.length); i++) {
			int higher = alphabet[i];
			if (higher > lower) {
				rangeLower[numRanges] = lower;
				rangeUpper[numRanges] = higher - 1;
				(numRanges)++;
			}
			lower = higher + 1;
		}
		if (lower <= alphaMax) {
			rangeLower[numRanges] = lower;
			rangeUpper[numRanges] = alphaMax;
			(numRanges)++;
		}
	}

	private static int[] codePoints(String input) {
		int length = Character.codePointCount(input, 0, input.length());
		int[] word = new int[length];
		for (int i = 0, j = 0, cp = 0; i < (input.length()); i += Character.charCount(cp)) {
			word[(j++)] = cp = input.codePointAt(i);
		}
		return word;
	}

	public Automaton toAutomaton(int n) {
		return toAutomaton(n, "");
	}

	public Automaton toAutomaton(int n, String prefix) {
		assert prefix != null;
		if (n == 0) {
			return Automata.makeString((prefix + (UnicodeUtil.newString(word, 0, word.length))));
		}
		if (n >= (descriptions.length))
			return null;

		final int range = (2 * n) + 1;
		LevenshteinAutomata.ParametricDescription description = descriptions[n];
		final int numStates = description.size();
		final int numTransitions = numStates * (Math.min((1 + (2 * n)), alphabet.length));
		final int prefixStates = (prefix != null) ? prefix.codePointCount(0, prefix.length()) : 0;
		final Automaton a = new Automaton((numStates + prefixStates), numTransitions);
		int lastState;
		if (prefix != null) {
			lastState = a.createState();
			for (int i = 0, cp = 0; i < (prefix.length()); i += Character.charCount(cp)) {
				int state = a.createState();
				cp = prefix.codePointAt(i);
				a.addTransition(lastState, state, cp, cp);
				lastState = state;
			}
		}else {
			lastState = a.createState();
		}
		int stateOffset = lastState;
		a.setAccept(lastState, description.isAccept(0));
		for (int i = 1; i < numStates; i++) {
			int state = a.createState();
			a.setAccept(state, description.isAccept(i));
		}
		for (int k = 0; k < numStates; k++) {
			final int xpos = description.getPosition(k);
			if (xpos < 0)
				continue;

			final int end = xpos + (Math.min(((word.length) - xpos), range));
			for (int x = 0; x < (alphabet.length); x++) {
				final int ch = alphabet[x];
				final int cvec = getVector(ch, xpos, end);
				int dest = description.transition(k, xpos, cvec);
				if (dest >= 0) {
					a.addTransition((stateOffset + k), (stateOffset + dest), ch);
				}
			}
			int dest = description.transition(k, xpos, 0);
			if (dest >= 0) {
				for (int r = 0; r < (numRanges); r++) {
					a.addTransition((stateOffset + k), (stateOffset + dest), rangeLower[r], rangeUpper[r]);
				}
			}
		}
		a.finishState();
		assert a.isDeterministic();
		return a;
	}

	int getVector(int x, int pos, int end) {
		int vector = 0;
		for (int i = pos; i < end; i++) {
			vector <<= 1;
			if ((word[i]) == x)
				vector |= 1;

		}
		return vector;
	}

	abstract static class ParametricDescription {
		protected final int w;

		protected final int n;

		private final int[] minErrors;

		ParametricDescription(int w, int n, int[] minErrors) {
			this.w = w;
			this.n = n;
			this.minErrors = minErrors;
		}

		int size() {
			return (minErrors.length) * ((w) + 1);
		}

		boolean isAccept(int absState) {
			int state = absState / ((w) + 1);
			int offset = absState % ((w) + 1);
			assert offset >= 0;
			return (((w) - offset) + (minErrors[state])) <= (n);
		}

		int getPosition(int absState) {
			return absState % ((w) + 1);
		}

		abstract int transition(int state, int position, int vector);

		private static final long[] MASKS = new long[]{ 1, 3, 7, 15, 31, 63, 127, 255, 511, 1023, 2047, 4095, 8191, 16383, 32767, 65535, 131071, 262143, 524287, 1048575, 2097151, 4194303, 8388607, 16777215, 33554431, 67108863, 134217727, 268435455, 536870911, 1073741823, 2147483647L, 4294967295L, 8589934591L, 17179869183L, 34359738367L, 68719476735L, 137438953471L, 274877906943L, 549755813887L, 1099511627775L, 2199023255551L, 4398046511103L, 8796093022207L, 17592186044415L, 35184372088831L, 70368744177663L, 140737488355327L, 281474976710655L, 562949953421311L, 1125899906842623L, 2251799813685247L, 4503599627370495L, 9007199254740991L, 18014398509481983L, 36028797018963967L, 72057594037927935L, 144115188075855871L, 288230376151711743L, 576460752303423487L, 1152921504606846975L, 2305843009213693951L, 4611686018427387903L, 9223372036854775807L };

		protected int unpack(long[] data, int index, int bitsPerValue) {
			final long bitLoc = bitsPerValue * index;
			final int dataLoc = ((int) (bitLoc >> 6));
			final int bitStart = ((int) (bitLoc & 63));
			if ((bitStart + bitsPerValue) <= 64) {
				return ((int) (((data[dataLoc]) >> bitStart) & (LevenshteinAutomata.ParametricDescription.MASKS[(bitsPerValue - 1)])));
			}else {
				final int part = 64 - bitStart;
				return ((int) ((((data[dataLoc]) >> bitStart) & (LevenshteinAutomata.ParametricDescription.MASKS[(part - 1)])) + (((data[(1 + dataLoc)]) & (LevenshteinAutomata.ParametricDescription.MASKS[((bitsPerValue - part) - 1)])) << part)));
			}
		}
	}
}

