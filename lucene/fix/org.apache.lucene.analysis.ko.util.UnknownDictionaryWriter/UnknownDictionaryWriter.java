

import java.io.IOException;
import org.apache.lucene.analysis.ko.dict.CharacterDefinition;


public class UnknownDictionaryWriter {
	public UnknownDictionaryWriter(int size) {
	}

	public int put(String[] entry) {
		int characterId = CharacterDefinition.lookupCharacterClass(entry[0]);
		return 0;
	}

	public void putCharacterCategory(int codePoint, String characterClassName) {
	}

	public void putInvokeDefinition(String characterClassName, int invoke, int group, int length) {
	}

	public void write(String baseDir) throws IOException {
	}
}

