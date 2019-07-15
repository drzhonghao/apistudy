import org.apache.lucene.queryparser.surround.query.*;

import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.index.Terms;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.StringHelper;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;

import java.io.IOException;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Query that matches wildcards
 */
public class SrndTruncQuery extends SimpleTerm {
  public SrndTruncQuery(String truncated, char unlimited, char mask) {
    super(false); /* not quoted */
    this.truncated = truncated;
    this.unlimited = unlimited;
    this.mask = mask;
    truncatedToPrefixAndPattern();
  }
  
  private final String truncated;
  private final char unlimited;
  private final char mask;
  
  private String prefix;
  private BytesRef prefixRef;
  private Pattern pattern;
  
  
  public String getTruncated() {return truncated;}
  
  @Override
  public String toStringUnquoted() {return getTruncated();}

  
  protected boolean matchingChar(char c) {
    return (c != unlimited) && (c != mask);
  }

  protected void appendRegExpForChar(char c, StringBuilder re) {
    if (c == unlimited)
      re.append(".*");
    else if (c == mask)
      re.append(".");
    else
      re.append(c);
  }
  
  protected void truncatedToPrefixAndPattern() {
    int i = 0;
    while ((i < truncated.length()) && matchingChar(truncated.charAt(i))) {
      i++;
    }
    prefix = truncated.substring(0, i);
    prefixRef = new BytesRef(prefix);
    
    StringBuilder re = new StringBuilder();
    while (i < truncated.length()) {
      appendRegExpForChar(truncated.charAt(i), re);
      i++;
    }
    pattern = Pattern.compile(re.toString());
  }
  
  @Override
  public void visitMatchingTerms(
    IndexReader reader,
    String fieldName,
    MatchingTermVisitor mtv) throws IOException
  {
    int prefixLength = prefix.length();
    Terms terms = MultiFields.getTerms(reader, fieldName);
    if (terms != null) {
      Matcher matcher = pattern.matcher("");
      try {
        TermsEnum termsEnum = terms.iterator();

        TermsEnum.SeekStatus status = termsEnum.seekCeil(prefixRef);
        BytesRef text;
        if (status == TermsEnum.SeekStatus.FOUND) {
          text = prefixRef;
        } else if (status == TermsEnum.SeekStatus.NOT_FOUND) {
          text = termsEnum.term();
        } else {
          text = null;
        }

        while(text != null) {
          if (text != null && StringHelper.startsWith(text, prefixRef)) {
            String textString = text.utf8ToString();
            matcher.reset(textString.substring(prefixLength));
            if (matcher.matches()) {
              mtv.visitMatchingTerm(new Term(fieldName, textString));
            }
          } else {
            break;
          }
          text = termsEnum.next();
        }
      } finally {
        matcher.reset();
      }
    }
  }
}