

import java.io.IOException;
import org.apache.lucene.index.AutomatonTermsEnum;
import org.apache.lucene.index.SingleTermsEnum;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.automaton.CompiledAutomaton;

import static org.apache.lucene.util.automaton.CompiledAutomaton.AUTOMATON_TYPE.ALL;
import static org.apache.lucene.util.automaton.CompiledAutomaton.AUTOMATON_TYPE.NONE;
import static org.apache.lucene.util.automaton.CompiledAutomaton.AUTOMATON_TYPE.NORMAL;
import static org.apache.lucene.util.automaton.CompiledAutomaton.AUTOMATON_TYPE.SINGLE;


public abstract class SortedSetDocValues {
	protected SortedSetDocValues() {
	}

	public static final long NO_MORE_ORDS = -1;

	public abstract long nextOrd() throws IOException;

	public abstract BytesRef lookupOrd(long ord) throws IOException;

	public abstract long getValueCount();

	public long lookupTerm(BytesRef key) throws IOException {
		long low = 0;
		long high = (getValueCount()) - 1;
		while (low <= high) {
			long mid = (low + high) >>> 1;
			final BytesRef term = lookupOrd(mid);
			int cmp = term.compareTo(key);
			if (cmp < 0) {
				low = mid + 1;
			}else
				if (cmp > 0) {
					high = mid - 1;
				}else {
					return mid;
				}

		} 
		return -(low + 1);
	}

	public TermsEnum termsEnum() throws IOException {
		return null;
	}

	public TermsEnum intersect(CompiledAutomaton automaton) throws IOException {
		TermsEnum in = termsEnum();
		switch (automaton.type) {
			case NONE :
				return TermsEnum.EMPTY;
			case ALL :
				return in;
			case SINGLE :
				return new SingleTermsEnum(in, automaton.term);
			case NORMAL :
				return new AutomatonTermsEnum(in, automaton);
			default :
				throw new RuntimeException("unhandled case");
		}
	}
}

