

import java.util.function.Consumer;
import java.util.function.Function;
import org.apache.poi.sl.usermodel.MasterSheet;
import org.apache.poi.sl.usermodel.Placeholder;
import org.apache.poi.sl.usermodel.PlaceholderDetails;
import org.apache.poi.sl.usermodel.Sheet;
import org.apache.poi.xslf.usermodel.XSLFNotesMaster;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSheet;
import org.apache.poi.xslf.usermodel.XSLFSlideLayout;
import org.apache.poi.xslf.usermodel.XSLFSlideMaster;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.xmlbeans.StringEnumAbstractBase;
import org.openxmlformats.schemas.presentationml.x2006.main.CTApplicationNonVisualDrawingProps;
import org.openxmlformats.schemas.presentationml.x2006.main.CTHeaderFooter;
import org.openxmlformats.schemas.presentationml.x2006.main.CTNotesMaster;
import org.openxmlformats.schemas.presentationml.x2006.main.CTPlaceholder;
import org.openxmlformats.schemas.presentationml.x2006.main.CTSlideMaster;
import org.openxmlformats.schemas.presentationml.x2006.main.STPlaceholderSize;
import org.openxmlformats.schemas.presentationml.x2006.main.STPlaceholderType;

import static org.apache.poi.sl.usermodel.PlaceholderDetails.PlaceholderSize.full;
import static org.apache.poi.sl.usermodel.PlaceholderDetails.PlaceholderSize.half;
import static org.apache.poi.sl.usermodel.PlaceholderDetails.PlaceholderSize.quarter;


public class XSLFPlaceholderDetails implements PlaceholderDetails {
	private final XSLFShape shape;

	private CTPlaceholder _ph;

	XSLFPlaceholderDetails(final XSLFShape shape) {
		this.shape = shape;
	}

	@Override
	public Placeholder getPlaceholder() {
		final CTPlaceholder ph = getCTPlaceholder(false);
		if ((ph == null) || (!((ph.isSetType()) || (ph.isSetIdx())))) {
			return null;
		}
		return Placeholder.lookupOoxml(ph.getType().intValue());
	}

	@Override
	public void setPlaceholder(final Placeholder placeholder) {
		CTPlaceholder ph = getCTPlaceholder((placeholder != null));
		if (ph != null) {
			if (placeholder != null) {
				ph.setType(STPlaceholderType.Enum.forInt(placeholder.ooxmlId));
			}else {
				getNvProps().unsetPh();
			}
		}
	}

	@Override
	public boolean isVisible() {
		final CTPlaceholder ph = getCTPlaceholder(false);
		if ((ph == null) || (!(ph.isSetType()))) {
			return true;
		}
		final CTHeaderFooter hf = getHeaderFooter(false);
		if (hf == null) {
			return false;
		}
		final Placeholder pl = Placeholder.lookupOoxml(ph.getType().intValue());
		if (pl == null) {
			return true;
		}
		switch (pl) {
			case DATETIME :
				return (!(hf.isSetDt())) || (hf.getDt());
			case FOOTER :
				return (!(hf.isSetFtr())) || (hf.getFtr());
			case HEADER :
				return (!(hf.isSetHdr())) || (hf.getHdr());
			case SLIDE_NUMBER :
				return (!(hf.isSetSldNum())) || (hf.getSldNum());
			default :
				return true;
		}
	}

	@Override
	public void setVisible(final boolean isVisible) {
		final Placeholder ph = getPlaceholder();
		if (ph == null) {
			return;
		}
		final Function<CTHeaderFooter, Consumer<Boolean>> fun;
		switch (ph) {
			case DATETIME :
				fun = ( hf) -> hf::setDt;
				break;
			case FOOTER :
				fun = ( hf) -> hf::setFtr;
				break;
			case HEADER :
				fun = ( hf) -> hf::setHdr;
				break;
			case SLIDE_NUMBER :
				fun = ( hf) -> hf::setSldNum;
				break;
			default :
				return;
		}
		final CTHeaderFooter hf = getHeaderFooter(true);
		if (hf == null) {
			return;
		}
		fun.apply(hf).accept(isVisible);
	}

	@Override
	public PlaceholderDetails.PlaceholderSize getSize() {
		final CTPlaceholder ph = getCTPlaceholder(false);
		if ((ph == null) || (!(ph.isSetSz()))) {
			return null;
		}
		switch (ph.getSz().intValue()) {
			case STPlaceholderSize.INT_FULL :
				return full;
			case STPlaceholderSize.INT_HALF :
				return half;
			case STPlaceholderSize.INT_QUARTER :
				return quarter;
			default :
				return null;
		}
	}

	@Override
	public void setSize(final PlaceholderDetails.PlaceholderSize size) {
		final CTPlaceholder ph = getCTPlaceholder(false);
		if (ph == null) {
			return;
		}
		if (size == null) {
			ph.unsetSz();
			return;
		}
		switch (size) {
			case full :
				ph.setSz(STPlaceholderSize.FULL);
				break;
			case half :
				ph.setSz(STPlaceholderSize.HALF);
				break;
			case quarter :
				ph.setSz(STPlaceholderSize.QUARTER);
				break;
		}
	}

	CTPlaceholder getCTPlaceholder(final boolean create) {
		if ((_ph) != null) {
			return _ph;
		}
		final CTApplicationNonVisualDrawingProps nv = getNvProps();
		if (nv == null) {
			return null;
		}
		_ph = ((nv.isSetPh()) || (!create)) ? nv.getPh() : nv.addNewPh();
		return _ph;
	}

	private CTApplicationNonVisualDrawingProps getNvProps() {
		return null;
	}

	private CTHeaderFooter getHeaderFooter(final boolean create) {
		final XSLFSheet sheet = shape.getSheet();
		final XSLFSheet master = ((sheet instanceof MasterSheet) && (!(sheet instanceof XSLFSlideLayout))) ? sheet : ((XSLFSheet) (sheet.getMasterSheet()));
		if (master instanceof XSLFSlideMaster) {
			final CTSlideMaster ct = ((XSLFSlideMaster) (master)).getXmlObject();
			return (ct.isSetHf()) || (!create) ? ct.getHf() : ct.addNewHf();
		}else
			if (master instanceof XSLFNotesMaster) {
				final CTNotesMaster ct = ((XSLFNotesMaster) (master)).getXmlObject();
				return (ct.isSetHf()) || (!create) ? ct.getHf() : ct.addNewHf();
			}else {
				return null;
			}

	}

	@Override
	public String getText() {
		return null;
	}

	@Override
	public void setText(String text) {
	}
}

