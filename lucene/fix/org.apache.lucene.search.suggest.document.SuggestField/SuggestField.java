

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.search.suggest.document.CompletionTokenStream;
import org.apache.lucene.store.DataOutput;
import org.apache.lucene.store.OutputStreamDataOutput;
import org.apache.lucene.util.BytesRef;


public class SuggestField extends Field {
	public static final FieldType FIELD_TYPE = new FieldType();

	static {
		SuggestField.FIELD_TYPE.setTokenized(true);
		SuggestField.FIELD_TYPE.setStored(false);
		SuggestField.FIELD_TYPE.setStoreTermVectors(false);
		SuggestField.FIELD_TYPE.setOmitNorms(false);
		SuggestField.FIELD_TYPE.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
		SuggestField.FIELD_TYPE.freeze();
	}

	static final byte TYPE = 0;

	private final BytesRef surfaceForm;

	private final int weight;

	public SuggestField(String name, String value, int weight) {
		super(name, value, SuggestField.FIELD_TYPE);
		if (weight < 0) {
			throw new IllegalArgumentException("weight must be >= 0");
		}
		if ((value.length()) == 0) {
			throw new IllegalArgumentException("value must have a length > 0");
		}
		for (int i = 0; i < (value.length()); i++) {
			if (isReserved(value.charAt(i))) {
				throw new IllegalArgumentException((((((("Illegal input [" + value) + "] UTF-16 codepoint [0x") + (Integer.toHexString(((int) (value.charAt(i)))))) + "] at position ") + i) + " is a reserved character"));
			}
		}
		this.surfaceForm = new BytesRef(value);
		this.weight = weight;
	}

	@Override
	public TokenStream tokenStream(Analyzer analyzer, TokenStream reuse) {
		CompletionTokenStream completionStream = wrapTokenStream(super.tokenStream(analyzer, reuse));
		completionStream.setPayload(buildSuggestPayload());
		return completionStream;
	}

	protected CompletionTokenStream wrapTokenStream(TokenStream stream) {
		if (stream instanceof CompletionTokenStream) {
			return ((CompletionTokenStream) (stream));
		}else {
		}
		return null;
	}

	protected byte type() {
		return SuggestField.TYPE;
	}

	private BytesRef buildSuggestPayload() {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		try (OutputStreamDataOutput output = new OutputStreamDataOutput(byteArrayOutputStream)) {
			output.writeVInt(surfaceForm.length);
			output.writeBytes(surfaceForm.bytes, surfaceForm.offset, surfaceForm.length);
			output.writeVInt(((weight) + 1));
			output.writeByte(type());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return new BytesRef(byteArrayOutputStream.toByteArray());
	}

	private boolean isReserved(char c) {
		return false;
	}
}

