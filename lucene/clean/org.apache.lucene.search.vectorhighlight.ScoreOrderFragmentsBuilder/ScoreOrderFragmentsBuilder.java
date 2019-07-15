import org.apache.lucene.search.vectorhighlight.BaseFragmentsBuilder;
import org.apache.lucene.search.vectorhighlight.*;


import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.lucene.search.vectorhighlight.FieldFragList.WeightedFragInfo;

/**
 * An implementation of FragmentsBuilder that outputs score-order fragments.
 */
public class ScoreOrderFragmentsBuilder extends BaseFragmentsBuilder {

  /**
   * a constructor.
   */
  public ScoreOrderFragmentsBuilder(){
    super();
  }

  /**
   * a constructor.
   * 
   * @param preTags array of pre-tags for markup terms.
   * @param postTags array of post-tags for markup terms.
   */
  public ScoreOrderFragmentsBuilder( String[] preTags, String[] postTags ){
    super( preTags, postTags );
  }

  public ScoreOrderFragmentsBuilder( BoundaryScanner bs ){
    super( bs );
  }

  public ScoreOrderFragmentsBuilder( String[] preTags, String[] postTags, BoundaryScanner bs ){
    super( preTags, postTags, bs );
  }

  /**
   * Sort by score the list of WeightedFragInfo
   */
  @Override
  public List<WeightedFragInfo> getWeightedFragInfoList( List<WeightedFragInfo> src ) {
    Collections.sort( src, new ScoreComparator() );
    return src;
  }

  /**
   * Comparator for {@link WeightedFragInfo} by boost, breaking ties
   * by offset.
   */
  public static class ScoreComparator implements Comparator<WeightedFragInfo> {

    @Override
    public int compare( WeightedFragInfo o1, WeightedFragInfo o2 ) {
      if( o1.getTotalBoost() > o2.getTotalBoost() ) return -1;
      else if( o1.getTotalBoost() < o2.getTotalBoost() ) return 1;
      // if same score then check startOffset
      else{
        if( o1.getStartOffset() < o2.getStartOffset() ) return -1;
        else if( o1.getStartOffset() > o2.getStartOffset() ) return 1;
      }
      return 0;
    }
  }
}
