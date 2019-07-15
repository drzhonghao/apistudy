

import java.nio.ByteBuffer;
import org.apache.cassandra.db.BufferDecoratedKey;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.rows.Row;
import org.apache.cassandra.dht.Token;
import org.apache.cassandra.utils.concurrent.OpOrder;
import org.apache.cassandra.utils.memory.AbstractAllocator;
import org.apache.cassandra.utils.memory.MemtableAllocator;


public abstract class MemtableBufferAllocator extends MemtableAllocator {
	public Row.Builder rowBuilder(OpOrder.Group writeOp) {
		return allocator(writeOp).cloningBTreeRowBuilder();
	}

	public DecoratedKey clone(DecoratedKey key, OpOrder.Group writeOp) {
		return new BufferDecoratedKey(key.getToken(), allocator(writeOp).clone(key.getKey()));
	}

	public abstract ByteBuffer allocate(int size, OpOrder.Group opGroup);

	protected AbstractAllocator allocator(OpOrder.Group writeOp) {
		return null;
	}
}

