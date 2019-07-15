import org.apache.lucene.spatial.prefix.tree.CellIterator;
import org.apache.lucene.spatial.prefix.tree.*;


/**
 * A singleton (one Cell) instance of CellIterator.
 *
 * @lucene.internal
 */
class SingletonCellIterator extends CellIterator {

  SingletonCellIterator(Cell cell) {
    this.nextCell = cell;//preload nextCell
  }

  @Override
  public boolean hasNext() {
    thisCell = null;
    return nextCell != null;
  }

}
