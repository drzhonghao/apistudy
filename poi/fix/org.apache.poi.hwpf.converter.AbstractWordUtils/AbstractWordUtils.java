

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.HWPFDocumentCore;
import org.apache.poi.hwpf.HWPFOldDocument;
import org.apache.poi.hwpf.OldWordFileFormatException;
import org.apache.poi.hwpf.converter.NumberFormatter;
import org.apache.poi.hwpf.usermodel.BorderCode;
import org.apache.poi.hwpf.usermodel.HWPFList;
import org.apache.poi.hwpf.usermodel.Table;
import org.apache.poi.hwpf.usermodel.TableCell;
import org.apache.poi.hwpf.usermodel.TableRow;
import org.apache.poi.poifs.filesystem.DirectoryNode;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.util.Beta;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


@Beta
public class AbstractWordUtils {
	static final String EMPTY = "";

	private static final POILogger logger = POILogFactory.getLogger(AbstractWordUtils.class);

	public static final float TWIPS_PER_INCH = 1440.0F;

	public static final int TWIPS_PER_PT = 20;

	static int[] buildTableCellEdgesArray(Table table) {
		Set<Integer> edges = new TreeSet<>();
		for (int r = 0; r < (table.numRows()); r++) {
			TableRow tableRow = table.getRow(r);
			for (int c = 0; c < (tableRow.numCells()); c++) {
				TableCell tableCell = tableRow.getCell(c);
				edges.add(Integer.valueOf(tableCell.getLeftEdge()));
				edges.add(Integer.valueOf(((tableCell.getLeftEdge()) + (tableCell.getWidth()))));
			}
		}
		Integer[] sorted = edges.toArray(new Integer[edges.size()]);
		int[] result = new int[sorted.length];
		for (int i = 0; i < (sorted.length); i++) {
			result[i] = sorted[i].intValue();
		}
		return result;
	}

	static boolean canBeMerged(Node node1, Node node2, String requiredTagName) {
		if (((node1.getNodeType()) != (Node.ELEMENT_NODE)) || ((node2.getNodeType()) != (Node.ELEMENT_NODE)))
			return false;

		Element element1 = ((Element) (node1));
		Element element2 = ((Element) (node2));
		if ((!(AbstractWordUtils.equals(requiredTagName, element1.getTagName()))) || (!(AbstractWordUtils.equals(requiredTagName, element2.getTagName()))))
			return false;

		NamedNodeMap attributes1 = element1.getAttributes();
		NamedNodeMap attributes2 = element2.getAttributes();
		if ((attributes1.getLength()) != (attributes2.getLength()))
			return false;

		for (int i = 0; i < (attributes1.getLength()); i++) {
			final Attr attr1 = ((Attr) (attributes1.item(i)));
			final Attr attr2;
			if (AbstractWordUtils.isNotEmpty(attr1.getNamespaceURI()))
				attr2 = ((Attr) (attributes2.getNamedItemNS(attr1.getNamespaceURI(), attr1.getLocalName())));
			else
				attr2 = ((Attr) (attributes2.getNamedItem(attr1.getName())));

			if ((attr2 == null) || (!(AbstractWordUtils.equals(attr1.getTextContent(), attr2.getTextContent()))))
				return false;

		}
		return true;
	}

	static void compactChildNodesR(Element parentElement, String childTagName) {
		NodeList childNodes = parentElement.getChildNodes();
		for (int i = 0; i < ((childNodes.getLength()) - 1); i++) {
			Node child1 = childNodes.item(i);
			Node child2 = childNodes.item((i + 1));
			while ((child2.getChildNodes().getLength()) > 0)
				child1.appendChild(child2.getFirstChild());

			child2.getParentNode().removeChild(child2);
			i--;
		}
		childNodes = parentElement.getChildNodes();
		for (int i = 0; i < ((childNodes.getLength()) - 1); i++) {
			Node child = childNodes.item(i);
			if (child instanceof Element) {
				AbstractWordUtils.compactChildNodesR(((Element) (child)), childTagName);
			}
		}
	}

	static boolean equals(String str1, String str2) {
		return str1 == null ? str2 == null : str1.equals(str2);
	}

	public static String getBorderType(BorderCode borderCode) {
		if (borderCode == null)
			throw new IllegalArgumentException("borderCode is null");

		switch (borderCode.getBorderType()) {
			case 1 :
			case 2 :
				return "solid";
			case 3 :
				return "double";
			case 5 :
				return "solid";
			case 6 :
				return "dotted";
			case 7 :
			case 8 :
				return "dashed";
			case 9 :
				return "dotted";
			case 10 :
			case 11 :
			case 12 :
			case 13 :
			case 14 :
			case 15 :
			case 16 :
			case 17 :
			case 18 :
			case 19 :
				return "double";
			case 20 :
				return "solid";
			case 21 :
				return "double";
			case 22 :
				return "dashed";
			case 23 :
				return "dashed";
			case 24 :
				return "ridge";
			case 25 :
				return "grooved";
			default :
				return "solid";
		}
	}

	public static String getBorderWidth(BorderCode borderCode) {
		int lineWidth = borderCode.getLineWidth();
		int pt = lineWidth / 8;
		int pte = lineWidth - (pt * 8);
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(pt);
		stringBuilder.append(".");
		stringBuilder.append(((1000 / 8) * pte));
		stringBuilder.append("pt");
		return stringBuilder.toString();
	}

	public static class NumberingState {
		private final Map<String, Integer> levels = new HashMap<>();
	}

