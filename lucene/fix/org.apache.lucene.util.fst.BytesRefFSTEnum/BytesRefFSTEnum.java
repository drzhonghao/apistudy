

import java.io.IOException;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.fst.FST;


public final class BytesRefFSTEnum<T> {
	private final BytesRef current = new BytesRef(10);

	private final BytesRefFSTEnum.InputOutput<T> result = new BytesRefFSTEnum.InputOutput<>();

	private BytesRef target;

	public static class InputOutput<T> {
		public BytesRef input;

		public T output;
	}

	public BytesRefFSTEnum(FST<T> fst) {
		result.input = current;
		current.offset = 1;
	}

	public BytesRefFSTEnum.InputOutput<T> current() {
		return result;
	}

	public BytesRefFSTEnum.InputOutput<T> next() throws IOException {
		return setResult();
	}

	public BytesRefFSTEnum.InputOutput<T> seekCeil(BytesRef target) throws IOException {
		this.target = target;
		return setResult();
	}

	public BytesRefFSTEnum.InputOutput<T> seekFloor(BytesRef target) throws IOException {
		this.target = target;
		return setResult();
	}

	public BytesRefFSTEnum.InputOutput<T> seekExact(BytesRef target) throws IOException {
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

	private BytesRefFSTEnum.InputOutput<T> setResult() {
		return null;
	}
}

