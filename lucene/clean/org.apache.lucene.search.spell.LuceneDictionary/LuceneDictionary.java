import org.apache.lucene.search.spell.*;


import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.suggest.InputIterator;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.MultiFields;

import java.io.*;

/**
 * Lucene Dictionary: terms taken from the given field
 * of a Lucene index.
 */
public class LuceneDictionary implements Dictionary {
  private IndexReader reader;
  private String field;

  /**
   * Creates a new Dictionary, pulling source terms from
   * the specified <code>field</code> in the provided <code>reader</code>
   */
  public LuceneDictionary(IndexReader reader, String field) {
    this.reader = reader;
    this.field = field;
  }

  @Override
  public final InputIterator getEntryIterator() throws IOException {
    final Terms terms = MultiFields.getTerms(reader, field);
    if (terms != null) {
      return new InputIterator.InputIteratorWrapper(terms.iterator());
    } else {
      return InputIterator.EMPTY;
    }
  }
}
