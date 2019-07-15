

import java.io.IOException;
import java.util.Objects;
import org.apache.lucene.search.spans.SpanCollector;
import org.apache.lucene.search.spans.Spans;


abstract class ContainSpans {
	Spans sourceSpans;

	Spans bigSpans;

	Spans littleSpans;

	ContainSpans(Spans bigSpans, Spans littleSpans, Spans sourceSpans) {
		this.bigSpans = Objects.requireNonNull(bigSpans);
		this.littleSpans = Objects.requireNonNull(littleSpans);
		this.sourceSpans = Objects.requireNonNull(sourceSpans);
	}

	public int startPosition() {
		return 0;
	}

	public int endPosition() {
		return 0;
	}

	public int width() {
		return sourceSpans.width();
	}

	public void collect(SpanCollector collector) throws IOException {
		bigSpans.collect(collector);
		littleSpans.collect(collector);
	}
}

