import org.apache.lucene.queryparser.surround.query.SrndQuery;
import org.apache.lucene.queryparser.surround.query.BasicQueryFactory;
import org.apache.lucene.queryparser.surround.query.*;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.lucene.search.Query;

/** Base class for composite queries (such as AND/OR/NOT) */
public abstract class ComposedQuery extends SrndQuery { 
  
  public ComposedQuery(List<SrndQuery> qs, boolean operatorInfix, String opName) {
    recompose(qs);
    this.operatorInfix = operatorInfix;
    this.opName = opName;
  }
  
  protected void recompose(List<SrndQuery> queries) {
    if (queries.size() < 2) throw new AssertionError("Too few subqueries"); 
    this.queries = queries;
  }
  
  protected String opName;
  public String getOperatorName() {return opName;}
  
  protected List<SrndQuery> queries;
  
  public Iterator<SrndQuery> getSubQueriesIterator() {return queries.listIterator();}

  public int getNrSubQueries() {return queries.size();}
  
  public SrndQuery getSubQuery(int qn) {return queries.get(qn);}

  private boolean operatorInfix; 
  public boolean isOperatorInfix() { return operatorInfix; } /* else prefix operator */
  
  public List<Query> makeLuceneSubQueriesField(String fn, BasicQueryFactory qf) {
    List<Query> luceneSubQueries = new ArrayList<>();
    Iterator<SrndQuery> sqi = getSubQueriesIterator();
    while (sqi.hasNext()) {
      luceneSubQueries.add( (sqi.next()).makeLuceneQueryField(fn, qf));
    }
    return luceneSubQueries;
  }

  @Override
  public String toString() {
    StringBuilder r = new StringBuilder();
    if (isOperatorInfix()) {
      infixToString(r);
    } else {
      prefixToString(r);
    }
    weightToString(r);
    return r.toString();
  }

  /* Override for different spacing */
  protected String getPrefixSeparator() { return ", ";}
  protected String getBracketOpen() { return "(";}
  protected String getBracketClose() { return ")";}
  
  protected void infixToString(StringBuilder r) {
    /* Brackets are possibly redundant in the result. */
    Iterator<SrndQuery> sqi = getSubQueriesIterator();
    r.append(getBracketOpen());
    if (sqi.hasNext()) {
      r.append(sqi.next().toString());
      while (sqi.hasNext()) {
        r.append(" ");
        r.append(getOperatorName()); /* infix operator */
        r.append(" ");
        r.append(sqi.next().toString());
      }
    }
    r.append(getBracketClose());
  }

  protected void prefixToString(StringBuilder r) {
    Iterator<SrndQuery> sqi = getSubQueriesIterator();
    r.append(getOperatorName()); /* prefix operator */
    r.append(getBracketOpen());
    if (sqi.hasNext()) {
      r.append(sqi.next().toString());
      while (sqi.hasNext()) {
        r.append(getPrefixSeparator());
        r.append(sqi.next().toString());
      }
    }
    r.append(getBracketClose());
  }
  
  
  @Override
  public boolean isFieldsSubQueryAcceptable() {
    /* at least one subquery should be acceptable */
    Iterator<SrndQuery> sqi = getSubQueriesIterator();
    while (sqi.hasNext()) {
      if ((sqi.next()).isFieldsSubQueryAcceptable()) {
        return true;
      }
    }
    return false;
  }
}

