import org.apache.lucene.search.highlight.*;

/**
 * Low-level class used to record information about a section of a document 
 * with a score.
 *
 * 
 */
public class TextFragment
{
  CharSequence markedUpText;
  int fragNum;
  int textStartPos;
  int textEndPos;
  float score;

  public TextFragment(CharSequence markedUpText,int textStartPos, int fragNum)
  {
    this.markedUpText=markedUpText;
    this.textStartPos = textStartPos;
    this.fragNum = fragNum;
  }

  void setScore(float score)
  {
    this.score=score;
  }
  public float getScore()
  {
    return score;
  }
  /**
   * @param frag2 Fragment to be merged into this one
   */
  public void merge(TextFragment frag2)
  {
    textEndPos = frag2.textEndPos;
    score=Math.max(score,frag2.score);
  }
  /**
   * @return true if this fragment follows the one passed
   */
  public boolean follows(TextFragment fragment)
  {
    return textStartPos == fragment.textEndPos;
  }

  /**
   * @return the fragment sequence number
   */
  public int getFragNum()
  {
    return fragNum;
  }

  /* Returns the marked-up text for this text fragment
   */
  @Override
  public String toString() {
    return markedUpText.subSequence(textStartPos, textEndPos).toString();
  }

}
