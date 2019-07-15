

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefBuilder;
import org.apache.lucene.util.IntsRef;
import org.apache.lucene.util.StringHelper;
import org.apache.lucene.util.UnicodeUtil;
import org.apache.lucene.util.automaton.Automaton;
import org.apache.lucene.util.automaton.ByteRunAutomaton;
import org.apache.lucene.util.automaton.Operations;
import org.apache.lucene.util.automaton.RunAutomaton;
import org.apache.lucene.util.automaton.Transition;
import org.apache.lucene.util.automaton.UTF32ToUTF8;


public class CompiledAutomaton {
	public enum AUTOMATON_TYPE {

		NONE,
		ALL,
		SINGLE,
		NORMAL;}

	public final CompiledAutomaton.AUTOMATON_TYPE type;

	public final BytesRef term;

	public final ByteRunAutomaton runAutomaton;

	public Automaton automaton = null;

	public final BytesRef commonSuffixRef;

	public final Boolean finite;

	public final int sinkState;

	public CompiledAutomaton(Automaton automaton) {
		this(automaton, null, true);
	}

	private static int findSinkState(Automaton automaton) {
		int numStates = automaton.getNumStates();
		Transition t = new Transition();
		int foundState = -1;
		for (int s = 0; s < numStates; s++) {
			if (automaton.isAccept(s)) {
				int count = automaton.initTransition(s, t);
				boolean isSinkState = false;
				for (int i = 0; i < count; i++) {
					automaton.getNextTransition(t);
					if ((((t.dest) == s) && ((t.min) == 0)) && ((t.max) == 255)) {
						isSinkState = true;
						break;
					}
				}
				if (isSinkState) {
					foundState = s;
					break;
				}
			}
		}
		return foundState;
	}

	public CompiledAutomaton(Automaton automaton, Boolean finite, boolean simplify) {
		this(automaton, finite, simplify, Operations.DEFAULT_MAX_DETERMINIZED_STATES, false);
	}

	public CompiledAutomaton(Automaton automaton, Boolean finite, boolean simplify, int maxDeterminizedStates, boolean isBinary) {
		if ((automaton.getNumStates()) == 0) {
			automaton = new Automaton();
			automaton.createState();
		}
		if (simplify) {
			if (Operations.isEmpty(automaton)) {
				type = CompiledAutomaton.AUTOMATON_TYPE.NONE;
				term = null;
				commonSuffixRef = null;
				runAutomaton = null;
				this.automaton = null;
				this.finite = null;
				sinkState = -1;
				return;
			}
			boolean isTotal;
			if (isBinary) {
				isTotal = Operations.isTotal(automaton, 0, 255);
			}else {
				isTotal = Operations.isTotal(automaton);
			}
			if (isTotal) {
				type = CompiledAutomaton.AUTOMATON_TYPE.ALL;
				term = null;
				commonSuffixRef = null;
				runAutomaton = null;
				this.automaton = null;
				this.finite = null;
				sinkState = -1;
				return;
			}
			automaton = Operations.determinize(automaton, maxDeterminizedStates);
			IntsRef singleton = Operations.getSingleton(automaton);
			if (singleton != null) {
				type = CompiledAutomaton.AUTOMATON_TYPE.SINGLE;
				commonSuffixRef = null;
				runAutomaton = null;
				this.automaton = null;
				this.finite = null;
				if (isBinary) {
					term = StringHelper.intsRefToBytesRef(singleton);
				}else {
					term = new BytesRef(UnicodeUtil.newString(singleton.ints, singleton.offset, singleton.length));
				}
				sinkState = -1;
				return;
			}
		}
		type = CompiledAutomaton.AUTOMATON_TYPE.NORMAL;
		term = null;
		if (finite == null) {
			this.finite = Operations.isFinite(automaton);
		}else {
			this.finite = finite;
		}
		Automaton binary;
		if (isBinary) {
			binary = automaton;
		}else {
			binary = new UTF32ToUTF8().convert(automaton);
		}
		if (this.finite) {
			commonSuffixRef = null;
		}else {
			BytesRef suffix = Operations.getCommonSuffixBytesRef(binary, maxDeterminizedStates);
			if ((suffix.length) == 0) {
				commonSuffixRef = null;
			}else {
				commonSuffixRef = suffix;
			}
		}
		runAutomaton = new ByteRunAutomaton(binary, true, maxDeterminizedStates);
		sinkState = CompiledAutomaton.findSinkState(this.automaton);
		automaton = null;
	}

