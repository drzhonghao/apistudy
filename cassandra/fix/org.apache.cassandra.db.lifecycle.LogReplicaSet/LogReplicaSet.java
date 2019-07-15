

import java.io.File;
import java.util.List;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class LogReplicaSet implements AutoCloseable {
	private static final Logger logger = LoggerFactory.getLogger(LogReplicaSet.class);

	void addReplicas(List<File> replicas) {
		replicas.forEach(this::addReplica);
	}

	void addReplica(File file) {
		File directory = file.getParentFile();
		if (LogReplicaSet.logger.isTraceEnabled())
			LogReplicaSet.logger.trace("Added log file replica {} ", file);

	}

	Throwable syncDirectory(Throwable accumulate) {
		return null;
	}

	Throwable delete(Throwable accumulate) {
		return null;
	}

	private static boolean isPrefixMatch(String first, String second) {
		return (first.length()) >= (second.length()) ? first.startsWith(second) : second.startsWith(first);
	}

	void printContentsWithAnyErrors(StringBuilder str) {
	}

	boolean exists() {
		return false;
	}

	public void close() {
	}

	@Override
	public String toString() {
		return null;
	}

	String getDirectories() {
		return null;
	}

	@com.google.common.annotations.VisibleForTesting
	List<File> getFiles() {
		return null;
	}

	@com.google.common.annotations.VisibleForTesting
	List<String> getFilePaths() {
		return null;
	}
}

