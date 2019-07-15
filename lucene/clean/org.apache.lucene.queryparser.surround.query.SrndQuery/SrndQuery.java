import org.apache.lucene.queryparser.surround.query.BasicQueryFactory;
import org.apache.lucene.queryparser.surround.query.*;

import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.Query;

/** Lowest level base class for surround queries */
public abstract class SrndQuery implements Cloneable {
  public SrndQuery() {}
  
  private float weight = (float) 1.0;
  private boolean weighted = false;

  public void setWeight(float w) {
    weight = w; /* as parsed from the query text */
    weighted = true;
  } 
  public boolean isWeighted() {return weighted;}
  public float getWeight() { return weight; }
  public String getWeightString() {return Float.toString(getWeight());}

  public String getWeightOperator() {return "^";}

  protected void weightToString(StringBuilder r) { /* append the weight part of a query */
    if (isWeighted()) {
      r.append(getWeightOperator());
      r.append(getWeightString());
    }
  }
  
  public Query makeLuceneQueryField(String fieldName, BasicQueryFactory qf){
    Query q = makeLuceneQueryFieldNoBoost(fieldName, qf);
    if (isWeighted()) {
      q = new BoostQuery(q, getWeight()); /* weight may be at any level in a SrndQuery */
    }
    return q;
  }
  
  public abstract Query makeLuceneQueryFieldNoBoost(String fieldName, BasicQueryFactory qf);
  
  /** This method is used by {@link #hashCode()} and {@link #equals(Object)},
   *  see LUCENE-2945.
   */
  @Override
  public abstract String toString();
  
  public boolean isFieldsSubQueryAcceptable() {return true;}
    
  @Override
  public SrndQuery clone() {
    try {
      return (SrndQuery)super.clone();
    } catch (CloneNotSupportedException cns) {
      throw new Error(cns);
    }
  }

  /** For subclasses of {@link SrndQuery} within the package
   *  {@link org.apache.lucene.queryparser.surround.query}
   *  it is not necessary to override this method,
   *  @see #toString()
   */
  @Override
  public int hashCode() {
    return getClass().hashCode() ^ toString().hashCode();
  }

  /** For subclasses of {@link SrndQuery} within the package
   *  {@link org.apache.lucene.queryparser.surround.query}
   *  it is not necessary to override this method,
   *  @see #toString()
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == null)
      return false;
    if (! getClass().equals(obj.getClass()))
      return false;
    return toString().equals(obj.toString());
  }

}

