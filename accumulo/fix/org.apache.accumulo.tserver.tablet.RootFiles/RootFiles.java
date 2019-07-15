

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import org.apache.accumulo.core.Constants;
import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.core.file.FileOperations;
import org.apache.accumulo.server.fs.FileRef;
import org.apache.accumulo.server.fs.VolumeManager;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class RootFiles {
	private static final Logger log = LoggerFactory.getLogger(RootFiles.class);

	public static void prepareReplacement(VolumeManager fs, Path location, Set<FileRef> oldDatafiles, String compactName) throws IOException {
		for (FileRef ref : oldDatafiles) {
			Path path = ref.path();
		}
	}

	public static void renameReplacement(VolumeManager fs, FileRef tmpDatafile, FileRef newDatafile) throws IOException {
		if (fs.exists(newDatafile.path())) {
			RootFiles.log.error(("Target map file already exist " + newDatafile), new Exception());
			throw new IllegalStateException(("Target map file already exist " + newDatafile));
		}
	}

	public static void finishReplacement(AccumuloConfiguration acuTableConf, VolumeManager fs, Path location, Set<FileRef> oldDatafiles, String compactName) throws IOException {
		for (FileRef ref : oldDatafiles) {
			Path path = ref.path();
			Path deleteFile = new Path(((((location + "/delete+") + compactName) + "+") + (path.getName())));
			if ((acuTableConf.getBoolean(Property.GC_TRASH_IGNORE)) || (!(fs.moveToTrash(deleteFile))))
				fs.deleteRecursively(deleteFile);

		}
	}

	public static void replaceFiles(AccumuloConfiguration acuTableConf, VolumeManager fs, Path location, Set<FileRef> oldDatafiles, FileRef tmpDatafile, FileRef newDatafile) throws IOException {
		String compactName = newDatafile.path().getName();
		RootFiles.prepareReplacement(fs, location, oldDatafiles, compactName);
		RootFiles.renameReplacement(fs, tmpDatafile, newDatafile);
		RootFiles.finishReplacement(acuTableConf, fs, location, oldDatafiles, compactName);
	}

	public static Collection<String> cleanupReplacement(VolumeManager fs, FileStatus[] files, boolean deleteTmp) throws IOException {
		Collection<String> goodFiles = new ArrayList<>(files.length);
		for (FileStatus file : files) {
			String path = file.getPath().toString();
			if ((file.getPath().toUri().getScheme()) == null) {
				throw new IllegalArgumentException(("Require fully qualified paths " + (file.getPath())));
			}
			String filename = file.getPath().getName();
			if (filename.startsWith("delete+")) {
				String expectedCompactedFile = ((path.substring(0, path.lastIndexOf("/delete+"))) + "/") + (filename.split("\\+")[1]);
				if (fs.exists(new Path(expectedCompactedFile))) {
					if (!(fs.deleteRecursively(file.getPath())))
						RootFiles.log.warn((("Delete of file: " + (file.getPath().toString())) + " return false"));

					continue;
				}
				filename = filename.split("\\+", 3)[2];
				path = ((path.substring(0, path.lastIndexOf("/delete+"))) + "/") + filename;
			}
			if (filename.endsWith("_tmp")) {
				if (deleteTmp) {
					RootFiles.log.warn(("cleaning up old tmp file: " + path));
					if (!(fs.deleteRecursively(file.getPath())))
						RootFiles.log.warn((("Delete of tmp file: " + (file.getPath().toString())) + " return false"));

				}
				continue;
			}
			if ((!(filename.startsWith(((Constants.MAPFILE_EXTENSION) + "_")))) && (!(FileOperations.getValidExtensions().contains(filename.split("\\.")[1])))) {
				RootFiles.log.error(("unknown file in tablet: " + path));
				continue;
			}
			goodFiles.add(path);
		}
		return goodFiles;
	}
}

