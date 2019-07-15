

import com.google.common.base.Strings;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.cassandra.concurrent.JMXEnabledThreadPoolExecutor;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.config.ParameterizedClass;
import org.apache.cassandra.db.commitlog.CommitLogDescriptor;
import org.apache.cassandra.db.commitlog.CommitLogSegment;
import org.apache.cassandra.exceptions.ConfigurationException;
import org.apache.cassandra.io.compress.ICompressor;
import org.apache.cassandra.schema.CompressionParams;
import org.apache.cassandra.security.EncryptionContext;
import org.apache.cassandra.utils.FBUtilities;
import org.apache.cassandra.utils.WrappedRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CommitLogArchiver {
	private static final Logger logger = LoggerFactory.getLogger(CommitLogArchiver.class);

	public static final SimpleDateFormat format = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");

	private static final String DELIMITER = ",";

	private static final Pattern NAME = Pattern.compile("%name");

	private static final Pattern PATH = Pattern.compile("%path");

	private static final Pattern FROM = Pattern.compile("%from");

	private static final Pattern TO = Pattern.compile("%to");

	static {
		CommitLogArchiver.format.setTimeZone(TimeZone.getTimeZone("GMT"));
	}

	public final Map<String, Future<?>> archivePending = new ConcurrentHashMap<String, Future<?>>();

	private final ExecutorService executor;

	final String archiveCommand;

	final String restoreCommand;

	final String restoreDirectories;

	public long restorePointInTime;

	public final TimeUnit precision;

	public CommitLogArchiver(String archiveCommand, String restoreCommand, String restoreDirectories, long restorePointInTime, TimeUnit precision) {
		this.archiveCommand = archiveCommand;
		this.restoreCommand = restoreCommand;
		this.restoreDirectories = restoreDirectories;
		this.restorePointInTime = restorePointInTime;
		this.precision = precision;
		executor = (!(Strings.isNullOrEmpty(archiveCommand))) ? new JMXEnabledThreadPoolExecutor("CommitLogArchiver") : null;
	}

	public static CommitLogArchiver disabled() {
		return new CommitLogArchiver(null, null, null, Long.MAX_VALUE, TimeUnit.MICROSECONDS);
	}

	public static CommitLogArchiver construct() {
		Properties commitlog_commands = new Properties();
		try (InputStream stream = CommitLogArchiver.class.getClassLoader().getResourceAsStream("commitlog_archiving.properties")) {
			if (stream == null) {
				CommitLogArchiver.logger.trace("No commitlog_archiving properties found; archive + pitr will be disabled");
				return CommitLogArchiver.disabled();
			}else {
				commitlog_commands.load(stream);
				String archiveCommand = commitlog_commands.getProperty("archive_command");
				String restoreCommand = commitlog_commands.getProperty("restore_command");
				String restoreDirectories = commitlog_commands.getProperty("restore_directories");
				if ((restoreDirectories != null) && (!(restoreDirectories.isEmpty()))) {
					for (String dir : restoreDirectories.split(CommitLogArchiver.DELIMITER)) {
						File directory = new File(dir);
						if (!(directory.exists())) {
							if (!(directory.mkdir())) {
								throw new RuntimeException(("Unable to create directory: " + dir));
							}
						}
					}
				}
				String targetTime = commitlog_commands.getProperty("restore_point_in_time");
				TimeUnit precision = TimeUnit.valueOf(commitlog_commands.getProperty("precision", "MICROSECONDS"));
				long restorePointInTime;
				try {
					restorePointInTime = (Strings.isNullOrEmpty(targetTime)) ? Long.MAX_VALUE : CommitLogArchiver.format.parse(targetTime).getTime();
				} catch (ParseException e) {
					throw new RuntimeException("Unable to parse restore target time", e);
				}
				return new CommitLogArchiver(archiveCommand, restoreCommand, restoreDirectories, restorePointInTime, precision);
			}
		} catch (IOException e) {
			throw new RuntimeException("Unable to load commitlog_archiving.properties", e);
		}
	}

	public void maybeArchive(final CommitLogSegment segment) {
		if (Strings.isNullOrEmpty(archiveCommand))
			return;

		archivePending.put(segment.getName(), executor.submit(new WrappedRunnable() {
			protected void runMayThrow() throws IOException {
				String command = CommitLogArchiver.NAME.matcher(archiveCommand).replaceAll(Matcher.quoteReplacement(segment.getName()));
				command = CommitLogArchiver.PATH.matcher(command).replaceAll(Matcher.quoteReplacement(segment.getPath()));
				exec(command);
			}
		}));
	}

	public void maybeArchive(final String path, final String name) {
		if (Strings.isNullOrEmpty(archiveCommand))
			return;

		archivePending.put(name, executor.submit(new Runnable() {
			public void run() {
				try {
					String command = CommitLogArchiver.NAME.matcher(archiveCommand).replaceAll(Matcher.quoteReplacement(name));
					command = CommitLogArchiver.PATH.matcher(command).replaceAll(Matcher.quoteReplacement(path));
					exec(command);
				} catch (IOException e) {
					CommitLogArchiver.logger.warn("Archiving file {} failed, file may have already been archived.", name, e);
				}
			}
		}));
	}

	public boolean maybeWaitForArchiving(String name) {
		Future<?> f = archivePending.remove(name);
		if (f == null)
			return true;

		try {
			f.get();
		} catch (InterruptedException e) {
			throw new AssertionError(e);
		} catch (ExecutionException e) {
			if ((e.getCause()) instanceof RuntimeException) {
				if ((e.getCause().getCause()) instanceof IOException) {
					CommitLogArchiver.logger.error("Looks like the archiving of file {} failed earlier, cassandra is going to ignore this segment for now.", name, e.getCause().getCause());
					return false;
				}
			}
			throw new RuntimeException(e);
		}
		return true;
	}

	public void maybeRestoreArchive() {
		if (Strings.isNullOrEmpty(restoreDirectories))
			return;

		for (String dir : restoreDirectories.split(CommitLogArchiver.DELIMITER)) {
			File[] files = new File(dir).listFiles();
			if (files == null) {
				throw new RuntimeException(("Unable to list directory " + dir));
			}
			for (File fromFile : files) {
				CommitLogDescriptor fromHeader = CommitLogDescriptor.fromHeader(fromFile, DatabaseDescriptor.getEncryptionContext());
				CommitLogDescriptor fromName = (CommitLogDescriptor.isValid(fromFile.getName())) ? CommitLogDescriptor.fromFileName(fromFile.getName()) : null;
				CommitLogDescriptor descriptor;
				if ((fromHeader == null) && (fromName == null))
					throw new IllegalStateException(("Cannot safely construct descriptor for segment, either from its name or its header: " + (fromFile.getPath())));
				else
					if (((fromHeader != null) && (fromName != null)) && (!(fromHeader.equalsIgnoringCompression(fromName))))
						throw new IllegalStateException(String.format("Cannot safely construct descriptor for segment, as name and header descriptors do not match (%s vs %s): %s", fromHeader, fromName, fromFile.getPath()));
					else {
					}

				descriptor = null;
				if ((descriptor.compression) != null) {
					try {
						CompressionParams.createCompressor(descriptor.compression);
					} catch (ConfigurationException e) {
						throw new IllegalStateException("Unknown compression", e);
					}
				}
				File toFile = new File(DatabaseDescriptor.getCommitLogLocation(), descriptor.fileName());
				if (toFile.exists()) {
					CommitLogArchiver.logger.trace("Skipping restore of archive {} as the segment already exists in the restore location {}", fromFile.getPath(), toFile.getPath());
					continue;
				}
				String command = CommitLogArchiver.FROM.matcher(restoreCommand).replaceAll(Matcher.quoteReplacement(fromFile.getPath()));
				command = CommitLogArchiver.TO.matcher(command).replaceAll(Matcher.quoteReplacement(toFile.getPath()));
				try {
					exec(command);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	private void exec(String command) throws IOException {
		ProcessBuilder pb = new ProcessBuilder(command.split(" "));
		pb.redirectErrorStream(true);
		FBUtilities.exec(pb);
	}
}

