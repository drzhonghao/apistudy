import org.apache.accumulo.core.util.Base64;
import org.apache.accumulo.core.util.*;


import static java.nio.charset.StandardCharsets.UTF_8;

import org.apache.hadoop.io.Text;

public class Encoding {

  public static String encodeAsBase64FileName(Text data) {
    String encodedRow = Base64.encodeBase64URLSafeString(TextUtil.getBytes(data));

    int index = encodedRow.length() - 1;
    while (index >= 0 && encodedRow.charAt(index) == '=')
      index--;

    encodedRow = encodedRow.substring(0, index + 1);
    return encodedRow;
  }

  public static byte[] decodeBase64FileName(String node) {
    while (node.length() % 4 != 0)
      node += "=";
    /* decode transparently handles URLSafe encodings */
    return Base64.decodeBase64(node.getBytes(UTF_8));
  }

}
