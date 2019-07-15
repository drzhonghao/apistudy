

import java.util.Arrays;
import org.apache.lucene.util.automaton.Automaton;
import org.apache.lucene.util.automaton.Operations;


public abstract class RunAutomaton {
	final Automaton automaton;

	final int alphabetSize;

	final int size;

	final boolean[] accept;

	final int[] transitions;

	int[] points = null;

	final int[] classmap;

	protected RunAutomaton(Automaton a, int alphabetSize) {
		this(a, alphabetSize, Operations.DEFAULT_MAX_DETERMINIZED_STATES);
	}

	protected RunAutomaton(Automaton a, int alphabetSize, int maxDeterminizedStates) {
		this.alphabetSize = alphabetSize;
		a = Operations.determinize(a, maxDeterminizedStates);
		this.automaton = a;
		size = Math.max(1, a.getNumStates());
		accept = new boolean[size];
		transitions = new int[(size) * (points.length)];
		Arrays.fill(transitions, (-1));
		for (int n = 0; n < (size); n++) {
			accept[n] = a.isAccept(n);
			for (int c = 0; c < (points.length); c++) {
				int dest = a.step(n, points[c]);
				assert (dest == (-1)) || (dest < (size));
				transitions[((n * (points.length)) + c)] = dest;
			}
		}
		classmap = new int[Math.min(256, alphabetSize)];
		int i = 0;
		for (int j = 0; j < (classmap.length); j++) {
			if (((i + 1) < (points.length)) && (j == (points[(i + 1)]))) {
				i++;
			}
			classmap[j] = i;
		}
		points = null;
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("initial state: 0\n");
		for (int i = 0; i < (size); i++) {
			b.append(("state " + i));
			if (accept[i])
				b.append(" [accept]:\n");
			else
				b.append(" [reject]:\n");

			for (int j = 0; j < (points.length); j++) {
				int k = transitions[((i * (points.length)) + j)];
				if (k != (-1)) {
					int min = points[j];
					int max;
					if ((j + 1) < (points.length))
						max = (points[(j + 1)]) - 1;
					else
						max = alphabetSize;

					b.append(" ");
					if (min != max) {
						b.append("-");
					}
					b.append(" -> ").append(k).append("\n");
				}
			}
		}
		return b.toString();
	}

	public final int getSize() {
		return size;
	}

	public final boolean isAccept(int state) {
		return accept[state];
	}

	public final int[] getCharIntervals() {
		return points.clone();
	}

	final int getCharClass(int c) {
		int a = 0;
		int b = points.length;
		while ((b - a) > 1) {
			int d = (a + b) >>> 1;
			if ((points[d]) > c)
				b = d;
			else
				if ((points[d]) < c)
					a = d;
				else
					return d;


		} 
		return a;
	}

	public final int step(int state, int c) {
		assert c < (alphabetSize);
		if (c >= (classmap.length)) {
			return transitions[((state * (points.length)) + (getCharClass(c)))];
		}else {
			return transitions[((state * (points.length)) + (classmap[c]))];
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = (prime * result) + (alphabetSize);
		result = (prime * result) + (points.length);
		result = (prime * result) + (size);
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

		RunAutomaton other = ((RunAutomaton) (obj));
		if ((alphabetSize) != (other.alphabetSize))
			return false;

		if ((size) != (other.size))
			return false;

		if (!(Arrays.equals(points, other.points)))
			return false;

		if (!(Arrays.equals(accept, other.accept)))
			return false;

		if (!(Arrays.equals(transitions, other.transitions)))
			return false;

		return true;
	}
}

