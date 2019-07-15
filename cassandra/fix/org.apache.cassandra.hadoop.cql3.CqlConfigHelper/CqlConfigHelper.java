

import com.datastax.driver.core.AuthProvider;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.HostDistance;
import com.datastax.driver.core.JdkSSLOptions;
import com.datastax.driver.core.PlainTextAuthProvider;
import com.datastax.driver.core.PoolingOptions;
import com.datastax.driver.core.ProtocolOptions;
import com.datastax.driver.core.ProtocolVersion;
import com.datastax.driver.core.QueryOptions;
import com.datastax.driver.core.SSLOptions;
import com.datastax.driver.core.SocketOptions;
import com.datastax.driver.core.policies.LoadBalancingPolicy;
import com.google.common.base.Optional;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.List;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import org.apache.cassandra.hadoop.ConfigHelper;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.conf.Configuration;

import static com.datastax.driver.core.ProtocolOptions.Compression.NONE;


public class CqlConfigHelper {
	private static final String INPUT_CQL_COLUMNS_CONFIG = "cassandra.input.columnfamily.columns";

	private static final String INPUT_CQL_PAGE_ROW_SIZE_CONFIG = "cassandra.input.page.row.size";

	private static final String INPUT_CQL_WHERE_CLAUSE_CONFIG = "cassandra.input.where.clause";

	private static final String INPUT_CQL = "cassandra.input.cql";

	private static final String USERNAME = "cassandra.username";

	private static final String PASSWORD = "cassandra.password";

	private static final String INPUT_NATIVE_PORT = "cassandra.input.native.port";

	private static final String INPUT_NATIVE_CORE_CONNECTIONS_PER_HOST = "cassandra.input.native.core.connections.per.host";

	private static final String INPUT_NATIVE_MAX_CONNECTIONS_PER_HOST = "cassandra.input.native.max.connections.per.host";

	private static final String INPUT_NATIVE_MAX_SIMULT_REQ_PER_CONNECTION = "cassandra.input.native.max.simult.reqs.per.connection";

	private static final String INPUT_NATIVE_CONNECTION_TIMEOUT = "cassandra.input.native.connection.timeout";

	private static final String INPUT_NATIVE_READ_CONNECTION_TIMEOUT = "cassandra.input.native.read.connection.timeout";

	private static final String INPUT_NATIVE_RECEIVE_BUFFER_SIZE = "cassandra.input.native.receive.buffer.size";

	private static final String INPUT_NATIVE_SEND_BUFFER_SIZE = "cassandra.input.native.send.buffer.size";

	private static final String INPUT_NATIVE_SOLINGER = "cassandra.input.native.solinger";

	private static final String INPUT_NATIVE_TCP_NODELAY = "cassandra.input.native.tcp.nodelay";

	private static final String INPUT_NATIVE_REUSE_ADDRESS = "cassandra.input.native.reuse.address";

	private static final String INPUT_NATIVE_KEEP_ALIVE = "cassandra.input.native.keep.alive";

	private static final String INPUT_NATIVE_AUTH_PROVIDER = "cassandra.input.native.auth.provider";

	private static final String INPUT_NATIVE_SSL_TRUST_STORE_PATH = "cassandra.input.native.ssl.trust.store.path";

	private static final String INPUT_NATIVE_SSL_KEY_STORE_PATH = "cassandra.input.native.ssl.key.store.path";

	private static final String INPUT_NATIVE_SSL_TRUST_STORE_PASSWARD = "cassandra.input.native.ssl.trust.store.password";

	private static final String INPUT_NATIVE_SSL_KEY_STORE_PASSWARD = "cassandra.input.native.ssl.key.store.password";

	private static final String INPUT_NATIVE_SSL_CIPHER_SUITES = "cassandra.input.native.ssl.cipher.suites";

	private static final String INPUT_NATIVE_PROTOCOL_VERSION = "cassandra.input.native.protocol.version";

	private static final String OUTPUT_CQL = "cassandra.output.cql";

	private static final String OUTPUT_NATIVE_PORT = "cassandra.output.native.port";

	public static void setInputColumns(Configuration conf, String columns) {
		if ((columns == null) || (columns.isEmpty()))
			return;

		conf.set(CqlConfigHelper.INPUT_CQL_COLUMNS_CONFIG, columns);
	}

