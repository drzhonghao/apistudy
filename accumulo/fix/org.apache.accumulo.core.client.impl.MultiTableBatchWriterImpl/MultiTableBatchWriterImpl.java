

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.UncheckedExecutionException;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.BatchWriter;
import org.apache.accumulo.core.client.BatchWriterConfig;
import org.apache.accumulo.core.client.MultiTableBatchWriter;
import org.apache.accumulo.core.client.MutationsRejectedException;
import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.client.TableOfflineException;
import org.apache.accumulo.core.client.impl.ClientContext;
import org.apache.accumulo.core.client.impl.TabletServerBatchWriter;
import org.apache.accumulo.core.data.Mutation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MultiTableBatchWriterImpl implements MultiTableBatchWriter {
	private static final Logger log = LoggerFactory.getLogger(MultiTableBatchWriterImpl.class);

	private AtomicBoolean closed;

	private class TableBatchWriter implements BatchWriter {
		private String tableId;

		TableBatchWriter(String tableId) {
			this.tableId = tableId;
		}

		@Override
		public void addMutation(Mutation m) throws MutationsRejectedException {
			Preconditions.checkArgument((m != null), "m is null");
			bw.addMutation(tableId, m);
		}

		@Override
		public void addMutations(Iterable<Mutation> iterable) throws MutationsRejectedException {
			bw.addMutation(tableId, iterable.iterator());
		}

		@Override
		public void close() {
			throw new UnsupportedOperationException("Must close all tables, can not close an individual table");
		}

		@Override
		public void flush() {
			throw new UnsupportedOperationException("Must flush all tables, can not flush an individual table");
		}
	}

	private TabletServerBatchWriter bw;

	private ConcurrentHashMap<String, BatchWriter> tableWriters;

	private final ClientContext context;

	public MultiTableBatchWriterImpl(ClientContext context, BatchWriterConfig config) {
		Preconditions.checkArgument((context != null), "context is null");
		Preconditions.checkArgument((config != null), "config is null");
		this.context = context;
		this.bw = new TabletServerBatchWriter(context, config);
		tableWriters = new ConcurrentHashMap<>();
		this.closed = new AtomicBoolean(false);
	}

	@Override
	public boolean isClosed() {
		return this.closed.get();
	}

	@Override
	public void close() throws MutationsRejectedException {
		this.closed.set(true);
		bw.close();
	}

	@Override
	protected void finalize() {
		if (!(closed.get())) {
			MultiTableBatchWriterImpl.log.warn(((MultiTableBatchWriterImpl.class.getSimpleName()) + " not shutdown; did you forget to call close()?"));
			try {
				close();
			} catch (MutationsRejectedException mre) {
				MultiTableBatchWriterImpl.log.error(((MultiTableBatchWriterImpl.class.getSimpleName()) + " internal error."), mre);
				throw new RuntimeException(("Exception when closing " + (MultiTableBatchWriterImpl.class.getSimpleName())), mre);
			}
		}
	}

	private String getId(String tableName) throws TableNotFoundException {
		try {
		} catch (UncheckedExecutionException e) {
			Throwable cause = e.getCause();
			MultiTableBatchWriterImpl.log.error(("Unexpected exception when fetching table id for " + tableName));
			if (null == cause) {
				throw new RuntimeException(e);
			}else
				if (cause instanceof TableNotFoundException) {
					throw ((TableNotFoundException) (cause));
				}else
					if (cause instanceof TableOfflineException) {
						throw ((TableOfflineException) (cause));
					}


			throw e;
		}
		return null;
	}

	@Override
	public BatchWriter getBatchWriter(String tableName) throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
		Preconditions.checkArgument((tableName != null), "tableName is null");
		String tableId = getId(tableName);
		BatchWriter tbw = tableWriters.get(tableId);
		if (tbw == null) {
			tbw = new MultiTableBatchWriterImpl.TableBatchWriter(tableId);
			BatchWriter current = tableWriters.putIfAbsent(tableId, tbw);
			return current != null ? current : tbw;
		}else {
			return tbw;
		}
	}

	@Override
	public void flush() throws MutationsRejectedException {
		bw.flush();
	}
}

