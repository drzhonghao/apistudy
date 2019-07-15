

import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import org.apache.lucene.analysis.CharFilter;
import org.apache.lucene.analysis.charfilter.BaseCharFilter;
import org.apache.lucene.analysis.charfilter.NormalizeCharMap;
import org.apache.lucene.analysis.util.RollingCharBuffer;
import org.apache.lucene.util.CharsRef;
import org.apache.lucene.util.fst.CharSequenceOutputs;
import org.apache.lucene.util.fst.FST;
import org.apache.lucene.util.fst.FST.Arc;
import org.apache.lucene.util.fst.Outputs;


public class MappingCharFilter extends BaseCharFilter {
	private final Outputs<CharsRef> outputs = CharSequenceOutputs.getSingleton();

	private final FST<CharsRef> map = null;

	private final FST.BytesReader fstReader;

	private final RollingCharBuffer buffer = new RollingCharBuffer();

	private final FST.Arc<CharsRef> scratchArc = new FST.Arc<>();

	private final Map<Character, FST.Arc<CharsRef>> cachedRootArcs;

	private CharsRef replacement;

	private int replacementPointer;

	private int inputOff;

	public MappingCharFilter(NormalizeCharMap normMap, Reader in) {
		super(in);
		buffer.reset(in);
		if ((map) != null) {
			fstReader = map.getBytesReader();
		}else {
			fstReader = null;
		}
		cachedRootArcs = null;
	}

	@Override
	public void reset() throws IOException {
		input.reset();
		buffer.reset(input);
		replacement = null;
		inputOff = 0;
	}

	@Override
	public int read() throws IOException {
		while (true) {
			if (((replacement) != null) && ((replacementPointer) < (replacement.length))) {
				return replacement.chars[((replacement.offset) + ((replacementPointer)++))];
			}
			int lastMatchLen = -1;
			CharsRef lastMatch = null;
			final int firstCH = buffer.get(inputOff);
			if (firstCH != (-1)) {
				FST.Arc<CharsRef> arc = cachedRootArcs.get(Character.valueOf(((char) (firstCH))));
				if (arc != null) {
					if (!(FST.targetHasArcs(arc))) {
						assert arc.isFinal();
						lastMatchLen = 1;
						lastMatch = arc.output;
					}else {
						int lookahead = 0;
						CharsRef output = arc.output;
						while (true) {
							lookahead++;
							if (arc.isFinal()) {
								lastMatchLen = lookahead;
								lastMatch = outputs.add(output, arc.nextFinalOutput);
							}
							if (!(FST.targetHasArcs(arc))) {
								break;
							}
							int ch = buffer.get(((inputOff) + lookahead));
							if (ch == (-1)) {
								break;
							}
							if ((arc = map.findTargetArc(ch, arc, scratchArc, fstReader)) == null) {
								break;
							}
							output = outputs.add(output, arc.output);
						} 
					}
				}
			}
			if (lastMatch != null) {
				inputOff += lastMatchLen;
				final int diff = lastMatchLen - (lastMatch.length);
				if (diff != 0) {
					final int prevCumulativeDiff = getLastCumulativeDiff();
					if (diff > 0) {
						addOffCorrectMap((((inputOff) - diff) - prevCumulativeDiff), (prevCumulativeDiff + diff));
					}else {
						final int outputStart = (inputOff) - prevCumulativeDiff;
						for (int extraIDX = 0; extraIDX < (-diff); extraIDX++) {
							addOffCorrectMap((outputStart + extraIDX), ((prevCumulativeDiff - extraIDX) - 1));
						}
					}
				}
				replacement = lastMatch;
				replacementPointer = 0;
			}else {
				final int ret = buffer.get(inputOff);
				if (ret != (-1)) {
					(inputOff)++;
					buffer.freeBefore(inputOff);
				}
				return ret;
			}
		} 
	}

	@Override
	public int read(char[] cbuf, int off, int len) throws IOException {
		int numRead = 0;
		for (int i = off; i < (off + len); i++) {
			int c = read();
			if (c == (-1))
				break;

			cbuf[i] = ((char) (c));
			numRead++;
		}
		return numRead == 0 ? -1 : numRead;
	}
}

