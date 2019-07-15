

import java.io.IOException;
import org.apache.lucene.codecs.blocktree.FieldReader;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.RamUsageEstimator;
import org.apache.lucene.util.automaton.Automaton;
import org.apache.lucene.util.automaton.RunAutomaton;
import org.apache.lucene.util.automaton.Transition;
import org.apache.lucene.util.fst.ByteSequenceOutputs;
import org.apache.lucene.util.fst.FST;
import org.apache.lucene.util.fst.Outputs;


final class IntersectTermsEnum extends TermsEnum {
	final IndexInput in;

	static final Outputs<BytesRef> fstOutputs = ByteSequenceOutputs.getSingleton();

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private FST.Arc<BytesRef>[] arcs = new FST.Arc[5];

	final RunAutomaton runAutomaton;

	final Automaton automaton;

	final BytesRef commonSuffix;

	private Transition currentTransition;

	private final BytesRef term = new BytesRef();

	private final FST.BytesReader fstReader;

	private final boolean allowAutoPrefixTerms;

	final FieldReader fr;

	private final int sinkState;

	private BytesRef savedStartTerm;

	private boolean useAutoPrefixTerm;

	public IntersectTermsEnum(FieldReader fr, Automaton automaton, RunAutomaton runAutomaton, BytesRef commonSuffix, BytesRef startTerm, int sinkState) throws IOException {
		this.fr = fr;
		this.sinkState = sinkState;
		assert automaton != null;
		assert runAutomaton != null;
		this.runAutomaton = runAutomaton;
		this.allowAutoPrefixTerms = sinkState != (-1);
		this.automaton = automaton;
		this.commonSuffix = commonSuffix;
		for (int arcIdx = 0; arcIdx < (arcs.length); arcIdx++) {
			arcs[arcIdx] = new FST.Arc<>();
		}
		assert setSavedStartTerm(startTerm);
		if (startTerm != null) {
			seekToStartTerm(startTerm);
		}
		fstReader = null;
		in = null;
	}

	private boolean setSavedStartTerm(BytesRef startTerm) {
		savedStartTerm = (startTerm == null) ? null : BytesRef.deepCopyOf(startTerm);
		return true;
	}

	private FST.Arc<BytesRef> getArc(int ord) {
		if (ord >= (arcs.length)) {
			@SuppressWarnings({ "rawtypes", "unchecked" })
			final FST.Arc<BytesRef>[] next = new FST.Arc[ArrayUtil.oversize((1 + ord), RamUsageEstimator.NUM_BYTES_OBJECT_REF)];
			System.arraycopy(arcs, 0, next, 0, arcs.length);
			for (int arcOrd = arcs.length; arcOrd < (next.length); arcOrd++) {
				next[arcOrd] = new FST.Arc<>();
			}
			arcs = next;
		}
		return arcs[ord];
	}

	@Override
	public BytesRef term() {
		return term;
	}

	@Override
	public int docFreq() throws IOException {
		return 0;
	}

	@Override
	public long totalTermFreq() throws IOException {
		return 0L;
	}

	@Override
	public PostingsEnum postings(PostingsEnum reuse, int flags) throws IOException {
		return null;
	}

	private int getState() {
		return 0;
	}

	private void seekToStartTerm(BytesRef target) throws IOException {
		if ((term.length) < (target.length)) {
			term.bytes = ArrayUtil.grow(term.bytes, target.length);
		}
		FST.Arc<BytesRef> arc = arcs[0];
		for (int idx = 0; idx <= (target.length); idx++) {
			while (true) {
				if ((term.bytes.length) < (term.length)) {
					term.bytes = ArrayUtil.grow(term.bytes, term.length);
				}
			} 
		}
		assert false;
	}

	private boolean popPushNext() throws IOException {
		return false;
	}

	private boolean skipPastLastAutoPrefixTerm() throws IOException {
		useAutoPrefixTerm = false;
		boolean isSubBlock;
		isSubBlock = false;
		return isSubBlock;
	}

	private static final class NoMoreTermsException extends RuntimeException {
		public static final IntersectTermsEnum.NoMoreTermsException INSTANCE = new IntersectTermsEnum.NoMoreTermsException();

		private NoMoreTermsException() {
		}

		@Override
		public Throwable fillInStackTrace() {
			return this;
		}
	}

	@Override
	public BytesRef next() throws IOException {
		try {
			return _next();
		} catch (IntersectTermsEnum.NoMoreTermsException eoi) {
			return null;
		}
	}

	private BytesRef _next() throws IOException {
		boolean isSubBlock;
		if (useAutoPrefixTerm) {
			isSubBlock = skipPastLastAutoPrefixTerm();
			assert (useAutoPrefixTerm) == false;
		}else {
			isSubBlock = popPushNext();
		}
		nextTerm : while (true) {
			int state;
			int lastState;
			if (isSubBlock) {
				copyTerm();
			}else {
			}
			isSubBlock = popPushNext();
		} 
	}

	private final Transition scratchTransition = new Transition();

	private boolean acceptsSuffixRange(int state, int start, int end) {
		int count = automaton.initTransition(state, scratchTransition);
		for (int i = 0; i < count; i++) {
			automaton.getNextTransition(scratchTransition);
			if (((start >= (scratchTransition.min)) && (end <= (scratchTransition.max))) && ((scratchTransition.dest) == (sinkState))) {
				return true;
			}
		}
		return false;
	}

	@SuppressWarnings("unused")
	static String brToString(BytesRef b) {
		try {
			return ((b.utf8ToString()) + " ") + b;
		} catch (Throwable t) {
			return b.toString();
		}
	}

	private void copyTerm() {
	}

	@Override
	public boolean seekExact(BytesRef text) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void seekExact(long ord) {
		throw new UnsupportedOperationException();
	}

	@Override
	public long ord() {
		throw new UnsupportedOperationException();
	}

	@Override
	public TermsEnum.SeekStatus seekCeil(BytesRef text) {
		throw new UnsupportedOperationException();
	}
}

