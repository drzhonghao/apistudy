import org.apache.accumulo.tserver.*;


import java.nio.ByteBuffer;
import java.util.Collections;

import org.apache.accumulo.core.client.impl.thrift.ThriftSecurityException;
import org.apache.accumulo.core.constraints.Constraint.Environment;
import org.apache.accumulo.core.data.ByteSequence;
import org.apache.accumulo.core.data.impl.KeyExtent;
import org.apache.accumulo.core.security.AuthorizationContainer;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.accumulo.core.security.thrift.TCredentials;
import org.apache.accumulo.server.security.SecurityOperation;

public class TservConstraintEnv implements Environment {

  private final TCredentials credentials;
  private final SecurityOperation security;
  private Authorizations auths;
  private KeyExtent ke;

  TservConstraintEnv(SecurityOperation secOp, TCredentials credentials) {
    this.security = secOp;
    this.credentials = credentials;
  }

  public void setExtent(KeyExtent ke) {
    this.ke = ke;
  }

  @Override
  public KeyExtent getExtent() {
    return ke;
  }

  @Override
  public String getUser() {
    return credentials.getPrincipal();
  }

  @Override
  @Deprecated
  public Authorizations getAuthorizations() {
    if (auths == null)
      try {
        this.auths = security.getUserAuthorizations(credentials);
      } catch (ThriftSecurityException e) {
        throw new RuntimeException(e);
      }
    return auths;
  }

  @Override
  public AuthorizationContainer getAuthorizationsContainer() {
    return new AuthorizationContainer() {
      @Override
      public boolean contains(ByteSequence auth) {
        try {
          return security.authenticatedUserHasAuthorizations(credentials,
              Collections.<ByteBuffer> singletonList(
                  ByteBuffer.wrap(auth.getBackingArray(), auth.offset(), auth.length())));
        } catch (ThriftSecurityException e) {
          throw new RuntimeException(e);
        }
      }
    };
  }
}
