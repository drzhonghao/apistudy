import org.apache.lucene.analysis.core.*;



import java.util.Set;

import org.apache.lucene.analysis.FilteringTokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;

/**
 * Removes tokens whose types appear in a set of blocked types from a token stream.
 */
public final class TypeTokenFilter extends FilteringTokenFilter {

  private final Set<String> stopTypes;
  private final TypeAttribute typeAttribute = addAttribute(TypeAttribute.class);
  private final boolean useWhiteList;

  /**
   * Create a new {@link TypeTokenFilter}.
   * @param input        the {@link TokenStream} to consume
   * @param stopTypes    the types to filter
   * @param useWhiteList if true, then tokens whose type is in stopTypes will
   *                     be kept, otherwise they will be filtered out
   */
  public TypeTokenFilter(TokenStream input, Set<String> stopTypes, boolean useWhiteList) {
    super(input);
    this.stopTypes = stopTypes;
    this.useWhiteList = useWhiteList;
  }

  /**
   * Create a new {@link TypeTokenFilter} that filters tokens out
   * (useWhiteList=false).
   */
  public TypeTokenFilter(TokenStream input, Set<String> stopTypes) {
    this(input, stopTypes, false);
  }

  /**
   * By default accept the token if its type is not a stop type.
   * When the useWhiteList parameter is set to true then accept the token if its type is contained in the stopTypes
   */
  @Override
  protected boolean accept() {
    return useWhiteList == stopTypes.contains(typeAttribute.type());
  }
}
