

import java.awt.Dimension;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.poi.ooxml.POIXMLDocument;
import org.apache.poi.ooxml.POIXMLDocumentPart;
import org.apache.poi.ooxml.POIXMLException;
import org.apache.poi.ooxml.POIXMLRelation;
import org.apache.poi.ooxml.POIXMLTypeLoader;
import org.apache.poi.ooxml.extractor.POIXMLPropertiesTextExtractor;
import org.apache.poi.ooxml.util.PackageHelper;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.openxml4j.opc.PackagePartName;
import org.apache.poi.openxml4j.opc.PackageRelationship;
import org.apache.poi.sl.usermodel.MasterSheet;
import org.apache.poi.sl.usermodel.PictureData;
import org.apache.poi.sl.usermodel.Resources;
import org.apache.poi.sl.usermodel.SlideShow;
import org.apache.poi.util.Beta;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.LittleEndian;
import org.apache.poi.util.LittleEndianConsts;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;
import org.apache.poi.util.Units;
import org.apache.poi.xddf.usermodel.chart.XDDFChart;
import org.apache.poi.xslf.usermodel.SlideLayout;
import org.apache.poi.xslf.usermodel.XSLFChart;
import org.apache.poi.xslf.usermodel.XSLFCommentAuthors;
import org.apache.poi.xslf.usermodel.XSLFFactory;
import org.apache.poi.xslf.usermodel.XSLFNotes;
import org.apache.poi.xslf.usermodel.XSLFNotesMaster;
import org.apache.poi.xslf.usermodel.XSLFPictureData;
import org.apache.poi.xslf.usermodel.XSLFRelation;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSheet;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFSlideLayout;
import org.apache.poi.xslf.usermodel.XSLFSlideMaster;
import org.apache.poi.xslf.usermodel.XSLFTableStyles;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTheme;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.XmlTokenSource;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextParagraphProperties;
import org.openxmlformats.schemas.presentationml.x2006.main.CTNotesMasterIdList;
import org.openxmlformats.schemas.presentationml.x2006.main.CTNotesMasterIdListEntry;
import org.openxmlformats.schemas.presentationml.x2006.main.CTPresentation;
import org.openxmlformats.schemas.presentationml.x2006.main.CTSlideIdList;
import org.openxmlformats.schemas.presentationml.x2006.main.CTSlideIdListEntry;
import org.openxmlformats.schemas.presentationml.x2006.main.CTSlideMasterIdList;
import org.openxmlformats.schemas.presentationml.x2006.main.CTSlideMasterIdListEntry;
import org.openxmlformats.schemas.presentationml.x2006.main.CTSlideSize;
import org.openxmlformats.schemas.presentationml.x2006.main.PresentationDocument;

import static org.openxmlformats.schemas.presentationml.x2006.main.CTSlideSize.Factory.newInstance;
import static org.openxmlformats.schemas.presentationml.x2006.main.PresentationDocument.Factory.parse;


@Beta
public class XMLSlideShow extends POIXMLDocument implements SlideShow<XSLFShape, XSLFTextParagraph> {
	private static final POILogger LOG = POILogFactory.getLogger(XMLSlideShow.class);

	private static final int MAX_RECORD_LENGTH = 1000000;

	private CTPresentation _presentation;

	private List<XSLFSlide> _slides;

	private List<XSLFSlideMaster> _masters;

	private List<XSLFPictureData> _pictures;

	private List<XSLFChart> _charts;

	private XSLFTableStyles _tableStyles;

	private XSLFNotesMaster _notesMaster;

	private XSLFCommentAuthors _commentAuthors;

	public XMLSlideShow() {
		this(XMLSlideShow.empty());
	}

	public XMLSlideShow(OPCPackage pkg) {
		super(pkg);
		try {
			if (getCorePart().getContentType().equals(XSLFRelation.THEME_MANAGER.getContentType())) {
				rebase(getPackage());
			}
			load(XSLFFactory.getInstance());
		} catch (Exception e) {
			throw new POIXMLException(e);
		}
	}

	public XMLSlideShow(InputStream is) throws IOException {
		this(PackageHelper.open(is));
	}

	static OPCPackage empty() {
		InputStream is = XMLSlideShow.class.getResourceAsStream("empty.pptx");
		if (is == null) {
			throw new POIXMLException("Missing resource 'empty.pptx'");
		}
		try {
			return OPCPackage.open(is);
		} catch (Exception e) {
			throw new POIXMLException(e);
		} finally {
			IOUtils.closeQuietly(is);
		}
	}

