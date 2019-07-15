

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefBuilder;
import org.apache.lucene.util.IntsRef;
import org.apache.lucene.util.IntsRefBuilder;
import org.apache.lucene.util.RamUsageEstimator;
import org.apache.lucene.util.automaton.Automata;
import org.apache.lucene.util.automaton.Automaton;
import org.apache.lucene.util.automaton.StatePair;
import org.apache.lucene.util.automaton.Transition;


public final class Operations {
	public static final int DEFAULT_MAX_DETERMINIZED_STATES = 10000;

	public static final int MAX_RECURSION_LEVEL = 1000;

	private Operations() {
	}

	public static Automaton concatenate(Automaton a1, Automaton a2) {
		return Operations.concatenate(Arrays.asList(a1, a2));
	}

	public static Automaton concatenate(List<Automaton> l) {
		Automaton result = new Automaton();
		for (Automaton a : l) {
			if ((a.getNumStates()) == 0) {
				result.finishState();
				return result;
			}
			int numStates = a.getNumStates();
			for (int s = 0; s < numStates; s++) {
				result.createState();
			}
		}
		int stateOffset = 0;
		Transition t = new Transition();
		for (int i = 0; i < (l.size()); i++) {
			Automaton a = l.get(i);
			int numStates = a.getNumStates();
			Automaton nextA = (i == ((l.size()) - 1)) ? null : l.get((i + 1));
			for (int s = 0; s < numStates; s++) {
				int numTransitions = a.initTransition(s, t);
				for (int j = 0; j < numTransitions; j++) {
					a.getNextTransition(t);
					result.addTransition((stateOffset + s), (stateOffset + (t.dest)), t.min, t.max);
				}
				if (a.isAccept(s)) {
					Automaton followA = nextA;
					int followOffset = stateOffset;
					int upto = i + 1;
					while (true) {
						if (followA != null) {
							numTransitions = followA.initTransition(0, t);
							for (int j = 0; j < numTransitions; j++) {
								followA.getNextTransition(t);
								result.addTransition((stateOffset + s), ((followOffset + numStates) + (t.dest)), t.min, t.max);
							}
							if (followA.isAccept(0)) {
								followOffset += followA.getNumStates();
								followA = (upto == ((l.size()) - 1)) ? null : l.get((upto + 1));
								upto++;
							}else {
								break;
							}
						}else {
							result.setAccept((stateOffset + s), true);
							break;
						}
					} 
				}
			}
			stateOffset += numStates;
		}
		if ((result.getNumStates()) == 0) {
			result.createState();
		}
		result.finishState();
		return result;
	}

	public static Automaton optional(Automaton a) {
		Automaton result = new Automaton();
		result.createState();
		result.setAccept(0, true);
		if ((a.getNumStates()) > 0) {
			result.copy(a);
			result.addEpsilon(0, 1);
		}
		result.finishState();
		return result;
	}

	public static Automaton repeat(Automaton a) {
		if ((a.getNumStates()) == 0) {
			return a;
		}
		Automaton.Builder builder = new Automaton.Builder();
		builder.createState();
		builder.setAccept(0, true);
		builder.copy(a);
		Transition t = new Transition();
		int count = a.initTransition(0, t);
		for (int i = 0; i < count; i++) {
			a.getNextTransition(t);
			builder.addTransition(0, ((t.dest) + 1), t.min, t.max);
		}
		int numStates = a.getNumStates();
		for (int s = 0; s < numStates; s++) {
			if (a.isAccept(s)) {
				count = a.initTransition(0, t);
				for (int i = 0; i < count; i++) {
					a.getNextTransition(t);
					builder.addTransition((s + 1), ((t.dest) + 1), t.min, t.max);
				}
			}
		}
		return builder.finish();
	}

	public static Automaton repeat(Automaton a, int count) {
		if (count == 0) {
			return Operations.repeat(a);
		}
		List<Automaton> as = new ArrayList<>();
		while ((count--) > 0) {
			as.add(a);
		} 
		as.add(Operations.repeat(a));
		return Operations.concatenate(as);
	}

