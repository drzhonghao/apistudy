import org.apache.lucene.queryparser.surround.query.BasicQueryFactory;
import org.apache.lucene.queryparser.surround.query.*;

/*
SpanNearClauseFactory:

Operations:

- create for a field name and an indexreader.

- add a weighted Term
  this should add a corresponding SpanTermQuery, or
  increase the weight of an existing one.
  
- add a weighted subquery SpanNearQuery 

- create a clause for SpanNearQuery from the things added above.
  For this, create an array of SpanQuery's from the added ones.
  The clause normally is a SpanOrQuery over the added subquery SpanNearQuery
  the SpanTermQuery's for the added Term's
*/

/* When  it is necessary to suppress double subqueries as much as possible:
   hashCode() and equals() on unweighted SpanQuery are needed (possibly via getTerms(),
   the terms are individually hashable).
   Idem SpanNearQuery: hash on the subqueries and the slop.
   Evt. merge SpanNearQuery's by adding the weights of the corresponding subqueries.
 */
 
/* To be determined:
   Are SpanQuery weights handled correctly during search by Lucene?
   Should the resulting SpanOrQuery be sorted?
   Could other SpanQueries be added for use in this factory:
   - SpanOrQuery: in principle yes, but it only has access to its terms
                  via getTerms(); are the corresponding weights available?
   - SpanFirstQuery: treat similar to subquery SpanNearQuery. (ok?)
   - SpanNotQuery: treat similar to subquery SpanNearQuery. (ok?)
 */

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.MatchNoDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.spans.SpanBoostQuery;
import org.apache.lucene.search.spans.SpanOrQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;


/**
 * Factory for {@link SpanOrQuery}
 */
public class SpanNearClauseFactory { // FIXME: rename to SpanClauseFactory
  public SpanNearClauseFactory(IndexReader reader, String fieldName, BasicQueryFactory qf) {
    this.reader = reader;
    this.fieldName = fieldName;
    this.weightBySpanQuery = new HashMap<>();
    this.qf = qf;
  }
  private IndexReader reader;
  private String fieldName;
  private HashMap<SpanQuery, Float> weightBySpanQuery;
  private BasicQueryFactory qf;
  
  public IndexReader getIndexReader() {return reader;}
  
  public String getFieldName() {return fieldName;}

  public BasicQueryFactory getBasicQueryFactory() {return qf;}
  
  public int size() {return weightBySpanQuery.size();}
  
  public void clear() {weightBySpanQuery.clear();}

  protected void addSpanQueryWeighted(SpanQuery sq, float weight) {
    Float w = weightBySpanQuery.get(sq);
    if (w != null)
      w = Float.valueOf(w.floatValue() + weight);
    else
      w = Float.valueOf(weight);
    weightBySpanQuery.put(sq, w); 
  }
  
  public void addTermWeighted(Term t, float weight) throws IOException {   
    SpanTermQuery stq = qf.newSpanTermQuery(t);
    /* CHECKME: wrap in Hashable...? */
    addSpanQueryWeighted(stq, weight);
  }

  public void addSpanQuery(Query q) {
    if (q.getClass() == MatchNoDocsQuery.class)
      return;
    if (! (q instanceof SpanQuery))
      throw new AssertionError("Expected SpanQuery: " + q.toString(getFieldName()));
    float boost = 1f;
    if (q instanceof SpanBoostQuery) {
      SpanBoostQuery bq = (SpanBoostQuery) q;
      boost = bq.getBoost();
      q = bq.getQuery();
    }
    addSpanQueryWeighted((SpanQuery)q, boost);
  }

  public SpanQuery makeSpanClause() {
    SpanQuery [] spanQueries = new SpanQuery[size()];
    Iterator<SpanQuery> sqi = weightBySpanQuery.keySet().iterator();
    int i = 0;
    while (sqi.hasNext()) {
      SpanQuery sq = sqi.next();
      float boost = weightBySpanQuery.get(sq);
      if (boost != 1f) {
        sq = new SpanBoostQuery(sq, boost);
      }
      spanQueries[i++] = sq;
    }
    
    if (spanQueries.length == 1)
      return spanQueries[0];
    else
      return new SpanOrQuery(spanQueries);
  }
}

