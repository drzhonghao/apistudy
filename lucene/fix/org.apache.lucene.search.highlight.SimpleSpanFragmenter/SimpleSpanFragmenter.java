

import java.util.List;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.search.highlight.Fragmenter;
import org.apache.lucene.search.highlight.PositionSpan;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.WeightedSpanTerm;
import org.apache.lucene.util.AttributeSource;


public class SimpleSpanFragmenter implements Fragmenter {
	private static final int DEFAULT_FRAGMENT_SIZE = 100;

	private int fragmentSize;

	private int currentNumFrags;

	private int position = -1;

	private QueryScorer queryScorer;

	private int waitForPos = -1;

	private int textSize;

	private CharTermAttribute termAtt;

	private PositionIncrementAttribute posIncAtt;

	private OffsetAttribute offsetAtt;

	public SimpleSpanFragmenter(QueryScorer queryScorer) {
		this(queryScorer, SimpleSpanFragmenter.DEFAULT_FRAGMENT_SIZE);
	}

	public SimpleSpanFragmenter(QueryScorer queryScorer, int fragmentSize) {
		this.fragmentSize = fragmentSize;
		this.queryScorer = queryScorer;
	}

	@Override
	public boolean isNewFragment() {
		position += posIncAtt.getPositionIncrement();
		if ((waitForPos) <= (position)) {
			waitForPos = -1;
		}else
			if ((waitForPos) != (-1)) {
				return false;
			}

		WeightedSpanTerm wSpanTerm = queryScorer.getWeightedSpanTerm(termAtt.toString());
		if (wSpanTerm != null) {
			List<PositionSpan> positionSpans = wSpanTerm.getPositionSpans();
			for (PositionSpan positionSpan : positionSpans) {
			}
		}
		boolean isNewFrag = ((offsetAtt.endOffset()) >= ((fragmentSize) * (currentNumFrags))) && (((textSize) - (offsetAtt.endOffset())) >= ((fragmentSize) >>> 1));
		if (isNewFrag) {
			(currentNumFrags)++;
		}
		return isNewFrag;
	}

	@Override
	public void start(String originalText, TokenStream tokenStream) {
		position = -1;
		currentNumFrags = 1;
		textSize = originalText.length();
		termAtt = tokenStream.addAttribute(CharTermAttribute.class);
		posIncAtt = tokenStream.addAttribute(PositionIncrementAttribute.class);
		offsetAtt = tokenStream.addAttribute(OffsetAttribute.class);
	}
}

