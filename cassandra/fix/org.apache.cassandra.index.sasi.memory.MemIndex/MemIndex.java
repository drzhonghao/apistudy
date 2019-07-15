

import java.nio.ByteBuffer;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.index.sasi.conf.ColumnIndex;
import org.apache.cassandra.index.sasi.disk.Token;
import org.apache.cassandra.index.sasi.plan.Expression;
import org.apache.cassandra.index.sasi.utils.RangeIterator;


public abstract class MemIndex {
	protected final AbstractType<?> keyValidator;

	protected final ColumnIndex columnIndex;

	protected MemIndex(AbstractType<?> keyValidator, ColumnIndex columnIndex) {
		this.keyValidator = keyValidator;
		this.columnIndex = columnIndex;
	}

	public abstract long add(DecoratedKey key, ByteBuffer value);

	public abstract RangeIterator<Long, Token> search(Expression expression);

	public static MemIndex forColumn(AbstractType<?> keyValidator, ColumnIndex columnIndex) {
		return null;
	}
}

