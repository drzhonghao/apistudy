

import org.apache.poi.util.Internal;


@Internal
public class CellNumberStringMod implements Comparable<CellNumberStringMod> {
	public static final int BEFORE = 1;

	public static final int AFTER = 2;

	public static final int REPLACE = 3;

	private final int op = 0;

	private CharSequence toAdd;

	private boolean startInclusive;

	private boolean endInclusive;

	@Override
	public int compareTo(CellNumberStringMod that) {
		return 0;
	}

	@Override
	public boolean equals(Object that) {
		return (that instanceof CellNumberStringMod) && ((compareTo(((CellNumberStringMod) (that)))) == 0);
	}

	@Override
	public int hashCode() {
		return 0;
	}

	public int getOp() {
		return op;
	}

	public CharSequence getToAdd() {
		return toAdd;
	}

	public boolean isStartInclusive() {
		return startInclusive;
	}

	public boolean isEndInclusive() {
		return endInclusive;
	}
}

