

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.poi.ddf.AbstractEscherOptRecord;
import org.apache.poi.ddf.EscherContainerRecord;
import org.apache.poi.ddf.EscherProperties;
import org.apache.poi.ddf.EscherSimpleProperty;
import org.apache.poi.ddf.EscherTextboxRecord;
import org.apache.poi.hslf.exceptions.HSLFException;
import org.apache.poi.hslf.model.HSLFMetroShape;
import org.apache.poi.hslf.model.textproperties.TextPropCollection;
import org.apache.poi.hslf.record.EscherTextboxWrapper;
import org.apache.poi.hslf.record.OEPlaceholderAtom;
import org.apache.poi.hslf.record.PPDrawing;
import org.apache.poi.hslf.record.Record;
import org.apache.poi.hslf.record.RecordContainer;
import org.apache.poi.hslf.record.RecordTypes;
import org.apache.poi.hslf.record.RoundTripHFPlaceholder12;
import org.apache.poi.hslf.record.StyleTextPropAtom;
import org.apache.poi.hslf.record.TextBytesAtom;
import org.apache.poi.hslf.record.TextCharsAtom;
import org.apache.poi.hslf.record.TextHeaderAtom;
import org.apache.poi.hslf.usermodel.HSLFGroupShape;
import org.apache.poi.hslf.usermodel.HSLFHyperlink;
import org.apache.poi.hslf.usermodel.HSLFMasterSheet;
import org.apache.poi.hslf.usermodel.HSLFShape;
import org.apache.poi.hslf.usermodel.HSLFSheet;
import org.apache.poi.hslf.usermodel.HSLFSimpleShape;
import org.apache.poi.hslf.usermodel.HSLFTextParagraph;
import org.apache.poi.hslf.usermodel.HSLFTextRun;
import org.apache.poi.sl.draw.DrawFactory;
import org.apache.poi.sl.draw.DrawTextShape;
import org.apache.poi.sl.usermodel.Insets2D;
import org.apache.poi.sl.usermodel.Placeholder;
import org.apache.poi.sl.usermodel.ShapeContainer;
import org.apache.poi.sl.usermodel.TextParagraph;
import org.apache.poi.sl.usermodel.TextRun;
import org.apache.poi.sl.usermodel.TextShape;
import org.apache.poi.sl.usermodel.VerticalAlignment;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;
import org.apache.poi.util.Units;

import static org.apache.poi.sl.usermodel.TextShape.TextDirection.HORIZONTAL;
import static org.apache.poi.sl.usermodel.TextShape.TextDirection.STACKED;
import static org.apache.poi.sl.usermodel.TextShape.TextDirection.VERTICAL;
import static org.apache.poi.sl.usermodel.TextShape.TextDirection.VERTICAL_270;
import static org.apache.poi.sl.usermodel.TextShape.TextPlaceholder.BODY;
import static org.apache.poi.sl.usermodel.TextShape.TextPlaceholder.CENTER_BODY;
import static org.apache.poi.sl.usermodel.TextShape.TextPlaceholder.CENTER_TITLE;
import static org.apache.poi.sl.usermodel.TextShape.TextPlaceholder.HALF_BODY;
import static org.apache.poi.sl.usermodel.TextShape.TextPlaceholder.NOTES;
import static org.apache.poi.sl.usermodel.TextShape.TextPlaceholder.OTHER;
import static org.apache.poi.sl.usermodel.TextShape.TextPlaceholder.QUARTER_BODY;
import static org.apache.poi.sl.usermodel.TextShape.TextPlaceholder.TITLE;


public abstract class HSLFTextShape extends HSLFSimpleShape implements TextShape<HSLFShape, HSLFTextParagraph> {
	private static final POILogger LOG = POILogFactory.getLogger(HSLFTextShape.class);

	private enum HSLFTextAnchor {

