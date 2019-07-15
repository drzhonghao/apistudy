import org.apache.accumulo.core.client.mock.MockAccumulo;
import org.apache.accumulo.core.client.mock.*;


import static com.google.common.base.Preconditions.checkArgument;

import org.apache.accumulo.core.client.BatchWriter;
import org.apache.accumulo.core.client.MutationsRejectedException;
import org.apache.accumulo.core.data.Mutation;

/**
 * @deprecated since 1.8.0; use MiniAccumuloCluster or a standard mock framework instead.
 */
@Deprecated
public class MockBatchWriter implements BatchWriter {

  final String tablename;
  final MockAccumulo acu;

  MockBatchWriter(MockAccumulo acu, String tablename) {
    this.acu = acu;
    this.tablename = tablename;
  }

  @Override
  public void addMutation(Mutation m) throws MutationsRejectedException {
    checkArgument(m != null, "m is null");
    acu.addMutation(tablename, m);
  }

  @Override
  public void addMutations(Iterable<Mutation> iterable) throws MutationsRejectedException {
    checkArgument(iterable != null, "iterable is null");
    for (Mutation m : iterable) {
      acu.addMutation(tablename, m);
    }
  }

  @Override
  public void flush() throws MutationsRejectedException {}

  @Override
  public void close() throws MutationsRejectedException {}

}
