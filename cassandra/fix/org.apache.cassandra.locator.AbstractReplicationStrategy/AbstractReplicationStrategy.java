

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.cassandra.db.ConsistencyLevel;
import org.apache.cassandra.db.Keyspace;
import org.apache.cassandra.db.WriteType;
import org.apache.cassandra.dht.Range;
import org.apache.cassandra.dht.RingPosition;
import org.apache.cassandra.dht.Token;
import org.apache.cassandra.exceptions.ConfigurationException;
import org.apache.cassandra.locator.IEndpointSnitch;
import org.apache.cassandra.locator.TokenMetadata;
import org.apache.cassandra.service.AbstractWriteResponseHandler;
import org.apache.cassandra.service.DatacenterWriteResponseHandler;
import org.apache.cassandra.service.WriteResponseHandler;
import org.apache.cassandra.utils.FBUtilities;
import org.cliffc.high_scale_lib.NonBlockingHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class AbstractReplicationStrategy {
	private static final Logger logger = LoggerFactory.getLogger(AbstractReplicationStrategy.class);

	@VisibleForTesting
	final String keyspaceName;

	private Keyspace keyspace;

	public final Map<String, String> configOptions;

	private final TokenMetadata tokenMetadata;

	private volatile long lastInvalidatedVersion = 0;

	public IEndpointSnitch snitch;

	protected AbstractReplicationStrategy(String keyspaceName, TokenMetadata tokenMetadata, IEndpointSnitch snitch, Map<String, String> configOptions) {
		assert keyspaceName != null;
		assert snitch != null;
		assert tokenMetadata != null;
		this.tokenMetadata = tokenMetadata;
		this.snitch = snitch;
		this.configOptions = (configOptions == null) ? Collections.<String, String>emptyMap() : configOptions;
		this.keyspaceName = keyspaceName;
	}

	private final Map<Token, ArrayList<InetAddress>> cachedEndpoints = new NonBlockingHashMap<Token, ArrayList<InetAddress>>();

	public ArrayList<InetAddress> getCachedEndpoints(Token t) {
		long lastVersion = tokenMetadata.getRingVersion();
		if (lastVersion > (lastInvalidatedVersion)) {
			synchronized(this) {
				if (lastVersion > (lastInvalidatedVersion)) {
					AbstractReplicationStrategy.logger.trace("clearing cached endpoints");
					cachedEndpoints.clear();
					lastInvalidatedVersion = lastVersion;
				}
			}
		}
		return cachedEndpoints.get(t);
	}

	public ArrayList<InetAddress> getNaturalEndpoints(RingPosition searchPosition) {
		Token searchToken = searchPosition.getToken();
		Token keyToken = TokenMetadata.firstToken(tokenMetadata.sortedTokens(), searchToken);
		ArrayList<InetAddress> endpoints = getCachedEndpoints(keyToken);
		if (endpoints == null) {
			TokenMetadata tm = tokenMetadata.cachedOnlyTokenMap();
			keyToken = TokenMetadata.firstToken(tm.sortedTokens(), searchToken);
			endpoints = new ArrayList<InetAddress>(calculateNaturalEndpoints(searchToken, tm));
			cachedEndpoints.put(keyToken, endpoints);
		}
		return new ArrayList<InetAddress>(endpoints);
	}

	public abstract List<InetAddress> calculateNaturalEndpoints(Token searchToken, TokenMetadata tokenMetadata);

	public <T> AbstractWriteResponseHandler<T> getWriteResponseHandler(Collection<InetAddress> naturalEndpoints, Collection<InetAddress> pendingEndpoints, ConsistencyLevel consistency_level, Runnable callback, WriteType writeType, long queryStartNanoTime) {
		if (consistency_level.isDatacenterLocal()) {
			return new DatacenterWriteResponseHandler<T>(naturalEndpoints, pendingEndpoints, consistency_level, getKeyspace(), callback, writeType, queryStartNanoTime);
		}else {
		}
		return new WriteResponseHandler<T>(naturalEndpoints, pendingEndpoints, consistency_level, getKeyspace(), callback, writeType, queryStartNanoTime);
	}

	private Keyspace getKeyspace() {
		if ((keyspace) == null)
			keyspace = Keyspace.open(keyspaceName);

		return keyspace;
	}

	public abstract int getReplicationFactor();

	public Multimap<InetAddress, Range<Token>> getAddressRanges(TokenMetadata metadata) {
		Multimap<InetAddress, Range<Token>> map = HashMultimap.create();
		for (Token token : metadata.sortedTokens()) {
			Range<Token> range = metadata.getPrimaryRangeFor(token);
			for (InetAddress ep : calculateNaturalEndpoints(token, metadata)) {
				map.put(ep, range);
			}
		}
		return map;
	}

	public Multimap<Range<Token>, InetAddress> getRangeAddresses(TokenMetadata metadata) {
		Multimap<Range<Token>, InetAddress> map = HashMultimap.create();
		for (Token token : metadata.sortedTokens()) {
			Range<Token> range = metadata.getPrimaryRangeFor(token);
			for (InetAddress ep : calculateNaturalEndpoints(token, metadata)) {
				map.put(range, ep);
			}
		}
		return map;
	}

	public Multimap<InetAddress, Range<Token>> getAddressRanges() {
		return getAddressRanges(tokenMetadata.cloneOnlyTokenMap());
	}

	public Collection<Range<Token>> getPendingAddressRanges(TokenMetadata metadata, Token pendingToken, InetAddress pendingAddress) {
		return getPendingAddressRanges(metadata, Arrays.asList(pendingToken), pendingAddress);
	}

	public Collection<Range<Token>> getPendingAddressRanges(TokenMetadata metadata, Collection<Token> pendingTokens, InetAddress pendingAddress) {
		TokenMetadata temp = metadata.cloneOnlyTokenMap();
		temp.updateNormalTokens(pendingTokens, pendingAddress);
		return getAddressRanges(temp).get(pendingAddress);
	}

	public abstract void validateOptions() throws ConfigurationException;

	public Collection<String> recognizedOptions() {
		return null;
	}

	private static AbstractReplicationStrategy createInternal(String keyspaceName, Class<? extends AbstractReplicationStrategy> strategyClass, TokenMetadata tokenMetadata, IEndpointSnitch snitch, Map<String, String> strategyOptions) throws ConfigurationException {
		AbstractReplicationStrategy strategy;
		Class[] parameterTypes = new Class[]{ String.class, TokenMetadata.class, IEndpointSnitch.class, Map.class };
		try {
			Constructor<? extends AbstractReplicationStrategy> constructor = strategyClass.getConstructor(parameterTypes);
			strategy = constructor.newInstance(keyspaceName, tokenMetadata, snitch, strategyOptions);
		} catch (InvocationTargetException e) {
			Throwable targetException = e.getTargetException();
			throw new ConfigurationException(targetException.getMessage(), targetException);
		} catch (Exception e) {
			throw new ConfigurationException("Error constructing replication strategy class", e);
		}
		return strategy;
	}

	public static AbstractReplicationStrategy createReplicationStrategy(String keyspaceName, Class<? extends AbstractReplicationStrategy> strategyClass, TokenMetadata tokenMetadata, IEndpointSnitch snitch, Map<String, String> strategyOptions) {
		AbstractReplicationStrategy strategy = AbstractReplicationStrategy.createInternal(keyspaceName, strategyClass, tokenMetadata, snitch, strategyOptions);
		try {
			strategy.validateExpectedOptions();
		} catch (ConfigurationException e) {
			AbstractReplicationStrategy.logger.warn("Ignoring {}", e.getMessage());
		}
		strategy.validateOptions();
		return strategy;
	}

	public static void validateReplicationStrategy(String keyspaceName, Class<? extends AbstractReplicationStrategy> strategyClass, TokenMetadata tokenMetadata, IEndpointSnitch snitch, Map<String, String> strategyOptions) throws ConfigurationException {
		AbstractReplicationStrategy strategy = AbstractReplicationStrategy.createInternal(keyspaceName, strategyClass, tokenMetadata, snitch, strategyOptions);
		strategy.validateExpectedOptions();
		strategy.validateOptions();
	}

	public static Class<AbstractReplicationStrategy> getClass(String cls) throws ConfigurationException {
		String className = (cls.contains(".")) ? cls : "org.apache.cassandra.locator." + cls;
		Class<AbstractReplicationStrategy> strategyClass = FBUtilities.classForName(className, "replication strategy");
		if (!(AbstractReplicationStrategy.class.isAssignableFrom(strategyClass))) {
			throw new ConfigurationException(String.format("Specified replication strategy class (%s) is not derived from AbstractReplicationStrategy", className));
		}
		return strategyClass;
	}

	public boolean hasSameSettings(AbstractReplicationStrategy other) {
		return (getClass().equals(other.getClass())) && ((getReplicationFactor()) == (other.getReplicationFactor()));
	}

	protected void validateReplicationFactor(String rf) throws ConfigurationException {
		try {
			if ((Integer.parseInt(rf)) < 0) {
				throw new ConfigurationException(("Replication factor must be non-negative; found " + rf));
			}
		} catch (NumberFormatException e2) {
			throw new ConfigurationException(("Replication factor must be numeric; found " + rf));
		}
	}

	private void validateExpectedOptions() throws ConfigurationException {
		Collection expectedOptions = recognizedOptions();
		if (expectedOptions == null)
			return;

		for (String key : configOptions.keySet()) {
			if (!(expectedOptions.contains(key)))
				throw new ConfigurationException(String.format("Unrecognized strategy option {%s} passed to %s for keyspace %s", key, getClass().getSimpleName(), keyspaceName));

		}
	}
}

