

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DockerClient {
	private static final Logger LOGGER = LoggerFactory.getLogger(DockerClient.class);

	public static final String DEFAULT_URL = "http://localhost:2375";

	private String url;

	private ObjectMapper mapper;

	public DockerClient(String url) {
		if (url == null) {
			this.url = DockerClient.DEFAULT_URL;
		}else {
			this.url = url;
		}
		mapper = new ObjectMapper();
	}

	public void rm(String id, boolean removeVolumes, boolean force) throws Exception {
		URL dockerUrl = new URL((((((((this.url) + "/containers/") + id) + "?v=") + removeVolumes) + "&force=") + force));
		HttpURLConnection connection = ((HttpURLConnection) (dockerUrl.openConnection()));
		connection.setRequestMethod("DELETE");
		if ((connection.getResponseCode()) != 204) {
			throw new IllegalStateException(((("Can't remove Docker container " + id) + ": ") + (connection.getResponseMessage())));
		}
	}

	public void start(String id) throws Exception {
		URL dockerUrl = new URL(((((this.url) + "/containers/") + id) + "/start"));
		HttpURLConnection connection = ((HttpURLConnection) (dockerUrl.openConnection()));
		connection.setRequestMethod("POST");
		if ((connection.getResponseCode()) != 204) {
			throw new IllegalStateException(((("Can't start Docker container " + id) + ": ") + (connection.getResponseMessage())));
		}
	}

	public void stop(String id, int timeToWait) throws Exception {
		URL dockerUrl = new URL((((((this.url) + "/containers/") + id) + "/stop?t=") + timeToWait));
		HttpURLConnection connection = ((HttpURLConnection) (dockerUrl.openConnection()));
		connection.setRequestMethod("POST");
		if ((connection.getResponseCode()) != 204) {
			throw new IllegalStateException(((("Can't stop Docker container " + id) + ": ") + (connection.getResponseMessage())));
		}
	}

	public void restart(String id, int timeToWait) throws Exception {
		URL dockerUrl = new URL((((((this.url) + "/containers/") + id) + "/restart?t=") + timeToWait));
		HttpURLConnection connection = ((HttpURLConnection) (dockerUrl.openConnection()));
		connection.setRequestMethod("POST");
		if ((connection.getResponseCode()) != 204) {
			throw new IllegalStateException(((("Can't restart Docker container " + id) + ": ") + (connection.getResponseMessage())));
		}
	}

	public void kill(String id, String signal) throws Exception {
		URL dockerUrl = new URL((((((this.url) + "/containers/") + id) + "/kill?signal=") + signal));
		HttpURLConnection connection = ((HttpURLConnection) (dockerUrl.openConnection()));
		connection.setRequestMethod("POST");
		if ((connection.getResponseCode()) != 204) {
			throw new IllegalStateException(((("Can't kill Docker container " + id) + ": ") + (connection.getResponseMessage())));
		}
	}

	public void rename(String id, String name) throws Exception {
		URL dockerUrl = new URL((((((this.url) + "/containers/") + id) + "/rename?name=") + name));
		HttpURLConnection connection = ((HttpURLConnection) (dockerUrl.openConnection()));
		connection.setRequestMethod("POST");
		if ((connection.getResponseCode()) != 204) {
			throw new IllegalStateException(((("Can't rename Docker container " + id) + ": ") + (connection.getResponseMessage())));
		}
	}

	public void pause(String id) throws Exception {
		URL dockerUrl = new URL(((((this.url) + "/containers/") + id) + "/pause"));
		HttpURLConnection connection = ((HttpURLConnection) (dockerUrl.openConnection()));
		connection.setRequestMethod("POST");
		if ((connection.getResponseCode()) != 204) {
			throw new IllegalStateException(((("Can't pause Docker container " + id) + ": ") + (connection.getResponseMessage())));
		}
	}

	public void unpause(String id) throws Exception {
		URL dockerUrl = new URL(((((this.url) + "/containers/") + id) + "/unpause"));
		HttpURLConnection connection = ((HttpURLConnection) (dockerUrl.openConnection()));
		connection.setRequestMethod("POST");
		if ((connection.getResponseCode()) != 204) {
			throw new IllegalStateException(((("Can't unpause Docker container " + id) + ": ") + (connection.getResponseMessage())));
		}
	}

	public String logs(String id, boolean stdout, boolean stderr, boolean timestamps, boolean details) throws Exception {
		URL dockerUrl = new URL((((((((((((this.url) + "/containers/") + id) + "/logs?stdout=") + stdout) + "&stderr=") + stderr) + "&timestamps=") + timestamps) + "&details=") + details));
		HttpURLConnection connection = ((HttpURLConnection) (dockerUrl.openConnection()));
		connection.setRequestMethod("GET");
		StringBuffer buffer = new StringBuffer();
		BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		String line;
		while ((line = reader.readLine()) != null) {
			buffer.append(line).append("\n");
		} 
		return buffer.toString();
	}

	public void pull(String name, String tag, boolean verbose) throws Exception {
		URL dockerUrl = new URL((((((this.url) + "/images/create?fromImage=") + name) + "&tag=") + tag));
		HttpURLConnection connection = ((HttpURLConnection) (dockerUrl.openConnection()));
		connection.setRequestMethod("POST");
		BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		String line;
		while ((line = reader.readLine()) != null) {
			DockerClient.LOGGER.debug(line);
			if (verbose) {
				System.out.println(line);
			}
		} 
		if ((connection.getResponseCode()) != 200) {
			throw new IllegalStateException(((("Can't pull image " + name) + ": ") + (connection.getResponseMessage())));
		}
	}

	public void push(String name, String tag, boolean verbose) throws Exception {
		URL dockerUrl = new URL((((((this.url) + "/images/") + name) + "/push?tag=") + tag));
		HttpURLConnection connection = ((HttpURLConnection) (dockerUrl.openConnection()));
		connection.setRequestMethod("POST");
		BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		String line;
		while ((line = reader.readLine()) != null) {
			DockerClient.LOGGER.debug(line);
			if (verbose) {
				System.out.println(line);
			}
		} 
		if ((connection.getResponseCode()) != 200) {
			throw new IllegalStateException(((("Can't push image " + name) + ": ") + (connection.getResponseMessage())));
		}
	}

	public void tag(String name, String repo, String tag) throws Exception {
		URL dockerUrl = new URL((((((((this.url) + "/images/") + name) + "/tag?repo=") + repo) + "&tag=") + tag));
		HttpURLConnection connection = ((HttpURLConnection) (dockerUrl.openConnection()));
		connection.setRequestMethod("POST");
		if ((connection.getResponseCode()) != 201) {
			throw new IllegalStateException(((("Can't tag image " + name) + ": ") + (connection.getResponseMessage())));
		}
	}

	public void rmi(String name, boolean force, boolean noprune) throws Exception {
		URL dockerUrl = new URL((((((((this.url) + "/images/") + name) + "?force=") + force) + "&noprune=") + noprune));
		HttpURLConnection connection = ((HttpURLConnection) (dockerUrl.openConnection()));
		connection.setRequestMethod("DELETE");
		if ((connection.getResponseCode()) != 200) {
			throw new IllegalStateException(((("Can't remove image " + name) + ": ") + (connection.getResponseMessage())));
		}
	}
}