	public static Automaton repeat(Automaton a, int min, int max) {
		if (min > max) {
			return Automata.makeEmpty();
		}
		Automaton b;
		if (min == 0) {
			b = Automata.makeEmptyString();
		}else
			if (min == 1) {
				b = new Automaton();
				b.copy(a);
			}else {
				List<Automaton> as = new ArrayList<>();
				for (int i = 0; i < min; i++) {
					as.add(a);
				}
				b = Operations.concatenate(as);
			}

		Set<Integer> prevAcceptStates = Operations.toSet(b, 0);
		Automaton.Builder builder = new Automaton.Builder();
		builder.copy(b);
		for (int i = min; i < max; i++) {
			int numStates = builder.getNumStates();
			builder.copy(a);
			for (int s : prevAcceptStates) {
				builder.addEpsilon(s, numStates);
			}
			prevAcceptStates = Operations.toSet(a, numStates);
		}
		return builder.finish();
	}

	private static Set<Integer> toSet(Automaton a, int offset) {
		int numStates = a.getNumStates();
		Set<Integer> result = new HashSet<Integer>();
		int upto = 0;
		return result;
	}

	public static Automaton complement(Automaton a, int maxDeterminizedStates) {
		a = Operations.totalize(Operations.determinize(a, maxDeterminizedStates));
		int numStates = a.getNumStates();
		for (int p = 0; p < numStates; p++) {
			a.setAccept(p, (!(a.isAccept(p))));
		}
		return Operations.removeDeadStates(a);
	}

	public static Automaton minus(Automaton a1, Automaton a2, int maxDeterminizedStates) {
		if ((Operations.isEmpty(a1)) || (a1 == a2)) {
			return Automata.makeEmpty();
		}
		if (Operations.isEmpty(a2)) {
			return a1;
		}
		return Operations.intersection(a1, Operations.complement(a2, maxDeterminizedStates));
	}

	public static Automaton intersection(Automaton a1, Automaton a2) {
		if (a1 == a2) {
			return a1;
		}
		if ((a1.getNumStates()) == 0) {
			return a1;
		}
		if ((a2.getNumStates()) == 0) {
			return a2;
		}
		Transition[][] transitions1 = a1.getSortedTransitions();
		Transition[][] transitions2 = a2.getSortedTransitions();
		Automaton c = new Automaton();
		c.createState();
		ArrayDeque<StatePair> worklist = new ArrayDeque<>();
		HashMap<StatePair, StatePair> newstates = new HashMap<>();
		while ((worklist.size()) > 0) {
		} 
		c.finishState();
		return Operations.removeDeadStates(c);
	}

	public static boolean sameLanguage(Automaton a1, Automaton a2) {
		if (a1 == a2) {
			return true;
		}
		return (Operations.subsetOf(a2, a1)) && (Operations.subsetOf(a1, a2));
	}

	public static boolean hasDeadStates(Automaton a) {
		BitSet liveStates = Operations.getLiveStates(a);
		int numLive = liveStates.cardinality();
		int numStates = a.getNumStates();
		assert numLive <= numStates : (((("numLive=" + numLive) + " numStates=") + numStates) + " ") + liveStates;
		return numLive < numStates;
	}

	public static boolean hasDeadStatesFromInitial(Automaton a) {
		BitSet reachableFromInitial = Operations.getLiveStatesFromInitial(a);
		BitSet reachableFromAccept = Operations.getLiveStatesToAccept(a);
		reachableFromInitial.andNot(reachableFromAccept);
		return (reachableFromInitial.isEmpty()) == false;
	}

	public static boolean hasDeadStatesToAccept(Automaton a) {
		BitSet reachableFromInitial = Operations.getLiveStatesFromInitial(a);
		BitSet reachableFromAccept = Operations.getLiveStatesToAccept(a);
		reachableFromAccept.andNot(reachableFromInitial);
		return (reachableFromAccept.isEmpty()) == false;
	}

	public static boolean subsetOf(Automaton a1, Automaton a2) {
		if ((a1.isDeterministic()) == false) {
			throw new IllegalArgumentException("a1 must be deterministic");
		}
		if ((a2.isDeterministic()) == false) {
			throw new IllegalArgumentException("a2 must be deterministic");
		}
		assert (Operations.hasDeadStatesFromInitial(a1)) == false;
		assert (Operations.hasDeadStatesFromInitial(a2)) == false;
		if ((a1.getNumStates()) == 0) {
			return true;
		}else
			if ((a2.getNumStates()) == 0) {
				return Operations.isEmpty(a1);
			}

		Transition[][] transitions1 = a1.getSortedTransitions();
		Transition[][] transitions2 = a2.getSortedTransitions();
		ArrayDeque<StatePair> worklist = new ArrayDeque<>();
		HashSet<StatePair> visited = new HashSet<>();
		StatePair p = new StatePair(0, 0);
		worklist.add(p);
		visited.add(p);
		while ((worklist.size()) > 0) {
			p = worklist.removeFirst();
		} 
		return true;
	}

