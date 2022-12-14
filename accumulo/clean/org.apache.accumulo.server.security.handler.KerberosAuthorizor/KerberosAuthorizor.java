import org.apache.accumulo.server.security.handler.*;


import static java.nio.charset.StandardCharsets.UTF_8;

import java.nio.ByteBuffer;
import java.util.List;

import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.impl.thrift.ThriftSecurityException;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.accumulo.core.security.thrift.TCredentials;
import org.apache.accumulo.core.util.Base64;

/**
 * Kerberos principals might contains identifiers that are not valid ZNodes ('/'). Base64-encodes
 * the principals before interacting with ZooKeeper.
 */
public class KerberosAuthorizor implements Authorizor {

  private final ZKAuthorizor zkAuthorizor;

  public KerberosAuthorizor() {
    zkAuthorizor = new ZKAuthorizor();
  }

  @Override
  public void initialize(String instanceId, boolean initialize) {
    zkAuthorizor.initialize(instanceId, initialize);
  }

  @Override
  public boolean validSecurityHandlers(Authenticator auth, PermissionHandler pm) {
    return auth instanceof KerberosAuthenticator && pm instanceof KerberosPermissionHandler;
  }

  @Override
  public void initializeSecurity(TCredentials credentials, String rootuser)
      throws AccumuloSecurityException, ThriftSecurityException {
    zkAuthorizor.initializeSecurity(credentials,
        Base64.encodeBase64String(rootuser.getBytes(UTF_8)));
  }

  @Override
  public void changeAuthorizations(String user, Authorizations authorizations)
      throws AccumuloSecurityException {
    zkAuthorizor.changeAuthorizations(Base64.encodeBase64String(user.getBytes(UTF_8)),
        authorizations);
  }

  @Override
  public Authorizations getCachedUserAuthorizations(String user) throws AccumuloSecurityException {
    return zkAuthorizor
        .getCachedUserAuthorizations(Base64.encodeBase64String(user.getBytes(UTF_8)));
  }

  @Override
  public boolean isValidAuthorizations(String user, List<ByteBuffer> list)
      throws AccumuloSecurityException {
    return zkAuthorizor.isValidAuthorizations(Base64.encodeBase64String(user.getBytes(UTF_8)),
        list);
  }

  @Override
  public void initUser(String user) throws AccumuloSecurityException {
    zkAuthorizor.initUser(Base64.encodeBase64String(user.getBytes(UTF_8)));
  }

  @Override
  public void dropUser(String user) throws AccumuloSecurityException {
    user = Base64.encodeBase64String(user.getBytes(UTF_8));
    zkAuthorizor.dropUser(user);
  }

}
