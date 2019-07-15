

import java.io.IOException;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.FilterLeafReader;
import org.apache.lucene.index.MergeState;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.MultiTerms;
import org.apache.lucene.index.MultiTermsEnum;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;


public class MappedMultiFields extends FilterLeafReader.FilterFields {
	final MergeState mergeState;

	public MappedMultiFields(MergeState mergeState, MultiFields multiFields) {
		super(multiFields);
		this.mergeState = mergeState;
	}

	@Override
	public Terms terms(String field) throws IOException {
		MultiTerms terms = ((MultiTerms) (in.terms(field)));
		if (terms == null) {
			return null;
		}else {
			return new MappedMultiFields.MappedMultiTerms(field, mergeState, terms);
		}
	}

	private static class MappedMultiTerms extends FilterLeafReader.FilterTerms {
		final MergeState mergeState;

		final String field;

		public MappedMultiTerms(String field, MergeState mergeState, MultiTerms multiTerms) {
			super(multiTerms);
			this.field = field;
			this.mergeState = mergeState;
		}

		@Override
		public TermsEnum iterator() throws IOException {
			TermsEnum iterator = in.iterator();
			if (iterator == (TermsEnum.EMPTY)) {
				return TermsEnum.EMPTY;
			}else {
				return new MappedMultiFields.MappedMultiTermsEnum(field, mergeState, ((MultiTermsEnum) (iterator)));
			}
		}

		@Override
		public long size() throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public long getSumTotalTermFreq() throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public long getSumDocFreq() throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public int getDocCount() throws IOException {
			throw new UnsupportedOperationException();
		}
	}

	private static class MappedMultiTermsEnum extends FilterLeafReader.FilterTermsEnum {
		final MergeState mergeState;

		final String field;

		public MappedMultiTermsEnum(String field, MergeState mergeState, MultiTermsEnum multiTermsEnum) {
			super(multiTermsEnum);
			this.field = field;
			this.mergeState = mergeState;
		}

		@Override
		public int docFreq() throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public long totalTermFreq() throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public PostingsEnum postings(PostingsEnum reuse, int flags) throws IOException {
			return null;
		}
	}
}

