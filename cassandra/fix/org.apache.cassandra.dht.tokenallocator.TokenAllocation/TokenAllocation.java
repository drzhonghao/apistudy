

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import org.apache.cassandra.dht.IPartitioner;
import org.apache.cassandra.dht.Token;
import org.apache.cassandra.dht.tokenallocator.TokenAllocator;
import org.apache.cassandra.exceptions.ConfigurationException;
import org.apache.cassandra.locator.AbstractReplicationStrategy;
import org.apache.cassandra.locator.IEndpointSnitch;
import org.apache.cassandra.locator.NetworkTopologyStrategy;
import org.apache.cassandra.locator.SimpleStrategy;
import org.apache.cassandra.locator.TokenMetadata;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TokenAllocation {
	private static final Logger logger = LoggerFactory.getLogger(TokenAllocation.class);

	public static Collection<Token> allocateTokens(final TokenMetadata tokenMetadata, final AbstractReplicationStrategy rs, final InetAddress endpoint, int numTokens) {
		TokenMetadata tokenMetadataCopy = tokenMetadata.cloneOnlyTokenMap();
		TokenAllocation.StrategyAdapter strategy = TokenAllocation.getStrategy(tokenMetadataCopy, rs, endpoint);
		Collection<Token> tokens = TokenAllocation.create(tokenMetadata, strategy).addUnit(endpoint, numTokens);
		tokens = TokenAllocation.adjustForCrossDatacenterClashes(tokenMetadata, strategy, tokens);
		if (TokenAllocation.logger.isWarnEnabled()) {
			TokenAllocation.logger.warn("Selected tokens {}", tokens);
			SummaryStatistics os = TokenAllocation.replicatedOwnershipStats(tokenMetadataCopy, rs, endpoint);
			tokenMetadataCopy.updateNormalTokens(tokens, endpoint);
			SummaryStatistics ns = TokenAllocation.replicatedOwnershipStats(tokenMetadataCopy, rs, endpoint);
			TokenAllocation.logger.warn("Replicated node load in datacentre before allocation {}", TokenAllocation.statToString(os));
			TokenAllocation.logger.warn("Replicated node load in datacentre after allocation {}", TokenAllocation.statToString(ns));
			if ((ns.getStandardDeviation()) > (os.getStandardDeviation()))
				TokenAllocation.logger.warn("Unexpected growth in standard deviation after allocation.");

		}
		return tokens;
	}

	private static Collection<Token> adjustForCrossDatacenterClashes(final TokenMetadata tokenMetadata, TokenAllocation.StrategyAdapter strategy, Collection<Token> tokens) {
		List<Token> filtered = Lists.newArrayListWithCapacity(tokens.size());
		for (Token t : tokens) {
			while ((tokenMetadata.getEndpoint(t)) != null) {
				InetAddress other = tokenMetadata.getEndpoint(t);
				if (strategy.inAllocationRing(other))
					throw new ConfigurationException(String.format("Allocated token %s already assigned to node %s. Is another node also allocating tokens?", t, other));

				t = t.increaseSlightly();
			} 
			filtered.add(t);
		}
		return filtered;
	}

	public static Map<InetAddress, Double> evaluateReplicatedOwnership(TokenMetadata tokenMetadata, AbstractReplicationStrategy rs) {
		Map<InetAddress, Double> ownership = Maps.newHashMap();
		List<Token> sortedTokens = tokenMetadata.sortedTokens();
		Iterator<Token> it = sortedTokens.iterator();
		Token current = it.next();
		while (it.hasNext()) {
			Token next = it.next();
			TokenAllocation.addOwnership(tokenMetadata, rs, current, next, ownership);
			current = next;
		} 
		TokenAllocation.addOwnership(tokenMetadata, rs, current, sortedTokens.get(0), ownership);
		return ownership;
	}

	static void addOwnership(final TokenMetadata tokenMetadata, final AbstractReplicationStrategy rs, Token current, Token next, Map<InetAddress, Double> ownership) {
		double size = current.size(next);
		Token representative = current.getPartitioner().midpoint(current, next);
		for (InetAddress n : rs.calculateNaturalEndpoints(representative, tokenMetadata)) {
			Double v = ownership.get(n);
			ownership.put(n, (v != null ? v + size : size));
		}
	}

	public static String statToString(SummaryStatistics stat) {
		return String.format("max %.2f min %.2f stddev %.4f", ((stat.getMax()) / (stat.getMean())), ((stat.getMin()) / (stat.getMean())), stat.getStandardDeviation());
	}

	public static SummaryStatistics replicatedOwnershipStats(TokenMetadata tokenMetadata, AbstractReplicationStrategy rs, InetAddress endpoint) {
		SummaryStatistics stat = new SummaryStatistics();
		TokenAllocation.StrategyAdapter strategy = TokenAllocation.getStrategy(tokenMetadata, rs, endpoint);
		for (Map.Entry<InetAddress, Double> en : TokenAllocation.evaluateReplicatedOwnership(tokenMetadata, rs).entrySet()) {
			if (strategy.inAllocationRing(en.getKey()))
				stat.addValue(((en.getValue()) / (tokenMetadata.getTokens(en.getKey()).size())));

		}
		return stat;
	}

	static TokenAllocator<InetAddress> create(TokenMetadata tokenMetadata, TokenAllocation.StrategyAdapter strategy) {
		NavigableMap<Token, InetAddress> sortedTokens = new TreeMap<>();
		for (Map.Entry<Token, InetAddress> en : tokenMetadata.getNormalAndBootstrappingTokenToEndpointMap().entrySet()) {
			if (strategy.inAllocationRing(en.getValue()))
				sortedTokens.put(en.getKey(), en.getValue());

		}
		return null;
	}

	interface StrategyAdapter {
		public abstract boolean inAllocationRing(InetAddress other);
	}

	static TokenAllocation.StrategyAdapter getStrategy(final TokenMetadata tokenMetadata, final AbstractReplicationStrategy rs, final InetAddress endpoint) {
		if (rs instanceof NetworkTopologyStrategy)
			return TokenAllocation.getStrategy(tokenMetadata, ((NetworkTopologyStrategy) (rs)), rs.snitch, endpoint);

		if (rs instanceof SimpleStrategy)
			return TokenAllocation.getStrategy(tokenMetadata, ((SimpleStrategy) (rs)), endpoint);

		throw new ConfigurationException(("Token allocation does not support replication strategy " + (rs.getClass().getSimpleName())));
	}

	static TokenAllocation.StrategyAdapter getStrategy(final TokenMetadata tokenMetadata, final SimpleStrategy rs, final InetAddress endpoint) {
		final int replicas = rs.getReplicationFactor();
		return new TokenAllocation.StrategyAdapter() {
			public int replicas() {
				return replicas;
			}

			public Object getGroup(InetAddress unit) {
				return unit;
			}

			@Override
			public boolean inAllocationRing(InetAddress other) {
				return true;
			}
		};
	}

	static TokenAllocation.StrategyAdapter getStrategy(final TokenMetadata tokenMetadata, final NetworkTopologyStrategy rs, final IEndpointSnitch snitch, final InetAddress endpoint) {
		final String dc = snitch.getDatacenter(endpoint);
		final int replicas = rs.getReplicationFactor(dc);
		if ((replicas == 0) || (replicas == 1)) {
			return new TokenAllocation.StrategyAdapter() {
				public int replicas() {
					return 1;
				}

				public Object getGroup(InetAddress unit) {
					return unit;
				}

				@Override
				public boolean inAllocationRing(InetAddress other) {
					return dc.equals(snitch.getDatacenter(other));
				}
			};
		}
		TokenMetadata.Topology topology = tokenMetadata.getTopology();
		int racks = topology.getDatacenterRacks().get(dc).asMap().size();
		if (racks >= replicas) {
			return new TokenAllocation.StrategyAdapter() {
				public int replicas() {
					return replicas;
				}

				public Object getGroup(InetAddress unit) {
					return snitch.getRack(unit);
				}

				@Override
				public boolean inAllocationRing(InetAddress other) {
					return dc.equals(snitch.getDatacenter(other));
				}
			};
		}else
			if (racks == 1) {
				return new TokenAllocation.StrategyAdapter() {
					public int replicas() {
						return replicas;
					}

					public Object getGroup(InetAddress unit) {
						return unit;
					}

					@Override
					public boolean inAllocationRing(InetAddress other) {
						return dc.equals(snitch.getDatacenter(other));
					}
				};
			}else
				throw new ConfigurationException(String.format("Token allocation failed: the number of racks %d in datacenter %s is lower than its replication factor %d.", racks, dc, replicas));


	}
}

