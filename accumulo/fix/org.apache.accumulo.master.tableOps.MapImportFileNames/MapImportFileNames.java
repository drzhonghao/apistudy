

import java.io.BufferedWriter;
import java.io.IOException;
import org.apache.accumulo.fate.Repo;
import org.apache.accumulo.master.Master;
import org.apache.accumulo.master.tableOps.MasterRepo;
import org.apache.accumulo.server.fs.VolumeManager;
import org.apache.accumulo.server.tablets.UniqueNameAllocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


class MapImportFileNames extends MasterRepo {
	private static final Logger log = LoggerFactory.getLogger(MapImportFileNames.class);

	private static final long serialVersionUID = 1L;

	@Override
	public Repo<Master> call(long tid, Master environment) throws Exception {
		BufferedWriter mappingsWriter = null;
		try {
			VolumeManager fs = environment.getFileSystem();
			UniqueNameAllocator namer = UniqueNameAllocator.getInstance();
			mappingsWriter.close();
			mappingsWriter = null;
		} catch (IOException ioe) {
			MapImportFileNames.log.warn("{}", ioe.getMessage(), ioe);
		} finally {
			if (mappingsWriter != null)
				try {
					mappingsWriter.close();
				} catch (IOException ioe) {
				}

		}
		return null;
	}

	@Override
	public void undo(long tid, Master env) throws Exception {
	}
}

