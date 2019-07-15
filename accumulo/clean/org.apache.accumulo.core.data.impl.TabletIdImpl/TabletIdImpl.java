import org.apache.accumulo.core.data.impl.*;


import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.TabletId;
import org.apache.hadoop.io.Text;

import com.google.common.base.Function;

public class TabletIdImpl implements TabletId {

  private KeyExtent ke;

  @SuppressWarnings("deprecation")
  // @formatter:off
  public static final Function<org.apache.accumulo.core.data.KeyExtent,TabletId> KE_2_TID_OLD =
    new Function<org.apache.accumulo.core.data.KeyExtent,TabletId>() {
    @Override
    public TabletId apply(org.apache.accumulo.core.data.KeyExtent input) {
      // the following if null check is to appease findbugs... grumble grumble spent a good part of
      // my morning looking into this
      // http://sourceforge.net/p/findbugs/bugs/1139/
      // https://code.google.com/p/guava-libraries/issues/detail?id=920
      if (input == null)
        return null;
      return new TabletIdImpl(input);
    }
  };
  // @formatter:on

  @SuppressWarnings("deprecation")
  // @formatter:off
  public static final Function<TabletId,org.apache.accumulo.core.data.KeyExtent> TID_2_KE_OLD =
    new Function<TabletId,org.apache.accumulo.core.data.KeyExtent>() {
    @Override
    public org.apache.accumulo.core.data.KeyExtent apply(TabletId input) {
      if (input == null)
        return null;
      return new org.apache.accumulo.core.data.KeyExtent(input.getTableId(), input.getEndRow(),
          input.getPrevEndRow());
    }
  };
  // @formatter:on

  @Deprecated
  public TabletIdImpl(org.apache.accumulo.core.data.KeyExtent ke) {
    this.ke = new KeyExtent(ke.getTableId().toString(), ke.getEndRow(), ke.getPrevEndRow());
  }

  public TabletIdImpl(KeyExtent ke) {
    this.ke = ke;
  }

  @Override
  public int compareTo(TabletId o) {
    return ke.compareTo(((TabletIdImpl) o).ke);
  }

  @Override
  public Text getTableId() {
    return new Text(ke.getTableId());
  }

  @Override
  public Text getEndRow() {
    return ke.getEndRow();
  }

  @Override
  public Text getPrevEndRow() {
    return ke.getPrevEndRow();
  }

  @Override
  public int hashCode() {
    return ke.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof TabletIdImpl) {
      return ke.equals(((TabletIdImpl) o).ke);
    }

    return false;
  }

  @Override
  public String toString() {
    return ke.toString();
  }

  @Override
  public Range toRange() {
    return ke.toDataRange();
  }
}
