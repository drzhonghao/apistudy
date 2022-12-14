

import java.nio.Buffer;
import java.nio.ByteBuffer;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.index.sasi.conf.ColumnIndex;
import org.apache.cassandra.index.sasi.disk.Token;
import org.apache.cassandra.index.sasi.memory.MemIndex;
import org.apache.cassandra.index.sasi.plan.Expression;
import org.apache.cassandra.index.sasi.utils.RangeIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class IndexMemtable {
	private static final Logger logger = LoggerFactory.getLogger(IndexMemtable.class);

	private final MemIndex index;

	public IndexMemtable(ColumnIndex columnIndex) {
		this.index = MemIndex.forColumn(columnIndex.keyValidator(), columnIndex);
	}

	public long index(DecoratedKey key, ByteBuffer value) {
		if ((value == null) || ((value.remaining()) == 0))
			return 0;

		return index.add(key, value);
	}

	public RangeIterator<Long, Token> search(Expression expression) {
		return (index) == null ? null : index.search(expression);
	}
}

