import org.apache.accumulo.core.client.impl.TabletServerBatchWriter;
import org.apache.accumulo.core.client.impl.*;


import static com.google.common.base.Preconditions.checkArgument;

import org.apache.accumulo.core.client.BatchWriter;
import org.apache.accumulo.core.client.BatchWriterConfig;
import org.apache.accumulo.core.client.MutationsRejectedException;
import org.apache.accumulo.core.data.Mutation;

public class BatchWriterImpl implements BatchWriter {

  private final String tableId;
  private final TabletServerBatchWriter bw;

  public BatchWriterImpl(ClientContext context, String tableId, BatchWriterConfig config) {
    checkArgument(context != null, "context is null");
    checkArgument(tableId != null, "tableId is null");
    if (config == null)
      config = new BatchWriterConfig();
    this.tableId = tableId;
    this.bw = new TabletServerBatchWriter(context, config);
  }

  @Override
  public void addMutation(Mutation m) throws MutationsRejectedException {
    checkArgument(m != null, "m is null");
    bw.addMutation(tableId, m);
  }

  @Override
  public void addMutations(Iterable<Mutation> iterable) throws MutationsRejectedException {
    checkArgument(iterable != null, "iterable is null");
    bw.addMutation(tableId, iterable.iterator());
  }

  @Override
  public void close() throws MutationsRejectedException {
    bw.close();
  }

  @Override
  public void flush() throws MutationsRejectedException {
    bw.flush();
  }

}
