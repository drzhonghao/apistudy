import org.apache.lucene.queryparser.xml.builders.*;


import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.TermToBytesRefAttribute;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.queryparser.xml.DOMUtils;
import org.apache.lucene.queryparser.xml.ParserException;
import org.apache.lucene.queryparser.xml.QueryBuilder;
import org.w3c.dom.Element;

import java.io.IOException;

/**
 * Builds a BooleanQuery from all of the terms found in the XML element using the choice of analyzer
 */
public class TermsQueryBuilder implements QueryBuilder {

  private final Analyzer analyzer;

  public TermsQueryBuilder(Analyzer analyzer) {
    this.analyzer = analyzer;
  }

  @Override
  public Query getQuery(Element e) throws ParserException {
    String fieldName = DOMUtils.getAttributeWithInheritanceOrFail(e, "fieldName");
    String text = DOMUtils.getNonBlankTextOrFail(e);

    BooleanQuery.Builder bq = new BooleanQuery.Builder();
    bq.setMinimumNumberShouldMatch(DOMUtils.getAttribute(e, "minimumNumberShouldMatch", 0));
    try (TokenStream ts = analyzer.tokenStream(fieldName, text)) {
      TermToBytesRefAttribute termAtt = ts.addAttribute(TermToBytesRefAttribute.class);
      Term term = null;
      ts.reset();
      while (ts.incrementToken()) {
        term = new Term(fieldName, BytesRef.deepCopyOf(termAtt.getBytesRef()));
        bq.add(new BooleanClause(new TermQuery(term), BooleanClause.Occur.SHOULD));
      }
      ts.end();
    }
    catch (IOException ioe) {
      throw new RuntimeException("Error constructing terms from index:" + ioe);
    }

    Query q = bq.build();
    float boost = DOMUtils.getAttribute(e, "boost", 1.0f);
    return new BoostQuery(q, boost);
  }

}
