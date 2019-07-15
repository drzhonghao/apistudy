

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Multimap;
import com.google.common.collect.UnmodifiableIterator;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.dht.IPartitioner;
import org.apache.cassandra.dht.Range;
import org.apache.cassandra.dht.Token;
import org.apache.cassandra.gms.FailureDetector;
import org.apache.cassandra.gms.IFailureDetector;
import org.apache.cassandra.locator.AbstractReplicationStrategy;
import org.apache.cassandra.locator.IEndpointSnitch;
import org.apache.cassandra.locator.PendingRangeMaps;
import org.apache.cassandra.utils.BiMultiValMap;
import org.apache.cassandra.utils.Pair;
import org.apache.cassandra.utils.SortedBiMultiValMap;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TokenMetadata {
	private static final Logger logger = LoggerFactory.getLogger(TokenMetadata.class);

	private final BiMultiValMap<Token, InetAddress> tokenToEndpointMap;

	private final BiMap<InetAddress, UUID> endpointToHostIdMap;

	private final BiMultiValMap<Token, InetAddress> bootstrapTokens = new BiMultiValMap<>();

	private final BiMap<InetAddress, InetAddress> replacementToOriginal = HashBiMap.create();

	private final Set<InetAddress> leavingEndpoints = new HashSet<>();

	private final ConcurrentMap<String, PendingRangeMaps> pendingRanges = new ConcurrentHashMap<String, PendingRangeMaps>();

	private final Set<Pair<Token, InetAddress>> movingEndpoints = new HashSet<>();

	private final ReadWriteLock lock = new ReentrantReadWriteLock(true);

	private volatile ArrayList<Token> sortedTokens;

	private final TokenMetadata.Topology topology;

	public final IPartitioner partitioner;

	private static final Comparator<InetAddress> inetaddressCmp = new Comparator<InetAddress>() {
		public int compare(InetAddress o1, InetAddress o2) {
			return ByteBuffer.wrap(o1.getAddress()).compareTo(ByteBuffer.wrap(o2.getAddress()));
		}
	};

	private volatile long ringVersion = 0;

	public TokenMetadata() {
		this(SortedBiMultiValMap.<Token, InetAddress>create(null, TokenMetadata.inetaddressCmp), HashBiMap.<InetAddress, UUID>create(), new TokenMetadata.Topology(), DatabaseDescriptor.getPartitioner());
	}

	private TokenMetadata(BiMultiValMap<Token, InetAddress> tokenToEndpointMap, BiMap<InetAddress, UUID> endpointsMap, TokenMetadata.Topology topology, IPartitioner partitioner) {
		this.tokenToEndpointMap = tokenToEndpointMap;
		this.topology = topology;
		this.partitioner = partitioner;
		endpointToHostIdMap = endpointsMap;
		sortedTokens = sortTokens();
	}

	@com.google.common.annotations.VisibleForTesting
	public TokenMetadata cloneWithNewPartitioner(IPartitioner newPartitioner) {
		return new TokenMetadata(tokenToEndpointMap, endpointToHostIdMap, topology, newPartitioner);
	}

	private ArrayList<Token> sortTokens() {
		return new ArrayList<>(tokenToEndpointMap.keySet());
	}

	public int pendingRangeChanges(InetAddress source) {
		int n = 0;
		Collection<Range<Token>> sourceRanges = getPrimaryRangesFor(getTokens(source));
		lock.readLock().lock();
		try {
			for (Token token : bootstrapTokens.keySet())
				for (Range<Token> range : sourceRanges)
					if (range.contains(token))
						n++;



		} finally {
			lock.readLock().unlock();
		}
		return n;
	}

	public void updateNormalToken(Token token, InetAddress endpoint) {
		updateNormalTokens(Collections.singleton(token), endpoint);
	}

	public void updateNormalTokens(Collection<Token> tokens, InetAddress endpoint) {
		Multimap<InetAddress, Token> endpointTokens = HashMultimap.create();
		for (Token token : tokens)
			endpointTokens.put(endpoint, token);

		updateNormalTokens(endpointTokens);
	}

	public void updateNormalTokens(Multimap<InetAddress, Token> endpointTokens) {
		if (endpointTokens.isEmpty())
			return;

		lock.writeLock().lock();
		try {
			boolean shouldSortTokens = false;
			for (InetAddress endpoint : endpointTokens.keySet()) {
				Collection<Token> tokens = endpointTokens.get(endpoint);
				assert (tokens != null) && (!(tokens.isEmpty()));
				bootstrapTokens.removeValue(endpoint);
				tokenToEndpointMap.removeValue(endpoint);
				topology.addEndpoint(endpoint);
				leavingEndpoints.remove(endpoint);
				replacementToOriginal.remove(endpoint);
				removeFromMoving(endpoint);
				for (Token token : tokens) {
					InetAddress prev = tokenToEndpointMap.put(token, endpoint);
					if (!(endpoint.equals(prev))) {
						if (prev != null)
							TokenMetadata.logger.warn("Token {} changing ownership from {} to {}", token, prev, endpoint);

						shouldSortTokens = true;
					}
				}
			}
			if (shouldSortTokens)
				sortedTokens = sortTokens();

		} finally {
			lock.writeLock().unlock();
		}
	}

	public void updateHostId(UUID hostId, InetAddress endpoint) {
		assert hostId != null;
		assert endpoint != null;
		lock.writeLock().lock();
		try {
			InetAddress storedEp = endpointToHostIdMap.inverse().get(hostId);
			if (storedEp != null) {
				if ((!(storedEp.equals(endpoint))) && (FailureDetector.instance.isAlive(storedEp))) {
					throw new RuntimeException(String.format("Host ID collision between active endpoint %s and %s (id=%s)", storedEp, endpoint, hostId));
				}
			}
			UUID storedId = endpointToHostIdMap.get(endpoint);
			if ((storedId != null) && (!(storedId.equals(hostId))))
				TokenMetadata.logger.warn("Changing {}'s host ID from {} to {}", endpoint, storedId, hostId);

			endpointToHostIdMap.forcePut(endpoint, hostId);
		} finally {
			lock.writeLock().unlock();
		}
	}

	public UUID getHostId(InetAddress endpoint) {
		lock.readLock().lock();
		try {
			return endpointToHostIdMap.get(endpoint);
		} finally {
			lock.readLock().unlock();
		}
	}

	public InetAddress getEndpointForHostId(UUID hostId) {
		lock.readLock().lock();
		try {
			return endpointToHostIdMap.inverse().get(hostId);
		} finally {
			lock.readLock().unlock();
		}
	}

	public Map<InetAddress, UUID> getEndpointToHostIdMapForReading() {
		lock.readLock().lock();
		try {
			Map<InetAddress, UUID> readMap = new HashMap<>();
			readMap.putAll(endpointToHostIdMap);
			return readMap;
		} finally {
			lock.readLock().unlock();
		}
	}

	@Deprecated
	public void addBootstrapToken(Token token, InetAddress endpoint) {
		addBootstrapTokens(Collections.singleton(token), endpoint);
	}

	public void addBootstrapTokens(Collection<Token> tokens, InetAddress endpoint) {
		addBootstrapTokens(tokens, endpoint, null);
	}

	private void addBootstrapTokens(Collection<Token> tokens, InetAddress endpoint, InetAddress original) {
		assert (tokens != null) && (!(tokens.isEmpty()));
		assert endpoint != null;
		lock.writeLock().lock();
		try {
			InetAddress oldEndpoint;
			for (Token token : tokens) {
				oldEndpoint = bootstrapTokens.get(token);
				if ((oldEndpoint != null) && (!(oldEndpoint.equals(endpoint))))
					throw new RuntimeException(((((("Bootstrap Token collision between " + oldEndpoint) + " and ") + endpoint) + " (token ") + token));

				oldEndpoint = tokenToEndpointMap.get(token);
				if (((oldEndpoint != null) && (!(oldEndpoint.equals(endpoint)))) && (!(oldEndpoint.equals(original))))
					throw new RuntimeException(((((("Bootstrap Token collision between " + oldEndpoint) + " and ") + endpoint) + " (token ") + token));

			}
			bootstrapTokens.removeValue(endpoint);
			for (Token token : tokens)
				bootstrapTokens.put(token, endpoint);

		} finally {
			lock.writeLock().unlock();
		}
	}

	public void addReplaceTokens(Collection<Token> replacingTokens, InetAddress newNode, InetAddress oldNode) {
		assert (replacingTokens != null) && (!(replacingTokens.isEmpty()));
		assert (newNode != null) && (oldNode != null);
		lock.writeLock().lock();
		try {
			Collection<Token> oldNodeTokens = tokenToEndpointMap.inverse().get(oldNode);
			if ((!(replacingTokens.containsAll(oldNodeTokens))) || (!(oldNodeTokens.containsAll(replacingTokens)))) {
				throw new RuntimeException(String.format(("Node %s is trying to replace node %s with tokens %s with a " + "different set of tokens %s."), newNode, oldNode, oldNodeTokens, replacingTokens));
			}
			TokenMetadata.logger.debug("Replacing {} with {}", newNode, oldNode);
			replacementToOriginal.put(newNode, oldNode);
			addBootstrapTokens(replacingTokens, newNode, oldNode);
		} finally {
			lock.writeLock().unlock();
		}
	}

	public Optional<InetAddress> getReplacementNode(InetAddress endpoint) {
		return Optional.ofNullable(replacementToOriginal.inverse().get(endpoint));
	}

	public Optional<InetAddress> getReplacingNode(InetAddress endpoint) {
		return Optional.ofNullable(replacementToOriginal.get(endpoint));
	}

	public void removeBootstrapTokens(Collection<Token> tokens) {
		assert (tokens != null) && (!(tokens.isEmpty()));
		lock.writeLock().lock();
		try {
			for (Token token : tokens)
				bootstrapTokens.remove(token);

		} finally {
			lock.writeLock().unlock();
		}
	}

	public void addLeavingEndpoint(InetAddress endpoint) {
		assert endpoint != null;
		lock.writeLock().lock();
		try {
			leavingEndpoints.add(endpoint);
		} finally {
			lock.writeLock().unlock();
		}
	}

	public void addMovingEndpoint(Token token, InetAddress endpoint) {
		assert endpoint != null;
		lock.writeLock().lock();
		try {
			movingEndpoints.add(Pair.create(token, endpoint));
		} finally {
			lock.writeLock().unlock();
		}
	}

	public void removeEndpoint(InetAddress endpoint) {
		assert endpoint != null;
		lock.writeLock().lock();
		try {
			bootstrapTokens.removeValue(endpoint);
			tokenToEndpointMap.removeValue(endpoint);
			topology.removeEndpoint(endpoint);
			leavingEndpoints.remove(endpoint);
			if ((replacementToOriginal.remove(endpoint)) != null) {
				TokenMetadata.logger.debug("Node {} failed during replace.", endpoint);
			}
			endpointToHostIdMap.remove(endpoint);
			sortedTokens = sortTokens();
			invalidateCachedRings();
		} finally {
			lock.writeLock().unlock();
		}
	}

	public void updateTopology(InetAddress endpoint) {
		assert endpoint != null;
		lock.writeLock().lock();
		try {
			TokenMetadata.logger.info("Updating topology for {}", endpoint);
			topology.updateEndpoint(endpoint);
			invalidateCachedRings();
		} finally {
			lock.writeLock().unlock();
		}
	}

	public void updateTopology() {
		lock.writeLock().lock();
		try {
			TokenMetadata.logger.info("Updating topology for all endpoints that have changed");
			topology.updateEndpoints();
			invalidateCachedRings();
		} finally {
			lock.writeLock().unlock();
		}
	}

	public void removeFromMoving(InetAddress endpoint) {
		assert endpoint != null;
		lock.writeLock().lock();
		try {
			for (Pair<Token, InetAddress> pair : movingEndpoints) {
				if (pair.right.equals(endpoint)) {
					movingEndpoints.remove(pair);
					break;
				}
			}
			invalidateCachedRings();
		} finally {
			lock.writeLock().unlock();
		}
	}

	public Collection<Token> getTokens(InetAddress endpoint) {
		assert endpoint != null;
		assert isMember(endpoint);
		lock.readLock().lock();
		try {
			return new ArrayList<>(tokenToEndpointMap.inverse().get(endpoint));
		} finally {
			lock.readLock().unlock();
		}
	}

	@Deprecated
	public Token getToken(InetAddress endpoint) {
		return getTokens(endpoint).iterator().next();
	}

	public boolean isMember(InetAddress endpoint) {
		assert endpoint != null;
		lock.readLock().lock();
		try {
			return tokenToEndpointMap.inverse().containsKey(endpoint);
		} finally {
			lock.readLock().unlock();
		}
	}

	public boolean isLeaving(InetAddress endpoint) {
		assert endpoint != null;
		lock.readLock().lock();
		try {
			return leavingEndpoints.contains(endpoint);
		} finally {
			lock.readLock().unlock();
		}
	}

	public boolean isMoving(InetAddress endpoint) {
		assert endpoint != null;
		lock.readLock().lock();
		try {
			for (Pair<Token, InetAddress> pair : movingEndpoints) {
				if (pair.right.equals(endpoint))
					return true;

			}
			return false;
		} finally {
			lock.readLock().unlock();
		}
	}

	private final AtomicReference<TokenMetadata> cachedTokenMap = new AtomicReference<>();

	public TokenMetadata cloneOnlyTokenMap() {
		lock.readLock().lock();
		try {
			return new TokenMetadata(SortedBiMultiValMap.create(tokenToEndpointMap, null, TokenMetadata.inetaddressCmp), HashBiMap.create(endpointToHostIdMap), new TokenMetadata.Topology(topology), partitioner);
		} finally {
			lock.readLock().unlock();
		}
	}

	public TokenMetadata cachedOnlyTokenMap() {
		TokenMetadata tm = cachedTokenMap.get();
		if (tm != null)
			return tm;

		synchronized(this) {
			if ((tm = cachedTokenMap.get()) != null)
				return tm;

			tm = cloneOnlyTokenMap();
			cachedTokenMap.set(tm);
			return tm;
		}
	}

	public TokenMetadata cloneAfterAllLeft() {
		lock.readLock().lock();
		try {
			return TokenMetadata.removeEndpoints(cloneOnlyTokenMap(), leavingEndpoints);
		} finally {
			lock.readLock().unlock();
		}
	}

	private static TokenMetadata removeEndpoints(TokenMetadata allLeftMetadata, Set<InetAddress> leavingEndpoints) {
		for (InetAddress endpoint : leavingEndpoints)
			allLeftMetadata.removeEndpoint(endpoint);

		return allLeftMetadata;
	}

	public TokenMetadata cloneAfterAllSettled() {
		lock.readLock().lock();
		try {
			TokenMetadata metadata = cloneOnlyTokenMap();
			for (InetAddress endpoint : leavingEndpoints)
				metadata.removeEndpoint(endpoint);

			for (Pair<Token, InetAddress> pair : movingEndpoints)
				metadata.updateNormalToken(pair.left, pair.right);

			return metadata;
		} finally {
			lock.readLock().unlock();
		}
	}

	public InetAddress getEndpoint(Token token) {
		lock.readLock().lock();
		try {
			return tokenToEndpointMap.get(token);
		} finally {
			lock.readLock().unlock();
		}
	}

	public Collection<Range<Token>> getPrimaryRangesFor(Collection<Token> tokens) {
		Collection<Range<Token>> ranges = new ArrayList<>(tokens.size());
		for (Token right : tokens)
			ranges.add(new Range<>(getPredecessor(right), right));

		return ranges;
	}

	@Deprecated
	public Range<Token> getPrimaryRangeFor(Token right) {
		return getPrimaryRangesFor(Arrays.asList(right)).iterator().next();
	}

	public ArrayList<Token> sortedTokens() {
		return sortedTokens;
	}

	public Multimap<Range<Token>, InetAddress> getPendingRangesMM(String keyspaceName) {
		Multimap<Range<Token>, InetAddress> map = HashMultimap.create();
		PendingRangeMaps pendingRangeMaps = this.pendingRanges.get(keyspaceName);
		if (pendingRangeMaps != null) {
			for (Map.Entry<Range<Token>, List<InetAddress>> entry : pendingRangeMaps) {
				Range<Token> range = entry.getKey();
				for (InetAddress address : entry.getValue()) {
					map.put(range, address);
				}
			}
		}
		return map;
	}

	public PendingRangeMaps getPendingRanges(String keyspaceName) {
		return this.pendingRanges.get(keyspaceName);
	}

	public List<Range<Token>> getPendingRanges(String keyspaceName, InetAddress endpoint) {
		List<Range<Token>> ranges = new ArrayList<>();
		for (Map.Entry<Range<Token>, InetAddress> entry : getPendingRangesMM(keyspaceName).entries()) {
			if (entry.getValue().equals(endpoint)) {
				ranges.add(entry.getKey());
			}
		}
		return ranges;
	}

	public void calculatePendingRanges(AbstractReplicationStrategy strategy, String keyspaceName) {
		synchronized(pendingRanges) {
			if (((bootstrapTokens.isEmpty()) && (leavingEndpoints.isEmpty())) && (movingEndpoints.isEmpty())) {
				if (TokenMetadata.logger.isTraceEnabled())
					TokenMetadata.logger.trace("No bootstrapping, leaving or moving nodes -> empty pending ranges for {}", keyspaceName);

				pendingRanges.put(keyspaceName, new PendingRangeMaps());
			}else {
				if (TokenMetadata.logger.isDebugEnabled())
					TokenMetadata.logger.debug("Starting pending range calculation for {}", keyspaceName);

				long startedAt = System.currentTimeMillis();
				BiMultiValMap<Token, InetAddress> bootstrapTokens = new BiMultiValMap<>();
				Set<InetAddress> leavingEndpoints = new HashSet<>();
				Set<Pair<Token, InetAddress>> movingEndpoints = new HashSet<>();
				TokenMetadata metadata;
				lock.readLock().lock();
				try {
					bootstrapTokens.putAll(this.bootstrapTokens);
					leavingEndpoints.addAll(this.leavingEndpoints);
					movingEndpoints.addAll(this.movingEndpoints);
					metadata = this.cloneOnlyTokenMap();
				} finally {
					lock.readLock().unlock();
				}
				pendingRanges.put(keyspaceName, TokenMetadata.calculatePendingRanges(strategy, metadata, bootstrapTokens, leavingEndpoints, movingEndpoints));
				long took = (System.currentTimeMillis()) - startedAt;
				if (TokenMetadata.logger.isDebugEnabled())
					TokenMetadata.logger.debug("Pending range calculation for {} completed (took: {}ms)", keyspaceName, took);

				if (TokenMetadata.logger.isTraceEnabled())
					TokenMetadata.logger.trace("Calculated pending ranges for {}:\n{}", keyspaceName, (pendingRanges.isEmpty() ? "<empty>" : printPendingRanges()));

			}
		}
	}

	private static PendingRangeMaps calculatePendingRanges(AbstractReplicationStrategy strategy, TokenMetadata metadata, BiMultiValMap<Token, InetAddress> bootstrapTokens, Set<InetAddress> leavingEndpoints, Set<Pair<Token, InetAddress>> movingEndpoints) {
		PendingRangeMaps newPendingRanges = new PendingRangeMaps();
		TokenMetadata allLeftMetadata = TokenMetadata.removeEndpoints(metadata.cloneOnlyTokenMap(), leavingEndpoints);
		Set<Range<Token>> affectedRanges = new HashSet<Range<Token>>();
		for (InetAddress endpoint : leavingEndpoints) {
		}
		for (Range<Token> range : affectedRanges) {
		}
		Multimap<InetAddress, Token> bootstrapAddresses = bootstrapTokens.inverse();
		for (InetAddress endpoint : bootstrapAddresses.keySet()) {
			Collection<Token> tokens = bootstrapAddresses.get(endpoint);
			allLeftMetadata.updateNormalTokens(tokens, endpoint);
			allLeftMetadata.removeEndpoint(endpoint);
		}
		for (Pair<Token, InetAddress> moving : movingEndpoints) {
			Set<Range<Token>> moveAffectedRanges = new HashSet<>();
			InetAddress endpoint = moving.right;
			allLeftMetadata.updateNormalToken(moving.left, endpoint);
			for (Range<Token> range : moveAffectedRanges) {
			}
			allLeftMetadata.removeEndpoint(endpoint);
		}
		return newPendingRanges;
	}

	public Token getPredecessor(Token token) {
		List<Token> tokens = sortedTokens();
		int index = Collections.binarySearch(tokens, token);
		assert index >= 0 : (token + " not found in ") + (StringUtils.join(tokenToEndpointMap.keySet(), ", "));
		return index == 0 ? tokens.get(((tokens.size()) - 1)) : tokens.get((index - 1));
	}

	public Token getSuccessor(Token token) {
		List<Token> tokens = sortedTokens();
		int index = Collections.binarySearch(tokens, token);
		assert index >= 0 : (token + " not found in ") + (StringUtils.join(tokenToEndpointMap.keySet(), ", "));
		return index == ((tokens.size()) - 1) ? tokens.get(0) : tokens.get((index + 1));
	}

	public BiMultiValMap<Token, InetAddress> getBootstrapTokens() {
		lock.readLock().lock();
		try {
			return new BiMultiValMap<Token, InetAddress>(bootstrapTokens);
		} finally {
			lock.readLock().unlock();
		}
	}

	public Set<InetAddress> getAllEndpoints() {
		lock.readLock().lock();
		try {
			return ImmutableSet.copyOf(endpointToHostIdMap.keySet());
		} finally {
			lock.readLock().unlock();
		}
	}

	public Set<InetAddress> getLeavingEndpoints() {
		lock.readLock().lock();
		try {
			return ImmutableSet.copyOf(leavingEndpoints);
		} finally {
			lock.readLock().unlock();
		}
	}

	public Set<Pair<Token, InetAddress>> getMovingEndpoints() {
		lock.readLock().lock();
		try {
			return ImmutableSet.copyOf(movingEndpoints);
		} finally {
			lock.readLock().unlock();
		}
	}

	public static int firstTokenIndex(final ArrayList<Token> ring, Token start, boolean insertMin) {
		assert (ring.size()) > 0;
		int i = Collections.binarySearch(ring, start);
		if (i < 0) {
			i = (i + 1) * (-1);
			if (i >= (ring.size()))
				i = (insertMin) ? -1 : 0;

		}
		return i;
	}

	public static Token firstToken(final ArrayList<Token> ring, Token start) {
		return ring.get(TokenMetadata.firstTokenIndex(ring, start, false));
	}

	public static Iterator<Token> ringIterator(final ArrayList<Token> ring, Token start, boolean includeMin) {
		if (ring.isEmpty())
			return includeMin ? Iterators.singletonIterator(start.getPartitioner().getMinimumToken()) : Collections.emptyIterator();

		final boolean insertMin = includeMin && (!(ring.get(0).isMinimum()));
		final int startIndex = TokenMetadata.firstTokenIndex(ring, start, insertMin);
		return new AbstractIterator<Token>() {
			int j = startIndex;

			protected Token computeNext() {
				if ((j) < (-1))
					return endOfData();

				try {
					if ((j) == (-1))
						return start.getPartitioner().getMinimumToken();

					return ring.get(j);
				} finally {
					(j)++;
					if ((j) == (ring.size()))
						j = (insertMin) ? -1 : 0;

					if ((j) == startIndex)
						j = -2;

				}
			}
		};
	}

	public void clearUnsafe() {
		lock.writeLock().lock();
		try {
			tokenToEndpointMap.clear();
			endpointToHostIdMap.clear();
			bootstrapTokens.clear();
			leavingEndpoints.clear();
			pendingRanges.clear();
			movingEndpoints.clear();
			sortedTokens.clear();
			topology.clear();
			invalidateCachedRings();
		} finally {
			lock.writeLock().unlock();
		}
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		lock.readLock().lock();
		try {
			Multimap<InetAddress, Token> endpointToTokenMap = tokenToEndpointMap.inverse();
			Set<InetAddress> eps = endpointToTokenMap.keySet();
			if (!(eps.isEmpty())) {
				sb.append("Normal Tokens:");
				sb.append(System.getProperty("line.separator"));
				for (InetAddress ep : eps) {
					sb.append(ep);
					sb.append(':');
					sb.append(endpointToTokenMap.get(ep));
					sb.append(System.getProperty("line.separator"));
				}
			}
			if (!(bootstrapTokens.isEmpty())) {
				sb.append("Bootstrapping Tokens:");
				sb.append(System.getProperty("line.separator"));
				for (Map.Entry<Token, InetAddress> entry : bootstrapTokens.entrySet()) {
					sb.append(entry.getValue()).append(':').append(entry.getKey());
					sb.append(System.getProperty("line.separator"));
				}
			}
			if (!(leavingEndpoints.isEmpty())) {
				sb.append("Leaving Endpoints:");
				sb.append(System.getProperty("line.separator"));
				for (InetAddress ep : leavingEndpoints) {
					sb.append(ep);
					sb.append(System.getProperty("line.separator"));
				}
			}
			if (!(pendingRanges.isEmpty())) {
				sb.append("Pending Ranges:");
				sb.append(System.getProperty("line.separator"));
				sb.append(printPendingRanges());
			}
		} finally {
			lock.readLock().unlock();
		}
		return sb.toString();
	}

	private String printPendingRanges() {
		StringBuilder sb = new StringBuilder();
		for (PendingRangeMaps pendingRangeMaps : pendingRanges.values()) {
			sb.append(pendingRangeMaps.printPendingRanges());
		}
		return sb.toString();
	}

	public Collection<InetAddress> pendingEndpointsFor(Token token, String keyspaceName) {
		PendingRangeMaps pendingRangeMaps = this.pendingRanges.get(keyspaceName);
		if (pendingRangeMaps == null)
			return Collections.emptyList();

		return pendingRangeMaps.pendingEndpointsFor(token);
	}

	public Collection<InetAddress> getWriteEndpoints(Token token, String keyspaceName, Collection<InetAddress> naturalEndpoints) {
		return ImmutableList.copyOf(Iterables.concat(naturalEndpoints, pendingEndpointsFor(token, keyspaceName)));
	}

	public Multimap<InetAddress, Token> getEndpointToTokenMapForReading() {
		lock.readLock().lock();
		try {
			Multimap<InetAddress, Token> cloned = HashMultimap.create();
			for (Map.Entry<Token, InetAddress> entry : tokenToEndpointMap.entrySet())
				cloned.put(entry.getValue(), entry.getKey());

			return cloned;
		} finally {
			lock.readLock().unlock();
		}
	}

	public Map<Token, InetAddress> getNormalAndBootstrappingTokenToEndpointMap() {
		lock.readLock().lock();
		try {
			Map<Token, InetAddress> map = new HashMap<>(((tokenToEndpointMap.size()) + (bootstrapTokens.size())));
			map.putAll(tokenToEndpointMap);
			map.putAll(bootstrapTokens);
			return map;
		} finally {
			lock.readLock().unlock();
		}
	}

	public TokenMetadata.Topology getTopology() {
		return topology;
	}

	public long getRingVersion() {
		return ringVersion;
	}

	public void invalidateCachedRings() {
		(ringVersion)++;
		cachedTokenMap.set(null);
	}

	public DecoratedKey decorateKey(ByteBuffer key) {
		return partitioner.decorateKey(key);
	}

	public static class Topology {
		private final Multimap<String, InetAddress> dcEndpoints;

		private final Map<String, Multimap<String, InetAddress>> dcRacks;

		private final Map<InetAddress, Pair<String, String>> currentLocations;

		Topology() {
			dcEndpoints = HashMultimap.create();
			dcRacks = new HashMap<>();
			currentLocations = new HashMap<>();
		}

		void clear() {
			dcEndpoints.clear();
			dcRacks.clear();
			currentLocations.clear();
		}

		Topology(TokenMetadata.Topology other) {
			dcEndpoints = HashMultimap.create(other.dcEndpoints);
			dcRacks = new HashMap<>();
			for (String dc : other.dcRacks.keySet())
				dcRacks.put(dc, HashMultimap.create(other.dcRacks.get(dc)));

			currentLocations = new HashMap<>(other.currentLocations);
		}

		void addEndpoint(InetAddress ep) {
			IEndpointSnitch snitch = DatabaseDescriptor.getEndpointSnitch();
			String dc = snitch.getDatacenter(ep);
			String rack = snitch.getRack(ep);
			Pair<String, String> current = currentLocations.get(ep);
			if (current != null) {
				if ((current.left.equals(dc)) && (current.right.equals(rack)))
					return;

				doRemoveEndpoint(ep, current);
			}
			doAddEndpoint(ep, dc, rack);
		}

		private void doAddEndpoint(InetAddress ep, String dc, String rack) {
			dcEndpoints.put(dc, ep);
			if (!(dcRacks.containsKey(dc)))
				dcRacks.put(dc, HashMultimap.<String, InetAddress>create());

			dcRacks.get(dc).put(rack, ep);
			currentLocations.put(ep, Pair.create(dc, rack));
		}

		void removeEndpoint(InetAddress ep) {
			if (!(currentLocations.containsKey(ep)))
				return;

			doRemoveEndpoint(ep, currentLocations.remove(ep));
		}

		private void doRemoveEndpoint(InetAddress ep, Pair<String, String> current) {
			dcRacks.get(current.left).remove(current.right, ep);
			dcEndpoints.remove(current.left, ep);
		}

		void updateEndpoint(InetAddress ep) {
			IEndpointSnitch snitch = DatabaseDescriptor.getEndpointSnitch();
			if ((snitch == null) || (!(currentLocations.containsKey(ep))))
				return;

			updateEndpoint(ep, snitch);
		}

		void updateEndpoints() {
			IEndpointSnitch snitch = DatabaseDescriptor.getEndpointSnitch();
			if (snitch == null)
				return;

			for (InetAddress ep : currentLocations.keySet())
				updateEndpoint(ep, snitch);

		}

		private void updateEndpoint(InetAddress ep, IEndpointSnitch snitch) {
			Pair<String, String> current = currentLocations.get(ep);
			String dc = snitch.getDatacenter(ep);
			String rack = snitch.getRack(ep);
			if ((dc.equals(current.left)) && (rack.equals(current.right)))
				return;

			doRemoveEndpoint(ep, current);
			doAddEndpoint(ep, dc, rack);
		}

		public Multimap<String, InetAddress> getDatacenterEndpoints() {
			return dcEndpoints;
		}

		public Map<String, Multimap<String, InetAddress>> getDatacenterRacks() {
			return dcRacks;
		}

		public Pair<String, String> getLocation(InetAddress addr) {
			return currentLocations.get(addr);
		}
	}
}