	public static void setInputCQLPageRowSize(Configuration conf, String cqlPageRowSize) {
		if (cqlPageRowSize == null) {
			throw new UnsupportedOperationException("cql page row size may not be null");
		}
		conf.set(CqlConfigHelper.INPUT_CQL_PAGE_ROW_SIZE_CONFIG, cqlPageRowSize);
	}

	public static void setInputWhereClauses(Configuration conf, String clauses) {
		if ((clauses == null) || (clauses.isEmpty()))
			return;

		conf.set(CqlConfigHelper.INPUT_CQL_WHERE_CLAUSE_CONFIG, clauses);
	}

	public static void setOutputCql(Configuration conf, String cql) {
		if ((cql == null) || (cql.isEmpty()))
			return;

		conf.set(CqlConfigHelper.OUTPUT_CQL, cql);
	}

	public static void setInputCql(Configuration conf, String cql) {
		if ((cql == null) || (cql.isEmpty()))
			return;

		conf.set(CqlConfigHelper.INPUT_CQL, cql);
	}

	public static void setUserNameAndPassword(Configuration conf, String username, String password) {
		if (StringUtils.isNotBlank(username)) {
			conf.set(CqlConfigHelper.INPUT_NATIVE_AUTH_PROVIDER, PlainTextAuthProvider.class.getName());
			conf.set(CqlConfigHelper.USERNAME, username);
			conf.set(CqlConfigHelper.PASSWORD, password);
		}
	}

	public static Optional<Integer> getInputCoreConnections(Configuration conf) {
		return CqlConfigHelper.getIntSetting(CqlConfigHelper.INPUT_NATIVE_CORE_CONNECTIONS_PER_HOST, conf);
	}

	public static Optional<Integer> getInputMaxConnections(Configuration conf) {
		return CqlConfigHelper.getIntSetting(CqlConfigHelper.INPUT_NATIVE_MAX_CONNECTIONS_PER_HOST, conf);
	}

	public static int getInputNativePort(Configuration conf) {
		return Integer.parseInt(conf.get(CqlConfigHelper.INPUT_NATIVE_PORT, "9042"));
	}

	public static int getOutputNativePort(Configuration conf) {
		return Integer.parseInt(conf.get(CqlConfigHelper.OUTPUT_NATIVE_PORT, "9042"));
	}

	public static Optional<Integer> getInputMaxSimultReqPerConnections(Configuration conf) {
		return CqlConfigHelper.getIntSetting(CqlConfigHelper.INPUT_NATIVE_MAX_SIMULT_REQ_PER_CONNECTION, conf);
	}

	public static Optional<Integer> getInputNativeConnectionTimeout(Configuration conf) {
		return CqlConfigHelper.getIntSetting(CqlConfigHelper.INPUT_NATIVE_CONNECTION_TIMEOUT, conf);
	}

	public static Optional<Integer> getInputNativeReadConnectionTimeout(Configuration conf) {
		return CqlConfigHelper.getIntSetting(CqlConfigHelper.INPUT_NATIVE_READ_CONNECTION_TIMEOUT, conf);
	}

	public static Optional<Integer> getInputNativeReceiveBufferSize(Configuration conf) {
		return CqlConfigHelper.getIntSetting(CqlConfigHelper.INPUT_NATIVE_RECEIVE_BUFFER_SIZE, conf);
	}

	public static Optional<Integer> getInputNativeSendBufferSize(Configuration conf) {
		return CqlConfigHelper.getIntSetting(CqlConfigHelper.INPUT_NATIVE_SEND_BUFFER_SIZE, conf);
	}

	public static Optional<Integer> getInputNativeSolinger(Configuration conf) {
		return CqlConfigHelper.getIntSetting(CqlConfigHelper.INPUT_NATIVE_SOLINGER, conf);
	}

	public static Optional<Boolean> getInputNativeTcpNodelay(Configuration conf) {
		return CqlConfigHelper.getBooleanSetting(CqlConfigHelper.INPUT_NATIVE_TCP_NODELAY, conf);
	}

	public static Optional<Boolean> getInputNativeReuseAddress(Configuration conf) {
		return CqlConfigHelper.getBooleanSetting(CqlConfigHelper.INPUT_NATIVE_REUSE_ADDRESS, conf);
	}

