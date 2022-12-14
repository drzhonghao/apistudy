import org.apache.accumulo.core.client.security.tokens.PasswordToken;
import org.apache.accumulo.core.client.security.tokens.*;


import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.CharBuffer;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.accumulo.core.conf.CredentialProviderFactoryShim;
import org.apache.accumulo.core.util.CachedConfiguration;
import org.apache.hadoop.conf.Configuration;

/**
 * An {@link AuthenticationToken} backed by a Hadoop CredentialProvider.
 */
public class CredentialProviderToken extends PasswordToken {
  public static final String NAME_PROPERTY = "name",
      CREDENTIAL_PROVIDERS_PROPERTY = "credentialProviders";

  public CredentialProviderToken() {
    super();
  }

  public CredentialProviderToken(String name, String credentialProviders) throws IOException {
    requireNonNull(name);
    requireNonNull(credentialProviders);

    setWithCredentialProviders(name, credentialProviders);
  }

  protected void setWithCredentialProviders(String name, String credentialProviders)
      throws IOException {
    final Configuration conf = new Configuration(CachedConfiguration.getInstance());
    conf.set(CredentialProviderFactoryShim.CREDENTIAL_PROVIDER_PATH, credentialProviders);

    char[] password = CredentialProviderFactoryShim.getValueFromCredentialProvider(conf, name);

    if (null == password) {
      throw new IOException(
          "No password could be extracted from CredentialProvider(s) with " + name);
    }

    setPassword(CharBuffer.wrap(password));
  }

  @Override
  public void init(Properties properties) {
    char[] nameCharArray = properties.get(NAME_PROPERTY),
        credentialProvidersCharArray = properties.get(CREDENTIAL_PROVIDERS_PROPERTY);
    if (null != nameCharArray && null != credentialProvidersCharArray) {
      try {
        this.setWithCredentialProviders(new String(nameCharArray),
            new String(credentialProvidersCharArray));
      } catch (IOException e) {
        throw new IllegalArgumentException("Could not extract password from CredentialProvider", e);
      }

      return;
    }

    throw new IllegalArgumentException(
        "Expected " + NAME_PROPERTY + " and " + CREDENTIAL_PROVIDERS_PROPERTY + " properties.");
  }

  @Override
  public Set<TokenProperty> getProperties() {
    LinkedHashSet<TokenProperty> properties = new LinkedHashSet<>();
    // Neither name or CPs are sensitive
    properties
        .add(new TokenProperty(NAME_PROPERTY, "Alias to extract from CredentialProvider", false));
    properties.add(new TokenProperty(CREDENTIAL_PROVIDERS_PROPERTY,
        "Comma separated list of URLs defining CredentialProvider(s)", false));
    return properties;
  }

  @Override
  public CredentialProviderToken clone() {
    CredentialProviderToken clone = (CredentialProviderToken) super.clone();
    clone.setPassword(this.getPassword());
    return clone;
  }

}
