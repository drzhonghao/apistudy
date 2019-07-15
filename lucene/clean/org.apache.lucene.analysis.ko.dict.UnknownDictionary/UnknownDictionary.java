import org.apache.lucene.analysis.ko.dict.BinaryDictionary;
import org.apache.lucene.analysis.ko.dict.CharacterDefinition;
import org.apache.lucene.analysis.ko.dict.*;



import java.io.IOException;

/**
 * Dictionary for unknown-word handling.
 */
public final class UnknownDictionary extends BinaryDictionary {
  private final CharacterDefinition characterDefinition = CharacterDefinition.getInstance();

  private UnknownDictionary() throws IOException {
    super();
  }

  public CharacterDefinition getCharacterDefinition() {
    return characterDefinition;
  }

  public static UnknownDictionary getInstance() {
    return SingletonHolder.INSTANCE;
  }

  @Override
  public String getReading(int wordId) {
    return null;
  }

  @Override
  public Morpheme[] getMorphemes(int wordId, char[] surfaceForm, int off, int len) {
    return null;
  }

  private static class SingletonHolder {
    static final UnknownDictionary INSTANCE;

    static {
      try {
        INSTANCE = new UnknownDictionary();
      } catch (IOException ioe) {
        throw new RuntimeException("Cannot load UnknownDictionary.", ioe);
      }
    }
  }
}
