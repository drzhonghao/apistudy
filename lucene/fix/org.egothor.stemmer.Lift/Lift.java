

import java.util.ArrayList;
import java.util.List;
import org.egothor.stemmer.Reduce;
import org.egothor.stemmer.Row;
import org.egothor.stemmer.Trie;


public class Lift extends Reduce {
	boolean changeSkip;

	public Lift(boolean changeSkip) {
		this.changeSkip = changeSkip;
	}

	@Override
	public Trie optimize(Trie orig) {
		List<Row> rows = new ArrayList<>();
		return null;
	}

	public void liftUp(Row in, List<Row> nodes) {
	}
}