	public static Automaton union(Automaton a1, Automaton a2) {
		return Operations.union(Arrays.asList(a1, a2));
	}

	public static Automaton union(Collection<Automaton> l) {
		Automaton result = new Automaton();
		result.createState();
		for (Automaton a : l) {
			result.copy(a);
		}
		int stateOffset = 1;
		for (Automaton a : l) {
			if ((a.getNumStates()) == 0) {
				continue;
			}
			result.addEpsilon(0, stateOffset);
			stateOffset += a.getNumStates();
		}
		result.finishState();
		return Operations.removeDeadStates(result);
	}

	private static final class TransitionList {
		int[] transitions = new int[3];

		int next;

		public void add(Transition t) {
			if ((transitions.length) < ((next) + 3)) {
				transitions = ArrayUtil.grow(transitions, ((next) + 3));
			}
			transitions[next] = t.dest;
			transitions[((next) + 1)] = t.min;
			transitions[((next) + 2)] = t.max;
			next += 3;
		}
	}

	private static final class PointTransitions implements Comparable<Operations.PointTransitions> {
		int point;

		final Operations.TransitionList ends = new Operations.TransitionList();

		final Operations.TransitionList starts = new Operations.TransitionList();

		@Override
		public int compareTo(Operations.PointTransitions other) {
			return (point) - (other.point);
		}

		public void reset(int point) {
			this.point = point;
			ends.next = 0;
			starts.next = 0;
		}

		@Override
		public boolean equals(Object other) {
			return (((Operations.PointTransitions) (other)).point) == (point);
		}

		@Override
		public int hashCode() {
			return point;
		}
	}

	private static final class PointTransitionSet {
		int count;

		Operations.PointTransitions[] points = new Operations.PointTransitions[5];

		private static final int HASHMAP_CUTOVER = 30;

		private final HashMap<Integer, Operations.PointTransitions> map = new HashMap<>();

		private boolean useHash = false;

		private Operations.PointTransitions next(int point) {
			if ((count) == (points.length)) {
				final Operations.PointTransitions[] newArray = new Operations.PointTransitions[ArrayUtil.oversize((1 + (count)), RamUsageEstimator.NUM_BYTES_OBJECT_REF)];
				System.arraycopy(points, 0, newArray, 0, count);
				points = newArray;
			}
			Operations.PointTransitions points0 = points[count];
			if (points0 == null) {
				points0 = points[count] = new Operations.PointTransitions();
			}
			points0.reset(point);
			(count)++;
			return points0;
		}

		private Operations.PointTransitions find(int point) {
			if (useHash) {
				final Integer pi = point;
				Operations.PointTransitions p = map.get(pi);
				if (p == null) {
					p = next(point);
					map.put(pi, p);
				}
				return p;
			}else {
				for (int i = 0; i < (count); i++) {
					if ((points[i].point) == point) {
						return points[i];
					}
				}
				final Operations.PointTransitions p = next(point);
				if ((count) == (Operations.PointTransitionSet.HASHMAP_CUTOVER)) {
					assert (map.size()) == 0;
					for (int i = 0; i < (count); i++) {
						map.put(points[i].point, points[i]);
					}
					useHash = true;
				}
				return p;
			}
		}

		public void reset() {
			if (useHash) {
				map.clear();
				useHash = false;
			}
			count = 0;
		}

		public void sort() {
			if ((count) > 1)
				ArrayUtil.timSort(points, 0, count);

		}

		public void add(Transition t) {
			find(t.min).starts.add(t);
			find((1 + (t.max))).ends.add(t);
		}

		@Override
		public String toString() {
			StringBuilder s = new StringBuilder();
			for (int i = 0; i < (count); i++) {
				if (i > 0) {
					s.append(' ');
				}
				s.append(points[i].point).append(':').append(((points[i].starts.next) / 3)).append(',').append(((points[i].ends.next) / 3));
			}
			return s.toString();
		}
	}

