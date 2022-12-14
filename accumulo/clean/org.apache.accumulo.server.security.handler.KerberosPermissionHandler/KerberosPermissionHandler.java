import org.apache.accumulo.server.security.handler.*;


import static java.nio.charset.StandardCharsets.UTF_8;

import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.NamespaceNotFoundException;
import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.client.impl.thrift.ThriftSecurityException;
import org.apache.accumulo.core.security.NamespacePermission;
import org.apache.accumulo.core.security.SystemPermission;
import org.apache.accumulo.core.security.TablePermission;
import org.apache.accumulo.core.security.thrift.TCredentials;
import org.apache.accumulo.core.util.Base64;

/**
 * Kerberos principals might contains identifiers that are not valid ZNodes ('/'). Base64-encodes
 * the principals before interacting with ZooKeeper.
 */
public class KerberosPermissionHandler implements PermissionHandler {

  private final ZKPermHandler zkPermissionHandler;

  public KerberosPermissionHandler() {
    zkPermissionHandler = new ZKPermHandler();
  }

  @Override
  public void initialize(String instanceId, boolean initialize) {
    zkPermissionHandler.initialize(instanceId, initialize);
  }

  @Override
  public boolean validSecurityHandlers(Authenticator authent, Authorizor author) {
    return authent instanceof KerberosAuthenticator && author instanceof KerberosAuthorizor;
  }

  @Override
  public void initializeSecurity(TCredentials credentials, String rootuser)
      throws AccumuloSecurityException, ThriftSecurityException {
    zkPermissionHandler.initializeSecurity(credentials,
        Base64.encodeBase64String(rootuser.getBytes(UTF_8)));
  }

  @Override
  public boolean hasSystemPermission(String user, SystemPermission permission)
      throws AccumuloSecurityException {
    return zkPermissionHandler.hasSystemPermission(Base64.encodeBase64String(user.getBytes(UTF_8)),
        permission);
  }

  @Override
  public boolean hasCachedSystemPermission(String user, SystemPermission permission)
      throws AccumuloSecurityException {
    return zkPermissionHandler
        .hasCachedSystemPermission(Base64.encodeBase64String(user.getBytes(UTF_8)), permission);
  }

  @Override
  public boolean hasTablePermission(String user, String table, TablePermission permission)
      throws AccumuloSecurityException, TableNotFoundException {
    return zkPermissionHandler.hasTablePermission(Base64.encodeBase64String(user.getBytes(UTF_8)),
        table, permission);
  }

  @Override
  public boolean hasCachedTablePermission(String user, String table, TablePermission permission)
      throws AccumuloSecurityException, TableNotFoundException {
    return zkPermissionHandler.hasCachedTablePermission(
        Base64.encodeBase64String(user.getBytes(UTF_8)), table, permission);
  }

  @Override
  public boolean hasNamespacePermission(String user, String namespace,
      NamespacePermission permission) throws AccumuloSecurityException, NamespaceNotFoundException {
    return zkPermissionHandler.hasNamespacePermission(
        Base64.encodeBase64String(user.getBytes(UTF_8)), namespace, permission);
  }

  @Override
  public boolean hasCachedNamespacePermission(String user, String namespace,
      NamespacePermission permission) throws AccumuloSecurityException, NamespaceNotFoundException {
    return zkPermissionHandler.hasCachedNamespacePermission(
        Base64.encodeBase64String(user.getBytes(UTF_8)), namespace, permission);
  }

  @Override
  public void grantSystemPermission(String user, SystemPermission permission)
      throws AccumuloSecurityException {
    zkPermissionHandler.grantSystemPermission(Base64.encodeBase64String(user.getBytes(UTF_8)),
        permission);
  }

  @Override
  public void revokeSystemPermission(String user, SystemPermission permission)
      throws AccumuloSecurityException {
    zkPermissionHandler.revokeSystemPermission(Base64.encodeBase64String(user.getBytes(UTF_8)),
        permission);
  }

  @Override
  public void grantTablePermission(String user, String table, TablePermission permission)
      throws AccumuloSecurityException, TableNotFoundException {
    zkPermissionHandler.grantTablePermission(Base64.encodeBase64String(user.getBytes(UTF_8)), table,
        permission);
  }

  @Override
  public void revokeTablePermission(String user, String table, TablePermission permission)
      throws AccumuloSecurityException, TableNotFoundException {
    zkPermissionHandler.revokeTablePermission(Base64.encodeBase64String(user.getBytes(UTF_8)),
        table, permission);
  }

  @Override
  public void grantNamespacePermission(String user, String namespace,
      NamespacePermission permission) throws AccumuloSecurityException, NamespaceNotFoundException {
    zkPermissionHandler.grantNamespacePermission(Base64.encodeBase64String(user.getBytes(UTF_8)),
        namespace, permission);
  }

  @Override
  public void revokeNamespacePermission(String user, String namespace,
      NamespacePermission permission) throws AccumuloSecurityException, NamespaceNotFoundException {
    zkPermissionHandler.revokeNamespacePermission(Base64.encodeBase64String(user.getBytes(UTF_8)),
        namespace, permission);
  }

  @Override
  public void cleanTablePermissions(String table)
      throws AccumuloSecurityException, TableNotFoundException {
    zkPermissionHandler.cleanTablePermissions(table);
  }

  @Override
  public void cleanNamespacePermissions(String namespace)
      throws AccumuloSecurityException, NamespaceNotFoundException {
    zkPermissionHandler.cleanNamespacePermissions(namespace);
  }

  @Override
  public void initUser(String user) throws AccumuloSecurityException {
    zkPermissionHandler.initUser(Base64.encodeBase64String(user.getBytes(UTF_8)));
  }

  @Override
  public void initTable(String table) throws AccumuloSecurityException {
    zkPermissionHandler.initTable(table);
  }

  @Override
  public void cleanUser(String user) throws AccumuloSecurityException {
    zkPermissionHandler.cleanUser(Base64.encodeBase64String(user.getBytes(UTF_8)));
  }

}