		TOP(0, VerticalAlignment.TOP, false, false),
		MIDDLE(1, VerticalAlignment.MIDDLE, false, false),
		BOTTOM(2, VerticalAlignment.BOTTOM, false, false),
		TOP_CENTER(3, VerticalAlignment.TOP, true, false),
		MIDDLE_CENTER(4, VerticalAlignment.MIDDLE, true, false),
		BOTTOM_CENTER(5, VerticalAlignment.BOTTOM, true, false),
		TOP_BASELINE(6, VerticalAlignment.TOP, false, true),
		BOTTOM_BASELINE(7, VerticalAlignment.BOTTOM, false, true),
		TOP_CENTER_BASELINE(8, VerticalAlignment.TOP, true, true),
		BOTTOM_CENTER_BASELINE(9, VerticalAlignment.BOTTOM, true, true);
		public final int nativeId;

		public final VerticalAlignment vAlign;

		public final boolean centered;

		public final Boolean baseline;

		HSLFTextAnchor(int nativeId, VerticalAlignment vAlign, boolean centered, Boolean baseline) {
			this.nativeId = nativeId;
			this.vAlign = vAlign;
			this.centered = centered;
			this.baseline = baseline;
		}

		static HSLFTextShape.HSLFTextAnchor fromNativeId(int nativeId) {
			for (HSLFTextShape.HSLFTextAnchor ta : HSLFTextShape.HSLFTextAnchor.values()) {
				if ((ta.nativeId) == nativeId) {
					return ta;
				}
			}
			return null;
		}
	}

	public static final int WrapSquare = 0;

	public static final int WrapByPoints = 1;

	public static final int WrapNone = 2;

	public static final int WrapTopBottom = 3;

	public static final int WrapThrough = 4;

	private List<HSLFTextParagraph> _paragraphs = new ArrayList<>();

	private EscherTextboxWrapper _txtbox;

	protected HSLFTextShape(EscherContainerRecord escherRecord, ShapeContainer<HSLFShape, HSLFTextParagraph> parent) {
		super(escherRecord, parent);
	}

	public HSLFTextShape(ShapeContainer<HSLFShape, HSLFTextParagraph> parent) {
		super(null, parent);
		createSpContainer((parent instanceof HSLFGroupShape));
	}

	public HSLFTextShape() {
		this(null);
	}

	protected void setDefaultTextProperties(HSLFTextParagraph _txtrun) {
	}

	@Override
	protected void afterInsert(HSLFSheet sh) {
		super.afterInsert(sh);
		storeText();
		EscherTextboxWrapper thisTxtbox = getEscherTextboxWrapper();
		if (thisTxtbox != null) {
			getSpContainer().addChildRecord(thisTxtbox.getEscherRecord());
			PPDrawing ppdrawing = sh.getPPDrawing();
			ppdrawing.addTextboxWrapper(thisTxtbox);
			try {
				thisTxtbox.writeOut(null);
			} catch (IOException e) {
				throw new HSLFException(e);
			}
			boolean isInitialAnchor = getAnchor().equals(new Rectangle2D.Double());
			boolean isFilledTxt = !("".equals(getText()));
			if (isInitialAnchor && isFilledTxt) {
				resizeToFitText();
			}
		}
		for (HSLFTextParagraph htp : _paragraphs) {
		}
	}

	protected EscherTextboxWrapper getEscherTextboxWrapper() {
		if ((_txtbox) != null) {
			return _txtbox;
		}
		EscherTextboxRecord textRecord = getEscherChild(EscherTextboxRecord.RECORD_ID);
		if (textRecord == null) {
			return null;
		}
		HSLFSheet sheet = getSheet();
		if (sheet != null) {
			PPDrawing drawing = sheet.getPPDrawing();
			if (drawing != null) {
				EscherTextboxWrapper[] wrappers = drawing.getTextboxWrappers();
				if (wrappers != null) {
					for (EscherTextboxWrapper w : wrappers) {
						if (textRecord == (w.getEscherRecord())) {
							_txtbox = w;
							return _txtbox;
						}
					}
				}
			}
		}
		_txtbox = new EscherTextboxWrapper(textRecord);
		return _txtbox;
	}