	public static Automaton determinize(Automaton a, int maxDeterminizedStates) {
		if (a.isDeterministic()) {
			return a;
		}
		if ((a.getNumStates()) <= 1) {
			return a;
		}
		Automaton.Builder b = new Automaton.Builder();
		b.createState();
		b.setAccept(0, a.isAccept(0));
		final Operations.PointTransitionSet points = new Operations.PointTransitionSet();
		Transition t = new Transition();
		Automaton result = b.finish();
		assert result.isDeterministic();
		return result;
	}

	public static boolean isEmpty(Automaton a) {
		if ((a.getNumStates()) == 0) {
			return true;
		}
		if (((a.isAccept(0)) == false) && ((a.getNumTransitions(0)) == 0)) {
			return true;
		}
		if ((a.isAccept(0)) == true) {
			return false;
		}
		ArrayDeque<Integer> workList = new ArrayDeque<>();
		BitSet seen = new BitSet(a.getNumStates());
		workList.add(0);
		seen.set(0);
		Transition t = new Transition();
		while ((workList.isEmpty()) == false) {
			int state = workList.removeFirst();
			if (a.isAccept(state)) {
				return false;
			}
			int count = a.initTransition(state, t);
			for (int i = 0; i < count; i++) {
				a.getNextTransition(t);
				if ((seen.get(t.dest)) == false) {
					workList.add(t.dest);
					seen.set(t.dest);
				}
			}
		} 
		return true;
	}

	public static boolean isTotal(Automaton a) {
		return Operations.isTotal(a, Character.MIN_CODE_POINT, Character.MAX_CODE_POINT);
	}

	public static boolean isTotal(Automaton a, int minAlphabet, int maxAlphabet) {
		if ((a.isAccept(0)) && ((a.getNumTransitions(0)) == 1)) {
			Transition t = new Transition();
			a.getTransition(0, 0, t);
			return (((t.dest) == 0) && ((t.min) == minAlphabet)) && ((t.max) == maxAlphabet);
		}
		return false;
	}

	public static boolean run(Automaton a, String s) {
		assert a.isDeterministic();
		int state = 0;
		for (int i = 0, cp = 0; i < (s.length()); i += Character.charCount(cp)) {
			int nextState = a.step(state, (cp = s.codePointAt(i)));
			if (nextState == (-1)) {
				return false;
			}
			state = nextState;
		}
		return a.isAccept(state);
	}

	public static boolean run(Automaton a, IntsRef s) {
		assert a.isDeterministic();
		int state = 0;
		for (int i = 0; i < (s.length); i++) {
			int nextState = a.step(state, s.ints[((s.offset) + i)]);
			if (nextState == (-1)) {
				return false;
			}
			state = nextState;
		}
		return a.isAccept(state);
	}

	private static BitSet getLiveStates(Automaton a) {
		BitSet live = Operations.getLiveStatesFromInitial(a);
		live.and(Operations.getLiveStatesToAccept(a));
		return live;
	}

	private static BitSet getLiveStatesFromInitial(Automaton a) {
		int numStates = a.getNumStates();
		BitSet live = new BitSet(numStates);
		if (numStates == 0) {
			return live;
		}
		ArrayDeque<Integer> workList = new ArrayDeque<>();
		live.set(0);
		workList.add(0);
		Transition t = new Transition();
		while ((workList.isEmpty()) == false) {
			int s = workList.removeFirst();
			int count = a.initTransition(s, t);
			for (int i = 0; i < count; i++) {
				a.getNextTransition(t);
				if ((live.get(t.dest)) == false) {
					live.set(t.dest);
					workList.add(t.dest);
				}
			}
		} 
		return live;
	}

	private static BitSet getLiveStatesToAccept(Automaton a) {
		Automaton.Builder builder = new Automaton.Builder();
		Transition t = new Transition();
		int numStates = a.getNumStates();
		for (int s = 0; s < numStates; s++) {
			builder.createState();
		}
		for (int s = 0; s < numStates; s++) {
			int count = a.initTransition(s, t);
			for (int i = 0; i < count; i++) {
				a.getNextTransition(t);
				builder.addTransition(t.dest, s, t.min, t.max);
			}
		}
		Automaton a2 = builder.finish();
		ArrayDeque<Integer> workList = new ArrayDeque<>();
		BitSet live = new BitSet(numStates);
		int s = 0;
		while ((workList.isEmpty()) == false) {
			s = workList.removeFirst();
			int count = a2.initTransition(s, t);
			for (int i = 0; i < count; i++) {
				a2.getNextTransition(t);
				if ((live.get(t.dest)) == false) {
					live.set(t.dest);
					workList.add(t.dest);
				}
			}
		} 
		return live;
	}

