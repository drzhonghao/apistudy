import org.apache.lucene.index.SortedSetDocValues;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.OrdTermState;
import org.apache.lucene.index.*;



import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefBuilder;

import java.io.IOException;

/** Implements a {@link TermsEnum} wrapping a provided
 * {@link SortedSetDocValues}. */

class SortedSetDocValuesTermsEnum extends TermsEnum {
  private final SortedSetDocValues values;
  private long currentOrd = -1;
  private final BytesRefBuilder scratch;

  /** Creates a new TermsEnum over the provided values */
  public SortedSetDocValuesTermsEnum(SortedSetDocValues values) {
    this.values = values;
    scratch = new BytesRefBuilder();
  }

  @Override
  public SeekStatus seekCeil(BytesRef text) throws IOException {
    long ord = values.lookupTerm(text);
    if (ord >= 0) {
      currentOrd = ord;
      scratch.copyBytes(text);
      return SeekStatus.FOUND;
    } else {
      currentOrd = -ord-1;
      if (currentOrd == values.getValueCount()) {
        return SeekStatus.END;
      } else {
        // TODO: hmm can we avoid this "extra" lookup?:
        scratch.copyBytes(values.lookupOrd(currentOrd));
        return SeekStatus.NOT_FOUND;
      }
    }
  }

  @Override
  public boolean seekExact(BytesRef text) throws IOException {
    long ord = values.lookupTerm(text);
    if (ord >= 0) {
      currentOrd = ord;
      scratch.copyBytes(text);
      return true;
    } else {
      return false;
    }
  }

  @Override
  public void seekExact(long ord) throws IOException {
    assert ord >= 0 && ord < values.getValueCount();
    currentOrd = (int) ord;
    scratch.copyBytes(values.lookupOrd(currentOrd));
  }

  @Override
  public BytesRef next() throws IOException {
    currentOrd++;
    if (currentOrd >= values.getValueCount()) {
      return null;
    }
    scratch.copyBytes(values.lookupOrd(currentOrd));
    return scratch.get();
  }

  @Override
  public BytesRef term() throws IOException {
    return scratch.get();
  }

  @Override
  public long ord() throws IOException {
    return currentOrd;
  }

  @Override
  public int docFreq() {
    throw new UnsupportedOperationException();
  }

  @Override
  public long totalTermFreq() {
    return -1;
  }

  @Override
  public PostingsEnum postings(PostingsEnum reuse, int flags) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void seekExact(BytesRef term, TermState state) throws IOException {
    assert state != null && state instanceof OrdTermState;
    this.seekExact(((OrdTermState)state).ord);
  }

  @Override
  public TermState termState() throws IOException {
    OrdTermState state = new OrdTermState();
    state.ord = currentOrd;
    return state;
  }
}