	public static Optional<String> getInputNativeAuthProvider(Configuration conf) {
		return CqlConfigHelper.getStringSetting(CqlConfigHelper.INPUT_NATIVE_AUTH_PROVIDER, conf);
	}

	public static Optional<String> getInputNativeSSLTruststorePath(Configuration conf) {
		return CqlConfigHelper.getStringSetting(CqlConfigHelper.INPUT_NATIVE_SSL_TRUST_STORE_PATH, conf);
	}

	public static Optional<String> getInputNativeSSLKeystorePath(Configuration conf) {
		return CqlConfigHelper.getStringSetting(CqlConfigHelper.INPUT_NATIVE_SSL_KEY_STORE_PATH, conf);
	}

	public static Optional<String> getInputNativeSSLKeystorePassword(Configuration conf) {
		return CqlConfigHelper.getStringSetting(CqlConfigHelper.INPUT_NATIVE_SSL_KEY_STORE_PASSWARD, conf);
	}

	public static Optional<String> getInputNativeSSLTruststorePassword(Configuration conf) {
		return CqlConfigHelper.getStringSetting(CqlConfigHelper.INPUT_NATIVE_SSL_TRUST_STORE_PASSWARD, conf);
	}

	public static Optional<String> getInputNativeSSLCipherSuites(Configuration conf) {
		return CqlConfigHelper.getStringSetting(CqlConfigHelper.INPUT_NATIVE_SSL_CIPHER_SUITES, conf);
	}

	public static Optional<Boolean> getInputNativeKeepAlive(Configuration conf) {
		return CqlConfigHelper.getBooleanSetting(CqlConfigHelper.INPUT_NATIVE_KEEP_ALIVE, conf);
	}

	public static String getInputcolumns(Configuration conf) {
		return conf.get(CqlConfigHelper.INPUT_CQL_COLUMNS_CONFIG);
	}

	public static Optional<Integer> getInputPageRowSize(Configuration conf) {
		return CqlConfigHelper.getIntSetting(CqlConfigHelper.INPUT_CQL_PAGE_ROW_SIZE_CONFIG, conf);
	}

	public static String getInputWhereClauses(Configuration conf) {
		return conf.get(CqlConfigHelper.INPUT_CQL_WHERE_CLAUSE_CONFIG);
	}

	public static String getInputCql(Configuration conf) {
		return conf.get(CqlConfigHelper.INPUT_CQL);
	}

	public static String getOutputCql(Configuration conf) {
		return conf.get(CqlConfigHelper.OUTPUT_CQL);
	}

	private static Optional<Integer> getProtocolVersion(Configuration conf) {
		return CqlConfigHelper.getIntSetting(CqlConfigHelper.INPUT_NATIVE_PROTOCOL_VERSION, conf);
	}

	public static Cluster getInputCluster(String host, Configuration conf) {
		return CqlConfigHelper.getInputCluster(new String[]{ host }, conf);
	}

	public static Cluster getInputCluster(String[] hosts, Configuration conf) {
		int port = CqlConfigHelper.getInputNativePort(conf);
		return CqlConfigHelper.getCluster(hosts, conf, port);
	}

	public static Cluster getOutputCluster(String host, Configuration conf) {
		return CqlConfigHelper.getOutputCluster(new String[]{ host }, conf);
	}

	public static Cluster getOutputCluster(String[] hosts, Configuration conf) {
		int port = CqlConfigHelper.getOutputNativePort(conf);
		return CqlConfigHelper.getCluster(hosts, conf, port);
	}

	public static Cluster getCluster(String[] hosts, Configuration conf, int port) {
		Optional<AuthProvider> authProvider = CqlConfigHelper.getAuthProvider(conf);
		Optional<SSLOptions> sslOptions = CqlConfigHelper.getSSLOptions(conf);
		Optional<Integer> protocolVersion = CqlConfigHelper.getProtocolVersion(conf);
		LoadBalancingPolicy loadBalancingPolicy = CqlConfigHelper.getReadLoadBalancingPolicy(hosts);
		SocketOptions socketOptions = CqlConfigHelper.getReadSocketOptions(conf);
		QueryOptions queryOptions = CqlConfigHelper.getReadQueryOptions(conf);
		PoolingOptions poolingOptions = CqlConfigHelper.getReadPoolingOptions(conf);
		Cluster.Builder builder = Cluster.builder().addContactPoints(hosts).withPort(port).withCompression(NONE);
		if (authProvider.isPresent())
			builder.withAuthProvider(authProvider.get());

		if (sslOptions.isPresent())
			builder.withSSL(sslOptions.get());

		if (protocolVersion.isPresent()) {
			builder.withProtocolVersion(ProtocolVersion.fromInt(protocolVersion.get()));
		}
		builder.withLoadBalancingPolicy(loadBalancingPolicy).withSocketOptions(socketOptions).withQueryOptions(queryOptions).withPoolingOptions(poolingOptions);
		return builder.build();
	}

