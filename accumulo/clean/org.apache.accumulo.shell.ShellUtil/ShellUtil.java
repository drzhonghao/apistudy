import org.apache.accumulo.shell.*;


import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.accumulo.core.util.Base64;
import org.apache.hadoop.io.Text;

public class ShellUtil {

  /**
   * Scans the given file line-by-line (ignoring empty lines) and returns a list containing those
   * lines. If decode is set to true, every line is decoded using
   * {@link Base64#decodeBase64(byte[])} from the UTF-8 bytes of that line before inserting in the
   * list.
   *
   * @param filename
   *          Path to the file that needs to be scanned
   * @param decode
   *          Whether to decode lines in the file
   * @return List of {@link Text} objects containing data in the given file
   * @throws FileNotFoundException
   *           if the given file doesn't exist
   */
  public static List<Text> scanFile(String filename, boolean decode) throws FileNotFoundException {
    String line;
    Scanner file = new Scanner(new File(filename), UTF_8.name());
    List<Text> result = new ArrayList<>();
    try {
      while (file.hasNextLine()) {
        line = file.nextLine();
        if (!line.isEmpty()) {
          result.add(decode ? new Text(Base64.decodeBase64(line.getBytes(UTF_8))) : new Text(line));
        }
      }
    } finally {
      file.close();
    }
    return result;
  }
}