	public static Automaton removeDeadStates(Automaton a) {
		int numStates = a.getNumStates();
		BitSet liveSet = Operations.getLiveStates(a);
		int[] map = new int[numStates];
		Automaton result = new Automaton();
		for (int i = 0; i < numStates; i++) {
			if (liveSet.get(i)) {
				map[i] = result.createState();
				result.setAccept(map[i], a.isAccept(i));
			}
		}
		Transition t = new Transition();
		for (int i = 0; i < numStates; i++) {
			if (liveSet.get(i)) {
				int numTransitions = a.initTransition(i, t);
				for (int j = 0; j < numTransitions; j++) {
					a.getNextTransition(t);
					if (liveSet.get(t.dest)) {
						result.addTransition(map[i], map[t.dest], t.min, t.max);
					}
				}
			}
		}
		result.finishState();
		assert (Operations.hasDeadStates(result)) == false;
		return result;
	}

	public static boolean isFinite(Automaton a) {
		if ((a.getNumStates()) == 0) {
			return true;
		}
		return Operations.isFinite(new Transition(), a, 0, new BitSet(a.getNumStates()), new BitSet(a.getNumStates()), 0);
	}

	private static boolean isFinite(Transition scratch, Automaton a, int state, BitSet path, BitSet visited, int level) {
		if (level > (Operations.MAX_RECURSION_LEVEL)) {
			throw new IllegalArgumentException(("input automaton is too large: " + level));
		}
		path.set(state);
		int numTransitions = a.initTransition(state, scratch);
		for (int t = 0; t < numTransitions; t++) {
			a.getTransition(state, t, scratch);
			if ((path.get(scratch.dest)) || ((!(visited.get(scratch.dest))) && (!(Operations.isFinite(scratch, a, scratch.dest, path, visited, (level + 1)))))) {
				return false;
			}
		}
		path.clear(state);
		visited.set(state);
		return true;
	}

	public static String getCommonPrefix(Automaton a) {
		if ((a.isDeterministic()) == false) {
			throw new IllegalArgumentException("input automaton must be deterministic");
		}
		StringBuilder b = new StringBuilder();
		HashSet<Integer> visited = new HashSet<>();
		int s = 0;
		boolean done;
		Transition t = new Transition();
		do {
			done = true;
			visited.add(s);
			if (((a.isAccept(s)) == false) && ((a.getNumTransitions(s)) == 1)) {
				a.getTransition(s, 0, t);
				if (((t.min) == (t.max)) && (!(visited.contains(t.dest)))) {
					b.appendCodePoint(t.min);
					s = t.dest;
					done = false;
				}
			}
		} while (!done );
		return b.toString();
	}

	public static BytesRef getCommonPrefixBytesRef(Automaton a) {
		BytesRefBuilder builder = new BytesRefBuilder();
		HashSet<Integer> visited = new HashSet<>();
		int s = 0;
		boolean done;
		Transition t = new Transition();
		do {
			done = true;
			visited.add(s);
			if (((a.isAccept(s)) == false) && ((a.getNumTransitions(s)) == 1)) {
				a.getTransition(s, 0, t);
				if (((t.min) == (t.max)) && (!(visited.contains(t.dest)))) {
					builder.append(((byte) (t.min)));
					s = t.dest;
					done = false;
				}
			}
		} while (!done );
		return builder.get();
	}

	public static IntsRef getSingleton(Automaton a) {
		if ((a.isDeterministic()) == false) {
			throw new IllegalArgumentException("input automaton must be deterministic");
		}
		IntsRefBuilder builder = new IntsRefBuilder();
		HashSet<Integer> visited = new HashSet<>();
		int s = 0;
		Transition t = new Transition();
		while (true) {
			visited.add(s);
			if ((a.isAccept(s)) == false) {
				if ((a.getNumTransitions(s)) == 1) {
					a.getTransition(s, 0, t);
					if (((t.min) == (t.max)) && (!(visited.contains(t.dest)))) {
						builder.append(t.min);
						s = t.dest;
						continue;
					}
				}
			}else
				if ((a.getNumTransitions(s)) == 0) {
					return builder.get();
				}

			return null;
		} 
	}