	public static String getBulletText(AbstractWordUtils.NumberingState numberingState, HWPFList list, char level) {
		StringBuffer bulletBuffer = new StringBuffer();
		char[] xst = list.getNumberText(level).toCharArray();
		for (char element : xst) {
			if (element < 9) {
				int lsid = list.getLsid();
				final String key = (lsid + "#") + ((int) (element));
				int num;
				if ((!(list.isStartAtOverriden(element))) && (numberingState.levels.containsKey(key))) {
					num = numberingState.levels.get(key).intValue();
					if (level == element) {
						num++;
						numberingState.levels.put(key, Integer.valueOf(num));
					}
				}else {
					num = list.getStartAt(element);
					numberingState.levels.put(key, Integer.valueOf(num));
				}
				if (level == element) {
					for (int i = element + 1; i < 9; i++) {
						final String childKey = (lsid + "#") + i;
						numberingState.levels.remove(childKey);
					}
				}
				bulletBuffer.append(NumberFormatter.getNumber(num, list.getNumberFormat(level)));
			}else {
				bulletBuffer.append(element);
			}
		}
		byte follow = list.getTypeOfCharFollowingTheNumber(level);
		switch (follow) {
			case 0 :
				bulletBuffer.append("\t");
				break;
			case 1 :
				bulletBuffer.append(" ");
				break;
			default :
				break;
		}
		return bulletBuffer.toString();
	}

	public static String getColor(int ico) {
		switch (ico) {
			case 1 :
				return "black";
			case 2 :
				return "blue";
			case 3 :
				return "cyan";
			case 4 :
				return "green";
			case 5 :
				return "magenta";
			case 6 :
				return "red";
			case 7 :
				return "yellow";
			case 8 :
				return "white";
			case 9 :
				return "darkblue";
			case 10 :
				return "darkcyan";
			case 11 :
				return "darkgreen";
			case 12 :
				return "darkmagenta";
			case 13 :
				return "darkred";
			case 14 :
				return "darkyellow";
			case 15 :
				return "darkgray";
			case 16 :
				return "lightgray";
			default :
				return "black";
		}
	}

	public static String getOpacity(int argbValue) {
		int opacity = ((int) ((argbValue & 4278190080L) >>> 24));
		if ((opacity == 0) || (opacity == 255))
			return ".0";

		return "" + (opacity / ((float) (255)));
	}

	public static String getColor24(int argbValue) {
		if (argbValue == (-1))
			throw new IllegalArgumentException("This colorref is empty");

		int bgrValue = argbValue & 16777215;
		int rgbValue = (((bgrValue & 255) << 16) | (bgrValue & 65280)) | ((bgrValue & 16711680) >> 16);
		switch (rgbValue) {
			case 16777215 :
				return "white";
			case 12632256 :
				return "silver";
			case 8421504 :
				return "gray";
			case 0 :
				return "black";
			case 16711680 :
				return "red";
			case 8388608 :
				return "maroon";
			case 16776960 :
				return "yellow";
			case 8421376 :
				return "olive";
			case 65280 :
				return "lime";
			case 32768 :
				return "green";
			case 65535 :
				return "aqua";
			case 32896 :
				return "teal";
			case 255 :
				return "blue";
			case 128 :
				return "navy";
			case 16711935 :
				return "fuchsia";
			case 8388736 :
				return "purple";
		}
		StringBuilder result = new StringBuilder("#");
		String hex = Integer.toHexString(rgbValue);
		for (int i = hex.length(); i < 6; i++) {
			result.append('0');
		}
		result.append(hex);
		return result.toString();
	}

	public static String getJustification(int js) {
		switch (js) {
			case 0 :
				return "start";
			case 1 :
				return "center";
			case 2 :
				return "end";
			case 3 :
			case 4 :
				return "justify";
			case 5 :
				return "center";
			case 6 :
				return "left";
			case 7 :
				return "start";
			case 8 :
				return "end";
			case 9 :
				return "justify";
		}
		return "";
	}

	public static String getLanguage(int languageCode) {
		switch (languageCode) {
			case 1024 :
				return AbstractWordUtils.EMPTY;
			case 1033 :
				return "en-us";
			case 1049 :
				return "ru-ru";
			case 2057 :
				return "en-uk";
			default :
				AbstractWordUtils.logger.log(POILogger.WARN, "Uknown or unmapped language code: ", Integer.valueOf(languageCode));
				return AbstractWordUtils.EMPTY;
		}
	}

	public static String getListItemNumberLabel(int number, int format) {
		if (format != 0)
			AbstractWordUtils.logger.log(POILogger.INFO, ("NYI: toListItemNumberLabel(): " + format));

		return String.valueOf(number);
	}

	static boolean isEmpty(String str) {
		return (str == null) || ((str.length()) == 0);
	}

	static boolean isNotEmpty(String str) {
		return !(AbstractWordUtils.isEmpty(str));
	}

	public static HWPFDocumentCore loadDoc(final DirectoryNode root) throws IOException {
		try {
			return new HWPFDocument(root);
		} catch (OldWordFileFormatException exc) {
			return new HWPFOldDocument(root);
		}
	}

	public static HWPFDocumentCore loadDoc(File docFile) throws IOException {
		final FileInputStream istream = new FileInputStream(docFile);
		try {
			return AbstractWordUtils.loadDoc(istream);
		} finally {
			istream.close();
		}
	}

	public static HWPFDocumentCore loadDoc(InputStream inputStream) throws IOException {
		return AbstractWordUtils.loadDoc(HWPFDocumentCore.verifyAndBuildPOIFS(inputStream));
	}

	public static HWPFDocumentCore loadDoc(final POIFSFileSystem poifsFileSystem) throws IOException {
		return AbstractWordUtils.loadDoc(poifsFileSystem.getRoot());
	}
}

