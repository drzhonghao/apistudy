

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import org.egothor.stemmer.Reduce;
import org.egothor.stemmer.Trie;


public class MultiTrie extends Trie {
	final char EOM = '*';

	final String EOM_NODE = "" + (EOM);

	List<Trie> tries = new ArrayList<>();

	int BY = 1;

	public MultiTrie(DataInput is) throws IOException {
		super(false);
		BY = is.readInt();
		for (int i = is.readInt(); i > 0; i--) {
			tries.add(new Trie(is));
		}
	}

	public MultiTrie(boolean forward) {
		super(forward);
	}

	@Override
	public CharSequence getFully(CharSequence key) {
		StringBuilder result = new StringBuilder(((tries.size()) * 2));
		for (int i = 0; i < (tries.size()); i++) {
			CharSequence r = tries.get(i).getFully(key);
			if ((r == null) || (((r.length()) == 1) && ((r.charAt(0)) == (EOM)))) {
				return result;
			}
			result.append(r);
		}
		return result;
	}

	@Override
	public CharSequence getLastOnPath(CharSequence key) {
		StringBuilder result = new StringBuilder(((tries.size()) * 2));
		for (int i = 0; i < (tries.size()); i++) {
			CharSequence r = tries.get(i).getLastOnPath(key);
			if ((r == null) || (((r.length()) == 1) && ((r.charAt(0)) == (EOM)))) {
				return result;
			}
			result.append(r);
		}
		return result;
	}

	@Override
	public void store(DataOutput os) throws IOException {
		os.writeInt(BY);
		os.writeInt(tries.size());
		for (Trie trie : tries)
			trie.store(os);

	}

	public void add(CharSequence key, CharSequence cmd) {
		if ((cmd.length()) == 0) {
			return;
		}
		int levels = (cmd.length()) / (BY);
		while (levels >= (tries.size())) {
		} 
		for (int i = 0; i < levels; i++) {
		}
	}

	@Override
	public Trie reduce(Reduce by) {
		List<Trie> h = new ArrayList<>();
		for (Trie trie : tries)
			h.add(trie.reduce(by));

		return null;
	}

	@Override
	public void printInfo(PrintStream out, CharSequence prefix) {
		int c = 0;
		for (Trie trie : tries)
			trie.printInfo(out, (((prefix + "[") + (++c)) + "] "));

	}
}