	public static BytesRef getCommonSuffixBytesRef(Automaton a, int maxDeterminizedStates) {
		Automaton r = Operations.determinize(Operations.reverse(a), maxDeterminizedStates);
		BytesRef ref = Operations.getCommonPrefixBytesRef(r);
		Operations.reverseBytes(ref);
		return ref;
	}

	private static void reverseBytes(BytesRef ref) {
		if ((ref.length) <= 1)
			return;

		int num = (ref.length) >> 1;
		for (int i = ref.offset; i < ((ref.offset) + num); i++) {
			byte b = ref.bytes[i];
			ref.bytes[i] = ref.bytes[(((((ref.offset) * 2) + (ref.length)) - i) - 1)];
			ref.bytes[(((((ref.offset) * 2) + (ref.length)) - i) - 1)] = b;
		}
	}

	public static Automaton reverse(Automaton a) {
		return Operations.reverse(a, null);
	}

	static Automaton reverse(Automaton a, Set<Integer> initialStates) {
		if (Operations.isEmpty(a)) {
			return new Automaton();
		}
		int numStates = a.getNumStates();
		Automaton.Builder builder = new Automaton.Builder();
		builder.createState();
		for (int s = 0; s < numStates; s++) {
			builder.createState();
		}
		builder.setAccept(1, true);
		Transition t = new Transition();
		for (int s = 0; s < numStates; s++) {
			int numTransitions = a.getNumTransitions(s);
			a.initTransition(s, t);
			for (int i = 0; i < numTransitions; i++) {
				a.getNextTransition(t);
				builder.addTransition(((t.dest) + 1), (s + 1), t.min, t.max);
			}
		}
		Automaton result = builder.finish();
		int s = 0;
		result.finishState();
		return result;
	}

	static Automaton totalize(Automaton a) {
		Automaton result = new Automaton();
		int numStates = a.getNumStates();
		for (int i = 0; i < numStates; i++) {
			result.createState();
			result.setAccept(i, a.isAccept(i));
		}
		int deadState = result.createState();
		result.addTransition(deadState, deadState, Character.MIN_CODE_POINT, Character.MAX_CODE_POINT);
		Transition t = new Transition();
		for (int i = 0; i < numStates; i++) {
			int maxi = Character.MIN_CODE_POINT;
			int count = a.initTransition(i, t);
			for (int j = 0; j < count; j++) {
				a.getNextTransition(t);
				result.addTransition(i, t.dest, t.min, t.max);
				if ((t.min) > maxi) {
					result.addTransition(i, deadState, maxi, ((t.min) - 1));
				}
				if (((t.max) + 1) > maxi) {
					maxi = (t.max) + 1;
				}
			}
			if (maxi <= (Character.MAX_CODE_POINT)) {
				result.addTransition(i, deadState, maxi, Character.MAX_CODE_POINT);
			}
		}
		result.finishState();
		return result;
	}

	public static int[] topoSortStates(Automaton a) {
		if ((a.getNumStates()) == 0) {
			return new int[0];
		}
		int numStates = a.getNumStates();
		int[] states = new int[numStates];
		final BitSet visited = new BitSet(numStates);
		int upto = Operations.topoSortStatesRecurse(a, visited, states, 0, 0, 0);
		if (upto < (states.length)) {
			int[] newStates = new int[upto];
			System.arraycopy(states, 0, newStates, 0, upto);
			states = newStates;
		}
		for (int i = 0; i < ((states.length) / 2); i++) {
			int s = states[i];
			states[i] = states[(((states.length) - 1) - i)];
			states[(((states.length) - 1) - i)] = s;
		}
		return states;
	}

	private static int topoSortStatesRecurse(Automaton a, BitSet visited, int[] states, int upto, int state, int level) {
		if (level > (Operations.MAX_RECURSION_LEVEL)) {
			throw new IllegalArgumentException(("input automaton is too large: " + level));
		}
		Transition t = new Transition();
		int count = a.initTransition(state, t);
		for (int i = 0; i < count; i++) {
			a.getNextTransition(t);
			if (!(visited.get(t.dest))) {
				visited.set(t.dest);
				upto = Operations.topoSortStatesRecurse(a, visited, states, upto, t.dest, (level + 1));
			}
		}
		states[upto] = state;
		upto++;
		return upto;
	}
}

