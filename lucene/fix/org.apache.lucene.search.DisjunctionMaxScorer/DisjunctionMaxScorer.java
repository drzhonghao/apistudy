

import java.io.IOException;
import java.util.List;
import org.apache.lucene.search.DisiWrapper;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;


final class DisjunctionMaxScorer {
	private final float tieBreakerMultiplier;

	DisjunctionMaxScorer(Weight weight, float tieBreakerMultiplier, List<Scorer> subScorers, boolean needsScores) {
		this.tieBreakerMultiplier = tieBreakerMultiplier;
	}

	protected float score(DisiWrapper topList) throws IOException {
		float scoreSum = 0;
		float scoreMax = Float.NEGATIVE_INFINITY;
		for (DisiWrapper w = topList; w != null; w = w.next) {
			final float subScore = w.scorer.score();
			scoreSum += subScore;
			if (subScore > scoreMax) {
				scoreMax = subScore;
			}
		}
		return scoreMax + ((scoreSum - scoreMax) * (tieBreakerMultiplier));
	}
}

