import org.apache.poi.hpbf.*;


import java.io.IOException;
import java.io.InputStream;

import org.apache.poi.POIReadOnlyDocument;
import org.apache.poi.hpbf.model.EscherDelayStm;
import org.apache.poi.hpbf.model.EscherStm;
import org.apache.poi.hpbf.model.MainContents;
import org.apache.poi.hpbf.model.QuillContents;
import org.apache.poi.poifs.filesystem.DirectoryNode;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

/**
 * This class provides the basic functionality
 *  for HPBF, our implementation of the publisher
 *  file format.
 */
public final class HPBFDocument extends POIReadOnlyDocument {
	private MainContents mainContents;
	private QuillContents quillContents;
	private EscherStm escherStm;
	private EscherDelayStm escherDelayStm;

	/**
	 * Opens a new publisher document
	 */
	public HPBFDocument(POIFSFileSystem fs) throws IOException {
	   this(fs.getRoot());
	}

	public HPBFDocument(InputStream inp) throws IOException {
	   this(new POIFSFileSystem(inp));
	}

	/**
	 * Opens an embedded publisher document,
	 *  at the given directory.
	 */
	public HPBFDocument(DirectoryNode dir) throws IOException {
	   super(dir);

	   // Go looking for our interesting child
	   //  streams
	   mainContents = new MainContents(dir);
	   quillContents = new QuillContents(dir);

	   // Now the Escher bits
	   escherStm = new EscherStm(dir);
	   escherDelayStm = new EscherDelayStm(dir);
	}

	public MainContents getMainContents() {
		return mainContents;
	}
	public QuillContents getQuillContents() {
		return quillContents;
	}
	public EscherStm getEscherStm() {
		return escherStm;
	}
	public EscherDelayStm getEscherDelayStm() {
		return escherDelayStm;
	}
}
