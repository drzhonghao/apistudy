

import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;
import org.apache.poi.poifs.poibrowser.Util;


public class DocumentDescriptorRenderer extends DefaultTreeCellRenderer {
	@Override
	public Component getTreeCellRendererComponent(final JTree tree, final Object value, final boolean selectedCell, final boolean expanded, final boolean leaf, final int row, final boolean hasCellFocus) {
		final JPanel p = new JPanel();
		final JTextArea text = new JTextArea();
		text.setFont(new Font("Monospaced", Font.PLAIN, 10));
		p.add(text);
		if (selectedCell) {
			Util.invert(text);
		}
		return p;
	}
}

