

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import javax.security.auth.Destroyable;
import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.ClientConfiguration;
import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.ZooKeeperInstance;
import org.apache.accumulo.core.client.impl.Credentials;
import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.conf.CredentialProviderFactoryShim;
import org.apache.accumulo.core.conf.DefaultConfiguration;
import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.core.rpc.SaslConnectionParams;
import org.apache.accumulo.core.rpc.SslConnectionParams;
import org.apache.accumulo.core.security.thrift.TCredentials;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.accumulo.core.client.ClientConfiguration.ClientProperty.KERBEROS_SERVER_PRIMARY;


public class ClientContext {
	private static final Logger log = LoggerFactory.getLogger(ClientContext.class);

	protected final Instance inst;

	private Credentials creds;

	private ClientConfiguration clientConf;

	private final AccumuloConfiguration rpcConf;

	protected Connector conn;

	private Supplier<Long> timeoutSupplier;

	private Supplier<SaslConnectionParams> saslSupplier;

	private Supplier<SslConnectionParams> sslSupplier;

	private TCredentials rpcCreds;

	public ClientContext(Instance instance, Credentials credentials, ClientConfiguration clientConf) {
		this(instance, credentials, ClientContext.convertClientConfig(Objects.requireNonNull(clientConf, "clientConf is null")));
		this.clientConf = clientConf;
	}

	public ClientContext(Instance instance, Credentials credentials, AccumuloConfiguration serverConf) {
		inst = Objects.requireNonNull(instance, "instance is null");
		creds = Objects.requireNonNull(credentials, "credentials is null");
		rpcConf = Objects.requireNonNull(serverConf, "serverConf is null");
		clientConf = null;
		timeoutSupplier = new Supplier<Long>() {
			@Override
			public Long get() {
				return getConfiguration().getTimeInMillis(Property.GENERAL_RPC_TIMEOUT);
			}
		};
		sslSupplier = new Supplier<SslConnectionParams>() {
			@Override
			public SslConnectionParams get() {
				return SslConnectionParams.forClient(getConfiguration());
			}
		};
		saslSupplier = new Supplier<SaslConnectionParams>() {
			@Override
			public SaslConnectionParams get() {
				if (null != (clientConf)) {
					if (!(clientConf.hasSasl())) {
						return null;
					}
					return new SaslConnectionParams(clientConf, getCredentials().getToken());
				}
				AccumuloConfiguration conf = getConfiguration();
				if (!(conf.getBoolean(Property.INSTANCE_RPC_SASL_ENABLED))) {
					return null;
				}
				return new SaslConnectionParams(conf, getCredentials().getToken());
			}
		};
		timeoutSupplier = Suppliers.memoizeWithExpiration(timeoutSupplier, 100, TimeUnit.MILLISECONDS);
		sslSupplier = Suppliers.memoizeWithExpiration(sslSupplier, 100, TimeUnit.MILLISECONDS);
		saslSupplier = Suppliers.memoizeWithExpiration(saslSupplier, 100, TimeUnit.MILLISECONDS);
	}

	public Instance getInstance() {
		return inst;
	}

	public synchronized Credentials getCredentials() {
		return creds;
	}

	public synchronized void setCredentials(Credentials newCredentials) {
		Preconditions.checkArgument((newCredentials != null), "newCredentials is null");
		creds = newCredentials;
		rpcCreds = null;
	}

	public AccumuloConfiguration getConfiguration() {
		return rpcConf;
	}

	public long getClientTimeoutInMillis() {
		return timeoutSupplier.get();
	}

	public SslConnectionParams getClientSslParams() {
		return sslSupplier.get();
	}

	public SaslConnectionParams getSaslParams() {
		return saslSupplier.get();
	}

	public Connector getConnector() throws AccumuloException, AccumuloSecurityException {
		if ((conn) == null) {
			if ((getInstance()) instanceof ZooKeeperInstance) {
			}else {
				Credentials c = getCredentials();
				conn = getInstance().getConnector(c.getPrincipal(), c.getToken());
			}
		}
		return conn;
	}

