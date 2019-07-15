

import java.util.ArrayList;
import java.util.List;
import org.apache.lucene.search.vectorhighlight.FieldFragList;
import org.apache.lucene.search.vectorhighlight.FieldPhraseList;
import org.apache.lucene.search.vectorhighlight.FieldPhraseList.WeightedPhraseInfo;
import org.apache.lucene.search.vectorhighlight.FragListBuilder;
import org.apache.lucene.search.vectorhighlight.SimpleFieldFragList;


public class SingleFragListBuilder implements FragListBuilder {
	@Override
	public FieldFragList createFieldFragList(FieldPhraseList fieldPhraseList, int fragCharSize) {
		FieldFragList ffl = new SimpleFieldFragList(fragCharSize);
		List<FieldPhraseList.WeightedPhraseInfo> wpil = new ArrayList<>();
		FieldPhraseList.WeightedPhraseInfo phraseInfo = null;
		while (true) {
			if (phraseInfo == null)
				break;

			wpil.add(phraseInfo);
		} 
		if ((wpil.size()) > 0)
			ffl.add(0, Integer.MAX_VALUE, wpil);

		return ffl;
	}
}

