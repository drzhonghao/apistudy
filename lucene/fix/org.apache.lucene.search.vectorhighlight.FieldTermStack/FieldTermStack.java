

import java.io.IOException;
import java.util.AbstractSequentialList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.vectorhighlight.FieldQuery;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefIterator;
import org.apache.lucene.util.CharsRefBuilder;


public class FieldTermStack {
	private final String fieldName;

	LinkedList<FieldTermStack.TermInfo> termList = new LinkedList<>();

	public FieldTermStack(IndexReader reader, int docId, String fieldName, final FieldQuery fieldQuery) throws IOException {
		this.fieldName = fieldName;
		final Fields vectors = reader.getTermVectors(docId);
		if (vectors == null) {
			return;
		}
		final Terms vector = vectors.terms(fieldName);
		if ((vector == null) || ((vector.hasPositions()) == false)) {
			return;
		}
		final CharsRefBuilder spare = new CharsRefBuilder();
		final TermsEnum termsEnum = vector.iterator();
		PostingsEnum dpEnum = null;
		BytesRef text;
		int numDocs = reader.maxDoc();
		while ((text = termsEnum.next()) != null) {
			spare.copyUTF8Bytes(text);
			final String term = spare.toString();
			dpEnum = termsEnum.postings(dpEnum, PostingsEnum.POSITIONS);
			dpEnum.nextDoc();
			final float weight = ((float) ((Math.log((numDocs / ((double) ((reader.docFreq(new Term(fieldName, text))) + 1))))) + 1.0));
			final int freq = dpEnum.freq();
			for (int i = 0; i < freq; i++) {
				int pos = dpEnum.nextPosition();
				if ((dpEnum.startOffset()) < 0) {
					return;
				}
				termList.add(new FieldTermStack.TermInfo(term, dpEnum.startOffset(), dpEnum.endOffset(), pos, weight));
			}
		} 
		Collections.sort(termList);
		int currentPos = -1;
		FieldTermStack.TermInfo previous = null;
		FieldTermStack.TermInfo first = null;
		Iterator<FieldTermStack.TermInfo> iterator = termList.iterator();
		while (iterator.hasNext()) {
			FieldTermStack.TermInfo current = iterator.next();
			if ((current.position) == currentPos) {
				assert previous != null;
				previous.setNext(current);
				previous = current;
				iterator.remove();
			}else {
				if (previous != null) {
					previous.setNext(first);
				}
				previous = first = current;
				currentPos = current.position;
			}
		} 
		if (previous != null) {
			previous.setNext(first);
		}
	}

	public String getFieldName() {
		return fieldName;
	}

	public FieldTermStack.TermInfo pop() {
		return termList.poll();
	}

	public void push(FieldTermStack.TermInfo termInfo) {
		termList.push(termInfo);
	}

	public boolean isEmpty() {
		return ((termList) == null) || ((termList.size()) == 0);
	}

	public static class TermInfo implements Comparable<FieldTermStack.TermInfo> {
		private final String text;

		private final int startOffset;

		private final int endOffset;

		private final int position;

		private final float weight;

		private FieldTermStack.TermInfo next;

		public TermInfo(String text, int startOffset, int endOffset, int position, float weight) {
			this.text = text;
			this.startOffset = startOffset;
			this.endOffset = endOffset;
			this.position = position;
			this.weight = weight;
			this.next = this;
		}

		void setNext(FieldTermStack.TermInfo next) {
			this.next = next;
		}

		public FieldTermStack.TermInfo getNext() {
			return next;
		}

		public String getText() {
			return text;
		}

		public int getStartOffset() {
			return startOffset;
		}

		public int getEndOffset() {
			return endOffset;
		}

		public int getPosition() {
			return position;
		}

		public float getWeight() {
			return weight;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(text).append('(').append(startOffset).append(',').append(endOffset).append(',').append(position).append(')');
			return sb.toString();
		}

		@Override
		public int compareTo(FieldTermStack.TermInfo o) {
			return (this.position) - (o.position);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = (prime * result) + (position);
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if ((this) == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if ((getClass()) != (obj.getClass())) {
				return false;
			}
			FieldTermStack.TermInfo other = ((FieldTermStack.TermInfo) (obj));
			if ((position) != (other.position)) {
				return false;
			}
			return true;
		}
	}
}

