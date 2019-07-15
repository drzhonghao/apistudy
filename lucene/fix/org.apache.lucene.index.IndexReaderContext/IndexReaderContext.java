

import java.util.List;
import org.apache.lucene.index.CompositeReaderContext;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReaderContext;


public abstract class IndexReaderContext {
	public final CompositeReaderContext parent;

	public final boolean isTopLevel;

	public final int docBaseInParent;

	public final int ordInParent;

	final Object identity = new Object();

	IndexReaderContext(CompositeReaderContext parent, int ordInParent, int docBaseInParent) {
		this.parent = parent;
		this.docBaseInParent = docBaseInParent;
		this.ordInParent = ordInParent;
		this.isTopLevel = parent == null;
	}

	public Object id() {
		return identity;
	}

	public abstract IndexReader reader();

	public abstract List<LeafReaderContext> leaves() throws UnsupportedOperationException;

	public abstract List<IndexReaderContext> children();
}

