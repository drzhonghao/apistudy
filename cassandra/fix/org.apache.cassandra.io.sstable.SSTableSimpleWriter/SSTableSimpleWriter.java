

import com.google.common.base.Throwables;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.PartitionColumns;
import org.apache.cassandra.db.partitions.AbstractBTreePartition;
import org.apache.cassandra.db.partitions.PartitionUpdate;
import org.apache.cassandra.db.rows.UnfilteredRowIterator;
import org.apache.cassandra.io.sstable.SSTableTxnWriter;
import org.apache.cassandra.io.sstable.format.SSTableReader;
import org.apache.cassandra.utils.concurrent.Transactional;


class SSTableSimpleWriter {
	protected DecoratedKey currentKey;

	protected PartitionUpdate update;

	private SSTableTxnWriter writer;

	protected SSTableSimpleWriter(File directory, CFMetaData metadata, PartitionColumns columns) {
	}

	private SSTableTxnWriter getOrCreateWriter() {
		if ((writer) == null) {
		}
		return writer;
	}

	PartitionUpdate getUpdateFor(DecoratedKey key) throws IOException {
		assert key != null;
		if (!(key.equals(currentKey))) {
			if ((update) != null)
				writePartition(update);

			currentKey = key;
		}
		assert (update) != null;
		return update;
	}

	public void close() {
		try {
			if ((update) != null)
				writePartition(update);

			if ((writer) != null)
				writer.finish(false);

		} catch (Throwable t) {
			throw Throwables.propagate(((writer) == null ? t : writer.abort(t)));
		}
	}

	private void writePartition(PartitionUpdate update) throws IOException {
		getOrCreateWriter().append(update.unfilteredIterator());
	}
}

