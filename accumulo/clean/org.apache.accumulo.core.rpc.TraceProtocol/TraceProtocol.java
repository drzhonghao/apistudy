import org.apache.accumulo.core.rpc.*;


import org.apache.accumulo.core.trace.Span;
import org.apache.accumulo.core.trace.Trace;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TMessage;
import org.apache.thrift.transport.TTransport;

/**
 * TCompactProtocol implementation which automatically tracks tracing information
 */
public class TraceProtocol extends TCompactProtocol {
  private Span span = null;

  @Override
  public void writeMessageBegin(TMessage message) throws TException {
    span = Trace.start("client:" + message.name);
    super.writeMessageBegin(message);
  }

  @Override
  public void writeMessageEnd() throws TException {
    super.writeMessageEnd();
    span.stop();
  }

  public TraceProtocol(TTransport transport) {
    super(transport);
  }
}
