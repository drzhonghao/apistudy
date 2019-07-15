import org.apache.lucene.search.similarities.Axiomatic;
import org.apache.lucene.search.similarities.BasicStats;
import org.apache.lucene.search.similarities.*;


/**
 * F2EXP is defined as Sum(tfln(term_doc_freq, docLen)*IDF(term))
 * where IDF(t) = pow((N+1)/df(t), k) N=total num of docs, df=doc freq
 *
 * @lucene.experimental
 */
public class AxiomaticF2EXP extends Axiomatic {
  /**
   * Constructor setting s and k, letting queryLen to default
   * @param s hyperparam for the growth function
   * @param k hyperparam for the primitive weighting function
   */
  public AxiomaticF2EXP(float s, float k) {
    super(s, 1, k);
  }

  /**
   * Constructor setting s only, letting k and queryLen to default
   * @param s hyperparam for the growth function
   */
  public AxiomaticF2EXP(float s) {
    this(s, 0.35f);
  }

  /**
   * Default constructor
   */
  public AxiomaticF2EXP() {
    super();
  }

  @Override
  public String toString() {
    return "F2EXP";
  }

  /**
   * compute the term frequency component
   */
  @Override
  protected float tf(BasicStats stats, float freq, float docLen) {
    return 1f;
  }

  /**
   * compute the document length component
   */
  @Override
  protected float ln(BasicStats stats, float freq, float docLen) {
    return 1f;
  }

  /**
   * compute the mixed term frequency and document length component
   */
  @Override
  protected float tfln(BasicStats stats, float freq, float docLen) {
    return freq / (freq + this.s + this.s * docLen / stats.getAvgFieldLength());
  }

  /**
   * compute the inverted document frequency component
   */
  @Override
  protected float idf(BasicStats stats, float freq, float docLen) {
    return (float) Math.pow((stats.getNumberOfDocuments() + 1.0) / stats.getDocFreq(), this.k);
  }

  /**
   * compute the gamma component
   */
  @Override
  protected float gamma(BasicStats stats, float freq, float docLen) {
    return 0f;
  }
}