	public static void setInputCoreConnections(Configuration conf, String connections) {
		conf.set(CqlConfigHelper.INPUT_NATIVE_CORE_CONNECTIONS_PER_HOST, connections);
	}

	public static void setInputMaxConnections(Configuration conf, String connections) {
		conf.set(CqlConfigHelper.INPUT_NATIVE_MAX_CONNECTIONS_PER_HOST, connections);
	}

	public static void setInputMaxSimultReqPerConnections(Configuration conf, String reqs) {
		conf.set(CqlConfigHelper.INPUT_NATIVE_MAX_SIMULT_REQ_PER_CONNECTION, reqs);
	}

	public static void setInputNativeConnectionTimeout(Configuration conf, String timeout) {
		conf.set(CqlConfigHelper.INPUT_NATIVE_CONNECTION_TIMEOUT, timeout);
	}

	public static void setInputNativeReadConnectionTimeout(Configuration conf, String timeout) {
		conf.set(CqlConfigHelper.INPUT_NATIVE_READ_CONNECTION_TIMEOUT, timeout);
	}

	public static void setInputNativeReceiveBufferSize(Configuration conf, String size) {
		conf.set(CqlConfigHelper.INPUT_NATIVE_RECEIVE_BUFFER_SIZE, size);
	}

	public static void setInputNativeSendBufferSize(Configuration conf, String size) {
		conf.set(CqlConfigHelper.INPUT_NATIVE_SEND_BUFFER_SIZE, size);
	}

	public static void setInputNativeSolinger(Configuration conf, String solinger) {
		conf.set(CqlConfigHelper.INPUT_NATIVE_SOLINGER, solinger);
	}

	public static void setInputNativeTcpNodelay(Configuration conf, String tcpNodelay) {
		conf.set(CqlConfigHelper.INPUT_NATIVE_TCP_NODELAY, tcpNodelay);
	}

	public static void setInputNativeAuthProvider(Configuration conf, String authProvider) {
		conf.set(CqlConfigHelper.INPUT_NATIVE_AUTH_PROVIDER, authProvider);
	}

	public static void setInputNativeSSLTruststorePath(Configuration conf, String path) {
		conf.set(CqlConfigHelper.INPUT_NATIVE_SSL_TRUST_STORE_PATH, path);
	}

	public static void setInputNativeSSLKeystorePath(Configuration conf, String path) {
		conf.set(CqlConfigHelper.INPUT_NATIVE_SSL_KEY_STORE_PATH, path);
	}

	public static void setInputNativeSSLKeystorePassword(Configuration conf, String pass) {
		conf.set(CqlConfigHelper.INPUT_NATIVE_SSL_KEY_STORE_PASSWARD, pass);
	}

	public static void setInputNativeSSLTruststorePassword(Configuration conf, String pass) {
		conf.set(CqlConfigHelper.INPUT_NATIVE_SSL_TRUST_STORE_PASSWARD, pass);
	}

	public static void setInputNativeSSLCipherSuites(Configuration conf, String suites) {
		conf.set(CqlConfigHelper.INPUT_NATIVE_SSL_CIPHER_SUITES, suites);
	}

	public static void setInputNativeReuseAddress(Configuration conf, String reuseAddress) {
		conf.set(CqlConfigHelper.INPUT_NATIVE_REUSE_ADDRESS, reuseAddress);
	}

	public static void setInputNativeKeepAlive(Configuration conf, String keepAlive) {
		conf.set(CqlConfigHelper.INPUT_NATIVE_KEEP_ALIVE, keepAlive);
	}

	public static void setInputNativePort(Configuration conf, String port) {
		conf.set(CqlConfigHelper.INPUT_NATIVE_PORT, port);
	}

