

import java.util.ArrayList;
import org.apache.lucene.benchmark.byTask.feeds.DocMaker;
import org.apache.lucene.benchmark.byTask.feeds.SimpleQueryMaker;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;


public class SimpleSloppyPhraseQueryMaker extends SimpleQueryMaker {
	@Override
	protected Query[] prepareQueries() throws Exception {
		String[] words;
		ArrayList<String> w = new ArrayList<>();
		words = w.toArray(new String[0]);
		ArrayList<Query> queries = new ArrayList<>();
		for (int slop = 0; slop < 8; slop++) {
			for (int qlen = 2; qlen < 6; qlen++) {
				for (int wd = 0; wd < (((words.length) - qlen) - slop); wd++) {
					int remainedSlop = slop;
					int wind = wd;
					PhraseQuery.Builder builder = new PhraseQuery.Builder();
					for (int i = 0; i < qlen; i++) {
						builder.add(new Term(DocMaker.BODY_FIELD, words[(wind++)]), i);
						if (remainedSlop > 0) {
							remainedSlop--;
							wind++;
						}
					}
					builder.setSlop(slop);
					PhraseQuery q = builder.build();
					queries.add(q);
					remainedSlop = slop;
					wind = ((wd + qlen) + remainedSlop) - 1;
					builder = new PhraseQuery.Builder();
					for (int i = 0; i < qlen; i++) {
						builder.add(new Term(DocMaker.BODY_FIELD, words[(wind--)]), i);
						if (remainedSlop > 0) {
							remainedSlop--;
							wind--;
						}
					}
					builder.setSlop((slop + (2 * qlen)));
					q = builder.build();
					queries.add(q);
				}
			}
		}
		return queries.toArray(new Query[0]);
	}
}

