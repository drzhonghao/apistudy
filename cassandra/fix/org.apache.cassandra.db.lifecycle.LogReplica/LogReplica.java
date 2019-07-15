

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.apache.cassandra.io.util.FileUtils;
import org.apache.cassandra.utils.NativeLibrary;


final class LogReplica implements AutoCloseable {
	private final File file;

	private int directoryDescriptor;

	private final Map<String, String> errors = new HashMap<>();

	static LogReplica create(File directory, String fileName) {
		return new LogReplica(new File(fileName), NativeLibrary.tryOpenDirectory(directory.getPath()));
	}

	static LogReplica open(File file) {
		return new LogReplica(file, NativeLibrary.tryOpenDirectory(file.getParentFile().getPath()));
	}

	LogReplica(File file, int directoryDescriptor) {
		this.file = file;
		this.directoryDescriptor = directoryDescriptor;
	}

	File file() {
		return file;
	}

	List<String> readLines() {
		return FileUtils.readLines(file);
	}

	String getFileName() {
		return file.getName();
	}

	String getDirectory() {
		return file.getParent();
	}

	void syncDirectory() {
		if ((directoryDescriptor) >= 0)
			NativeLibrary.trySync(directoryDescriptor);

	}

	void delete() {
		syncDirectory();
	}

	boolean exists() {
		return file.exists();
	}

	public void close() {
		if ((directoryDescriptor) >= 0) {
			NativeLibrary.tryCloseFD(directoryDescriptor);
			directoryDescriptor = -1;
		}
	}

	@Override
	public String toString() {
		return String.format("[%s] ", file);
	}

	void setError(String line, String error) {
		errors.put(line, error);
	}

	void printContentsWithAnyErrors(StringBuilder str) {
		str.append(file.getPath());
		str.append(System.lineSeparator());
		FileUtils.readLines(file).forEach(( line) -> printLineWithAnyError(str, line));
	}

	private void printLineWithAnyError(StringBuilder str, String line) {
		str.append('\t');
		str.append(line);
		str.append(System.lineSeparator());
		String error = errors.get(line);
		if (error != null) {
			str.append("\t\t***");
			str.append(error);
			str.append(System.lineSeparator());
		}
	}
}

