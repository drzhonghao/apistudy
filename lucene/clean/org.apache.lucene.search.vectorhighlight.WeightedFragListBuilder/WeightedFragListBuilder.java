import org.apache.lucene.search.vectorhighlight.BaseFragListBuilder;
import org.apache.lucene.search.vectorhighlight.FieldFragList;
import org.apache.lucene.search.vectorhighlight.FieldPhraseList;
import org.apache.lucene.search.vectorhighlight.*;


/**
 * A weighted implementation of {@link FragListBuilder}.
 */
public class WeightedFragListBuilder extends BaseFragListBuilder {

  public WeightedFragListBuilder() {
    super();
  }

  public WeightedFragListBuilder(int margin) {
    super(margin);
  }

  /* (non-Javadoc)
   * @see org.apache.lucene.search.vectorhighlight.FragListBuilder#createFieldFragList(FieldPhraseList fieldPhraseList, int fragCharSize)
   */ 
  @Override
  public FieldFragList createFieldFragList( FieldPhraseList fieldPhraseList, int fragCharSize ){
    return createFieldFragList( fieldPhraseList, new WeightedFieldFragList( fragCharSize ), fragCharSize );
  }
  
}
