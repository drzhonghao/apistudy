

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ddf.EscherContainerRecord;
import org.apache.poi.ddf.EscherDgRecord;
import org.apache.poi.ddf.EscherDggRecord;
import org.apache.poi.ddf.EscherRecord;
import org.apache.poi.ddf.EscherSpRecord;
import org.apache.poi.hslf.exceptions.HSLFException;
import org.apache.poi.hslf.model.HeadersFooters;
import org.apache.poi.hslf.record.CString;
import org.apache.poi.hslf.record.ColorSchemeAtom;
import org.apache.poi.hslf.record.Comment2000;
import org.apache.poi.hslf.record.Document;
import org.apache.poi.hslf.record.EscherTextboxWrapper;
import org.apache.poi.hslf.record.HeadersFootersContainer;
import org.apache.poi.hslf.record.PPDrawing;
import org.apache.poi.hslf.record.PPDrawingGroup;
import org.apache.poi.hslf.record.PositionDependentRecordContainer;
import org.apache.poi.hslf.record.Record;
import org.apache.poi.hslf.record.RecordContainer;
import org.apache.poi.hslf.record.RecordTypes;
import org.apache.poi.hslf.record.SSSlideInfoAtom;
import org.apache.poi.hslf.record.SheetContainer;
import org.apache.poi.hslf.record.SlideAtom;
import org.apache.poi.hslf.record.SlideAtomLayout;
import org.apache.poi.hslf.record.SlideListWithText;
import org.apache.poi.hslf.record.StyleTextProp9Atom;
import org.apache.poi.hslf.record.TextHeaderAtom;
import org.apache.poi.hslf.usermodel.HSLFBackground;
import org.apache.poi.hslf.usermodel.HSLFComment;
import org.apache.poi.hslf.usermodel.HSLFMasterSheet;
import org.apache.poi.hslf.usermodel.HSLFNotes;
import org.apache.poi.hslf.usermodel.HSLFPlaceholder;
import org.apache.poi.hslf.usermodel.HSLFShape;
import org.apache.poi.hslf.usermodel.HSLFSheet;
import org.apache.poi.hslf.usermodel.HSLFSimpleShape;
import org.apache.poi.hslf.usermodel.HSLFSlideMaster;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.hslf.usermodel.HSLFTextBox;
import org.apache.poi.hslf.usermodel.HSLFTextParagraph;
import org.apache.poi.hslf.usermodel.HSLFTextRun;
import org.apache.poi.hslf.usermodel.HSLFTextShape;
import org.apache.poi.hslf.usermodel.HSLFTitleMaster;
import org.apache.poi.sl.draw.DrawFactory;
import org.apache.poi.sl.draw.DrawSlide;
import org.apache.poi.sl.draw.Drawable;
import org.apache.poi.sl.usermodel.Notes;
import org.apache.poi.sl.usermodel.Placeholder;
import org.apache.poi.sl.usermodel.ShapeType;
import org.apache.poi.sl.usermodel.Slide;

import static org.apache.poi.hslf.record.SlideAtomLayout.SlideLayoutType.MASTER_TITLE;
import static org.apache.poi.hslf.record.SlideAtomLayout.SlideLayoutType.TITLE_ONLY;
import static org.apache.poi.hslf.record.SlideAtomLayout.SlideLayoutType.TITLE_SLIDE;


public final class HSLFSlide extends HSLFSheet implements Slide<HSLFShape, HSLFTextParagraph> {
	private int _slideNo;

	private SlideListWithText.SlideAtomsSet _atomSet;

	private final List<List<HSLFTextParagraph>> _paragraphs = new ArrayList<>();

	private HSLFNotes _notes;

	public HSLFSlide(org.apache.poi.hslf.record.Slide slide, HSLFNotes notes, SlideListWithText.SlideAtomsSet atomSet, int slideIdentifier, int slideNumber) {
		super(slide, slideIdentifier);
		_notes = notes;
		_atomSet = atomSet;
		_slideNo = slideNumber;
		if (((_atomSet) != null) && ((_atomSet.getSlideRecords().length) > 0)) {
			if (_paragraphs.isEmpty()) {
				throw new HSLFException("No text records found for slide");
			}
		}
		for (List<HSLFTextParagraph> l : HSLFTextParagraph.findTextParagraphs(getPPDrawing(), this)) {
			if (!(_paragraphs.contains(l))) {
				_paragraphs.add(l);
			}
		}
	}

	public HSLFSlide(int sheetNumber, int sheetRefId, int slideNumber) {
		super(new org.apache.poi.hslf.record.Slide(), sheetNumber);
		_slideNo = slideNumber;
		getSheetContainer().setSheetId(sheetRefId);
	}

	@Override
	public HSLFNotes getNotes() {
		return _notes;
	}

