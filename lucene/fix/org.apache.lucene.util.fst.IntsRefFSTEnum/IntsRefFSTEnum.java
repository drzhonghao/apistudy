

import java.io.IOException;
import org.apache.lucene.util.IntsRef;
import org.apache.lucene.util.fst.FST;


public final class IntsRefFSTEnum<T> {
	private final IntsRef current = new IntsRef(10);

	private final IntsRefFSTEnum.InputOutput<T> result = new IntsRefFSTEnum.InputOutput<>();

	private IntsRef target;

	public static class InputOutput<T> {
		public IntsRef input;

		public T output;
	}

	public IntsRefFSTEnum(FST<T> fst) {
		result.input = current;
		current.offset = 1;
	}

	public IntsRefFSTEnum.InputOutput<T> current() {
		return result;
	}

	public IntsRefFSTEnum.InputOutput<T> next() throws IOException {
		return setResult();
	}

	public IntsRefFSTEnum.InputOutput<T> seekCeil(IntsRef target) throws IOException {
		this.target = target;
		return setResult();
	}

	public IntsRefFSTEnum.InputOutput<T> seekFloor(IntsRef target) throws IOException {
		this.target = target;
		return setResult();
	}

	public IntsRefFSTEnum.InputOutput<T> seekExact(IntsRef target) throws IOException {
		this.target = target;
		return null;
	}

	protected int getTargetLabel() {
		return 0;
	}

	protected int getCurrentLabel() {
		return 0;
	}

	protected void setCurrentLabel(int label) {
	}

	protected void grow() {
	}

	private IntsRefFSTEnum.InputOutput<T> setResult() {
		return null;
	}
}