	private static PoolingOptions getReadPoolingOptions(Configuration conf) {
		Optional<Integer> coreConnections = CqlConfigHelper.getInputCoreConnections(conf);
		Optional<Integer> maxConnections = CqlConfigHelper.getInputMaxConnections(conf);
		Optional<Integer> maxSimultaneousRequests = CqlConfigHelper.getInputMaxSimultReqPerConnections(conf);
		PoolingOptions poolingOptions = new PoolingOptions();
		for (HostDistance hostDistance : Arrays.asList(HostDistance.LOCAL, HostDistance.REMOTE)) {
			if (coreConnections.isPresent())
				poolingOptions.setCoreConnectionsPerHost(hostDistance, coreConnections.get());

			if (maxConnections.isPresent())
				poolingOptions.setMaxConnectionsPerHost(hostDistance, maxConnections.get());

			if (maxSimultaneousRequests.isPresent())
				poolingOptions.setNewConnectionThreshold(hostDistance, maxSimultaneousRequests.get());

		}
		return poolingOptions;
	}

	private static QueryOptions getReadQueryOptions(Configuration conf) {
		String CL = ConfigHelper.getReadConsistencyLevel(conf);
		Optional<Integer> fetchSize = CqlConfigHelper.getInputPageRowSize(conf);
		QueryOptions queryOptions = new QueryOptions();
		if ((CL != null) && (!(CL.isEmpty())))
			queryOptions.setConsistencyLevel(ConsistencyLevel.valueOf(CL));

		if (fetchSize.isPresent())
			queryOptions.setFetchSize(fetchSize.get());

		return queryOptions;
	}

	private static SocketOptions getReadSocketOptions(Configuration conf) {
		SocketOptions socketOptions = new SocketOptions();
		Optional<Integer> connectTimeoutMillis = CqlConfigHelper.getInputNativeConnectionTimeout(conf);
		Optional<Integer> readTimeoutMillis = CqlConfigHelper.getInputNativeReadConnectionTimeout(conf);
		Optional<Integer> receiveBufferSize = CqlConfigHelper.getInputNativeReceiveBufferSize(conf);
		Optional<Integer> sendBufferSize = CqlConfigHelper.getInputNativeSendBufferSize(conf);
		Optional<Integer> soLinger = CqlConfigHelper.getInputNativeSolinger(conf);
		Optional<Boolean> tcpNoDelay = CqlConfigHelper.getInputNativeTcpNodelay(conf);
		Optional<Boolean> reuseAddress = CqlConfigHelper.getInputNativeReuseAddress(conf);
		Optional<Boolean> keepAlive = CqlConfigHelper.getInputNativeKeepAlive(conf);
		if (connectTimeoutMillis.isPresent())
			socketOptions.setConnectTimeoutMillis(connectTimeoutMillis.get());

		if (readTimeoutMillis.isPresent())
			socketOptions.setReadTimeoutMillis(readTimeoutMillis.get());

		if (receiveBufferSize.isPresent())
			socketOptions.setReceiveBufferSize(receiveBufferSize.get());

		if (sendBufferSize.isPresent())
			socketOptions.setSendBufferSize(sendBufferSize.get());

		if (soLinger.isPresent())
			socketOptions.setSoLinger(soLinger.get());

		if (tcpNoDelay.isPresent())
			socketOptions.setTcpNoDelay(tcpNoDelay.get());

		if (reuseAddress.isPresent())
			socketOptions.setReuseAddress(reuseAddress.get());

		if (keepAlive.isPresent())
			socketOptions.setKeepAlive(keepAlive.get());

		return socketOptions;
	}

	private static LoadBalancingPolicy getReadLoadBalancingPolicy(final String[] stickHosts) {
		return null;
	}

	private static Optional<AuthProvider> getDefaultAuthProvider(Configuration conf) {
		Optional<String> username = CqlConfigHelper.getStringSetting(CqlConfigHelper.USERNAME, conf);
		Optional<String> password = CqlConfigHelper.getStringSetting(CqlConfigHelper.PASSWORD, conf);
		if ((username.isPresent()) && (password.isPresent())) {
			return Optional.of(new PlainTextAuthProvider(username.get(), password.get()));
		}else {
			return Optional.absent();
		}
	}

