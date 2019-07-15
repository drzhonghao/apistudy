import org.apache.lucene.search.DisiWrapper;
import org.apache.lucene.search.intervals.*;


import org.apache.lucene.search.DocIdSetIterator;

class DisiWrapper {

  public final DocIdSetIterator iterator;
  public final IntervalIterator intervals;
  public final long cost;
  public final float matchCost; // the match cost for two-phase iterators, 0 otherwise
  public int doc; // the current doc, used for comparison
  public DisiWrapper next; // reference to a next element, see #topList

  // An approximation of the iterator, or the iterator itself if it does not
  // support two-phase iteration
  public final DocIdSetIterator approximation;

  public DisiWrapper(IntervalIterator iterator) {
    this.intervals = iterator;
    this.iterator = iterator;
    this.cost = iterator.cost();
    this.doc = -1;
    this.approximation = iterator;
    this.matchCost = iterator.matchCost();
  }

}
