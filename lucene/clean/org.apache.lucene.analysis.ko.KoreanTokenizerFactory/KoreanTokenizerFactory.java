import org.apache.lucene.analysis.ko.KoreanTokenizer;
import org.apache.lucene.analysis.ko.*;


import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.util.Locale;
import java.util.Map;

import org.apache.lucene.analysis.ko.dict.UserDictionary;
import org.apache.lucene.analysis.util.TokenizerFactory;
import org.apache.lucene.util.AttributeFactory;
import org.apache.lucene.util.IOUtils;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoaderAware;

/**
 * Factory for {@link KoreanTokenizer}.
 * @lucene.experimental
 */
public class KoreanTokenizerFactory extends TokenizerFactory implements ResourceLoaderAware {
  private static final String USER_DICT_PATH = "userDictionary";
  private static final String USER_DICT_ENCODING = "userDictionaryEncoding";
  private static final String DECOMPOUND_MODE = "decompoundMode";
  private static final String OUTPUT_UNKNOWN_UNIGRAMS = "outputUnknownUnigrams";

  private final String userDictionaryPath;
  private final String userDictionaryEncoding;
  private UserDictionary userDictionary;

  private final KoreanTokenizer.DecompoundMode mode;
  private final boolean outputUnknownUnigrams;

  /** Creates a new KoreanTokenizerFactory */
  public KoreanTokenizerFactory(Map<String, String> args) {
    super(args);
    userDictionaryPath = args.remove(USER_DICT_PATH);
    userDictionaryEncoding = args.remove(USER_DICT_ENCODING);
    mode = KoreanTokenizer.DecompoundMode.valueOf(get(args, DECOMPOUND_MODE, KoreanTokenizer.DEFAULT_DECOMPOUND.toString()).toUpperCase(Locale.ROOT));
    outputUnknownUnigrams = getBoolean(args, OUTPUT_UNKNOWN_UNIGRAMS, false);

    if (!args.isEmpty()) {
      throw new IllegalArgumentException("Unknown parameters: " + args);
    }
  }

  @Override
  public void inform(ResourceLoader loader) throws IOException {
    if (userDictionaryPath != null) {
      try (InputStream stream = loader.openResource(userDictionaryPath)) {
        String encoding = userDictionaryEncoding;
        if (encoding == null) {
          encoding = IOUtils.UTF_8;
        }
        CharsetDecoder decoder = Charset.forName(encoding).newDecoder()
          .onMalformedInput(CodingErrorAction.REPORT)
          .onUnmappableCharacter(CodingErrorAction.REPORT);
        Reader reader = new InputStreamReader(stream, decoder);
        userDictionary = UserDictionary.open(reader);
      }
    } else {
      userDictionary = null;
    }
  }

  @Override
  public KoreanTokenizer create(AttributeFactory factory) {
    return new KoreanTokenizer(factory, userDictionary, mode, outputUnknownUnigrams);
  }
}