	private static Optional<AuthProvider> getAuthProvider(Configuration conf) {
		Optional<String> authProvider = CqlConfigHelper.getInputNativeAuthProvider(conf);
		if (!(authProvider.isPresent()))
			return CqlConfigHelper.getDefaultAuthProvider(conf);

		return Optional.of(CqlConfigHelper.getClientAuthProvider(authProvider.get(), conf));
	}

	public static Optional<SSLOptions> getSSLOptions(Configuration conf) {
		Optional<String> truststorePath = CqlConfigHelper.getInputNativeSSLTruststorePath(conf);
		if (truststorePath.isPresent()) {
			Optional<String> keystorePath = CqlConfigHelper.getInputNativeSSLKeystorePath(conf);
			Optional<String> truststorePassword = CqlConfigHelper.getInputNativeSSLTruststorePassword(conf);
			Optional<String> keystorePassword = CqlConfigHelper.getInputNativeSSLKeystorePassword(conf);
			Optional<String> cipherSuites = CqlConfigHelper.getInputNativeSSLCipherSuites(conf);
			SSLContext context;
			try {
				context = CqlConfigHelper.getSSLContext(truststorePath, truststorePassword, keystorePath, keystorePassword);
			} catch (UnrecoverableKeyException | KeyManagementException | NoSuchAlgorithmException | KeyStoreException | CertificateException | IOException e) {
				throw new RuntimeException(e);
			}
			String[] css = null;
			if (cipherSuites.isPresent())
				css = cipherSuites.get().split(",");

			return Optional.of(JdkSSLOptions.builder().withSSLContext(context).withCipherSuites(css).build());
		}
		return Optional.absent();
	}

	private static Optional<Integer> getIntSetting(String parameter, Configuration conf) {
		String setting = conf.get(parameter);
		if (setting == null)
			return Optional.absent();

		return Optional.of(Integer.valueOf(setting));
	}

	private static Optional<Boolean> getBooleanSetting(String parameter, Configuration conf) {
		String setting = conf.get(parameter);
		if (setting == null)
			return Optional.absent();

		return Optional.of(Boolean.valueOf(setting));
	}

	private static Optional<String> getStringSetting(String parameter, Configuration conf) {
		String setting = conf.get(parameter);
		if (setting == null)
			return Optional.absent();

		return Optional.of(setting);
	}

	private static AuthProvider getClientAuthProvider(String factoryClassName, Configuration conf) {
		try {
			Class<?> c = Class.forName(factoryClassName);
			if (PlainTextAuthProvider.class.equals(c)) {
				String username = CqlConfigHelper.getStringSetting(CqlConfigHelper.USERNAME, conf).or("");
				String password = CqlConfigHelper.getStringSetting(CqlConfigHelper.PASSWORD, conf).or("");
				return ((AuthProvider) (c.getConstructor(String.class, String.class).newInstance(username, password)));
			}else {
				return ((AuthProvider) (c.newInstance()));
			}
		} catch (Exception e) {
			throw new RuntimeException(("Failed to instantiate auth provider:" + factoryClassName), e);
		}
	}

	private static SSLContext getSSLContext(Optional<String> truststorePath, Optional<String> truststorePassword, Optional<String> keystorePath, Optional<String> keystorePassword) throws IOException, KeyManagementException, KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException, CertificateException {
		SSLContext ctx = SSLContext.getInstance("SSL");
		TrustManagerFactory tmf = null;
		if (truststorePath.isPresent()) {
			try (FileInputStream tsf = new FileInputStream(truststorePath.get())) {
				KeyStore ts = KeyStore.getInstance("JKS");
				ts.load(tsf, (truststorePassword.isPresent() ? truststorePassword.get().toCharArray() : null));
				tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
				tmf.init(ts);
			}
		}
		KeyManagerFactory kmf = null;
		if (keystorePath.isPresent()) {
			try (FileInputStream ksf = new FileInputStream(keystorePath.get())) {
				KeyStore ks = KeyStore.getInstance("JKS");
				ks.load(ksf, (keystorePassword.isPresent() ? keystorePassword.get().toCharArray() : null));
				kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
				kmf.init(ks, (keystorePassword.isPresent() ? keystorePassword.get().toCharArray() : null));
			}
		}
		ctx.init((kmf != null ? kmf.getKeyManagers() : null), (tmf != null ? tmf.getTrustManagers() : null), new SecureRandom());
		return ctx;
	}
}

