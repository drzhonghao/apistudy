import org.apache.lucene.search.highlight.*;


import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;

/**
 * {@link Fragmenter} implementation which breaks text up into same-size
 * fragments with no concerns over spotting sentence boundaries.
 */
public class SimpleFragmenter implements Fragmenter {
  private static final int DEFAULT_FRAGMENT_SIZE = 100;
  private int currentNumFrags;
  private int fragmentSize;
  private OffsetAttribute offsetAtt;

  public SimpleFragmenter() {
    this(DEFAULT_FRAGMENT_SIZE);
  }

  /**
   * 
   * @param fragmentSize size in number of characters of each fragment
   */
  public SimpleFragmenter(int fragmentSize) {
    this.fragmentSize = fragmentSize;
  }


  /* (non-Javadoc)
   * @see org.apache.lucene.search.highlight.Fragmenter#start(java.lang.String, org.apache.lucene.analysis.TokenStream)
   */
  @Override
  public void start(String originalText, TokenStream stream) {
    offsetAtt = stream.addAttribute(OffsetAttribute.class);
    currentNumFrags = 1;
  }


  /* (non-Javadoc)
   * @see org.apache.lucene.search.highlight.Fragmenter#isNewFragment()
   */
  @Override
  public boolean isNewFragment() {
    boolean isNewFrag = offsetAtt.endOffset() >= (fragmentSize * currentNumFrags);
    if (isNewFrag) {
      currentNumFrags++;
    }
    return isNewFrag;
  }

  /**
   * @return size in number of characters of each fragment
   */
  public int getFragmentSize() {
    return fragmentSize;
  }

  /**
   * @param size size in characters of each fragment
   */
  public void setFragmentSize(int size) {
    fragmentSize = size;
  }

}
