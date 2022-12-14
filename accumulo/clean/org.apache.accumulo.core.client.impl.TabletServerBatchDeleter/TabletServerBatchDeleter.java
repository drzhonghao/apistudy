import org.apache.accumulo.core.client.impl.*;


import java.util.Iterator;
import java.util.Map.Entry;

import org.apache.accumulo.core.client.BatchDeleter;
import org.apache.accumulo.core.client.BatchWriter;
import org.apache.accumulo.core.client.BatchWriterConfig;
import org.apache.accumulo.core.client.IteratorSetting;
import org.apache.accumulo.core.client.MutationsRejectedException;
import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Mutation;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.iterators.SortedKeyIterator;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.accumulo.core.security.ColumnVisibility;

public class TabletServerBatchDeleter extends TabletServerBatchReader implements BatchDeleter {

  private final ClientContext context;
  private String tableId;
  private BatchWriterConfig bwConfig;

  public TabletServerBatchDeleter(ClientContext context, String tableId,
      Authorizations authorizations, int numQueryThreads, BatchWriterConfig bwConfig)
      throws TableNotFoundException {
    super(context, tableId, authorizations, numQueryThreads);
    this.context = context;
    this.tableId = tableId;
    this.bwConfig = bwConfig;
    super.addScanIterator(new IteratorSetting(Integer.MAX_VALUE,
        BatchDeleter.class.getName().replaceAll("[.]", "_") + "_NOVALUE", SortedKeyIterator.class));
  }

  @Override
  public void delete() throws MutationsRejectedException, TableNotFoundException {
    BatchWriter bw = null;
    try {
      bw = new BatchWriterImpl(context, tableId, bwConfig);
      Iterator<Entry<Key,Value>> iter = super.iterator();
      while (iter.hasNext()) {
        Entry<Key,Value> next = iter.next();
        Key k = next.getKey();
        Mutation m = new Mutation(k.getRow());
        m.putDelete(k.getColumnFamily(), k.getColumnQualifier(),
            new ColumnVisibility(k.getColumnVisibility()), k.getTimestamp());
        bw.addMutation(m);
      }
    } finally {
      if (bw != null)
        bw.close();
    }
  }

}