	@Override
	protected void onDocumentRead() throws IOException {
		try {
			PresentationDocument doc = parse(getCorePart().getInputStream(), POIXMLTypeLoader.DEFAULT_XML_OPTIONS);
			_presentation = doc.getPresentation();
			Map<String, XSLFSlideMaster> masterMap = new HashMap<>();
			Map<String, XSLFSlide> shIdMap = new HashMap<>();
			Map<String, XSLFChart> chartMap = new HashMap<>();
			for (POIXMLDocumentPart.RelationPart rp : getRelationParts()) {
				POIXMLDocumentPart p = rp.getDocumentPart();
				if (p instanceof XSLFSlide) {
					shIdMap.put(rp.getRelationship().getId(), ((XSLFSlide) (p)));
					for (POIXMLDocumentPart c : p.getRelations()) {
						if (c instanceof XSLFChart) {
							chartMap.put(c.getPackagePart().getPartName().getName(), ((XSLFChart) (c)));
						}
					}
				}else
					if (p instanceof XSLFSlideMaster) {
						masterMap.put(getRelationId(p), ((XSLFSlideMaster) (p)));
					}else
						if (p instanceof XSLFTableStyles) {
							_tableStyles = ((XSLFTableStyles) (p));
						}else
							if (p instanceof XSLFNotesMaster) {
								_notesMaster = ((XSLFNotesMaster) (p));
							}else
								if (p instanceof XSLFCommentAuthors) {
									_commentAuthors = ((XSLFCommentAuthors) (p));
								}




			}
			_charts = new ArrayList<>(chartMap.size());
			for (XSLFChart chart : chartMap.values()) {
				_charts.add(chart);
			}
			_masters = new ArrayList<>(masterMap.size());
			for (CTSlideMasterIdListEntry masterId : _presentation.getSldMasterIdLst().getSldMasterIdList()) {
				XSLFSlideMaster master = masterMap.get(masterId.getId2());
				_masters.add(master);
			}
			_slides = new ArrayList<>(shIdMap.size());
			if (_presentation.isSetSldIdLst()) {
				for (CTSlideIdListEntry slId : _presentation.getSldIdLst().getSldIdList()) {
					XSLFSlide sh = shIdMap.get(slId.getId2());
					if (sh == null) {
						XMLSlideShow.LOG.log(POILogger.WARN, (("Slide with r:id " + (slId.getId())) + " was defined, but didn't exist in package, skipping"));
						continue;
					}
					_slides.add(sh);
				}
			}
		} catch (XmlException e) {
			throw new POIXMLException(e);
		}
	}

	@Override
	protected void commit() throws IOException {
		PackagePart part = getPackagePart();
		OutputStream out = part.getOutputStream();
		_presentation.save(out, POIXMLTypeLoader.DEFAULT_XML_OPTIONS);
		out.close();
	}

	@Override
	public List<PackagePart> getAllEmbeddedParts() throws OpenXML4JException {
		return Collections.unmodifiableList(getPackage().getPartsByName(Pattern.compile("/ppt/embeddings/.*?")));
	}

	@Override
	public List<XSLFPictureData> getPictureData() {
		if ((_pictures) == null) {
			List<PackagePart> mediaParts = getPackage().getPartsByName(Pattern.compile("/ppt/media/.*?"));
			_pictures = new ArrayList<>(mediaParts.size());
			for (PackagePart part : mediaParts) {
				XSLFPictureData pd = new XSLFPictureData(part);
				pd.setIndex(_pictures.size());
				_pictures.add(pd);
			}
		}
		return Collections.unmodifiableList(_pictures);
	}

	public XSLFSlide createSlide(XSLFSlideLayout layout) {
		int slideNumber = 256;
		int cnt = 1;
		CTSlideIdList slideList;
		XSLFRelation relationType = XSLFRelation.SLIDE;
		if (!(_presentation.isSetSldIdLst())) {
			slideList = _presentation.addNewSldIdLst();
		}else {
			slideList = _presentation.getSldIdLst();
			for (CTSlideIdListEntry slideId : slideList.getSldIdArray()) {
				slideNumber = ((int) (Math.max(((slideId.getId()) + 1), slideNumber)));
				cnt++;
			}
			cnt = findNextAvailableFileNameIndex(relationType, cnt);
		}
		POIXMLDocumentPart.RelationPart rp = createRelationship(relationType, XSLFFactory.getInstance(), cnt, false);
		XSLFSlide slide = rp.getDocumentPart();
		CTSlideIdListEntry slideId = slideList.addNewSldId();
		slideId.setId(slideNumber);
		slideId.setId2(rp.getRelationship().getId());
		layout.copyLayout(slide);
		slide.getPackagePart().clearRelationships();
		slide.addRelation(null, XSLFRelation.SLIDE_LAYOUT, layout);
		_slides.add(slide);
		return slide;
	}

