import org.apache.lucene.search.suggest.tst.*;


import org.apache.lucene.util.RamUsageEstimator;

/**
 * The class creates a TST node.
 */

public class TernaryTreeNode {
  
  /** Creates a new empty node */ 
  public TernaryTreeNode() {}
  /** the character stored by a node. */
  char splitchar;
  /** a reference object to the node containing character smaller than this node's character. */
  TernaryTreeNode loKid;
  /** 
   *  a reference object to the node containing character next to this node's character as 
   *  occurring in the inserted token.
   */
  TernaryTreeNode eqKid;
  /** a reference object to the node containing character higher than this node's character. */
  TernaryTreeNode hiKid;
  /** 
   * used by leaf nodes to store the complete tokens to be added to suggest list while 
   * auto-completing the prefix.
   */
  String token;
  Object val;

  long sizeInBytes() {
    long mem = RamUsageEstimator.shallowSizeOf(this);
    if (loKid != null) {
      mem += loKid.sizeInBytes();
    }
    if (eqKid != null) {
      mem += eqKid.sizeInBytes();
    }
    if (hiKid != null) {
      mem += hiKid.sizeInBytes();
    }
    if (token != null) {
      mem += RamUsageEstimator.shallowSizeOf(token) + RamUsageEstimator.NUM_BYTES_ARRAY_HEADER + Character.BYTES * token.length();
    }
    mem += RamUsageEstimator.shallowSizeOf(val);
    return mem;
  }
}
