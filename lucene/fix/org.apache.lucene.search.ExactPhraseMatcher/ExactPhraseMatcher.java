

import java.io.IOException;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.search.similarities.Similarity;


final class ExactPhraseMatcher {
	private static class PostingsAndPosition {
		private final PostingsEnum postings;

		private final int offset;

		private int freq;

		private int upTo;

		private int pos;

		public PostingsAndPosition(PostingsEnum postings, int offset) {
			this.postings = postings;
			this.offset = offset;
		}
	}

	private final ExactPhraseMatcher.PostingsAndPosition[] postings = null;

	float maxFreq() {
		int minFreq = postings[0].freq;
		for (int i = 1; i < (postings.length); i++) {
			minFreq = Math.min(minFreq, postings[i].freq);
		}
		return minFreq;
	}

	private static boolean advancePosition(ExactPhraseMatcher.PostingsAndPosition posting, int target) throws IOException {
		while ((posting.pos) < target) {
			if ((posting.upTo) == (posting.freq)) {
				return false;
			}else {
				posting.pos = posting.postings.nextPosition();
				posting.upTo += 1;
			}
		} 
		return true;
	}

	public void reset() throws IOException {
		for (ExactPhraseMatcher.PostingsAndPosition posting : postings) {
			posting.freq = posting.postings.freq();
			posting.pos = -1;
			posting.upTo = 0;
		}
	}

	public boolean nextMatch() throws IOException {
		final ExactPhraseMatcher.PostingsAndPosition lead = postings[0];
		if ((lead.upTo) < (lead.freq)) {
			lead.pos = lead.postings.nextPosition();
			lead.upTo += 1;
		}else {
			return false;
		}
		advanceHead : while (true) {
			final int phrasePos = (lead.pos) - (lead.offset);
			for (int j = 1; j < (postings.length); ++j) {
				final ExactPhraseMatcher.PostingsAndPosition posting = postings[j];
				final int expectedPos = phrasePos + (posting.offset);
				if ((ExactPhraseMatcher.advancePosition(posting, expectedPos)) == false) {
					break advanceHead;
				}
				if ((posting.pos) != expectedPos) {
					if (ExactPhraseMatcher.advancePosition(lead, (((posting.pos) - (posting.offset)) + (lead.offset)))) {
						continue advanceHead;
					}else {
						break advanceHead;
					}
				}
			}
			return true;
		} 
		return false;
	}

	float sloppyWeight(Similarity.SimScorer simScorer) {
		return 1;
	}

	public int startPosition() {
		return postings[0].pos;
	}

	public int endPosition() {
		return postings[((postings.length) - 1)].pos;
	}

	public int startOffset() throws IOException {
		return postings[0].postings.startOffset();
	}

	public int endOffset() throws IOException {
		return postings[((postings.length) - 1)].postings.endOffset();
	}
}

