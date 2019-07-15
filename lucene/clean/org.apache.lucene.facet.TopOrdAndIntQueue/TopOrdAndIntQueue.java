import org.apache.lucene.facet.*;


import org.apache.lucene.util.PriorityQueue;

/** Keeps highest results, first by largest int value,
 *  then tie break by smallest ord. */
public class TopOrdAndIntQueue extends PriorityQueue<TopOrdAndIntQueue.OrdAndValue> {

  /** Holds a single entry. */
  public static final class OrdAndValue {

    /** Ordinal of the entry. */
    public int ord;

    /** Value associated with the ordinal. */
    public int value;

    /** Default constructor. */
    public OrdAndValue() {
    }
  }

  /** Sole constructor. */
  public TopOrdAndIntQueue(int topN) {
    super(topN, false);
  }

  @Override
  protected boolean lessThan(OrdAndValue a, OrdAndValue b) {
    if (a.value < b.value) {
      return true;
    } else if (a.value > b.value) {
      return false;
    } else {
      return a.ord > b.ord;
    }
  }
}