	private void createEmptyParagraph() {
		TextHeaderAtom tha = ((TextHeaderAtom) (_txtbox.findFirstOfType(TextHeaderAtom._type)));
		if (tha == null) {
			tha = new TextHeaderAtom();
			tha.setParentRecord(_txtbox);
			_txtbox.appendChildRecord(tha);
		}
		TextBytesAtom tba = ((TextBytesAtom) (_txtbox.findFirstOfType(TextBytesAtom._type)));
		TextCharsAtom tca = ((TextCharsAtom) (_txtbox.findFirstOfType(TextCharsAtom._type)));
		if ((tba == null) && (tca == null)) {
			tba = new TextBytesAtom();
			tba.setText(new byte[0]);
			_txtbox.appendChildRecord(tba);
		}
		final String text = (tba != null) ? tba.getText() : tca.getText();
		StyleTextPropAtom sta = ((StyleTextPropAtom) (_txtbox.findFirstOfType(StyleTextPropAtom._type)));
		TextPropCollection paraStyle = null;
		TextPropCollection charStyle = null;
		if (sta == null) {
			int parSiz = text.length();
			sta = new StyleTextPropAtom((parSiz + 1));
			if (_paragraphs.isEmpty()) {
				paraStyle = sta.addParagraphTextPropCollection((parSiz + 1));
				charStyle = sta.addCharacterTextPropCollection((parSiz + 1));
			}else {
				for (HSLFTextParagraph htp : _paragraphs) {
					int runsLen = 0;
					for (HSLFTextRun htr : htp.getTextRuns()) {
						runsLen += htr.getLength();
						charStyle = sta.addCharacterTextPropCollection(htr.getLength());
						htr.setCharacterStyle(charStyle);
					}
					paraStyle = sta.addParagraphTextPropCollection(runsLen);
					htp.setParagraphStyle(paraStyle);
				}
				assert (paraStyle != null) && (charStyle != null);
			}
			_txtbox.appendChildRecord(sta);
		}else {
			paraStyle = sta.getParagraphStyles().get(0);
			charStyle = sta.getCharacterStyles().get(0);
		}
		if (_paragraphs.isEmpty()) {
		}
	}

	@Override
	public Rectangle2D resizeToFitText() {
		return resizeToFitText(null);
	}

	@Override
	public Rectangle2D resizeToFitText(Graphics2D graphics) {
		Rectangle2D anchor = getAnchor();
		if ((anchor.getWidth()) == 0.0) {
			HSLFTextShape.LOG.log(POILogger.WARN, "Width of shape wasn't set. Defaulting to 200px");
			anchor.setRect(anchor.getX(), anchor.getY(), 200.0, anchor.getHeight());
			setAnchor(anchor);
		}
		double height = getTextHeight(graphics);
		height += 1;
		Insets2D insets = getInsets();
		anchor.setRect(anchor.getX(), anchor.getY(), anchor.getWidth(), ((height + (insets.top)) + (insets.bottom)));
		setAnchor(anchor);
		return anchor;
	}

	public int getRunType() {
		getEscherTextboxWrapper();
		if ((_txtbox) == null) {
			return -1;
		}
		return 0;
	}

	public void setRunType(int type) {
		getEscherTextboxWrapper();
		if ((_txtbox) == null) {
			return;
		}
	}

	HSLFTextShape.HSLFTextAnchor getAlignment() {
		AbstractEscherOptRecord opt = getEscherOptRecord();
		EscherSimpleProperty prop = HSLFShape.getEscherProperty(opt, EscherProperties.TEXT__ANCHORTEXT);
		HSLFTextShape.HSLFTextAnchor align = HSLFTextShape.HSLFTextAnchor.TOP;
		if (prop == null) {
			int type = getRunType();
			HSLFSheet sh = getSheet();
			HSLFMasterSheet master = (sh != null) ? sh.getMasterSheet() : null;
		}else {
			align = HSLFTextShape.HSLFTextAnchor.fromNativeId(prop.getPropertyValue());
		}
		if (align == null) {
			align = HSLFTextShape.HSLFTextAnchor.TOP;
		}
		return align;
	}

