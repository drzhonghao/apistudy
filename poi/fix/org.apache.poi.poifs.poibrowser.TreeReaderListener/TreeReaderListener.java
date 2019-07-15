

import java.util.HashMap;
import java.util.Map;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import org.apache.poi.poifs.eventfilesystem.POIFSReaderEvent;
import org.apache.poi.poifs.eventfilesystem.POIFSReaderListener;
import org.apache.poi.poifs.filesystem.DocumentDescriptor;
import org.apache.poi.poifs.filesystem.DocumentInputStream;
import org.apache.poi.poifs.filesystem.POIFSDocumentPath;


public class TreeReaderListener implements POIFSReaderListener {
	protected MutableTreeNode rootNode;

	protected Map<Object, MutableTreeNode> pathToNode;

	protected String filename;

	public TreeReaderListener(final String filename, final MutableTreeNode rootNode) {
		this.filename = filename;
		this.rootNode = rootNode;
		pathToNode = new HashMap<>(15);
	}

	private int nrOfBytes = 50;

	public void setNrOfBytes(final int nrOfBytes) {
		this.nrOfBytes = nrOfBytes;
	}

	public int getNrOfBytes() {
		return nrOfBytes;
	}

	@Override
	public void processPOIFSReaderEvent(final POIFSReaderEvent event) {
		DocumentDescriptor d;
		final DocumentInputStream is = event.getStream();
		if (!(is.markSupported())) {
			throw new UnsupportedOperationException(((is.getClass().getName()) + " does not support mark()."));
		}
		try {
		} catch (Exception t) {
			throw new RuntimeException(((("Unexpected exception while processing " + (event.getName())) + " in ") + (event.getPath())), t);
		}
		is.close();
		d = null;
		final MutableTreeNode dNode = new DefaultMutableTreeNode(d);
	}

	private MutableTreeNode getNode(final POIFSDocumentPath path, final String fsName, final MutableTreeNode root) {
		MutableTreeNode n = pathToNode.get(path);
		if (n != null) {
			return n;
		}
		if ((path.length()) == 0) {
			n = pathToNode.get(fsName);
			if (n == null) {
				n = new DefaultMutableTreeNode(fsName);
				pathToNode.put(fsName, n);
				root.insert(n, 0);
			}
			return n;
		}
		final String name = path.getComponent(((path.length()) - 1));
		final POIFSDocumentPath parentPath = path.getParent();
		final MutableTreeNode parentNode = getNode(parentPath, fsName, root);
		n = new DefaultMutableTreeNode(name);
		pathToNode.put(path, n);
		parentNode.insert(n, 0);
		return n;
	}
}

