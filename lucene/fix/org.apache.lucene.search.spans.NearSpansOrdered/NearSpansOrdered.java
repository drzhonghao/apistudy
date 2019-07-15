

import java.io.IOException;
import java.util.List;
import org.apache.lucene.search.spans.SpanCollector;
import org.apache.lucene.search.spans.Spans;


public class NearSpansOrdered {
	protected int matchStart = -1;

	protected int matchEnd = -1;

	protected int matchWidth = -1;

	private final int allowedSlop;

	public NearSpansOrdered(int allowedSlop, List<Spans> subSpans) throws IOException {
		this.allowedSlop = allowedSlop;
	}

	boolean twoPhaseCurrentDocMatches() throws IOException {
		assert unpositioned();
		return false;
	}

	private boolean unpositioned() {
		return true;
	}

	public int nextStartPosition() throws IOException {
		return matchStart = matchEnd = Spans.NO_MORE_POSITIONS;
	}

	private boolean stretchToOrder() throws IOException {
		matchWidth = 0;
		return true;
	}

	private static int advancePosition(Spans spans, int position) throws IOException {
		while ((spans.startPosition()) < position) {
			spans.nextStartPosition();
		} 
		return spans.startPosition();
	}

	public int startPosition() {
		return 0;
	}

	public int endPosition() {
		return 0;
	}

	public int width() {
		return matchWidth;
	}

	public void collect(SpanCollector collector) throws IOException {
	}
}