	void setAlignment(Boolean isCentered, VerticalAlignment vAlign, boolean baseline) {
		for (HSLFTextShape.HSLFTextAnchor hta : HSLFTextShape.HSLFTextAnchor.values()) {
			if ((((hta.centered) == ((isCentered != null) && isCentered)) && ((hta.vAlign) == vAlign)) && (((hta.baseline) == null) || ((hta.baseline) == baseline))) {
				setEscherProperty(EscherProperties.TEXT__ANCHORTEXT, hta.nativeId);
				break;
			}
		}
	}

	public boolean isAlignToBaseline() {
		return getAlignment().baseline;
	}

	public void setAlignToBaseline(boolean alignToBaseline) {
		setAlignment(isHorizontalCentered(), getVerticalAlignment(), alignToBaseline);
	}

	@Override
	public boolean isHorizontalCentered() {
		return getAlignment().centered;
	}

	@Override
	public void setHorizontalCentered(Boolean isCentered) {
		setAlignment(isCentered, getVerticalAlignment(), getAlignment().baseline);
	}

	@Override
	public VerticalAlignment getVerticalAlignment() {
		return getAlignment().vAlign;
	}

	@Override
	public void setVerticalAlignment(VerticalAlignment vAlign) {
		setAlignment(isHorizontalCentered(), vAlign, getAlignment().baseline);
	}

	public double getBottomInset() {
		return getInset(EscherProperties.TEXT__TEXTBOTTOM, 0.05);
	}

	public void setBottomInset(double margin) {
		setInset(EscherProperties.TEXT__TEXTBOTTOM, margin);
	}

	public double getLeftInset() {
		return getInset(EscherProperties.TEXT__TEXTLEFT, 0.1);
	}

	public void setLeftInset(double margin) {
		setInset(EscherProperties.TEXT__TEXTLEFT, margin);
	}

	public double getRightInset() {
		return getInset(EscherProperties.TEXT__TEXTRIGHT, 0.1);
	}

	public void setRightInset(double margin) {
		setInset(EscherProperties.TEXT__TEXTRIGHT, margin);
	}

	public double getTopInset() {
		return getInset(EscherProperties.TEXT__TEXTTOP, 0.05);
	}

	public void setTopInset(double margin) {
		setInset(EscherProperties.TEXT__TEXTTOP, margin);
	}

	private double getInset(short propId, double defaultInch) {
		AbstractEscherOptRecord opt = getEscherOptRecord();
		EscherSimpleProperty prop = HSLFShape.getEscherProperty(opt, propId);
		int val = (prop == null) ? ((int) ((Units.toEMU(Units.POINT_DPI)) * defaultInch)) : prop.getPropertyValue();
		return Units.toPoints(val);
	}

	private void setInset(short propId, double margin) {
		setEscherProperty(propId, Units.toEMU(margin));
	}

	public int getWordWrapEx() {
		AbstractEscherOptRecord opt = getEscherOptRecord();
		EscherSimpleProperty prop = HSLFShape.getEscherProperty(opt, EscherProperties.TEXT__WRAPTEXT);
		return prop == null ? HSLFTextShape.WrapSquare : prop.getPropertyValue();
	}

	public void setWordWrapEx(int wrap) {
		setEscherProperty(EscherProperties.TEXT__WRAPTEXT, wrap);
	}

	@Override
	public boolean getWordWrap() {
		int ww = getWordWrapEx();
		return ww != (HSLFTextShape.WrapNone);
	}

	@Override
	public void setWordWrap(boolean wrap) {
		setWordWrapEx((wrap ? HSLFTextShape.WrapSquare : HSLFTextShape.WrapNone));
	}

