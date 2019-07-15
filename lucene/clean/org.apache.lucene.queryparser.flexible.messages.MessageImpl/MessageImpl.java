import org.apache.lucene.queryparser.flexible.messages.NLS;
import org.apache.lucene.queryparser.flexible.messages.*;


import java.util.Locale;

/**
 * Default implementation of Message interface.
 * For Native Language Support (NLS), system of software internationalization.
 */
public class MessageImpl implements Message {

  private String key;

  private Object[] arguments = new Object[0];

  public MessageImpl(String key) {
    this.key = key;

  }

  public MessageImpl(String key, Object... args) {
    this(key);
    this.arguments = args;
  }

  @Override
  public Object[] getArguments() {
    return this.arguments;
  }

  @Override
  public String getKey() {
    return this.key;
  }

  @Override
  public String getLocalizedMessage() {
    return getLocalizedMessage(Locale.getDefault());
  }

  @Override
  public String getLocalizedMessage(Locale locale) {
    return NLS.getLocalizedMessage(getKey(), locale, getArguments());
  }

  @Override
  public String toString() {
    Object[] args = getArguments();
    StringBuilder sb = new StringBuilder(getKey());
    if (args != null) {
      for (int i = 0; i < args.length; i++) {
        sb.append(i == 0 ? " " : ", ").append(args[i]);
      }
    }
    return sb.toString();
  }

}
