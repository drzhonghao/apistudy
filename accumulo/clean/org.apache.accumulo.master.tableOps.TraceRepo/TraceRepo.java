import org.apache.accumulo.master.tableOps.*;


import org.apache.accumulo.core.trace.Span;
import org.apache.accumulo.core.trace.Trace;
import org.apache.accumulo.core.trace.Tracer;
import org.apache.accumulo.core.trace.thrift.TInfo;
import org.apache.accumulo.fate.Repo;

/**
 *
 */
public class TraceRepo<T> implements Repo<T> {

  private static final long serialVersionUID = 1L;

  long traceId;
  long parentId;
  Repo<T> repo;

  public TraceRepo(Repo<T> repo) {
    this.repo = repo;
    TInfo tinfo = Tracer.traceInfo();
    traceId = tinfo.traceId;
    parentId = tinfo.parentId;
  }

  @Override
  public long isReady(long tid, T environment) throws Exception {
    Span span = Trace.trace(new TInfo(traceId, parentId), repo.getDescription());
    try {
      return repo.isReady(tid, environment);
    } finally {
      span.stop();
    }
  }

  @Override
  public Repo<T> call(long tid, T environment) throws Exception {
    Span span = Trace.trace(new TInfo(traceId, parentId), repo.getDescription());
    try {
      Repo<T> result = repo.call(tid, environment);
      if (result == null)
        return null;
      return new TraceRepo<>(result);
    } finally {
      span.stop();
    }
  }

  @Override
  public void undo(long tid, T environment) throws Exception {
    Span span = Trace.trace(new TInfo(traceId, parentId), repo.getDescription());
    try {
      repo.undo(tid, environment);
    } finally {
      span.stop();
    }
  }

  @Override
  public String getDescription() {
    return repo.getDescription();
  }

  @Override
  public String getReturn() {
    return repo.getReturn();
  }

}