	private Transition transition = new Transition();

	private BytesRef addTail(int state, BytesRefBuilder term, int idx, int leadLabel) {
		int maxIndex = -1;
		int numTransitions = automaton.initTransition(state, transition);
		for (int i = 0; i < numTransitions; i++) {
			automaton.getNextTransition(transition);
			if ((transition.min) < leadLabel) {
				maxIndex = i;
			}else {
				break;
			}
		}
		assert maxIndex != (-1);
		automaton.getTransition(state, maxIndex, transition);
		final int floorLabel;
		if ((transition.max) > (leadLabel - 1)) {
			floorLabel = leadLabel - 1;
		}else {
			floorLabel = transition.max;
		}
		term.grow((1 + idx));
		term.setByteAt(idx, ((byte) (floorLabel)));
		state = transition.dest;
		idx++;
		while (true) {
			numTransitions = automaton.getNumTransitions(state);
			if (numTransitions == 0) {
				assert runAutomaton.isAccept(state);
				term.setLength(idx);
				return term.get();
			}else {
				automaton.getTransition(state, (numTransitions - 1), transition);
				term.grow((1 + idx));
				term.setByteAt(idx, ((byte) (transition.max)));
				state = transition.dest;
				idx++;
			}
		} 
	}

	public TermsEnum getTermsEnum(Terms terms) throws IOException {
		return null;
	}

	public BytesRef floor(BytesRef input, BytesRefBuilder output) {
		int state = 0;
		if ((input.length) == 0) {
			if (runAutomaton.isAccept(state)) {
				output.clear();
				return output.get();
			}else {
				return null;
			}
		}
		final List<Integer> stack = new ArrayList<>();
		int idx = 0;
		while (true) {
			int label = (input.bytes[((input.offset) + idx)]) & 255;
			int nextState = runAutomaton.step(state, label);
			if (idx == ((input.length) - 1)) {
				if ((nextState != (-1)) && (runAutomaton.isAccept(nextState))) {
					output.grow((1 + idx));
					output.setByteAt(idx, ((byte) (label)));
					output.setLength(input.length);
					return output.get();
				}else {
					nextState = -1;
				}
			}
			if (nextState == (-1)) {
				while (true) {
					int numTransitions = automaton.getNumTransitions(state);
					if (numTransitions == 0) {
						assert runAutomaton.isAccept(state);
						output.setLength(idx);
						return output.get();
					}else {
						automaton.getTransition(state, 0, transition);
						if ((label - 1) < (transition.min)) {
							if (runAutomaton.isAccept(state)) {
								output.setLength(idx);
								return output.get();
							}
							if ((stack.size()) == 0) {
								return null;
							}else {
								state = stack.remove(((stack.size()) - 1));
								idx--;
								label = (input.bytes[((input.offset) + idx)]) & 255;
							}
						}else {
							break;
						}
					}
				} 
				return addTail(state, output, idx, label);
			}else {
				output.grow((1 + idx));
				output.setByteAt(idx, ((byte) (label)));
				stack.add(state);
				state = nextState;
				idx++;
			}
		} 
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = (prime * result) + ((runAutomaton) == null ? 0 : runAutomaton.hashCode());
		result = (prime * result) + ((term) == null ? 0 : term.hashCode());
		result = (prime * result) + ((type) == null ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ((this) == obj)
			return true;

		if (obj == null)
			return false;

		if ((getClass()) != (obj.getClass()))
			return false;

		CompiledAutomaton other = ((CompiledAutomaton) (obj));
		if ((type) != (other.type))
			return false;

		if ((type) == (CompiledAutomaton.AUTOMATON_TYPE.SINGLE)) {
			if (!(term.equals(other.term)))
				return false;

		}else
			if ((type) == (CompiledAutomaton.AUTOMATON_TYPE.NORMAL)) {
				if (!(runAutomaton.equals(other.runAutomaton)))
					return false;

			}

		return true;
	}
}

