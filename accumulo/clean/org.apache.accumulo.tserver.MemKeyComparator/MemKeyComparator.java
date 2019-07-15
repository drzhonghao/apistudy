import org.apache.accumulo.tserver.*;


import java.io.Serializable;
import java.util.Comparator;

import org.apache.accumulo.core.data.Key;

class MemKeyComparator implements Comparator<Key>, Serializable {

  private static final long serialVersionUID = 1L;

  @Override
  public int compare(Key k1, Key k2) {
    int cmp = k1.compareTo(k2);

    if (cmp == 0) {
      if (k1 instanceof MemKey)
        if (k2 instanceof MemKey)
          cmp = ((MemKey) k2).getKVCount() - ((MemKey) k1).getKVCount();
        else
          cmp = 1;
      else if (k2 instanceof MemKey)
        cmp = -1;
    }

    return cmp;
  }
}
