import org.apache.lucene.analysis.ko.util.*;


import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import org.apache.lucene.analysis.ko.dict.CharacterDefinition;

import org.apache.lucene.codecs.CodecUtil;
import org.apache.lucene.store.DataOutput;
import org.apache.lucene.store.OutputStreamDataOutput;

public final class CharacterDefinitionWriter {

  private final byte[] characterCategoryMap = new byte[0x10000];
  
  private final boolean[] invokeMap = new boolean[CharacterDefinition.CLASS_COUNT];
  private final boolean[] groupMap = new boolean[CharacterDefinition.CLASS_COUNT];
    
  /**
   * Constructor for building. TODO: remove write access
   */
  public CharacterDefinitionWriter() {
    Arrays.fill(characterCategoryMap, CharacterDefinition.DEFAULT);
  }
  
  /**
   * Put mapping from unicode code point to character class.
   * 
   * @param codePoint
   *            code point
   * @param characterClassName character class name
   */
  public void putCharacterCategory(int codePoint, String characterClassName) {
    characterClassName = characterClassName.split(" ")[0]; // use first
    // category
    // class
    
    // Override Nakaguro
    if (codePoint == 0x30FB) {
      characterClassName = "SYMBOL";
    }
    characterCategoryMap[codePoint] = CharacterDefinition.lookupCharacterClass(characterClassName);
  }
  
  public void putInvokeDefinition(String characterClassName, int invoke, int group, int length) {
    final byte characterClass = CharacterDefinition.lookupCharacterClass(characterClassName);
    invokeMap[characterClass] = invoke == 1;
    groupMap[characterClass] = group == 1;
    // TODO: length def ignored
  }
  
  public void write(String baseDir) throws IOException {
    String filename = baseDir + File.separator +
      CharacterDefinition.class.getName().replace('.', File.separatorChar) + CharacterDefinition.FILENAME_SUFFIX;
    new File(filename).getParentFile().mkdirs();
    OutputStream os = new FileOutputStream(filename);
    try {
      os = new BufferedOutputStream(os);
      final DataOutput out = new OutputStreamDataOutput(os);
      CodecUtil.writeHeader(out, CharacterDefinition.HEADER, CharacterDefinition.VERSION);
      out.writeBytes(characterCategoryMap, 0, characterCategoryMap.length);
      for (int i = 0; i < CharacterDefinition.CLASS_COUNT; i++) {
        final byte b = (byte) (
          (invokeMap[i] ? 0x01 : 0x00) | 
          (groupMap[i] ? 0x02 : 0x00)
        );
        out.writeByte(b);
      }
    } finally {
      os.close();
    }
  }
  
}