	public synchronized TCredentials rpcCreds() {
		if (getCredentials().getToken().isDestroyed()) {
			rpcCreds = null;
		}
		if ((rpcCreds) == null) {
			rpcCreds = getCredentials().toThrift(getInstance());
		}
		return rpcCreds;
	}

	public static AccumuloConfiguration convertClientConfig(final Configuration config) {
		final AccumuloConfiguration defaults = DefaultConfiguration.getInstance();
		return new AccumuloConfiguration() {
			@Override
			public String get(Property property) {
				final String key = property.getKey();
				if (property.isSensitive()) {
					org.apache.hadoop.conf.Configuration hadoopConf = getHadoopConfiguration();
					if (null != hadoopConf) {
						try {
							char[] value = CredentialProviderFactoryShim.getValueFromCredentialProvider(hadoopConf, key);
							if (null != value) {
								ClientContext.log.trace("Loaded sensitive value for {} from CredentialProvider", key);
								return new String(value);
							}else {
								ClientContext.log.trace(("Tried to load sensitive value for {} from CredentialProvider, " + "but none was found"), key);
							}
						} catch (IOException e) {
							ClientContext.log.warn(("Failed to extract sensitive property ({}) from Hadoop CredentialProvider," + " falling back to base AccumuloConfiguration"), key, e);
						}
					}
				}
				if (config.containsKey(key))
					return config.getString(key);
				else {
					if ((Property.GENERAL_KERBEROS_PRINCIPAL) == property) {
						if (config.containsKey(KERBEROS_SERVER_PRIMARY.getKey())) {
							return ((config.getString(KERBEROS_SERVER_PRIMARY.getKey())) + "/_HOST@") + (SaslConnectionParams.getDefaultRealm());
						}
					}
					return defaults.get(property);
				}
			}

			@Override
			public void getProperties(Map<String, String> props, Predicate<String> filter) {
				defaults.getProperties(props, filter);
				Iterator<?> keyIter = config.getKeys();
				while (keyIter.hasNext()) {
					String key = keyIter.next().toString();
					if (filter.apply(key))
						props.put(key, config.getString(key));

				} 
				if (props.containsKey(KERBEROS_SERVER_PRIMARY.getKey())) {
					final String serverPrimary = props.remove(KERBEROS_SERVER_PRIMARY.getKey());
					if (filter.apply(Property.GENERAL_KERBEROS_PRINCIPAL.getKey())) {
						props.put(Property.GENERAL_KERBEROS_PRINCIPAL.getKey(), ((serverPrimary + "/_HOST@") + (SaslConnectionParams.getDefaultRealm())));
					}
				}
				org.apache.hadoop.conf.Configuration hadoopConf = getHadoopConfiguration();
				if (null != hadoopConf) {
					try {
						for (String key : CredentialProviderFactoryShim.getKeys(hadoopConf)) {
							if ((!(Property.isValidPropertyKey(key))) || (!(Property.isSensitive(key)))) {
								continue;
							}
							if (filter.apply(key)) {
								char[] value = CredentialProviderFactoryShim.getValueFromCredentialProvider(hadoopConf, key);
								if (null != value) {
									props.put(key, new String(value));
								}
							}
						}
					} catch (IOException e) {
						ClientContext.log.warn(("Failed to extract sensitive properties from Hadoop CredentialProvider, " + "falling back to accumulo-site.xml"), e);
					}
				}
			}

			private org.apache.hadoop.conf.Configuration getHadoopConfiguration() {
				String credProviderPaths = config.getString(Property.GENERAL_SECURITY_CREDENTIAL_PROVIDER_PATHS.getKey());
				if ((null != credProviderPaths) && (!(credProviderPaths.isEmpty()))) {
					org.apache.hadoop.conf.Configuration hConf = new org.apache.hadoop.conf.Configuration();
					hConf.set(CredentialProviderFactoryShim.CREDENTIAL_PROVIDER_PATH, credProviderPaths);
					return hConf;
				}
				ClientContext.log.trace("Did not find credential provider configuration in ClientConfiguration");
				return null;
			}
		};
	}
}

