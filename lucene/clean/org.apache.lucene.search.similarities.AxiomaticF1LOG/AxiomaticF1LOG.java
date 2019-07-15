import org.apache.lucene.search.similarities.Axiomatic;
import org.apache.lucene.search.similarities.BasicStats;
import org.apache.lucene.search.similarities.*;


/**
 * F1LOG is defined as Sum(tf(term_doc_freq)*ln(docLen)*IDF(term))
 * where IDF(t) = ln((N+1)/df(t)) N=total num of docs, df=doc freq
 *
 * @lucene.experimental
 */
public class AxiomaticF1LOG extends Axiomatic {

  /**
   * Constructor setting s only, letting k and queryLen to default
   *
   * @param s hyperparam for the growth function
   */
  public AxiomaticF1LOG(float s) {
    super(s);
  }

  /**
   * Default constructor
   */
  public AxiomaticF1LOG() {
    super();
  }

  @Override
  public String toString() {
    return "F1LOG";
  }

  /**
   * compute the term frequency component
   */
  @Override
  protected float tf(BasicStats stats, float freq, float docLen) {
    if (freq <= 0.0) return 0f;
    return (float) (1 + Math.log(1 + Math.log(freq)));
  }

  /**
   * compute the document length component
   */
  @Override
  protected float ln(BasicStats stats, float freq, float docLen) {
    return (stats.getAvgFieldLength() + this.s) / (stats.getAvgFieldLength() + docLen * this.s);
  }

  /**
   * compute the mixed term frequency and document length component
   */
  @Override
  protected float tfln(BasicStats stats, float freq, float docLen) {
    return 1f;
  }

  /**
   * compute the inverted document frequency component
   */
  @Override
  protected float idf(BasicStats stats, float freq, float docLen) {
    return (float) Math.log((stats.getNumberOfDocuments() + 1.0) / stats.getDocFreq());
  }

  /**
   * compute the gamma component
   */
  @Override
  protected float gamma(BasicStats stats, float freq, float docLen) {
    return 0f;
  }
}
