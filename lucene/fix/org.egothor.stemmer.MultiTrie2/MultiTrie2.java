

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.egothor.stemmer.MultiTrie;
import org.egothor.stemmer.Reduce;
import org.egothor.stemmer.Trie;


public class MultiTrie2 extends MultiTrie {
	public MultiTrie2(DataInput is) throws IOException {
		super(is);
	}

	public MultiTrie2(boolean forward) {
		super(forward);
	}

	@Override
	public CharSequence getFully(CharSequence key) {
		try {
			CharSequence lastkey = key;
			char lastch = ' ';
		} catch (IndexOutOfBoundsException x) {
		}
		return null;
	}

	@Override
	public CharSequence getLastOnPath(CharSequence key) {
		try {
			CharSequence lastkey = key;
			char lastch = ' ';
		} catch (IndexOutOfBoundsException x) {
		}
		return null;
	}

	@Override
	public void store(DataOutput os) throws IOException {
		super.store(os);
	}

	@Override
	public void add(CharSequence key, CharSequence cmd) {
		if ((cmd.length()) == 0) {
			return;
		}
		CharSequence[] p = decompose(cmd);
		int levels = p.length;
		CharSequence lastkey = key;
		for (int i = 0; i < levels; i++) {
			if ((key.length()) > 0) {
				lastkey = key;
			}else {
			}
			if (((p[i].length()) > 0) && ((p[i].charAt(0)) == '-')) {
				if (i > 0) {
					key = skip(key, lengthPP(p[(i - 1)]));
				}
				key = skip(key, lengthPP(p[i]));
			}
		}
		if ((key.length()) > 0) {
		}else {
		}
	}

	public CharSequence[] decompose(CharSequence cmd) {
		int parts = 0;
		for (int i = 0; (0 <= i) && (i < (cmd.length()));) {
			int next = dashEven(cmd, i);
			if (i == next) {
				parts++;
				i = next + 2;
			}else {
				parts++;
				i = next;
			}
		}
		CharSequence[] part = new CharSequence[parts];
		int x = 0;
		for (int i = 0; (0 <= i) && (i < (cmd.length()));) {
			int next = dashEven(cmd, i);
			if (i == next) {
				part[(x++)] = cmd.subSequence(i, (i + 2));
				i = next + 2;
			}else {
				part[(x++)] = (next < 0) ? cmd.subSequence(i, cmd.length()) : cmd.subSequence(i, next);
				i = next;
			}
		}
		return part;
	}

	@Override
	public Trie reduce(Reduce by) {
		List<Trie> h = new ArrayList<>();
		return null;
	}

	private boolean cannotFollow(char after, char goes) {
		switch (after) {
			case '-' :
			case 'D' :
				return after == goes;
		}
		return false;
	}

	private CharSequence skip(CharSequence in, int count) {
		return null;
	}

	private int dashEven(CharSequence in, int from) {
		while (from < (in.length())) {
			if ((in.charAt(from)) == '-') {
				return from;
			}else {
				from += 2;
			}
		} 
		return -1;
	}

	@SuppressWarnings("fallthrough")
	private int lengthPP(CharSequence cmd) {
		int len = 0;
		for (int i = 0; i < (cmd.length()); i++) {
			switch (cmd.charAt((i++))) {
				case '-' :
				case 'D' :
					len += ((cmd.charAt(i)) - 'a') + 1;
					break;
				case 'R' :
					len++;
				case 'I' :
					break;
			}
		}
		return len;
	}
}

