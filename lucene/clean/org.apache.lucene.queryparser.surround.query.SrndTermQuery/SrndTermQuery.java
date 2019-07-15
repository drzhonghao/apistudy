import org.apache.lucene.queryparser.surround.query.*;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.util.BytesRef;

 
/**
 * Simple single-term clause
 */
public class SrndTermQuery extends SimpleTerm {
  public SrndTermQuery(String termText, boolean quoted) {
    super(quoted);
    this.termText = termText;
  }

  private final String termText;
  public String getTermText() {return termText;}
        
  public Term getLuceneTerm(String fieldName) {
    return new Term(fieldName, getTermText());
  }
  
  @Override
  public String toStringUnquoted() {return getTermText();}
  
  @Override
  public void visitMatchingTerms(
    IndexReader reader,
    String fieldName,
    MatchingTermVisitor mtv) throws IOException
  {
    /* check term presence in index here for symmetry with other SimpleTerm's */
    Terms terms = MultiFields.getTerms(reader, fieldName);
    if (terms != null) {
      TermsEnum termsEnum = terms.iterator();

      TermsEnum.SeekStatus status = termsEnum.seekCeil(new BytesRef(getTermText()));
      if (status == TermsEnum.SeekStatus.FOUND) {
        mtv.visitMatchingTerm(getLuceneTerm(fieldName));
      }
    }
  }
}
  


