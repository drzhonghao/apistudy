

import com.microsoft.schemas.office.visio.x2012.main.MasterType;
import com.microsoft.schemas.office.visio.x2012.main.MastersDocument;
import com.microsoft.schemas.office.visio.x2012.main.MastersType;
import com.microsoft.schemas.office.visio.x2012.main.RelType;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ooxml.POIXMLDocumentPart;
import org.apache.poi.ooxml.POIXMLDocumentPart.RelationPart;
import org.apache.poi.ooxml.POIXMLException;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.openxml4j.opc.PackageRelationship;
import org.apache.poi.xdgf.exceptions.XDGFException;
import org.apache.poi.xdgf.usermodel.XDGFDocument;
import org.apache.poi.xdgf.usermodel.XDGFMaster;
import org.apache.poi.xdgf.usermodel.XDGFMasterContents;
import org.apache.poi.xdgf.xml.XDGFXMLDocumentPart;
import org.apache.xmlbeans.XmlException;

import static com.microsoft.schemas.office.visio.x2012.main.MastersDocument.Factory.parse;


public class XDGFMasters extends XDGFXMLDocumentPart {
	MastersType _mastersObject;

	protected Map<Long, XDGFMaster> _masters = new HashMap<>();

	public XDGFMasters(PackagePart part, XDGFDocument document) {
		super(part, document);
	}

	@org.apache.poi.util.Internal
	protected MastersType getXmlObject() {
		return _mastersObject;
	}

	@Override
	protected void onDocumentRead() {
		try {
			try {
				_mastersObject = parse(getPackagePart().getInputStream()).getMasters();
			} catch (XmlException | IOException e) {
				throw new POIXMLException(e);
			}
			Map<String, MasterType> masterSettings = new HashMap<>();
			for (MasterType master : _mastersObject.getMasterArray()) {
				masterSettings.put(master.getRel().getId(), master);
			}
			for (POIXMLDocumentPart.RelationPart rp : getRelationParts()) {
				POIXMLDocumentPart part = rp.getDocumentPart();
				String relId = rp.getRelationship().getId();
				MasterType settings = masterSettings.get(relId);
				if (settings == null) {
					throw new POIXMLException((("Master relationship for " + relId) + " not found"));
				}
				if (!(part instanceof XDGFMasterContents)) {
					throw new POIXMLException(((("Unexpected masters relationship for " + relId) + ": ") + part));
				}
				XDGFMasterContents contents = ((XDGFMasterContents) (part));
				XDGFMaster master = new XDGFMaster(settings, contents, _document);
				_masters.put(master.getID(), master);
			}
		} catch (POIXMLException e) {
			throw XDGFException.wrap(this, e);
		}
	}

	public Collection<XDGFMaster> getMastersList() {
		return Collections.unmodifiableCollection(_masters.values());
	}

	public XDGFMaster getMasterById(long masterId) {
		return _masters.get(masterId);
	}
}

