

import java.util.ArrayList;
import java.util.List;
import org.egothor.stemmer.Reduce;
import org.egothor.stemmer.Row;
import org.egothor.stemmer.Trie;


public class Optimizer extends Reduce {
	public Optimizer() {
	}

	@Override
	public Trie optimize(Trie orig) {
		List<Row> rows = new ArrayList<>();
		return null;
	}

	public Row merge(Row master, Row existing) {
		Row n = new Row();
		return n;
	}
}

