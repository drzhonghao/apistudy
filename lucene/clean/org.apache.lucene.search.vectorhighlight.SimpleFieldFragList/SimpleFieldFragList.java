import org.apache.lucene.search.vectorhighlight.FieldFragList;
import org.apache.lucene.search.vectorhighlight.*;


import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.search.vectorhighlight.FieldFragList.WeightedFragInfo.SubInfo;
import org.apache.lucene.search.vectorhighlight.FieldPhraseList.WeightedPhraseInfo;

/**
 * A simple implementation of {@link FieldFragList}.
 */
public class SimpleFieldFragList extends FieldFragList {

  /**
   * a constructor.
   * 
   * @param fragCharSize the length (number of chars) of a fragment
   */
  public SimpleFieldFragList( int fragCharSize ) {
    super( fragCharSize );
  }

  /* (non-Javadoc)
   * @see org.apache.lucene.search.vectorhighlight.FieldFragList#add( int startOffset, int endOffset, List<WeightedPhraseInfo> phraseInfoList )
   */
  @Override
  public void add( int startOffset, int endOffset, List<WeightedPhraseInfo> phraseInfoList ) {
    float totalBoost = 0;
    List<SubInfo> subInfos = new ArrayList<>();
    for( WeightedPhraseInfo phraseInfo : phraseInfoList ){
      subInfos.add( new SubInfo( phraseInfo.getText(), phraseInfo.getTermsOffsets(), phraseInfo.getSeqnum(), phraseInfo.getBoost() ) );
      totalBoost += phraseInfo.getBoost();
    }
    getFragInfos().add( new WeightedFragInfo( startOffset, endOffset, subInfos, totalBoost ) );
  }
  
}
