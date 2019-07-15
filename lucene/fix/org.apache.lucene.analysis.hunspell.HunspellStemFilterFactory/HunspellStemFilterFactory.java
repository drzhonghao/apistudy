

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.hunspell.Dictionary;
import org.apache.lucene.analysis.hunspell.HunspellStemFilter;
import org.apache.lucene.analysis.util.AbstractAnalysisFactory;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoaderAware;
import org.apache.lucene.analysis.util.TokenFilterFactory;


public class HunspellStemFilterFactory extends TokenFilterFactory implements ResourceLoaderAware {
	private static final String PARAM_DICTIONARY = "dictionary";

	private static final String PARAM_AFFIX = "affix";

	private static final String PARAM_RECURSION_CAP = "recursionCap";

	private static final String PARAM_IGNORE_CASE = "ignoreCase";

	private static final String PARAM_LONGEST_ONLY = "longestOnly";

	private final String dictionaryFiles;

	private final String affixFile;

	private final boolean ignoreCase;

	private final boolean longestOnly;

	private Dictionary dictionary;

	public HunspellStemFilterFactory(Map<String, String> args) {
		super(args);
		dictionaryFiles = require(args, HunspellStemFilterFactory.PARAM_DICTIONARY);
		affixFile = get(args, HunspellStemFilterFactory.PARAM_AFFIX);
		ignoreCase = getBoolean(args, HunspellStemFilterFactory.PARAM_IGNORE_CASE, false);
		longestOnly = getBoolean(args, HunspellStemFilterFactory.PARAM_LONGEST_ONLY, false);
		getBoolean(args, "strictAffixParsing", true);
		getInt(args, "recursionCap", 0);
		if (!(args.isEmpty())) {
			throw new IllegalArgumentException(("Unknown parameters: " + args));
		}
	}

	@Override
	public void inform(ResourceLoader loader) throws IOException {
		String[] dicts = dictionaryFiles.split(",");
		InputStream affix = null;
		List<InputStream> dictionaries = new ArrayList<>();
		dictionaries = new ArrayList<>();
		for (String file : dicts) {
			dictionaries.add(loader.openResource(file));
		}
		affix = loader.openResource(affixFile);
	}

	@Override
	public TokenStream create(TokenStream tokenStream) {
		return new HunspellStemFilter(tokenStream, dictionary, true, longestOnly);
	}
}

