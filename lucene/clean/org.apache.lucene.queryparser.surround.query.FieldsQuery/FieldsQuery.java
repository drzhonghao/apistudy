import org.apache.lucene.queryparser.surround.query.SrndQuery;
import org.apache.lucene.queryparser.surround.query.BasicQueryFactory;
import org.apache.lucene.queryparser.surround.query.OrQuery;
import org.apache.lucene.queryparser.surround.query.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

import org.apache.lucene.search.Query;

/**
 * Forms an OR query of the provided query across multiple fields.
 */
public class FieldsQuery extends SrndQuery { /* mostly untested */
  private SrndQuery q;
  private List<String> fieldNames;
  private final char fieldOp;
  private static final String OR_OPERATOR_NAME = "OR"; /* for expanded queries, not normally visible */
  
  public FieldsQuery(SrndQuery q, List<String> fieldNames, char fieldOp) {
    this.q = q;
    this.fieldNames = fieldNames;
    this.fieldOp = fieldOp;
  }
  
  public FieldsQuery(SrndQuery q, String fieldName, char fieldOp) {
    this.q = q;
    fieldNames = new ArrayList<>();
    fieldNames.add(fieldName);
    this.fieldOp = fieldOp;
  }
  
  @Override
  public boolean isFieldsSubQueryAcceptable() {
    return false;
  }
  
  public Query makeLuceneQueryNoBoost(BasicQueryFactory qf) {
    if (fieldNames.size() == 1) { /* single field name: no new queries needed */
      return q.makeLuceneQueryFieldNoBoost(fieldNames.get(0), qf);
    } else { /* OR query over the fields */
      List<SrndQuery> queries = new ArrayList<>();
      Iterator<String> fni = getFieldNames().listIterator();
      SrndQuery qc;
      while (fni.hasNext()) {
        qc = q.clone();
        queries.add( new FieldsQuery( qc, fni.next(), fieldOp));
      }
      OrQuery oq = new OrQuery(queries,
                              true /* infix OR for field names */,
                              OR_OPERATOR_NAME);
      // System.out.println(getClass().toString() + ", fields expanded: " + oq.toString()); /* needs testing */
      return oq.makeLuceneQueryField(null, qf);
    }
  }

  @Override
  public Query makeLuceneQueryFieldNoBoost(String fieldName, BasicQueryFactory qf) {
    return makeLuceneQueryNoBoost(qf); /* use this.fieldNames instead of fieldName */
  }

  
  public List<String> getFieldNames() {return fieldNames;}

  public char getFieldOperator() { return fieldOp;}
  
  @Override
  public String toString() {
    StringBuilder r = new StringBuilder();
    r.append("(");
    fieldNamesToString(r);
    r.append(q.toString());
    r.append(")");
    return r.toString();
  }
  
  protected void fieldNamesToString(StringBuilder r) {
    Iterator<String> fni = getFieldNames().listIterator();
    while (fni.hasNext()) {
      r.append(fni.next());
      r.append(getFieldOperator());
    }
  }
}

