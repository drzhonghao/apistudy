import org.apache.lucene.search.vectorhighlight.BaseFragListBuilder;
import org.apache.lucene.search.vectorhighlight.FieldFragList;
import org.apache.lucene.search.vectorhighlight.FieldPhraseList;
import org.apache.lucene.search.vectorhighlight.*;


/**
 * A simple implementation of {@link FragListBuilder}.
 */
public class SimpleFragListBuilder extends BaseFragListBuilder {
  
  public SimpleFragListBuilder() {
    super();
  }

  public SimpleFragListBuilder(int margin) {
    super(margin);
  }

  /* (non-Javadoc)
   * @see org.apache.lucene.search.vectorhighlight.FragListBuilder#createFieldFragList(FieldPhraseList fieldPhraseList, int fragCharSize)
   */ 
  @Override
  public FieldFragList createFieldFragList( FieldPhraseList fieldPhraseList, int fragCharSize ){
    return createFieldFragList( fieldPhraseList, new SimpleFieldFragList( fragCharSize ), fragCharSize );
  }

}