	public int getTextId() {
		AbstractEscherOptRecord opt = getEscherOptRecord();
		EscherSimpleProperty prop = HSLFShape.getEscherProperty(opt, EscherProperties.TEXT__TEXTID);
		return prop == null ? 0 : prop.getPropertyValue();
	}

	public void setTextId(int id) {
		setEscherProperty(EscherProperties.TEXT__TEXTID, id);
	}

	@Override
	public List<HSLFTextParagraph> getTextParagraphs() {
		if (!(_paragraphs.isEmpty())) {
			return _paragraphs;
		}
		_txtbox = getEscherTextboxWrapper();
		if ((_txtbox) == null) {
			_txtbox = new EscherTextboxWrapper();
			createEmptyParagraph();
		}else {
			if (_paragraphs.isEmpty()) {
				HSLFTextShape.LOG.log(POILogger.WARN, "TextRecord didn't contained any text lines");
			}
		}
		for (HSLFTextParagraph p : _paragraphs) {
		}
		return _paragraphs;
	}

	@Override
	public void setSheet(HSLFSheet sheet) {
		super.setSheet(sheet);
		List<HSLFTextParagraph> ltp = getTextParagraphs();
		HSLFTextParagraph.supplySheet(ltp, sheet);
	}

	public OEPlaceholderAtom getPlaceholderAtom() {
		return getClientDataRecord(RecordTypes.OEPlaceholderAtom.typeID);
	}

	public RoundTripHFPlaceholder12 getHFPlaceholderAtom() {
		return getClientDataRecord(RecordTypes.RoundTripHFPlaceholder12.typeID);
	}

	@Override
	public boolean isPlaceholder() {
		return ((getPlaceholderAtom()) != null) || ((getHFPlaceholderAtom()) != null);
	}

	@Override
	public Iterator<HSLFTextParagraph> iterator() {
		return _paragraphs.iterator();
	}

	@Override
	public Insets2D getInsets() {
		return new Insets2D(getTopInset(), getLeftInset(), getBottomInset(), getRightInset());
	}

	@Override
	public void setInsets(Insets2D insets) {
		setTopInset(insets.top);
		setLeftInset(insets.left);
		setBottomInset(insets.bottom);
		setRightInset(insets.right);
	}

	@Override
	public double getTextHeight() {
		return getTextHeight(null);
	}

	@Override
	public double getTextHeight(Graphics2D graphics) {
		DrawFactory drawFact = DrawFactory.getInstance(graphics);
		DrawTextShape dts = drawFact.getDrawable(this);
		return dts.getTextHeight(graphics);
	}

	@Override
	public TextShape.TextDirection getTextDirection() {
		AbstractEscherOptRecord opt = getEscherOptRecord();
		EscherSimpleProperty prop = HSLFShape.getEscherProperty(opt, EscherProperties.TEXT__TEXTFLOW);
		int msotxfl = (prop == null) ? 0 : prop.getPropertyValue();
		switch (msotxfl) {
			default :
			case 0 :
			case 4 :
				return HORIZONTAL;
			case 1 :
			case 3 :
			case 5 :
				return VERTICAL;
			case 2 :
				return VERTICAL_270;
		}
	}

	@Override
	public void setTextDirection(TextShape.TextDirection orientation) {
		AbstractEscherOptRecord opt = getEscherOptRecord();
		int msotxfl;
		if (orientation == null) {
			msotxfl = -1;
		}else {
			switch (orientation) {
				default :
				case STACKED :
					msotxfl = -1;
					break;
				case HORIZONTAL :
					msotxfl = 0;
					break;
				case VERTICAL :
					msotxfl = 1;
					break;
				case VERTICAL_270 :
					msotxfl = 2;
					break;
			}
		}
		HSLFShape.setEscherProperty(opt, EscherProperties.TEXT__TEXTFLOW, msotxfl);
	}

	@Override
	public Double getTextRotation() {
		AbstractEscherOptRecord opt = getEscherOptRecord();
		EscherSimpleProperty prop = HSLFShape.getEscherProperty(opt, EscherProperties.TEXT__FONTROTATION);
		return prop == null ? null : 90.0 * (prop.getPropertyValue());
	}