	@Override
	public void setNotes(Notes<HSLFShape, HSLFTextParagraph> notes) {
		if ((notes != null) && (!(notes instanceof HSLFNotes))) {
			throw new IllegalArgumentException("notes needs to be of type HSLFNotes");
		}
		_notes = ((HSLFNotes) (notes));
		SlideAtom sa = getSlideRecord().getSlideAtom();
		if ((_notes) == null) {
			sa.setNotesID(0);
		}else {
			sa.setNotesID(_notes._getSheetNumber());
		}
	}

	public void setSlideNumber(int newSlideNumber) {
		_slideNo = newSlideNumber;
	}

	@Override
	public void onCreate() {
		EscherDggRecord dgg = getSlideShow().getDocumentRecord().getPPDrawingGroup().getEscherDggRecord();
		EscherContainerRecord dgContainer = getSheetContainer().getPPDrawing().getDgContainer();
		EscherDgRecord dg = HSLFShape.getEscherChild(dgContainer, EscherDgRecord.RECORD_ID);
		int dgId = (dgg.getMaxDrawingGroupId()) + 1;
		dg.setOptions(((short) (dgId << 4)));
		dgg.setDrawingsSaved(((dgg.getDrawingsSaved()) + 1));
		for (EscherContainerRecord c : dgContainer.getChildContainers()) {
			EscherSpRecord spr = null;
			switch (c.getRecordId()) {
				case EscherContainerRecord.SPGR_CONTAINER :
					EscherContainerRecord dc = ((EscherContainerRecord) (c.getChild(0)));
					spr = dc.getChildById(EscherSpRecord.RECORD_ID);
					break;
				case EscherContainerRecord.SP_CONTAINER :
					spr = c.getChildById(EscherSpRecord.RECORD_ID);
					break;
				default :
					break;
			}
			if (spr != null) {
				spr.setShapeId(allocateShapeId());
			}
		}
		dg.setNumShapes(1);
	}

	public HSLFTextBox addTitle() {
		HSLFPlaceholder pl = new HSLFPlaceholder();
		pl.setShapeType(ShapeType.RECT);
		pl.setPlaceholder(Placeholder.TITLE);
		pl.setRunType(TextHeaderAtom.TITLE_TYPE);
		pl.setText("Click to edit title");
		pl.setAnchor(new Rectangle(54, 48, 612, 90));
		addShape(pl);
		return pl;
	}

	@Override
	public String getTitle() {
		for (List<HSLFTextParagraph> tp : getTextParagraphs()) {
			if (tp.isEmpty()) {
				continue;
			}
			int type = tp.get(0).getRunType();
			switch (type) {
				case TextHeaderAtom.CENTER_TITLE_TYPE :
				case TextHeaderAtom.TITLE_TYPE :
					String str = HSLFTextParagraph.getRawText(tp);
					return HSLFTextParagraph.toExternalString(str, type);
			}
		}
		return null;
	}

	@Override
	public String getSlideName() {
		final CString name = ((CString) (getSlideRecord().findFirstOfType(RecordTypes.CString.typeID)));
		return name != null ? name.getText() : "Slide" + (getSlideNumber());
	}

	@Override
	public List<List<HSLFTextParagraph>> getTextParagraphs() {
		return _paragraphs;
	}

	@Override
	public int getSlideNumber() {
		return _slideNo;
	}

	public org.apache.poi.hslf.record.Slide getSlideRecord() {
		return ((org.apache.poi.hslf.record.Slide) (getSheetContainer()));
	}

	protected SlideListWithText.SlideAtomsSet getSlideAtomsSet() {
		return _atomSet;
	}

	@Override
	public HSLFMasterSheet getMasterSheet() {
		int masterId = getSlideRecord().getSlideAtom().getMasterID();
		for (HSLFSlideMaster sm : getSlideShow().getSlideMasters()) {
			if (masterId == (sm._getSheetNumber())) {
				return sm;
			}
		}
		for (HSLFTitleMaster tm : getSlideShow().getTitleMasters()) {
			if (masterId == (tm._getSheetNumber())) {
				return tm;
			}
		}
		return null;
	}

	public void setMasterSheet(HSLFMasterSheet master) {
		SlideAtom sa = getSlideRecord().getSlideAtom();
		int sheetNo = master._getSheetNumber();
		sa.setMasterID(sheetNo);
	}

	@Override
	public void setFollowMasterBackground(boolean flag) {
		SlideAtom sa = getSlideRecord().getSlideAtom();
		sa.setFollowMasterBackground(flag);
	}

	@Override
	public boolean getFollowMasterBackground() {
		SlideAtom sa = getSlideRecord().getSlideAtom();
		return sa.getFollowMasterBackground();
	}

	@Override
	public void setFollowMasterObjects(boolean flag) {
		SlideAtom sa = getSlideRecord().getSlideAtom();
		sa.setFollowMasterObjects(flag);
	}

	public boolean getFollowMasterScheme() {
		SlideAtom sa = getSlideRecord().getSlideAtom();
		return sa.getFollowMasterScheme();
	}

