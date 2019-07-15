

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import org.egothor.stemmer.Reduce;
import org.egothor.stemmer.Row;


public class Trie {
	List<Row> rows = new ArrayList<>();

	List<CharSequence> cmds = new ArrayList<>();

	int root;

	boolean forward = false;

	public Trie(DataInput is) throws IOException {
		forward = is.readBoolean();
		root = is.readInt();
		for (int i = is.readInt(); i > 0; i--) {
			cmds.add(is.readUTF());
		}
		for (int i = is.readInt(); i > 0; i--) {
			rows.add(new Row(is));
		}
	}

	public Trie(boolean forward) {
		rows.add(new Row());
		root = 0;
		this.forward = forward;
	}

	public Trie(boolean forward, int root, List<CharSequence> cmds, List<Row> rows) {
		this.rows = rows;
		this.cmds = cmds;
		this.root = root;
		this.forward = forward;
	}

	public CharSequence[] getAll(CharSequence key) {
		int[] res = new int[key.length()];
		int resc = 0;
		Row now = getRow(root);
		int w;
		Trie.StrEnum e = new Trie.StrEnum(key, forward);
		boolean br = false;
		for (int i = 0; i < ((key.length()) - 1); i++) {
			Character ch = new Character(e.next());
			w = now.getCmd(ch);
			if (w >= 0) {
				int n = w;
				for (int j = 0; j < resc; j++) {
					if (n == (res[j])) {
						n = -1;
						break;
					}
				}
				if (n >= 0) {
					res[(resc++)] = n;
				}
			}
			w = now.getRef(ch);
			if (w >= 0) {
				now = getRow(w);
			}else {
				br = true;
				break;
			}
		}
		if (br == false) {
			w = now.getCmd(new Character(e.next()));
			if (w >= 0) {
				int n = w;
				for (int j = 0; j < resc; j++) {
					if (n == (res[j])) {
						n = -1;
						break;
					}
				}
				if (n >= 0) {
					res[(resc++)] = n;
				}
			}
		}
		if (resc < 1) {
			return null;
		}
		CharSequence[] R = new CharSequence[resc];
		for (int j = 0; j < resc; j++) {
			R[j] = cmds.get(res[j]);
		}
		return R;
	}

	public int getCells() {
		int size = 0;
		for (Row row : rows)
			size += row.getCells();

		return size;
	}

	public int getCellsPnt() {
		int size = 0;
		for (Row row : rows)
			size += row.getCellsPnt();

		return size;
	}

	public int getCellsVal() {
		int size = 0;
		for (Row row : rows)
			size += row.getCellsVal();

		return size;
	}

	public CharSequence getFully(CharSequence key) {
		Row now = getRow(root);
		int w;
		int cmd = -1;
		Trie.StrEnum e = new Trie.StrEnum(key, forward);
		Character ch = null;
		Character aux = null;
		for (int i = 0; i < (key.length());) {
			ch = new Character(e.next());
			i++;
			w = now.getRef(ch);
			if (w >= 0) {
				now = getRow(w);
			}else
				if (i < (key.length())) {
					return null;
				}

		}
		return cmd == (-1) ? null : cmds.get(cmd);
	}

	public CharSequence getLastOnPath(CharSequence key) {
		Row now = getRow(root);
		int w;
		CharSequence last = null;
		Trie.StrEnum e = new Trie.StrEnum(key, forward);
		for (int i = 0; i < ((key.length()) - 1); i++) {
			Character ch = new Character(e.next());
			w = now.getCmd(ch);
			if (w >= 0) {
				last = cmds.get(w);
			}
			w = now.getRef(ch);
			if (w >= 0) {
				now = getRow(w);
			}else {
				return last;
			}
		}
		w = now.getCmd(new Character(e.next()));
		return w >= 0 ? cmds.get(w) : last;
	}

	private Row getRow(int index) {
		if ((index < 0) || (index >= (rows.size()))) {
			return null;
		}
		return rows.get(index);
	}

	public void store(DataOutput os) throws IOException {
		os.writeBoolean(forward);
		os.writeInt(root);
		os.writeInt(cmds.size());
		for (CharSequence cmd : cmds)
			os.writeUTF(cmd.toString());

		os.writeInt(rows.size());
		for (Row row : rows)
			row.store(os);

	}

	void add(CharSequence key, CharSequence cmd) {
		if ((key == null) || (cmd == null)) {
			return;
		}
		if ((cmd.length()) == 0) {
			return;
		}
		int id_cmd = cmds.indexOf(cmd);
		if (id_cmd == (-1)) {
			id_cmd = cmds.size();
			cmds.add(cmd);
		}
		int node = root;
		Row r = getRow(node);
		Trie.StrEnum e = new Trie.StrEnum(key, forward);
		for (int i = 0; i < ((e.length()) - 1); i++) {
			Character ch = new Character(e.next());
			node = r.getRef(ch);
			if (node >= 0) {
				r = getRow(node);
			}else {
				node = rows.size();
				Row n;
				rows.add((n = new Row()));
				r.setRef(ch, node);
				r = n;
			}
		}
		r.setCmd(new Character(e.next()), id_cmd);
	}

	public Trie reduce(Reduce by) {
		return null;
	}

	public void printInfo(PrintStream out, CharSequence prefix) {
		out.println(((((((((((prefix + "nds ") + (rows.size())) + " cmds ") + (cmds.size())) + " cells ") + (getCells())) + " valcells ") + (getCellsVal())) + " pntcells ") + (getCellsPnt())));
	}

	class StrEnum {
		CharSequence s;

		int from;

		int by;

		StrEnum(CharSequence s, boolean up) {
			this.s = s;
			if (up) {
				from = 0;
				by = 1;
			}else {
				from = (s.length()) - 1;
				by = -1;
			}
		}

		int length() {
			return s.length();
		}

		char next() {
			char ch = s.charAt(from);
			from += by;
			return ch;
		}
	}
}

