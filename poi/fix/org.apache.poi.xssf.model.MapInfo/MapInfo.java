

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.apache.poi.ooxml.POIXMLDocumentPart;
import org.apache.poi.ooxml.POIXMLTypeLoader;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.xssf.usermodel.XSSFMap;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.XmlTokenSource;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTMap;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTMapInfo;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTSchema;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.MapInfoDocument;

import static org.openxmlformats.schemas.spreadsheetml.x2006.main.MapInfoDocument.Factory.newInstance;
import static org.openxmlformats.schemas.spreadsheetml.x2006.main.MapInfoDocument.Factory.parse;


public class MapInfo extends POIXMLDocumentPart {
	private CTMapInfo mapInfo;

	private Map<Integer, XSSFMap> maps;

	public MapInfo() {
		super();
		mapInfo = CTMapInfo.Factory.newInstance();
	}

	public MapInfo(PackagePart part) throws IOException {
		super(part);
		readFrom(part.getInputStream());
	}

	public void readFrom(InputStream is) throws IOException {
		try {
			MapInfoDocument doc = parse(is, POIXMLTypeLoader.DEFAULT_XML_OPTIONS);
			mapInfo = doc.getMapInfo();
			maps = new HashMap<>();
			for (CTMap map : mapInfo.getMapArray()) {
			}
		} catch (XmlException e) {
			throw new IOException(e.getLocalizedMessage());
		}
	}

	public XSSFWorkbook getWorkbook() {
		return ((XSSFWorkbook) (getParent()));
	}

	public CTMapInfo getCTMapInfo() {
		return mapInfo;
	}

	public CTSchema getCTSchemaById(String schemaId) {
		CTSchema xmlSchema = null;
		for (CTSchema schema : mapInfo.getSchemaArray()) {
			if (schema.getID().equals(schemaId)) {
				xmlSchema = schema;
				break;
			}
		}
		return xmlSchema;
	}

	public XSSFMap getXSSFMapById(int id) {
		return maps.get(id);
	}

	public XSSFMap getXSSFMapByName(String name) {
		XSSFMap matchedMap = null;
		for (XSSFMap map : maps.values()) {
			if (((map.getCtMap().getName()) != null) && (map.getCtMap().getName().equals(name))) {
				matchedMap = map;
			}
		}
		return matchedMap;
	}

	public Collection<XSSFMap> getAllXSSFMaps() {
		return maps.values();
	}

	protected void writeTo(OutputStream out) throws IOException {
		MapInfoDocument doc = newInstance();
		doc.setMapInfo(mapInfo);
		doc.save(out, POIXMLTypeLoader.DEFAULT_XML_OPTIONS);
	}

	@Override
	protected void commit() throws IOException {
		PackagePart part = getPackagePart();
		OutputStream out = part.getOutputStream();
		writeTo(out);
		out.close();
	}
}