	public void setFollowMasterScheme(boolean flag) {
		SlideAtom sa = getSlideRecord().getSlideAtom();
		sa.setFollowMasterScheme(flag);
	}

	@Override
	public boolean getFollowMasterObjects() {
		SlideAtom sa = getSlideRecord().getSlideAtom();
		return sa.getFollowMasterObjects();
	}

	@Override
	public HSLFBackground getBackground() {
		if (getFollowMasterBackground()) {
			final HSLFMasterSheet ms = getMasterSheet();
			return ms == null ? null : ms.getBackground();
		}
		return super.getBackground();
	}

	@Override
	public ColorSchemeAtom getColorScheme() {
		if (getFollowMasterScheme()) {
			final HSLFMasterSheet ms = getMasterSheet();
			return ms == null ? null : ms.getColorScheme();
		}
		return super.getColorScheme();
	}

	private static RecordContainer selectContainer(final RecordContainer root, final int index, final RecordTypes... path) {
		if ((root == null) || (index >= (path.length))) {
			return root;
		}
		final RecordContainer newRoot = ((RecordContainer) (root.findFirstOfType(path[index].typeID)));
		return HSLFSlide.selectContainer(newRoot, (index + 1), path);
	}

	public List<HSLFComment> getComments() {
		final List<HSLFComment> comments = new ArrayList<>();
		final RecordContainer binaryTags = HSLFSlide.selectContainer(getSheetContainer(), 0, RecordTypes.ProgTags, RecordTypes.ProgBinaryTag, RecordTypes.BinaryTagData);
		if (binaryTags != null) {
			for (final Record record : binaryTags.getChildRecords()) {
				if (record instanceof Comment2000) {
					comments.add(new HSLFComment(((Comment2000) (record))));
				}
			}
		}
		return comments;
	}

	public HeadersFooters getHeadersFooters() {
		return new HeadersFooters(this, HeadersFootersContainer.SlideHeadersFootersContainer);
	}

	@Override
	protected void onAddTextShape(HSLFTextShape shape) {
		List<HSLFTextParagraph> newParas = shape.getTextParagraphs();
		_paragraphs.add(newParas);
	}

	public StyleTextProp9Atom[] getNumberedListInfo() {
		return this.getPPDrawing().getNumberedListInfo();
	}

	public EscherTextboxWrapper[] getTextboxWrappers() {
		return this.getPPDrawing().getTextboxWrappers();
	}

	@Override
	public void setHidden(boolean hidden) {
		org.apache.poi.hslf.record.Slide cont = getSlideRecord();
		SSSlideInfoAtom slideInfo = ((SSSlideInfoAtom) (cont.findFirstOfType(RecordTypes.SSSlideInfoAtom.typeID)));
		if (slideInfo == null) {
			slideInfo = new SSSlideInfoAtom();
			cont.addChildAfter(slideInfo, cont.findFirstOfType(RecordTypes.SlideAtom.typeID));
		}
		slideInfo.setEffectTransitionFlagByBit(SSSlideInfoAtom.HIDDEN_BIT, hidden);
	}

	@Override
	public boolean isHidden() {
		SSSlideInfoAtom slideInfo = ((SSSlideInfoAtom) (getSlideRecord().findFirstOfType(RecordTypes.SSSlideInfoAtom.typeID)));
		return (slideInfo != null) && (slideInfo.getEffectTransitionFlagByBit(SSSlideInfoAtom.HIDDEN_BIT));
	}

	@Override
	public void draw(Graphics2D graphics) {
		DrawFactory drawFact = DrawFactory.getInstance(graphics);
		Drawable draw = drawFact.getDrawable(this);
		draw.draw(graphics);
	}

	@Override
	public boolean getFollowMasterColourScheme() {
		return false;
	}

	@Override
	public void setFollowMasterColourScheme(boolean follow) {
	}

	@Override
	public boolean getFollowMasterGraphics() {
		return getFollowMasterObjects();
	}

	@Override
	public boolean getDisplayPlaceholder(final Placeholder placeholder) {
		final HeadersFooters hf = getHeadersFooters();
		final SlideAtomLayout.SlideLayoutType slt = getSlideRecord().getSlideAtom().getSSlideLayoutAtom().getGeometryType();
		final boolean isTitle = ((slt == (TITLE_SLIDE)) || (slt == (TITLE_ONLY))) || (slt == (MASTER_TITLE));
		switch (placeholder) {
			case DATETIME :
				return (hf.isDateTimeVisible()) && (!isTitle);
			case SLIDE_NUMBER :
				return (hf.isSlideNumberVisible()) && (!isTitle);
			case HEADER :
				return (hf.isHeaderVisible()) && (!isTitle);
			case FOOTER :
				return (hf.isFooterVisible()) && (!isTitle);
			default :
				return false;
		}
	}

	@Override
	public HSLFMasterSheet getSlideLayout() {
		return getMasterSheet();
	}
}

