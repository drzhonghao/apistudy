import org.apache.accumulo.core.client.security.tokens.*;


import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import javax.security.auth.DestroyFailedException;

import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.security.UserGroupInformation.AuthenticationMethod;

/**
 * Authentication token for Kerberos authenticated clients
 *
 * @since 1.7.0
 */
public class KerberosToken implements AuthenticationToken {

  public static final String CLASS_NAME = KerberosToken.class.getName();

  private static final int VERSION = 1;

  private String principal;
  private File keytab;

  /**
   * Creates a token using the provided principal and the currently logged-in user via
   * {@link UserGroupInformation}.
   *
   * This method expects the current user (as defined by
   * {@link UserGroupInformation#getCurrentUser()} to be authenticated via Kerberos or as a Proxy
   * (on top of another user). An {@link IllegalArgumentException} will be thrown for all other
   * cases.
   *
   * @param principal
   *          The user that is logged in
   * @throws IllegalArgumentException
   *           If the current user is not authentication via Kerberos or Proxy methods.
   * @see UserGroupInformation#getCurrentUser()
   * @see UserGroupInformation#getAuthenticationMethod()
   */
  public KerberosToken(String principal) throws IOException {
    this.principal = requireNonNull(principal);
    validateAuthMethod(UserGroupInformation.getCurrentUser().getAuthenticationMethod());
  }

  static void validateAuthMethod(AuthenticationMethod authMethod) {
    // There is also KERBEROS_SSL but that appears to be deprecated/OBE
    checkArgument(
        AuthenticationMethod.KERBEROS == authMethod || AuthenticationMethod.PROXY == authMethod,
        "KerberosToken expects KERBEROS or PROXY authentication for the current "
            + "UserGroupInformation user. Saw " + authMethod);
  }

  /**
   * Creates a Kerberos token for the specified principal using the provided keytab. The principal
   * and keytab combination are verified by attempting a log in.
   * <p>
   * This constructor does not have any side effects.
   *
   * @param principal
   *          The Kerberos principal
   * @param keytab
   *          A keytab file containing the principal's credentials.
   */
  public KerberosToken(String principal, File keytab) throws IOException {
    this(principal, keytab, false);
  }

  /**
   * Creates a token and logs in via {@link UserGroupInformation} using the provided principal and
   * keytab. A key for the principal must exist in the keytab, otherwise login will fail.
   *
   * @param principal
   *          The Kerberos principal
   * @param keytab
   *          A keytab file
   * @param replaceCurrentUser
   *          Should the current Hadoop user be replaced with this user
   * @deprecated since 1.8.0, @see #KerberosToken(String, File)
   */
  @Deprecated
  public KerberosToken(String principal, File keytab, boolean replaceCurrentUser)
      throws IOException {
    this.principal = requireNonNull(principal, "Principal was null");
    this.keytab = requireNonNull(keytab, "Keytab was null");
    checkArgument(keytab.exists() && keytab.isFile(), "Keytab was not a normal file");
    if (replaceCurrentUser) {
      UserGroupInformation.loginUserFromKeytab(principal, keytab.getAbsolutePath());
    }
  }

  /**
   * Creates a token using the login user as returned by
   * {@link UserGroupInformation#getCurrentUser()}
   *
   * @throws IOException
   *           If the current logged in user cannot be computed.
   */
  public KerberosToken() throws IOException {
    this(UserGroupInformation.getCurrentUser().getUserName());
  }

  @Override
  public KerberosToken clone() {
    try {
      KerberosToken clone = (KerberosToken) super.clone();
      clone.principal = principal;
      clone.keytab = keytab == null ? keytab : keytab.getCanonicalFile();
      return clone;
    } catch (CloneNotSupportedException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (!(obj instanceof KerberosToken))
      return false;
    KerberosToken other = (KerberosToken) obj;

    return principal.equals(other.principal);
  }

  /**
   * The identity of the user to which this token belongs to according to Kerberos
   *
   * @return The principal
   */
  public String getPrincipal() {
    return principal;
  }

  /**
   * The keytab file used to perform Kerberos login. Optional, may be null.
   */
  public File getKeytab() {
    return keytab;
  }

  @Override
  public void write(DataOutput out) throws IOException {
    out.writeInt(VERSION);
  }

  @Override
  public void readFields(DataInput in) throws IOException {
    int actualVersion = in.readInt();
    if (VERSION != actualVersion) {
      throw new IOException("Did not find expected version in serialized KerberosToken");
    }
  }

  @Override
  public synchronized void destroy() throws DestroyFailedException {
    principal = null;
  }

  @Override
  public boolean isDestroyed() {
    return null == principal;
  }

  @Override
  public void init(Properties properties) {

  }

  @Override
  public Set<TokenProperty> getProperties() {
    return Collections.emptySet();
  }

  @Override
  public int hashCode() {
    return principal.hashCode();
  }
}
