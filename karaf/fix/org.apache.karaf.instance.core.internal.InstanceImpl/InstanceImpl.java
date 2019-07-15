

import org.apache.karaf.instance.core.Instance;
import org.apache.karaf.instance.core.internal.InstanceServiceImpl;


public class InstanceImpl implements Instance {
	private final InstanceServiceImpl service;

	private String name;

	public InstanceImpl(InstanceServiceImpl service, String name) {
		this.service = service;
		this.name = name;
	}

	public String getName() {
		return name;
	}

	void doSetName(String name) {
		this.name = name;
	}

	public boolean isRoot() {
		return false;
	}

	public String getLocation() {
		return null;
	}

	public int getPid() {
		return 0;
	}

	public int getSshPort() {
		return 0;
	}

	public String getSshHost() {
		return null;
	}

	public void changeSshPort(int port) throws Exception {
	}

	public int getRmiRegistryPort() {
		return 0;
	}

	public void changeRmiRegistryPort(int port) throws Exception {
	}

	public String getRmiRegistryHost() {
		return null;
	}

	public int getRmiServerPort() {
		return 0;
	}

	public void changeRmiServerPort(int port) throws Exception {
	}

	public String getRmiServerHost() {
		return null;
	}

	public String getJavaOpts() {
		return null;
	}

	public void changeJavaOpts(String javaOpts) throws Exception {
	}

	public void restart(String javaOpts) throws Exception {
		service.restartInstance(name, javaOpts);
	}

	public void start(String javaOpts) throws Exception {
		service.startInstance(name, javaOpts);
	}

	public void stop() throws Exception {
		service.stopInstance(name);
	}

	public void destroy() throws Exception {
		service.destroyInstance(name);
	}

	public String getState() throws Exception {
		return null;
	}

	public void changeSshHost(String host) throws Exception {
		service.changeInstanceSshHost(name, host);
	}
}

