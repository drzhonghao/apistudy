

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.FilteringTokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.memory.MemoryIndex;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.uhighlight.AnalysisOffsetStrategy;
import org.apache.lucene.search.uhighlight.FieldOffsetStrategy;
import org.apache.lucene.search.uhighlight.OffsetsEnum;
import org.apache.lucene.search.uhighlight.PhraseHelper;
import org.apache.lucene.util.AttributeSource;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.automaton.Automata;
import org.apache.lucene.util.automaton.Automaton;
import org.apache.lucene.util.automaton.CharacterRunAutomaton;


public class MemoryIndexOffsetStrategy extends AnalysisOffsetStrategy {
	private final MemoryIndex memoryIndex;

	private final LeafReader leafReader;

	private final CharacterRunAutomaton preMemIndexFilterAutomaton;

	public MemoryIndexOffsetStrategy(String field, Predicate<String> fieldMatcher, BytesRef[] extractedTerms, PhraseHelper phraseHelper, CharacterRunAutomaton[] automata, Analyzer analyzer, Function<Query, Collection<Query>> multiTermQueryRewrite) {
		super(field, extractedTerms, phraseHelper, automata, analyzer);
		boolean storePayloads = phraseHelper.hasPositionSensitivity();
		memoryIndex = new MemoryIndex(true, storePayloads);
		leafReader = ((LeafReader) (memoryIndex.createSearcher().getIndexReader()));
		preMemIndexFilterAutomaton = MemoryIndexOffsetStrategy.buildCombinedAutomaton(fieldMatcher, terms, this.automata, phraseHelper, multiTermQueryRewrite);
	}

	private static CharacterRunAutomaton buildCombinedAutomaton(Predicate<String> fieldMatcher, BytesRef[] terms, CharacterRunAutomaton[] automata, PhraseHelper strictPhrases, Function<Query, Collection<Query>> multiTermQueryRewrite) {
		List<CharacterRunAutomaton> allAutomata = new ArrayList<>();
		if ((terms.length) > 0) {
			allAutomata.add(new CharacterRunAutomaton(Automata.makeStringUnion(Arrays.asList(terms))));
		}
		Collections.addAll(allAutomata, automata);
		for (SpanQuery spanQuery : strictPhrases.getSpanQueries()) {
		}
		if ((allAutomata.size()) == 1) {
			return allAutomata.get(0);
		}
		return new CharacterRunAutomaton(Automata.makeEmpty()) {
			@Override
			public boolean run(char[] chars, int offset, int length) {
				for (int i = 0; i < (allAutomata.size()); i++) {
					if (allAutomata.get(i).run(chars, offset, length)) {
						return true;
					}
				}
				return false;
			}
		};
	}

	@Override
	public OffsetsEnum getOffsetsEnum(IndexReader reader, int docId, String content) throws IOException {
		TokenStream tokenStream = tokenStream(content);
		tokenStream = MemoryIndexOffsetStrategy.newKeepWordFilter(tokenStream, preMemIndexFilterAutomaton);
		memoryIndex.reset();
		memoryIndex.addField(field, tokenStream);
		docId = 0;
		return createOffsetsEnumFromReader(leafReader, docId);
	}

	private static FilteringTokenFilter newKeepWordFilter(final TokenStream tokenStream, final CharacterRunAutomaton charRunAutomaton) {
		return new FilteringTokenFilter(tokenStream) {
			final CharTermAttribute charAtt = addAttribute(CharTermAttribute.class);

			@Override
			protected boolean accept() throws IOException {
				return charRunAutomaton.run(charAtt.buffer(), 0, charAtt.length());
			}
		};
	}
}

