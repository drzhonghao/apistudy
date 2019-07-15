import org.apache.accumulo.tserver.session.*;


import org.apache.accumulo.core.security.thrift.TCredentials;
import org.apache.accumulo.server.rpc.TServerUtils;

public class Session {

  enum State {
    NEW, UNRESERVED, RESERVED, REMOVED
  }

  public final String client;
  long lastAccessTime;
  public long startTime;
  State state = State.NEW;
  private final TCredentials credentials;

  Session(TCredentials credentials) {
    this.credentials = credentials;
    this.client = TServerUtils.clientAddress.get();
  }

  public String getUser() {
    return credentials.getPrincipal();
  }

  public TCredentials getCredentials() {
    return credentials;
  }

  public boolean cleanup() {
    return true;
  }
}
