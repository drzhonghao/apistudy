

import java.util.ArrayList;
import java.util.List;
import org.egothor.stemmer.Row;
import org.egothor.stemmer.Trie;


public class Reduce {
	public Reduce() {
	}

	public Trie optimize(Trie orig) {
		List<Row> rows = new ArrayList<>();
		return null;
	}

	List<Row> removeGaps(int ind, List<Row> old, List<Row> to, int[] remap) {
		remap[ind] = to.size();
		Row now = old.get(ind);
		to.add(now);
		to.set(remap[ind], new Reduce.Remap(now, remap));
		return to;
	}

	class Remap extends Row {
		public Remap(Row old, int[] remap) {
			super();
		}
	}
}

