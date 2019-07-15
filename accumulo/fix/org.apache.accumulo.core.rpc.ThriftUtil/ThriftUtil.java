

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import javax.net.SocketFactory;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.security.auth.callback.CallbackHandler;
import org.apache.accumulo.core.client.impl.ClientContext;
import org.apache.accumulo.core.client.impl.ThriftTransportPool;
import org.apache.accumulo.core.rpc.SaslConnectionParams;
import org.apache.accumulo.core.rpc.SslConnectionParams;
import org.apache.accumulo.core.rpc.TTimeoutTransport;
import org.apache.accumulo.core.rpc.TraceProtocolFactory;
import org.apache.accumulo.core.rpc.UGIAssumingTransport;
import org.apache.accumulo.core.tabletserver.thrift.TabletClientService;
import org.apache.accumulo.core.util.HostAndPort;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.thrift.TException;
import org.apache.thrift.TServiceClient;
import org.apache.thrift.TServiceClientFactory;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSSLTransportFactory;
import org.apache.thrift.transport.TSaslClientTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.apache.thrift.transport.TTransportFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.hadoop.security.UserGroupInformation.AuthenticationMethod.PROXY;


public class ThriftUtil {
	private static final Logger log = LoggerFactory.getLogger(ThriftUtil.class);

	private static final TraceProtocolFactory protocolFactory = new TraceProtocolFactory();

	private static final TFramedTransport.Factory transportFactory = new TFramedTransport.Factory(Integer.MAX_VALUE);

	private static final Map<Integer, TTransportFactory> factoryCache = new HashMap<>();

	public static final String GSSAPI = "GSSAPI";

	public static final String DIGEST_MD5 = "DIGEST-MD5";

	private static final Random SASL_BACKOFF_RAND = new Random();

	private static final int RELOGIN_MAX_BACKOFF = 5000;

	public static TProtocolFactory protocolFactory() {
		return ThriftUtil.protocolFactory;
	}

	public static TTransportFactory transportFactory() {
		return ThriftUtil.transportFactory;
	}

	public static <T extends TServiceClient> T createClient(TServiceClientFactory<T> factory, TTransport transport) {
		return factory.getClient(ThriftUtil.protocolFactory.getProtocol(transport), ThriftUtil.protocolFactory.getProtocol(transport));
	}

	public static <T extends TServiceClient> T getClientNoTimeout(TServiceClientFactory<T> factory, HostAndPort address, ClientContext context) throws TTransportException {
		return ThriftUtil.getClient(factory, address, context, 0);
	}

	public static <T extends TServiceClient> T getClient(TServiceClientFactory<T> factory, HostAndPort address, ClientContext context) throws TTransportException {
		TTransport transport = ThriftTransportPool.getInstance().getTransport(address, context.getClientTimeoutInMillis(), context);
		return ThriftUtil.createClient(factory, transport);
	}

	public static <T extends TServiceClient> T getClient(TServiceClientFactory<T> factory, HostAndPort address, ClientContext context, long timeout) throws TTransportException {
		TTransport transport = ThriftTransportPool.getInstance().getTransport(address, timeout, context);
		return ThriftUtil.createClient(factory, transport);
	}

	public static void returnClient(TServiceClient iface) {
		if (iface != null) {
			ThriftTransportPool.getInstance().returnTransport(iface.getInputProtocol().getTransport());
		}
	}

	public static TabletClientService.Client getTServerClient(HostAndPort address, ClientContext context) throws TTransportException {
		return ThriftUtil.getClient(new TabletClientService.Client.Factory(), address, context);
	}

	public static TabletClientService.Client getTServerClient(HostAndPort address, ClientContext context, long timeout) throws TTransportException {
		return ThriftUtil.getClient(new TabletClientService.Client.Factory(), address, context, timeout);
	}

	public static TTransport createTransport(HostAndPort address, ClientContext context) throws TException {
		return ThriftUtil.createClientTransport(address, ((int) (context.getClientTimeoutInMillis())), context.getClientSslParams(), context.getSaslParams());
	}

