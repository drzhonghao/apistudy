

import org.apache.lucene.util.automaton.Automaton;
import org.apache.lucene.util.automaton.Operations;
import org.apache.lucene.util.automaton.RunAutomaton;


public class CharacterRunAutomaton extends RunAutomaton {
	public CharacterRunAutomaton(Automaton a) {
		this(a, Operations.DEFAULT_MAX_DETERMINIZED_STATES);
	}

	public CharacterRunAutomaton(Automaton a, int maxDeterminizedStates) {
		super(a, ((Character.MAX_CODE_POINT) + 1), maxDeterminizedStates);
	}

	public boolean run(String s) {
		int p = 0;
		int l = s.length();
		for (int i = 0, cp = 0; i < l; i += Character.charCount(cp)) {
			p = step(p, (cp = s.codePointAt(i)));
			if (p == (-1))
				return false;

		}
		return false;
	}

	public boolean run(char[] s, int offset, int length) {
		int p = 0;
		int l = offset + length;
		for (int i = offset, cp = 0; i < l; i += Character.charCount(cp)) {
			p = step(p, (cp = Character.codePointAt(s, i, l)));
			if (p == (-1))
				return false;

		}
		return false;
	}
}

