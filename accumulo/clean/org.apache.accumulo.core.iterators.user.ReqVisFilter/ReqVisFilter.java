import org.apache.accumulo.core.iterators.user.*;


import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.iterators.Filter;
import org.apache.accumulo.core.security.ColumnVisibility;

/**
 * A Filter that matches entries with a non-empty ColumnVisibility.
 */
public class ReqVisFilter extends Filter {

  @Override
  public boolean accept(Key k, Value v) {
    ColumnVisibility vis = new ColumnVisibility(k.getColumnVisibility());
    return vis.getExpression().length > 0;
  }

  @Override
  public IteratorOptions describeOptions() {
    IteratorOptions io = super.describeOptions();
    io.setName("reqvis");
    io.setDescription("ReqVisFilter hides entries without a visibility label");
    return io;
  }
}
