import org.apache.accumulo.core.util.*;


import org.apache.accumulo.core.client.ScannerBase;
import org.apache.accumulo.core.data.Column;
import org.apache.accumulo.core.data.ColumnUpdate;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Mutation;
import org.apache.accumulo.core.data.Value;
import org.apache.hadoop.io.Text;

public class ColumnFQ implements Comparable<ColumnFQ> {
  private Text colf;
  private Text colq;

  public ColumnFQ(Text colf, Text colq) {
    if (colf == null || colq == null) {
      throw new IllegalArgumentException();
    }

    this.colf = colf;
    this.colq = colq;
  }

  public ColumnFQ(Key k) {
    this(k.getColumnFamily(), k.getColumnQualifier());
  }

  public ColumnFQ(ColumnUpdate cu) {
    this(new Text(cu.getColumnFamily()), new Text(cu.getColumnQualifier()));
  }

  public Text getColumnQualifier() {
    return colq;
  }

  public Text getColumnFamily() {
    return colf;
  }

  public Column toColumn() {
    return new Column(TextUtil.getBytes(colf), TextUtil.getBytes(colq), null);
  }

  public void fetch(ScannerBase sb) {
    sb.fetchColumn(colf, colq);
  }

  public void put(Mutation m, Value v) {
    m.put(colf, colq, v);
  }

  public void putDelete(Mutation m) {
    m.putDelete(colf, colq);
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof ColumnFQ))
      return false;
    if (this == o)
      return true;
    ColumnFQ ocfq = (ColumnFQ) o;
    return ocfq.colf.equals(colf) && ocfq.colq.equals(colq);
  }

  @Override
  public int hashCode() {
    return colf.hashCode() + colq.hashCode();
  }

  public boolean hasColumns(Key key) {
    return key.compareColumnFamily(colf) == 0 && key.compareColumnQualifier(colq) == 0;
  }

  public boolean equals(Text colf, Text colq) {
    return this.colf.equals(colf) && this.colq.equals(colq);
  }

  @Override
  public int compareTo(ColumnFQ o) {
    int cmp = colf.compareTo(o.colf);

    if (cmp == 0)
      cmp = colq.compareTo(o.colq);

    return cmp;
  }

  @Override
  public String toString() {
    return colf + ":" + colq;
  }

}