	private int findNextAvailableFileNameIndex(XSLFRelation relationType, int idx) {
		while (true) {
			String fileName = relationType.getFileName(idx);
			boolean found = false;
			for (POIXMLDocumentPart relation : getRelations()) {
				if (((relation.getPackagePart()) != null) && (fileName.equals(relation.getPackagePart().getPartName().getName()))) {
					found = true;
					break;
				}
			}
			if ((!found) && ((getPackage().getPartsByName(Pattern.compile(Pattern.quote(fileName))).size()) > 0)) {
				found = true;
			}
			if (!found) {
				break;
			}
			idx++;
		} 
		return idx;
	}

	@Override
	public XSLFSlide createSlide() {
		XSLFSlideMaster sm = _masters.get(0);
		XSLFSlideLayout layout = sm.getLayout(SlideLayout.BLANK);
		if (layout == null) {
			XMLSlideShow.LOG.log(POILogger.WARN, "Blank layout was not found - defaulting to first slide layout in master");
			XSLFSlideLayout[] sl = sm.getSlideLayouts();
			if ((sl.length) == 0) {
				throw new POIXMLException("SlideMaster must contain a SlideLayout.");
			}
			layout = sl[0];
		}
		return createSlide(layout);
	}

	public XSLFChart createChart(XSLFSlide slide) {
		int chartIdx = findNextAvailableFileNameIndex(XSLFRelation.CHART, ((_charts.size()) + 1));
		XSLFChart chart = ((XSLFChart) (createRelationship(XSLFRelation.CHART, XSLFFactory.getInstance(), chartIdx, true).getDocumentPart()));
		slide.addRelation(null, XSLFRelation.CHART, chart);
		chart.setChartIndex(chartIdx);
		_charts.add(chart);
		return chart;
	}

	public XSLFNotes getNotesSlide(XSLFSlide slide) {
		XSLFNotes notesSlide = slide.getNotes();
		if (notesSlide == null) {
			notesSlide = createNotesSlide(slide);
		}
		return notesSlide;
	}

	private XSLFNotes createNotesSlide(XSLFSlide slide) {
		if ((_notesMaster) == null) {
			createNotesMaster();
		}
		int slideIndex = XSLFRelation.SLIDE.getFileNameIndex(slide);
		XSLFRelation relationType = XSLFRelation.NOTES;
		slideIndex = findNextAvailableFileNameIndex(relationType, slideIndex);
		XSLFNotes notesSlide = ((XSLFNotes) (createRelationship(relationType, XSLFFactory.getInstance(), slideIndex)));
		slide.addRelation(null, relationType, notesSlide);
		notesSlide.addRelation(null, XSLFRelation.NOTES_MASTER, _notesMaster);
		notesSlide.addRelation(null, XSLFRelation.SLIDE, slide);
		notesSlide.importContent(_notesMaster);
		return notesSlide;
	}

	public void createNotesMaster() {
		POIXMLDocumentPart.RelationPart rp = createRelationship(XSLFRelation.NOTES_MASTER, XSLFFactory.getInstance(), 1, false);
		_notesMaster = rp.getDocumentPart();
		CTNotesMasterIdList notesMasterIdList = _presentation.addNewNotesMasterIdLst();
		CTNotesMasterIdListEntry notesMasterId = notesMasterIdList.addNewNotesMasterId();
		notesMasterId.setId(rp.getRelationship().getId());
		int themeIndex = 1;
		List<Integer> themeIndexList = new ArrayList<>();
		for (POIXMLDocumentPart p : getRelations()) {
			if (p instanceof XSLFTheme) {
				themeIndexList.add(XSLFRelation.THEME.getFileNameIndex(p));
			}
		}
		if (!(themeIndexList.isEmpty())) {
			boolean found = false;
			for (int i = 1; i <= (themeIndexList.size()); i++) {
				if (!(themeIndexList.contains(i))) {
					found = true;
					themeIndex = i;
				}
			}
			if (!found) {
				themeIndex = (themeIndexList.size()) + 1;
			}
		}
		XSLFTheme theme = ((XSLFTheme) (createRelationship(XSLFRelation.THEME, XSLFFactory.getInstance(), themeIndex)));
		theme.importTheme(getSlides().get(0).getTheme());
		_notesMaster.addRelation(null, XSLFRelation.THEME, theme);
	}

	public XSLFNotesMaster getNotesMaster() {
		return _notesMaster;
	}

	@Override
	public List<XSLFSlideMaster> getSlideMasters() {
		return _masters;
	}

	@Override
	public List<XSLFSlide> getSlides() {
		return _slides;
	}

	public List<XSLFChart> getCharts() {
		return _charts;
	}

	public XSLFCommentAuthors getCommentAuthors() {
		return _commentAuthors;
	}

