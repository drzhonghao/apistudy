

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.core.zookeeper.ZooUtil;
import org.apache.accumulo.fate.zookeeper.ZooCache;
import org.apache.accumulo.fate.zookeeper.ZooCacheFactory;
import org.apache.accumulo.server.Accumulo;
import org.apache.accumulo.server.conf.ZooConfiguration;
import org.apache.accumulo.server.fs.VolumeManager;
import org.apache.accumulo.server.fs.VolumeManagerImpl;
import org.apache.hadoop.fs.Path;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;


class ZooConfigurationFactory {
	private static final Map<String, ZooConfiguration> instances = new HashMap<>();

	ZooConfiguration getInstance(Instance inst, ZooCacheFactory zcf, AccumuloConfiguration parent) {
		String instanceId;
		if (inst == null) {
			VolumeManager fs;
			try {
				fs = VolumeManagerImpl.get();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			Path instanceIdPath = Accumulo.getAccumuloInstanceIdPath(fs);
			instanceId = ZooUtil.getInstanceIDFromHdfs(instanceIdPath, parent);
		}else {
			instanceId = inst.getInstanceID();
		}
		ZooConfiguration config;
		synchronized(ZooConfigurationFactory.instances) {
			config = ZooConfigurationFactory.instances.get(instanceId);
			if (config == null) {
				ZooCache propCache;
				Watcher watcher = new Watcher() {
					@Override
					public void process(WatchedEvent arg0) {
					}
				};
				if (inst == null) {
					propCache = zcf.getZooCache(parent.get(Property.INSTANCE_ZK_HOST), ((int) (parent.getTimeInMillis(Property.INSTANCE_ZK_TIMEOUT))), watcher);
				}else {
					propCache = zcf.getZooCache(inst.getZooKeepers(), inst.getZooKeepersSessionTimeOut(), watcher);
				}
				ZooConfigurationFactory.instances.put(instanceId, config);
			}
		}
		return config;
	}

	public ZooConfiguration getInstance(Instance inst, AccumuloConfiguration parent) {
		return getInstance(inst, new ZooCacheFactory(), parent);
	}
}

