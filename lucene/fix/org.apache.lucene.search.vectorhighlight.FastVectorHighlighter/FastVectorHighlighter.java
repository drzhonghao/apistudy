

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.highlight.Encoder;
import org.apache.lucene.search.vectorhighlight.FieldFragList;
import org.apache.lucene.search.vectorhighlight.FieldPhraseList;
import org.apache.lucene.search.vectorhighlight.FieldQuery;
import org.apache.lucene.search.vectorhighlight.FieldTermStack;
import org.apache.lucene.search.vectorhighlight.FragListBuilder;
import org.apache.lucene.search.vectorhighlight.FragmentsBuilder;
import org.apache.lucene.search.vectorhighlight.ScoreOrderFragmentsBuilder;
import org.apache.lucene.search.vectorhighlight.SimpleFragListBuilder;


public class FastVectorHighlighter {
	public static final boolean DEFAULT_PHRASE_HIGHLIGHT = true;

	public static final boolean DEFAULT_FIELD_MATCH = true;

	private final boolean phraseHighlight;

	private final boolean fieldMatch;

	private final FragListBuilder fragListBuilder;

	private final FragmentsBuilder fragmentsBuilder;

	private int phraseLimit = Integer.MAX_VALUE;

	public FastVectorHighlighter() {
		this(FastVectorHighlighter.DEFAULT_PHRASE_HIGHLIGHT, FastVectorHighlighter.DEFAULT_FIELD_MATCH);
	}

	public FastVectorHighlighter(boolean phraseHighlight, boolean fieldMatch) {
		this(phraseHighlight, fieldMatch, new SimpleFragListBuilder(), new ScoreOrderFragmentsBuilder());
	}

	public FastVectorHighlighter(boolean phraseHighlight, boolean fieldMatch, FragListBuilder fragListBuilder, FragmentsBuilder fragmentsBuilder) {
		this.phraseHighlight = phraseHighlight;
		this.fieldMatch = fieldMatch;
		this.fragListBuilder = fragListBuilder;
		this.fragmentsBuilder = fragmentsBuilder;
	}

	public FieldQuery getFieldQuery(Query query) {
		return null;
	}

	public FieldQuery getFieldQuery(Query query, IndexReader reader) throws IOException {
		return null;
	}

	public final String getBestFragment(final FieldQuery fieldQuery, IndexReader reader, int docId, String fieldName, int fragCharSize) throws IOException {
		FieldFragList fieldFragList = getFieldFragList(fragListBuilder, fieldQuery, reader, docId, fieldName, fragCharSize);
		return fragmentsBuilder.createFragment(reader, docId, fieldName, fieldFragList);
	}

	public final String[] getBestFragments(final FieldQuery fieldQuery, IndexReader reader, int docId, String fieldName, int fragCharSize, int maxNumFragments) throws IOException {
		FieldFragList fieldFragList = getFieldFragList(fragListBuilder, fieldQuery, reader, docId, fieldName, fragCharSize);
		return fragmentsBuilder.createFragments(reader, docId, fieldName, fieldFragList, maxNumFragments);
	}

	public final String getBestFragment(final FieldQuery fieldQuery, IndexReader reader, int docId, String fieldName, int fragCharSize, FragListBuilder fragListBuilder, FragmentsBuilder fragmentsBuilder, String[] preTags, String[] postTags, Encoder encoder) throws IOException {
		FieldFragList fieldFragList = getFieldFragList(fragListBuilder, fieldQuery, reader, docId, fieldName, fragCharSize);
		return fragmentsBuilder.createFragment(reader, docId, fieldName, fieldFragList, preTags, postTags, encoder);
	}

	public final String[] getBestFragments(final FieldQuery fieldQuery, IndexReader reader, int docId, String fieldName, int fragCharSize, int maxNumFragments, FragListBuilder fragListBuilder, FragmentsBuilder fragmentsBuilder, String[] preTags, String[] postTags, Encoder encoder) throws IOException {
		FieldFragList fieldFragList = getFieldFragList(fragListBuilder, fieldQuery, reader, docId, fieldName, fragCharSize);
		return fragmentsBuilder.createFragments(reader, docId, fieldName, fieldFragList, maxNumFragments, preTags, postTags, encoder);
	}

	public final String[] getBestFragments(final FieldQuery fieldQuery, IndexReader reader, int docId, String storedField, Set<String> matchedFields, int fragCharSize, int maxNumFragments, FragListBuilder fragListBuilder, FragmentsBuilder fragmentsBuilder, String[] preTags, String[] postTags, Encoder encoder) throws IOException {
		FieldFragList fieldFragList = getFieldFragList(fragListBuilder, fieldQuery, reader, docId, matchedFields, fragCharSize);
		return fragmentsBuilder.createFragments(reader, docId, storedField, fieldFragList, maxNumFragments, preTags, postTags, encoder);
	}

	private FieldFragList getFieldFragList(FragListBuilder fragListBuilder, final FieldQuery fieldQuery, IndexReader reader, int docId, String matchedField, int fragCharSize) throws IOException {
		FieldTermStack fieldTermStack = new FieldTermStack(reader, docId, matchedField, fieldQuery);
		FieldPhraseList fieldPhraseList = new FieldPhraseList(fieldTermStack, fieldQuery, phraseLimit);
		return fragListBuilder.createFieldFragList(fieldPhraseList, fragCharSize);
	}

	private FieldFragList getFieldFragList(FragListBuilder fragListBuilder, final FieldQuery fieldQuery, IndexReader reader, int docId, Set<String> matchedFields, int fragCharSize) throws IOException {
		Iterator<String> matchedFieldsItr = matchedFields.iterator();
		if (!(matchedFieldsItr.hasNext())) {
			throw new IllegalArgumentException("matchedFields must contain at least on field name.");
		}
		FieldPhraseList[] toMerge = new FieldPhraseList[matchedFields.size()];
		int i = 0;
		while (matchedFieldsItr.hasNext()) {
			FieldTermStack stack = new FieldTermStack(reader, docId, matchedFieldsItr.next(), fieldQuery);
			toMerge[(i++)] = new FieldPhraseList(stack, fieldQuery, phraseLimit);
		} 
		return fragListBuilder.createFieldFragList(new FieldPhraseList(toMerge), fragCharSize);
	}

	public boolean isPhraseHighlight() {
		return phraseHighlight;
	}

	public boolean isFieldMatch() {
		return fieldMatch;
	}

	public int getPhraseLimit() {
		return phraseLimit;
	}

	public void setPhraseLimit(int phraseLimit) {
		this.phraseLimit = phraseLimit;
	}
}

