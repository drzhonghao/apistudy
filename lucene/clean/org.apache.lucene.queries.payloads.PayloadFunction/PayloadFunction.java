import org.apache.lucene.queries.payloads.*;

import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.spans.Spans;

/**
 * An abstract class that defines a way for PayloadScoreQuery instances to transform
 * the cumulative effects of payload scores for a document.
 * 
 * @see org.apache.lucene.queries.payloads.PayloadScoreQuery for more information
 * 
 * @lucene.experimental This class and its derivations are experimental and subject to
 *               change
 * 
 **/
public abstract class PayloadFunction {

  /**
   * Calculate the score up to this point for this doc and field
   * @param docId The current doc
   * @param field The field
   * @param start The start position of the matching Span
   * @param end The end position of the matching Span
   * @param numPayloadsSeen The number of payloads seen so far
   * @param currentScore The current score so far
   * @param currentPayloadScore The score for the current payload
   * @return The new current Score
   *
   * @see Spans
   */
  public abstract float currentScore(int docId, String field, int start, int end, int numPayloadsSeen, float currentScore, float currentPayloadScore);

  /**
   * Calculate the final score for all the payloads seen so far for this doc/field
   * @param docId The current doc
   * @param field The current field
   * @param numPayloadsSeen The total number of payloads seen on this document
   * @param payloadScore The raw score for those payloads
   * @return The final score for the payloads
   */
  public abstract float docScore(int docId, String field, int numPayloadsSeen, float payloadScore);
  
  public Explanation explain(int docId, String field, int numPayloadsSeen, float payloadScore){
    return Explanation.match(
        docScore(docId, field, numPayloadsSeen, payloadScore),
        getClass().getSimpleName() + ".docScore()");
  };
  
  @Override
  public abstract int hashCode();
  
  @Override
  public abstract boolean equals(Object o);

}
