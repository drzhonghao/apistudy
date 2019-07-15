import org.apache.lucene.search.similarities.Axiomatic;
import org.apache.lucene.search.similarities.BasicStats;
import org.apache.lucene.search.similarities.*;


/**
 * F2EXP is defined as Sum(tf(term_doc_freq)*IDF(term)-gamma(docLen, queryLen))
 * where IDF(t) = ln((N+1)/df(t)) N=total num of docs, df=doc freq
 * gamma(docLen, queryLen) = (docLen-queryLen)*queryLen*s/avdl
 *
 * @lucene.experimental
 */
public class AxiomaticF3LOG extends Axiomatic {

  /**
   * Constructor setting s and queryLen, letting k to default
   *
   * @param s        hyperparam for the growth function
   * @param queryLen the query length
   */
  public AxiomaticF3LOG(float s, int queryLen) {
    super(s, queryLen);
  }

  @Override
  public String toString() {
    return "F3LOG";
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
    return 1f;
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
    return (docLen - this.queryLen) * this.s * this.queryLen / stats.getAvgFieldLength();
  }
}
