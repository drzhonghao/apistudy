

import java.io.IOException;
import org.apache.lucene.index.AutomatonTermsEnum;
import org.apache.lucene.index.BinaryDocValues;
import org.apache.lucene.index.SingleTermsEnum;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.automaton.CompiledAutomaton;

import static org.apache.lucene.util.automaton.CompiledAutomaton.AUTOMATON_TYPE.ALL;
import static org.apache.lucene.util.automaton.CompiledAutomaton.AUTOMATON_TYPE.NONE;
import static org.apache.lucene.util.automaton.CompiledAutomaton.AUTOMATON_TYPE.NORMAL;
import static org.apache.lucene.util.automaton.CompiledAutomaton.AUTOMATON_TYPE.SINGLE;


public abstract class SortedDocValues extends BinaryDocValues {
	protected SortedDocValues() {
	}

	public abstract int ordValue() throws IOException;

	public abstract BytesRef lookupOrd(int ord) throws IOException;

	private final BytesRef empty = new BytesRef();

	@Override
	public BytesRef binaryValue() throws IOException {
		int ord = ordValue();
		if (ord == (-1)) {
			return empty;
		}else {
			return lookupOrd(ord);
		}
	}

	public abstract int getValueCount();

	public int lookupTerm(BytesRef key) throws IOException {
		int low = 0;
		int high = (getValueCount()) - 1;
		while (low <= high) {
			int mid = (low + high) >>> 1;
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

