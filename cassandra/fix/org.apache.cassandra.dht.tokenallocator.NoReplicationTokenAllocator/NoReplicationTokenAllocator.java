

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.cassandra.dht.Token;
import org.apache.cassandra.dht.tokenallocator.TokenAllocatorBase;


public class NoReplicationTokenAllocator<Unit> extends TokenAllocatorBase<Unit> {
	private static final double MAX_TAKEOVER_RATIO = 0.9;

	private static final double MIN_TAKEOVER_RATIO = 1.0 - (NoReplicationTokenAllocator.MAX_TAKEOVER_RATIO);

	protected void createTokenInfos() {
	}

	public Collection<Token> addUnit(Unit newUnit, int numTokens) {
		double targetAverage = 0.0;
		double sum = 0.0;
		for (int i = 0; i < numTokens; i++) {
		}
		List<Token> newTokens = Lists.newArrayListWithCapacity(numTokens);
		int nr = 0;
		return newTokens;
	}

	void removeUnit(Unit n) {
	}

	public int getReplicas() {
		return 1;
	}
}

