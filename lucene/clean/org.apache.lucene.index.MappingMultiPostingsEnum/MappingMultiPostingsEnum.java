import org.apache.lucene.index.DocIDMerger;
import org.apache.lucene.index.*;



import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.util.BytesRef;

/**
 * Exposes flex API, merged from flex API of sub-segments,
 * remapping docIDs (this is used for segment merging).
 *
 * @lucene.experimental
 */

final class MappingMultiPostingsEnum extends PostingsEnum {
  MultiPostingsEnum multiDocsAndPositionsEnum;
  final String field;
  final DocIDMerger<MappingPostingsSub> docIDMerger;
  private MappingPostingsSub current;
  private final MappingPostingsSub[] allSubs;
  private final List<MappingPostingsSub> subs = new ArrayList<>();

  private static class MappingPostingsSub extends DocIDMerger.Sub {
    public PostingsEnum postings;

    public MappingPostingsSub(MergeState.DocMap docMap) {
      super(docMap);
    }

    @Override
    public int nextDoc() {
      try {
        return postings.nextDoc();
      } catch (IOException ioe) {
        throw new RuntimeException(ioe);
      }
    }
  }

  /** Sole constructor. */
  public MappingMultiPostingsEnum(String field, MergeState mergeState) throws IOException {
    this.field = field;
    allSubs = new MappingPostingsSub[mergeState.fieldsProducers.length];
    for(int i=0;i<allSubs.length;i++) {
      allSubs[i] = new MappingPostingsSub(mergeState.docMaps[i]);
    }
    this.docIDMerger = DocIDMerger.of(subs, allSubs.length, mergeState.needsIndexSort);
  }

  MappingMultiPostingsEnum reset(MultiPostingsEnum postingsEnum) throws IOException {
    this.multiDocsAndPositionsEnum = postingsEnum;
    MultiPostingsEnum.EnumWithSlice[] subsArray = postingsEnum.getSubs();
    int count = postingsEnum.getNumSubs();
    subs.clear();
    for(int i=0;i<count;i++) {
      MappingPostingsSub sub = allSubs[subsArray[i].slice.readerIndex];
      sub.postings = subsArray[i].postingsEnum;
      subs.add(sub);
    }
    docIDMerger.reset();
    return this;
  }

  @Override
  public int freq() throws IOException {
    return current.postings.freq();
  }

  @Override
  public int docID() {
    if (current == null) {
      return -1;
    } else {
      return current.mappedDocID;
    }
  }

  @Override
  public int advance(int target) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int nextDoc() throws IOException {
    current = docIDMerger.next();
    if (current == null) {
      return NO_MORE_DOCS;
    } else {
      return current.mappedDocID;
    }
  }

  @Override
  public int nextPosition() throws IOException {
    int pos = current.postings.nextPosition();
    if (pos < 0) {
      throw new CorruptIndexException("position=" + pos + " is negative, field=\"" + field + " doc=" + current.mappedDocID,
                                      current.postings.toString());
    } else if (pos > IndexWriter.MAX_POSITION) {
      throw new CorruptIndexException("position=" + pos + " is too large (> IndexWriter.MAX_POSITION=" + IndexWriter.MAX_POSITION + "), field=\"" + field + "\" doc=" + current.mappedDocID,
                                      current.postings.toString());
    }
    return pos;
  }
  
  @Override
  public int startOffset() throws IOException {
    return current.postings.startOffset();
  }
  
  @Override
  public int endOffset() throws IOException {
    return current.postings.endOffset();
  }
  
  @Override
  public BytesRef getPayload() throws IOException {
    return current.postings.getPayload();
  }

  @Override
  public long cost() {
    long cost = 0;
    for (MappingPostingsSub sub : subs) {
      cost += sub.postings.cost();
    }
    return cost;
  }
}

