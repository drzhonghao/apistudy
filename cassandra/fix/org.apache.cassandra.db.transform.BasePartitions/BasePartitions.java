

import java.util.Collections;
import java.util.Set;
import org.apache.cassandra.db.partitions.BasePartitionIterator;
import org.apache.cassandra.db.rows.BaseRowIterator;
import org.apache.cassandra.db.transform.Transformation;
import org.apache.cassandra.utils.Throwables;


public abstract class BasePartitions<R extends BaseRowIterator<?>, I extends BasePartitionIterator<? extends BaseRowIterator<?>>> implements BasePartitionIterator<R> {
	public BasePartitions(I input) {
	}

	BasePartitions(BasePartitions<?, ? extends I> copyFrom) {
	}

	protected BaseRowIterator<?> applyOne(BaseRowIterator<?> value, Transformation transformation) {
		return null;
	}

	void add(Transformation transformation) {
	}

	protected Throwable runOnClose(int length) {
		Throwable fail = null;
		for (int i = 0; i < length; i++) {
			try {
			} catch (Throwable t) {
				fail = Throwables.merge(fail, t);
			}
		}
		return fail;
	}

	public final boolean hasNext() {
		BaseRowIterator<?> next = null;
		try {
			return true;
		} catch (Throwable t) {
			if (next != null)
				Throwables.close(t, Collections.singleton(next));

			throw t;
		}
	}
}

