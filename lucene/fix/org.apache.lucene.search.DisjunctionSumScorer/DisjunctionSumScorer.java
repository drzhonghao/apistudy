

import java.io.IOException;
import java.util.List;
import org.apache.lucene.search.DisiWrapper;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;


final class DisjunctionSumScorer {
	DisjunctionSumScorer(Weight weight, List<Scorer> subScorers, boolean needsScores) {
	}

	protected float score(DisiWrapper topList) throws IOException {
		double score = 0;
		for (DisiWrapper w = topList; w != null; w = w.next) {
			score += w.scorer.score();
		}
		return ((float) (score));
	}
}