	public void setSlideOrder(XSLFSlide slide, int newIndex) {
		int oldIndex = _slides.indexOf(slide);
		if (oldIndex == (-1)) {
			throw new IllegalArgumentException("Slide not found");
		}
		if (oldIndex == newIndex) {
			return;
		}
		_slides.add(newIndex, _slides.remove(oldIndex));
		CTSlideIdList sldIdLst = _presentation.getSldIdLst();
		CTSlideIdListEntry[] entries = sldIdLst.getSldIdArray();
		CTSlideIdListEntry oldEntry = entries[oldIndex];
		if (oldIndex < newIndex) {
			System.arraycopy(entries, (oldIndex + 1), entries, oldIndex, (newIndex - oldIndex));
		}else {
			System.arraycopy(entries, newIndex, entries, (newIndex + 1), (oldIndex - newIndex));
		}
		entries[newIndex] = oldEntry;
		sldIdLst.setSldIdArray(entries);
	}

	public XSLFSlide removeSlide(int index) {
		XSLFSlide slide = _slides.remove(index);
		removeRelation(slide);
		_presentation.getSldIdLst().removeSldId(index);
		for (POIXMLDocumentPart p : slide.getRelations()) {
			if (p instanceof XSLFChart) {
				XSLFChart chart = ((XSLFChart) (p));
				_charts.remove(chart);
			}else
				if (p instanceof XSLFSlideLayout) {
					XSLFSlideLayout layout = ((XSLFSlideLayout) (p));
				}

		}
		return slide;
	}

	@Override
	public Dimension getPageSize() {
		CTSlideSize sz = _presentation.getSldSz();
		int cx = sz.getCx();
		int cy = sz.getCy();
		return new Dimension(((int) (Units.toPoints(cx))), ((int) (Units.toPoints(cy))));
	}

	@Override
	public void setPageSize(Dimension pgSize) {
		CTSlideSize sz = newInstance();
		sz.setCx(Units.toEMU(pgSize.getWidth()));
		sz.setCy(Units.toEMU(pgSize.getHeight()));
		_presentation.setSldSz(sz);
	}

	@org.apache.poi.util.Internal
	public CTPresentation getCTPresentation() {
		return _presentation;
	}

	@Override
	public XSLFPictureData addPicture(byte[] pictureData, PictureData.PictureType format) {
		XSLFPictureData img = findPictureData(pictureData);
		if (img != null) {
			return img;
		}
		int imageNumber = _pictures.size();
		img.setIndex(imageNumber);
		_pictures.add(img);
		try (final OutputStream out = img.getPackagePart().getOutputStream()) {
			out.write(pictureData);
		} catch (IOException e) {
			throw new POIXMLException(e);
		}
		return img;
	}

	@Override
	public XSLFPictureData addPicture(InputStream is, PictureData.PictureType format) throws IOException {
		return addPicture(IOUtils.toByteArray(is), format);
	}

	@Override
	public XSLFPictureData addPicture(File pict, PictureData.PictureType format) throws IOException {
		int length = ((int) (pict.length()));
		byte[] data = IOUtils.safelyAllocate(length, XMLSlideShow.MAX_RECORD_LENGTH);
		try (InputStream is = new FileInputStream(pict)) {
			IOUtils.readFully(is, data);
		}
		return addPicture(data, format);
	}

	@Override
	public XSLFPictureData findPictureData(byte[] pictureData) {
		long checksum = IOUtils.calculateChecksum(pictureData);
		byte[] cs = new byte[LittleEndianConsts.LONG_SIZE];
		LittleEndian.putLong(cs, 0, checksum);
		for (XSLFPictureData pic : getPictureData()) {
			if (Arrays.equals(pic.getChecksum(), cs)) {
				return pic;
			}
		}
		return null;
	}

	public XSLFSlideLayout findLayout(String name) {
		for (XSLFSlideMaster master : getSlideMasters()) {
			XSLFSlideLayout layout = master.getLayout(name);
			if (layout != null) {
				return layout;
			}
		}
		return null;
	}

	public XSLFTableStyles getTableStyles() {
		return _tableStyles;
	}

	CTTextParagraphProperties getDefaultParagraphStyle(int level) {
		XmlObject[] o = _presentation.selectPath(((("declare namespace p='http://schemas.openxmlformats.org/presentationml/2006/main' " + ("declare namespace a='http://schemas.openxmlformats.org/drawingml/2006/main' " + ".//p:defaultTextStyle/a:lvl")) + (level + 1)) + "pPr"));
		if ((o.length) == 1) {
			return ((CTTextParagraphProperties) (o[0]));
		}
		return null;
	}

	@Override
	public MasterSheet<XSLFShape, XSLFTextParagraph> createMasterSheet() throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Resources getResources() {
		throw new UnsupportedOperationException();
	}

	@Override
	public POIXMLPropertiesTextExtractor getMetadataTextExtractor() {
		return new POIXMLPropertiesTextExtractor(this);
	}

	@Override
	public Object getPersistDocument() {
		return this;
	}
}

