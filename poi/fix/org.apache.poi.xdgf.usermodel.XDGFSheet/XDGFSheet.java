

import com.microsoft.schemas.office.visio.x2012.main.CellType;
import com.microsoft.schemas.office.visio.x2012.main.SectionType;
import com.microsoft.schemas.office.visio.x2012.main.SheetType;
import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import org.apache.poi.ooxml.POIXMLException;
import org.apache.poi.xdgf.exceptions.XDGFException;
import org.apache.poi.xdgf.usermodel.XDGFCell;
import org.apache.poi.xdgf.usermodel.XDGFDocument;
import org.apache.poi.xdgf.usermodel.XDGFStyleSheet;
import org.apache.poi.xdgf.usermodel.section.CharacterSection;
import org.apache.poi.xdgf.usermodel.section.GeometrySection;
import org.apache.poi.xdgf.usermodel.section.XDGFSection;


public abstract class XDGFSheet {
	protected XDGFDocument _document;

	protected SheetType _sheet;

	protected Map<String, XDGFCell> _cells = new HashMap<>();

	protected Map<String, XDGFSection> _sections = new HashMap<>();

	protected SortedMap<Long, GeometrySection> _geometry = new TreeMap<>();

	protected CharacterSection _character;

	public XDGFSheet(SheetType sheet, XDGFDocument document) {
		try {
			_sheet = sheet;
			_document = document;
			for (CellType cell : sheet.getCellArray()) {
				if (_cells.containsKey(cell.getN()))
					throw new POIXMLException(("Unexpected duplicate cell " + (cell.getN())));

				_cells.put(cell.getN(), new XDGFCell(cell));
			}
			for (SectionType section : sheet.getSectionArray()) {
				String name = section.getN();
				if (name.equals("Geometry")) {
				}else
					if (name.equals("Character")) {
					}else {
					}

			}
		} catch (POIXMLException e) {
			throw XDGFException.wrap(this.toString(), e);
		}
	}

	abstract SheetType getXmlObject();

	public XDGFDocument getDocument() {
		return _document;
	}

	public XDGFCell getCell(String cellName) {
		return _cells.get(cellName);
	}

	public XDGFSection getSection(String sectionName) {
		return _sections.get(sectionName);
	}

	public XDGFStyleSheet getLineStyle() {
		if (!(_sheet.isSetLineStyle()))
			return null;

		return _document.getStyleById(_sheet.getLineStyle());
	}

	public XDGFStyleSheet getFillStyle() {
		if (!(_sheet.isSetFillStyle()))
			return null;

		return _document.getStyleById(_sheet.getFillStyle());
	}

	public XDGFStyleSheet getTextStyle() {
		if (!(_sheet.isSetTextStyle()))
			return null;

		return _document.getStyleById(_sheet.getTextStyle());
	}

	public Color getFontColor() {
		Color fontColor;
		if ((_character) != null) {
			fontColor = _character.getFontColor();
			if (fontColor != null)
				return fontColor;

		}
		XDGFStyleSheet style = getTextStyle();
		if (style != null)
			return style.getFontColor();

		return null;
	}

	public Double getFontSize() {
		Double fontSize;
		if ((_character) != null) {
			fontSize = _character.getFontSize();
			if (fontSize != null)
				return fontSize;

		}
		XDGFStyleSheet style = getTextStyle();
		if (style != null)
			return style.getFontSize();

		return null;
	}

	public Integer getLineCap() {
		Integer lineCap = XDGFCell.maybeGetInteger(_cells, "LineCap");
		if (lineCap != null)
			return lineCap;

		XDGFStyleSheet style = getLineStyle();
		if (style != null)
			return style.getLineCap();

		return null;
	}

	public Color getLineColor() {
		String lineColor = XDGFCell.maybeGetString(_cells, "LineColor");
		if (lineColor != null)
			return Color.decode(lineColor);

		XDGFStyleSheet style = getLineStyle();
		if (style != null)
			return style.getLineColor();

		return null;
	}

	public Integer getLinePattern() {
		Integer linePattern = XDGFCell.maybeGetInteger(_cells, "LinePattern");
		if (linePattern != null)
			return linePattern;

		XDGFStyleSheet style = getLineStyle();
		if (style != null)
			return style.getLinePattern();

		return null;
	}

	public Double getLineWeight() {
		Double lineWeight = XDGFCell.maybeGetDouble(_cells, "LineWeight");
		if (lineWeight != null)
			return lineWeight;

		XDGFStyleSheet style = getLineStyle();
		if (style != null)
			return style.getLineWeight();

		return null;
	}
}

