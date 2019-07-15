

import org.apache.poi.util.Beta;
import org.apache.poi.xslf.usermodel.XSLFAutoShape;
import org.apache.poi.xslf.usermodel.XSLFConnectorShape;
import org.apache.poi.xslf.usermodel.XSLFFreeformShape;
import org.apache.poi.xslf.usermodel.XSLFGroupShape;
import org.apache.poi.xslf.usermodel.XSLFObjectShape;
import org.apache.poi.xslf.usermodel.XSLFPictureShape;
import org.apache.poi.xslf.usermodel.XSLFSheet;
import org.apache.poi.xslf.usermodel.XSLFTable;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.openxmlformats.schemas.presentationml.x2006.main.CTConnector;
import org.openxmlformats.schemas.presentationml.x2006.main.CTGraphicalObjectFrame;
import org.openxmlformats.schemas.presentationml.x2006.main.CTGroupShape;
import org.openxmlformats.schemas.presentationml.x2006.main.CTPicture;
import org.openxmlformats.schemas.presentationml.x2006.main.CTShape;


@Beta
public class XSLFDrawing {
	private XSLFSheet _sheet;

	private CTGroupShape _spTree;

	XSLFDrawing(XSLFSheet sheet, CTGroupShape spTree) {
		_sheet = sheet;
		_spTree = spTree;
	}

	public XSLFAutoShape createAutoShape() {
		CTShape sp = _spTree.addNewSp();
		return null;
	}

	public XSLFFreeformShape createFreeform() {
		CTShape sp = _spTree.addNewSp();
		return null;
	}

	public XSLFTextBox createTextBox() {
		CTShape sp = _spTree.addNewSp();
		return null;
	}

	public XSLFConnectorShape createConnector() {
		CTConnector sp = _spTree.addNewCxnSp();
		return null;
	}

	public XSLFGroupShape createGroup() {
		CTGroupShape sp = _spTree.addNewGrpSp();
		return null;
	}

	public XSLFPictureShape createPicture(String rel) {
		CTPicture sp = _spTree.addNewPic();
		return null;
	}

	public XSLFTable createTable() {
		CTGraphicalObjectFrame sp = _spTree.addNewGraphicFrame();
		return null;
	}

	public XSLFObjectShape createOleShape(String pictureRel) {
		CTGraphicalObjectFrame sp = _spTree.addNewGraphicFrame();
		return null;
	}
}

