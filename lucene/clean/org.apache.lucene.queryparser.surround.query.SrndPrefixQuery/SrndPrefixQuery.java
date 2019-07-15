import org.apache.lucene.queryparser.surround.query.*;

import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.StringHelper;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;

import java.io.IOException;


/**
 * Query that matches String prefixes
 */
public class SrndPrefixQuery extends SimpleTerm {
  private final BytesRef prefixRef;
  public SrndPrefixQuery(String prefix, boolean quoted, char truncator) {
    super(quoted);
    this.prefix = prefix;
    prefixRef = new BytesRef(prefix);
    this.truncator = truncator;
  }

  private final String prefix;
  public String getPrefix() {return prefix;}
  
  private final char truncator;
  public char getSuffixOperator() {return truncator;}
  
  public Term getLucenePrefixTerm(String fieldName) {
    return new Term(fieldName, getPrefix());
  }
  
  @Override
  public String toStringUnquoted() {return getPrefix();}
  
  @Override
  protected void suffixToString(StringBuilder r) {r.append(getSuffixOperator());}
  
  @Override
  public void visitMatchingTerms(
    IndexReader reader,
    String fieldName,
    MatchingTermVisitor mtv) throws IOException
  {
    /* inspired by PrefixQuery.rewrite(): */
    Terms terms = MultiFields.getTerms(reader, fieldName);
    if (terms != null) {
      TermsEnum termsEnum = terms.iterator();

      boolean skip = false;
      TermsEnum.SeekStatus status = termsEnum.seekCeil(new BytesRef(getPrefix()));
      if (status == TermsEnum.SeekStatus.FOUND) {
        mtv.visitMatchingTerm(getLucenePrefixTerm(fieldName));
      } else if (status == TermsEnum.SeekStatus.NOT_FOUND) {
        if (StringHelper.startsWith(termsEnum.term(), prefixRef)) {
          mtv.visitMatchingTerm(new Term(fieldName, termsEnum.term().utf8ToString()));
        } else {
          skip = true;
        }
      } else {
        // EOF
        skip = true;
      }

      if (!skip) {
        while(true) {
          BytesRef text = termsEnum.next();
          if (text != null && StringHelper.startsWith(text, prefixRef)) {
            mtv.visitMatchingTerm(new Term(fieldName, text.utf8ToString()));
          } else {
            break;
          }
        }
      }
    }
  }
}