	public static synchronized TTransportFactory transportFactory(int maxFrameSize) {
		TTransportFactory factory = ThriftUtil.factoryCache.get(maxFrameSize);
		if (factory == null) {
			factory = new TFramedTransport.Factory(maxFrameSize);
			ThriftUtil.factoryCache.put(maxFrameSize, factory);
		}
		return factory;
	}

	public static synchronized TTransportFactory transportFactory(long maxFrameSize) {
		if ((maxFrameSize > (Integer.MAX_VALUE)) || (maxFrameSize < 1))
			throw new RuntimeException(("Thrift transport frames are limited to " + (Integer.MAX_VALUE)));

		return ThriftUtil.transportFactory(((int) (maxFrameSize)));
	}

	public static TTransport createClientTransport(HostAndPort address, int timeout, SslConnectionParams sslParams, SaslConnectionParams saslParams) throws TTransportException {
		boolean success = false;
		TTransport transport = null;
		try {
			if (sslParams != null) {
				if (null != saslParams) {
					throw new IllegalStateException("Cannot use both SSL and SASL");
				}
				ThriftUtil.log.trace("Creating SSL client transport");
				if (sslParams.useJsse()) {
					transport = TSSLTransportFactory.getClientSocket(address.getHost(), address.getPort(), timeout);
				}else {
					SSLContext sslContext = ThriftUtil.createSSLContext(sslParams);
					SSLSocketFactory sslSockFactory = sslContext.getSocketFactory();
				}
				transport = ThriftUtil.transportFactory().getTransport(transport);
			}else
				if (null != saslParams) {
					if (!(UserGroupInformation.isSecurityEnabled())) {
						throw new IllegalStateException("Expected Kerberos security to be enabled if SASL is in use");
					}
					ThriftUtil.log.trace("Creating SASL connection to {}:{}", address.getHost(), address.getPort());
					try {
						transport = TTimeoutTransport.create(address, timeout);
					} catch (IOException e) {
						ThriftUtil.log.warn("Failed to open transport to {}", address);
						throw new TTransportException(e);
					}
					try {
						final UserGroupInformation currentUser = UserGroupInformation.getCurrentUser();
						final UserGroupInformation userForRpc;
						if ((PROXY) == (currentUser.getAuthenticationMethod())) {
							if ((currentUser.getRealUser()) != null) {
								userForRpc = currentUser.getRealUser();
								ThriftUtil.log.trace("{} is a proxy user, using real user instead {}", currentUser, userForRpc);
							}else {
								ThriftUtil.log.warn(("The current user is a proxy user but there is no" + " underlying real user (likely that RPCs will fail): {}"), currentUser);
								userForRpc = currentUser;
							}
						}else {
							userForRpc = currentUser;
						}
						final String hostname = InetAddress.getByName(address.getHost()).getCanonicalHostName();
						final SaslConnectionParams.SaslMechanism mechanism = saslParams.getMechanism();
						ThriftUtil.log.trace("Opening transport to server as {} to {}/{} using {}");
						transport = new TSaslClientTransport(mechanism.getMechanismName(), null, saslParams.getKerberosServerPrimary(), hostname, saslParams.getSaslProperties(), saslParams.getCallbackHandler(), transport);
						transport = new UGIAssumingTransport(transport, userForRpc);
						transport.open();
					} catch (TTransportException e) {
						ThriftUtil.log.warn("Failed to open SASL transport", e);
						ThriftUtil.log.debug(("Caught TTransportException opening SASL transport," + (" checking if re-login is necessary before propagating the" + " exception.")));
						ThriftUtil.attemptClientReLogin();
						throw e;
					} catch (IOException e) {
						ThriftUtil.log.warn("Failed to open SASL transport", e);
						throw new TTransportException(e);
					}
				}else {
					ThriftUtil.log.trace("Opening normal transport");
					if (timeout == 0) {
						transport = new TSocket(address.getHost(), address.getPort());
						transport.open();
					}else {
						try {
							transport = TTimeoutTransport.create(address, timeout);
						} catch (IOException ex) {
							ThriftUtil.log.warn(("Failed to open transport to " + address));
							throw new TTransportException(ex);
						}
						transport.open();
					}
					transport = ThriftUtil.transportFactory().getTransport(transport);
				}

			success = true;
		} finally {
			if ((!success) && (transport != null)) {
				transport.close();
			}
		}
		return transport;
	}

