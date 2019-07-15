import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.*;



import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

/**
 * A {@link Scorer} which wraps another scorer and caches the score of the
 * current document. Successive calls to {@link #score()} will return the same
 * result and will not invoke the wrapped Scorer's score() method, unless the
 * current document has changed.<br>
 * This class might be useful due to the changes done to the {@link Collector}
 * interface, in which the score is not computed for a document by default, only
 * if the collector requests it. Some collectors may need to use the score in
 * several places, however all they have in hand is a {@link Scorer} object, and
 * might end up computing the score of a document more than once.
 */
public class ScoreCachingWrappingScorer extends FilterScorer {

  private int curDoc = -1;
  private float curScore;

  /** Creates a new instance by wrapping the given scorer. */
  public ScoreCachingWrappingScorer(Scorer scorer) {
    super(scorer);
  }

  @Override
  public float score() throws IOException {
    int doc = in.docID();
    if (doc != curDoc) {
      curScore = in.score();
      curDoc = doc;
    }

    return curScore;
  }

  @Override
  public Collection<ChildScorer> getChildren() {
    return Collections.singleton(new ChildScorer(in, "CACHED"));
  }
}