	@Override
	public void setTextRotation(Double rotation) {
		AbstractEscherOptRecord opt = getEscherOptRecord();
		if (rotation == null) {
			opt.removeEscherProperty(EscherProperties.TEXT__FONTROTATION);
		}else {
			int rot = ((int) ((Math.round((rotation / 90.0))) % 4L));
			setEscherProperty(EscherProperties.TEXT__FONTROTATION, rot);
		}
	}

	public String getRawText() {
		return HSLFTextParagraph.getRawText(getTextParagraphs());
	}

	@Override
	public String getText() {
		String rawText = getRawText();
		return HSLFTextParagraph.toExternalString(rawText, getRunType());
	}

	@Override
	public HSLFTextRun appendText(String text, boolean newParagraph) {
		List<HSLFTextParagraph> paras = getTextParagraphs();
		setTextId(getRawText().hashCode());
		return null;
	}

	@Override
	public HSLFTextRun setText(String text) {
		List<HSLFTextParagraph> paras = getTextParagraphs();
		HSLFTextRun htr = HSLFTextParagraph.setText(paras, text);
		setTextId(getRawText().hashCode());
		return htr;
	}

	protected void storeText() {
		List<HSLFTextParagraph> paras = getTextParagraphs();
	}

	public List<HSLFHyperlink> getHyperlinks() {
		return null;
	}

	@Override
	public void setTextPlaceholder(TextShape.TextPlaceholder placeholder) {
		Placeholder ph = null;
		int runType;
		switch (placeholder) {
			default :
			case BODY :
				runType = TextHeaderAtom.BODY_TYPE;
				ph = Placeholder.BODY;
				break;
			case TITLE :
				runType = TextHeaderAtom.TITLE_TYPE;
				ph = Placeholder.TITLE;
				break;
			case CENTER_BODY :
				runType = TextHeaderAtom.CENTRE_BODY_TYPE;
				ph = Placeholder.BODY;
				break;
			case CENTER_TITLE :
				runType = TextHeaderAtom.CENTER_TITLE_TYPE;
				ph = Placeholder.TITLE;
				break;
			case HALF_BODY :
				runType = TextHeaderAtom.HALF_BODY_TYPE;
				ph = Placeholder.BODY;
				break;
			case QUARTER_BODY :
				runType = TextHeaderAtom.QUARTER_BODY_TYPE;
				ph = Placeholder.BODY;
				break;
			case NOTES :
				runType = TextHeaderAtom.NOTES_TYPE;
				break;
			case OTHER :
				runType = TextHeaderAtom.OTHER_TYPE;
				break;
		}
		setRunType(runType);
		if (ph != null) {
			setPlaceholder(ph);
		}
	}

	@Override
	public TextShape.TextPlaceholder getTextPlaceholder() {
		switch (getRunType()) {
			default :
			case TextHeaderAtom.BODY_TYPE :
				return BODY;
			case TextHeaderAtom.TITLE_TYPE :
				return TITLE;
			case TextHeaderAtom.NOTES_TYPE :
				return NOTES;
			case TextHeaderAtom.OTHER_TYPE :
				return OTHER;
			case TextHeaderAtom.CENTRE_BODY_TYPE :
				return CENTER_BODY;
			case TextHeaderAtom.CENTER_TITLE_TYPE :
				return CENTER_TITLE;
			case TextHeaderAtom.HALF_BODY_TYPE :
				return HALF_BODY;
			case TextHeaderAtom.QUARTER_BODY_TYPE :
				return QUARTER_BODY;
		}
	}

	public TextShape<?, ? extends TextParagraph<?, ?, ? extends TextRun>> getMetroShape() {
		HSLFMetroShape<TextShape<?, ? extends TextParagraph<?, ?, ? extends TextRun>>> mbs = new HSLFMetroShape<>(this);
		return mbs.getShape();
	}
}