	static void attemptClientReLogin() {
		try {
			UserGroupInformation loginUser = UserGroupInformation.getLoginUser();
			if ((null == loginUser) || (!(loginUser.hasKerberosCredentials()))) {
				throw new RuntimeException("Expected to find Kerberos UGI credentials, but did not");
			}
			UserGroupInformation currentUser = UserGroupInformation.getCurrentUser();
			UserGroupInformation realUser = currentUser.getRealUser();
			if ((loginUser.equals(currentUser)) || (loginUser.equals(realUser))) {
				if (UserGroupInformation.isLoginKeytabBased()) {
					ThriftUtil.log.info("Performing keytab-based Kerberos re-login");
					loginUser.reloginFromKeytab();
				}else {
					ThriftUtil.log.info("Performing ticket-cache-based Kerberos re-login");
					loginUser.reloginFromTicketCache();
				}
				try {
					Thread.sleep(((ThriftUtil.SASL_BACKOFF_RAND.nextInt(ThriftUtil.RELOGIN_MAX_BACKOFF)) + 1));
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
			}else {
				ThriftUtil.log.debug("Not attempting Kerberos re-login: loginUser={}, currentUser={}, realUser={}", loginUser, currentUser);
			}
		} catch (IOException e) {
			ThriftUtil.log.warn("Failed to check (and/or perform) Kerberos client re-login", e);
			throw new RuntimeException(e);
		}
	}

	private static SSLContext createSSLContext(SslConnectionParams params) throws TTransportException {
		SSLContext ctx;
		try {
			ctx = SSLContext.getInstance(params.getClientProtocol());
			TrustManagerFactory tmf = null;
			KeyManagerFactory kmf = null;
			if (params.isTrustStoreSet()) {
				tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
				KeyStore ts = KeyStore.getInstance(params.getTrustStoreType());
				try (FileInputStream fis = new FileInputStream(params.getTrustStorePath())) {
					ts.load(fis, params.getTrustStorePass().toCharArray());
				}
				tmf.init(ts);
			}
			if (params.isKeyStoreSet()) {
				kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
				KeyStore ks = KeyStore.getInstance(params.getKeyStoreType());
				try (FileInputStream fis = new FileInputStream(params.getKeyStorePath())) {
					ks.load(fis, params.getKeyStorePass().toCharArray());
				}
				kmf.init(ks, params.getKeyStorePass().toCharArray());
			}
			if ((params.isKeyStoreSet()) && (params.isTrustStoreSet())) {
				ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
			}else
				if (params.isKeyStoreSet()) {
					ctx.init(kmf.getKeyManagers(), null, null);
				}else {
					ctx.init(null, tmf.getTrustManagers(), null);
				}

		} catch (Exception e) {
			throw new TTransportException("Error creating the transport", e);
		}
		return ctx;
	}

	private static TSocket createClient(SSLSocketFactory factory, String host, int port, int timeout) throws TTransportException {
		SSLSocket socket = null;
		try {
			socket = ((SSLSocket) (factory.createSocket(host, port)));
			socket.setSoTimeout(timeout);
			return new TSocket(socket);
		} catch (Exception e) {
			try {
				if (socket != null)
					socket.close();

			} catch (IOException ioe) {
			}
			throw new TTransportException(((("Could not connect to " + host) + " on port ") + port), e);
		}
	}
}

