

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.PrintStream;


public class Row {
	int uniformCnt = 0;

	int uniformSkip = 0;

	public Row(DataInput is) throws IOException {
		for (int i = is.readInt(); i > 0; i--) {
			char ch = is.readChar();
		}
	}

	public Row() {
	}

	public Row(Row old) {
	}

	public void setCmd(Character way, int cmd) {
	}

	public void setRef(Character way, int ref) {
	}

	public int getCells() {
		int size = 0;
		return size;
	}

	public int getCellsPnt() {
		int size = 0;
		return size;
	}

	public int getCellsVal() {
		int size = 0;
		return size;
	}

	public int getCmd(Character way) {
		return 0;
	}

	public int getCnt(Character way) {
		return 0;
	}

	public int getRef(Character way) {
		return 0;
	}

	public void store(DataOutput os) throws IOException {
	}

	public int uniformCmd(boolean eqSkip) {
		int ret = -1;
		uniformCnt = 1;
		uniformSkip = 0;
		return ret;
	}

	public void print(PrintStream out) {
		out.println();
	}
}

