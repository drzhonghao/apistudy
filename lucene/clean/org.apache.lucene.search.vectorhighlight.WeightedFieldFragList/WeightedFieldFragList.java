import org.apache.lucene.search.vectorhighlight.FieldFragList;
import org.apache.lucene.search.vectorhighlight.*;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.lucene.search.vectorhighlight.FieldFragList.WeightedFragInfo.SubInfo;
import org.apache.lucene.search.vectorhighlight.FieldPhraseList.WeightedPhraseInfo;
import org.apache.lucene.search.vectorhighlight.FieldTermStack.TermInfo;

/**
 * A weighted implementation of {@link FieldFragList}.
 */
public class WeightedFieldFragList extends FieldFragList {

  /**
   * a constructor.
   * 
   * @param fragCharSize the length (number of chars) of a fragment
   */
  public WeightedFieldFragList( int fragCharSize ) {
    super( fragCharSize );
  }

  /* (non-Javadoc)
   * @see org.apache.lucene.search.vectorhighlight.FieldFragList#add( int startOffset, int endOffset, List<WeightedPhraseInfo> phraseInfoList )
   */ 
  @Override
  public void add( int startOffset, int endOffset, List<WeightedPhraseInfo> phraseInfoList ) {
    List<SubInfo> tempSubInfos = new ArrayList<>();
    List<SubInfo> realSubInfos = new ArrayList<>();
    HashSet<String> distinctTerms = new HashSet<>();
    int length = 0;

    for( WeightedPhraseInfo phraseInfo : phraseInfoList ){
      float phraseTotalBoost = 0;
      for ( TermInfo ti :  phraseInfo.getTermsInfos()) {
        if ( distinctTerms.add( ti.getText() ) )
          phraseTotalBoost += ti.getWeight() * phraseInfo.getBoost();
        length++;
      }
      tempSubInfos.add( new SubInfo( phraseInfo.getText(), phraseInfo.getTermsOffsets(),
        phraseInfo.getSeqnum(), phraseTotalBoost ) );
    }
    
    // We want that terms per fragment (length) is included into the weight. Otherwise a one-word-query
    // would cause an equal weight for all fragments regardless of how much words they contain.  
    // To avoid that fragments containing a high number of words possibly "outrank" more relevant fragments
    // we "bend" the length with a standard-normalization a little bit.
    float norm = length * ( 1 / (float)Math.sqrt( length ) );

    float totalBoost = 0;
    for ( SubInfo tempSubInfo : tempSubInfos ) {
      float subInfoBoost = tempSubInfo.getBoost() * norm;
      realSubInfos.add( new SubInfo( tempSubInfo.getText(), tempSubInfo.getTermsOffsets(),
        tempSubInfo.getSeqnum(), subInfoBoost ));
      totalBoost += subInfoBoost;
    }

    getFragInfos().add( new WeightedFragInfo( startOffset, endOffset, realSubInfos, totalBoost ) );
  }
  
}
