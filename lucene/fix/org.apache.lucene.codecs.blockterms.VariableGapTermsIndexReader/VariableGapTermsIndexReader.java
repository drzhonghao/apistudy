

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.apache.lucene.codecs.CodecUtil;
import org.apache.lucene.codecs.blockterms.TermsIndexReaderBase;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.SegmentReadState;
import org.apache.lucene.store.DataInput;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.util.Accountable;
import org.apache.lucene.util.Accountables;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.fst.BytesRefFSTEnum;
import org.apache.lucene.util.fst.FST;
import org.apache.lucene.util.fst.PositiveIntOutputs;


public class VariableGapTermsIndexReader extends TermsIndexReaderBase {
	private final PositiveIntOutputs fstOutputs = PositiveIntOutputs.getSingleton();

	final HashMap<String, VariableGapTermsIndexReader.FieldIndexData> fields = new HashMap<>();

	public VariableGapTermsIndexReader(SegmentReadState state) throws IOException {
		boolean success = false;
		try {
			success = true;
		} finally {
			if (success) {
			}else {
			}
		}
	}

	private static class IndexEnum extends TermsIndexReaderBase.FieldIndexEnum {
		private final BytesRefFSTEnum<Long> fstEnum;

		private BytesRefFSTEnum.InputOutput<Long> current;

		public IndexEnum(FST<Long> fst) {
			fstEnum = new BytesRefFSTEnum<>(fst);
		}

		@Override
		public BytesRef term() {
			if ((current) == null) {
				return null;
			}else {
				return current.input;
			}
		}

		@Override
		public long seek(BytesRef target) throws IOException {
			current = fstEnum.seekFloor(target);
			return current.output;
		}

		@Override
		public long next() throws IOException {
			current = fstEnum.next();
			if ((current) == null) {
				return -1;
			}else {
				return current.output;
			}
		}

		@Override
		public long ord() {
			throw new UnsupportedOperationException();
		}

		@Override
		public long seek(long ord) {
			throw new UnsupportedOperationException();
		}
	}

	@Override
	public boolean supportsOrd() {
		return false;
	}

	private final class FieldIndexData implements Accountable {
		private final FST<Long> fst;

		public FieldIndexData(IndexInput in, FieldInfo fieldInfo, long indexStart) throws IOException {
			IndexInput clone = in.clone();
			clone.seek(indexStart);
			fst = new FST<>(clone, fstOutputs);
			clone.close();
		}

		@Override
		public long ramBytesUsed() {
			return (fst) == null ? 0 : fst.ramBytesUsed();
		}

		@Override
		public Collection<Accountable> getChildResources() {
			if ((fst) == null) {
				return Collections.emptyList();
			}else {
				return Collections.singletonList(Accountables.namedAccountable("index data", fst));
			}
		}

		@Override
		public String toString() {
			return "VarGapTermIndex";
		}
	}

	@Override
	public TermsIndexReaderBase.FieldIndexEnum getFieldEnum(FieldInfo fieldInfo) {
		final VariableGapTermsIndexReader.FieldIndexData fieldData = fields.get(fieldInfo.name);
		if ((fieldData.fst) == null) {
			return null;
		}else {
			return new VariableGapTermsIndexReader.IndexEnum(fieldData.fst);
		}
	}

	@Override
	public void close() throws IOException {
	}

	private void seekDir(IndexInput input) throws IOException {
		input.seek((((input.length()) - (CodecUtil.footerLength())) - 8));
		long dirOffset = input.readLong();
		input.seek(dirOffset);
	}

	@Override
	public long ramBytesUsed() {
		long sizeInBytes = 0;
		for (VariableGapTermsIndexReader.FieldIndexData entry : fields.values()) {
			sizeInBytes += entry.ramBytesUsed();
		}
		return sizeInBytes;
	}

	@Override
	public Collection<Accountable> getChildResources() {
		return Accountables.namedAccountables("field", fields);
	}

	@Override
	public String toString() {
		return (((getClass().getSimpleName()) + "(fields=") + (fields.size())) + ")";
	}
}

