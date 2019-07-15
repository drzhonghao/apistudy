import org.apache.accumulo.core.data.Mutation;
import org.apache.accumulo.core.data.Condition;
import org.apache.accumulo.core.data.ByteSequence;
import org.apache.accumulo.core.data.*;


import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.hadoop.io.Text;

/**
 * A Mutation that contains a list of conditions that must all be met before the mutation is
 * applied.
 *
 * @since 1.6.0
 */
public class ConditionalMutation extends Mutation {

  private List<Condition> conditions = new ArrayList<>();

  public ConditionalMutation(byte[] row, Condition... conditions) {
    super(row);
    init(conditions);
  }

  public ConditionalMutation(byte[] row, int start, int length, Condition... conditions) {
    super(row, start, length);
    init(conditions);
  }

  public ConditionalMutation(Text row, Condition... conditions) {
    super(row);
    init(conditions);
  }

  public ConditionalMutation(CharSequence row, Condition... conditions) {
    super(row);
    init(conditions);
  }

  public ConditionalMutation(ByteSequence row, Condition... conditions) {
    // TODO add ByteSequence methods to mutations
    super(row.toArray());
    init(conditions);
  }

  public ConditionalMutation(ConditionalMutation cm) {
    super(cm);
    this.conditions = new ArrayList<>(cm.conditions);
  }

  private void init(Condition... conditions) {
    checkArgument(conditions != null, "conditions is null");
    this.conditions.addAll(Arrays.asList(conditions));
  }

  public void addCondition(Condition condition) {
    checkArgument(condition != null, "condition is null");
    this.conditions.add(condition);
  }

  public List<Condition> getConditions() {
    return Collections.unmodifiableList(conditions);
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o == null || !(o instanceof ConditionalMutation)) {
      return false;
    }
    ConditionalMutation cm = (ConditionalMutation) o;
    if (!conditions.equals(cm.conditions)) {
      return false;
    }
    return super.equals(o);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 37 * result + conditions.hashCode();
    return result;
  }

  @Override
  public void setReplicationSources(Set<String> sources) {
    throw new UnsupportedOperationException(
        "Conditional Mutations are not supported for replication");
  }
}
