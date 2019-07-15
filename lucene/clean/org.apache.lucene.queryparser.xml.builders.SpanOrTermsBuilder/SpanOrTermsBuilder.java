import org.apache.lucene.queryparser.xml.builders.*;


import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.TermToBytesRefAttribute;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.spans.SpanBoostQuery;
import org.apache.lucene.search.spans.SpanOrQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.queryparser.xml.DOMUtils;
import org.apache.lucene.queryparser.xml.ParserException;
import org.w3c.dom.Element;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Builder that analyzes the text into a {@link SpanOrQuery}
 */
public class SpanOrTermsBuilder extends SpanBuilderBase {

  private final Analyzer analyzer;

  public SpanOrTermsBuilder(Analyzer analyzer) {
    this.analyzer = analyzer;
  }

  @Override
  public SpanQuery getSpanQuery(Element e) throws ParserException {
    String fieldName = DOMUtils.getAttributeWithInheritanceOrFail(e, "fieldName");
    String value = DOMUtils.getNonBlankTextOrFail(e);

    List<SpanQuery> clausesList = new ArrayList<>();

    try (TokenStream ts = analyzer.tokenStream(fieldName, value)) {
      TermToBytesRefAttribute termAtt = ts.addAttribute(TermToBytesRefAttribute.class);
      ts.reset();
      while (ts.incrementToken()) {
        SpanTermQuery stq = new SpanTermQuery(new Term(fieldName, BytesRef.deepCopyOf(termAtt.getBytesRef())));
        clausesList.add(stq);
      }
      ts.end();
      SpanOrQuery soq = new SpanOrQuery(clausesList.toArray(new SpanQuery[clausesList.size()]));
      float boost = DOMUtils.getAttribute(e, "boost", 1.0f);
      return new SpanBoostQuery(soq, boost);
    }
    catch (IOException ioe) {
      throw new ParserException("IOException parsing value:" + value);
    }
  }

}
