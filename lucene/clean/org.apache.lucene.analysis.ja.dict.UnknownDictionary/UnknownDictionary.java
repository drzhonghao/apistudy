import org.apache.lucene.analysis.ja.dict.BinaryDictionary;
import org.apache.lucene.analysis.ja.dict.CharacterDefinition;
import org.apache.lucene.analysis.ja.dict.*;



import java.io.IOException;

/**
 * Dictionary for unknown-word handling.
 */
public final class UnknownDictionary extends BinaryDictionary {

  private final CharacterDefinition characterDefinition = CharacterDefinition.getInstance();
  
  private UnknownDictionary() throws IOException {
    super();
  }
  
  public int lookup(char[] text, int offset, int len) {
    if(!characterDefinition.isGroup(text[offset])) {
      return 1;
    }
    
    // Extract unknown word. Characters with the same character class are considered to be part of unknown word
    byte characterIdOfFirstCharacter = characterDefinition.getCharacterClass(text[offset]);
    int length = 1;
    for (int i = 1; i < len; i++) {
      if (characterIdOfFirstCharacter == characterDefinition.getCharacterClass(text[offset+i])){
        length++;
      } else {
        break;
      }
    }
    
    return length;
  }
  
  public CharacterDefinition getCharacterDefinition() {
    return characterDefinition;
  }
  
  @Override
  public String getReading(int wordId, char surface[], int off, int len) {
    return null;
  }

  @Override
  public String getInflectionType(int wordId) {
    return null;
  }

  @Override
  public String getInflectionForm(int wordId) {
    return null;
  }

  public static UnknownDictionary getInstance() {
    return SingletonHolder.INSTANCE;
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
