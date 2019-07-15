

import org.apache.lucene.util.automaton.Automaton;
import org.apache.lucene.util.automaton.Operations;
import org.apache.lucene.util.automaton.RunAutomaton;
import org.apache.lucene.util.automaton.UTF32ToUTF8;


public class ByteRunAutomaton extends RunAutomaton {
	public ByteRunAutomaton(Automaton a) {
		this(a, false, Operations.DEFAULT_MAX_DETERMINIZED_STATES);
	}

	public ByteRunAutomaton(Automaton a, boolean isBinary, int maxDeterminizedStates) {
		super((isBinary ? a : new UTF32ToUTF8().convert(a)), 256, maxDeterminizedStates);
	}

	public boolean run(byte[] s, int offset, int length) {
		int p = 0;
		int l = offset + length;
		for (int i = offset; i < l; i++) {
			p = step(p, ((s[i]) & 255));
			if (p == (-1))
				return false;

		}
		return false;
	}
}

