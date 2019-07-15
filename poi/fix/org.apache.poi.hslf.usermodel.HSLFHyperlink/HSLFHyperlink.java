

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.hslf.record.Document;
import org.apache.poi.hslf.record.ExHyperlink;
import org.apache.poi.hslf.record.ExHyperlinkAtom;
import org.apache.poi.hslf.record.ExObjList;
import org.apache.poi.hslf.record.InteractiveInfo;
import org.apache.poi.hslf.record.InteractiveInfoAtom;
import org.apache.poi.hslf.record.Record;
import org.apache.poi.hslf.record.TxInteractiveInfoAtom;
import org.apache.poi.hslf.usermodel.HSLFShape;
import org.apache.poi.hslf.usermodel.HSLFSheet;
import org.apache.poi.hslf.usermodel.HSLFSimpleShape;
import org.apache.poi.hslf.usermodel.HSLFSlide;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.hslf.usermodel.HSLFTextParagraph;
import org.apache.poi.hslf.usermodel.HSLFTextRun;
import org.apache.poi.hslf.usermodel.HSLFTextShape;
import org.apache.poi.sl.usermodel.Hyperlink;
import org.apache.poi.sl.usermodel.Slide;


public final class HSLFHyperlink implements Hyperlink<HSLFShape, HSLFTextParagraph> {
	private final ExHyperlink exHyper;

	private final InteractiveInfo info;

	private TxInteractiveInfoAtom txinfo;

	protected HSLFHyperlink(ExHyperlink exHyper, InteractiveInfo info) {
		this.info = info;
		this.exHyper = exHyper;
	}

	public ExHyperlink getExHyperlink() {
		return exHyper;
	}

	public InteractiveInfo getInfo() {
		return info;
	}

	public TxInteractiveInfoAtom getTextRunInfo() {
		return txinfo;
	}

	protected void setTextRunInfo(TxInteractiveInfoAtom txinfo) {
		this.txinfo = txinfo;
	}

	static HSLFHyperlink createHyperlink(HSLFSimpleShape shape) {
		ExHyperlink exHyper = new ExHyperlink();
		ExHyperlinkAtom obj = exHyper.getExHyperlinkAtom();
		InteractiveInfo info = new InteractiveInfo();
		HSLFHyperlink hyper = new HSLFHyperlink(exHyper, info);
		hyper.linkToNextSlide();
		return hyper;
	}

	static HSLFHyperlink createHyperlink(HSLFTextRun run) {
		ExHyperlink exHyper = new ExHyperlink();
		ExHyperlinkAtom obj = exHyper.getExHyperlinkAtom();
		InteractiveInfo info = new InteractiveInfo();
		HSLFHyperlink hyper = new HSLFHyperlink(exHyper, info);
		hyper.linkToNextSlide();
		TxInteractiveInfoAtom txinfo = new TxInteractiveInfoAtom();
		hyper.setTextRunInfo(txinfo);
		return hyper;
	}

	@Override
	public HyperlinkType getType() {
		switch (info.getInteractiveInfoAtom().getHyperlinkType()) {
			case InteractiveInfoAtom.LINK_Url :
				return exHyper.getLinkURL().startsWith("mailto:") ? HyperlinkType.EMAIL : HyperlinkType.URL;
			case InteractiveInfoAtom.LINK_NextSlide :
			case InteractiveInfoAtom.LINK_PreviousSlide :
			case InteractiveInfoAtom.LINK_FirstSlide :
			case InteractiveInfoAtom.LINK_LastSlide :
			case InteractiveInfoAtom.LINK_SlideNumber :
				return HyperlinkType.DOCUMENT;
			case InteractiveInfoAtom.LINK_CustomShow :
			case InteractiveInfoAtom.LINK_OtherPresentation :
			case InteractiveInfoAtom.LINK_OtherFile :
				return HyperlinkType.FILE;
			default :
			case InteractiveInfoAtom.LINK_NULL :
				return HyperlinkType.NONE;
		}
	}

	@Deprecated
	@org.apache.poi.util.Removal(version = "4.2")
	@Override
	public HyperlinkType getTypeEnum() {
		return getType();
	}

	@Override
	public void linkToEmail(String emailAddress) {
		InteractiveInfoAtom iia = info.getInteractiveInfoAtom();
		iia.setAction(InteractiveInfoAtom.ACTION_HYPERLINK);
		iia.setJump(InteractiveInfoAtom.JUMP_NONE);
		iia.setHyperlinkType(InteractiveInfoAtom.LINK_Url);
		exHyper.setLinkURL(("mailto:" + emailAddress));
		exHyper.setLinkTitle(emailAddress);
		exHyper.setLinkOptions(16);
	}

	@Override
	public void linkToUrl(String url) {
		InteractiveInfoAtom iia = info.getInteractiveInfoAtom();
		iia.setAction(InteractiveInfoAtom.ACTION_HYPERLINK);
		iia.setJump(InteractiveInfoAtom.JUMP_NONE);
		iia.setHyperlinkType(InteractiveInfoAtom.LINK_Url);
		exHyper.setLinkURL(url);
		exHyper.setLinkTitle(url);
		exHyper.setLinkOptions(16);
	}

	@Override
	public void linkToSlide(Slide<HSLFShape, HSLFTextParagraph> slide) {
		assert slide instanceof HSLFSlide;
		HSLFSlide sl = ((HSLFSlide) (slide));
		int slideNum = slide.getSlideNumber();
		String alias = "Slide " + slideNum;
		InteractiveInfoAtom iia = info.getInteractiveInfoAtom();
		iia.setAction(InteractiveInfoAtom.ACTION_HYPERLINK);
		iia.setJump(InteractiveInfoAtom.JUMP_NONE);
		iia.setHyperlinkType(InteractiveInfoAtom.LINK_SlideNumber);
		linkToDocument(sl._getSheetNumber(), slideNum, alias, 48);
	}

	@Override
	public void linkToNextSlide() {
		InteractiveInfoAtom iia = info.getInteractiveInfoAtom();
		iia.setAction(InteractiveInfoAtom.ACTION_JUMP);
		iia.setJump(InteractiveInfoAtom.JUMP_NEXTSLIDE);
		iia.setHyperlinkType(InteractiveInfoAtom.LINK_NextSlide);
		linkToDocument(1, (-1), "NEXT", 16);
	}

	@Override
	public void linkToPreviousSlide() {
		InteractiveInfoAtom iia = info.getInteractiveInfoAtom();
		iia.setAction(InteractiveInfoAtom.ACTION_JUMP);
		iia.setJump(InteractiveInfoAtom.JUMP_PREVIOUSSLIDE);
		iia.setHyperlinkType(InteractiveInfoAtom.LINK_PreviousSlide);
		linkToDocument(1, (-1), "PREV", 16);
	}

	@Override
	public void linkToFirstSlide() {
		InteractiveInfoAtom iia = info.getInteractiveInfoAtom();
		iia.setAction(InteractiveInfoAtom.ACTION_JUMP);
		iia.setJump(InteractiveInfoAtom.JUMP_FIRSTSLIDE);
		iia.setHyperlinkType(InteractiveInfoAtom.LINK_FirstSlide);
		linkToDocument(1, (-1), "FIRST", 16);
	}

	@Override
	public void linkToLastSlide() {
		InteractiveInfoAtom iia = info.getInteractiveInfoAtom();
		iia.setAction(InteractiveInfoAtom.ACTION_JUMP);
		iia.setJump(InteractiveInfoAtom.JUMP_LASTSLIDE);
		iia.setHyperlinkType(InteractiveInfoAtom.LINK_LastSlide);
		linkToDocument(1, (-1), "LAST", 16);
	}

	private void linkToDocument(int sheetNumber, int slideNumber, String alias, int options) {
		exHyper.setLinkURL(((((sheetNumber + ",") + slideNumber) + ",") + alias));
		exHyper.setLinkTitle(alias);
		exHyper.setLinkOptions(options);
	}

	@Override
	public String getAddress() {
		return exHyper.getLinkURL();
	}

	@Override
	public void setAddress(String str) {
		exHyper.setLinkURL(str);
	}

	public int getId() {
		return exHyper.getExHyperlinkAtom().getNumber();
	}

	@Override
	public String getLabel() {
		return exHyper.getLinkTitle();
	}

	@Override
	public void setLabel(String label) {
		exHyper.setLinkTitle(label);
	}

	public int getStartIndex() {
		return (txinfo) == null ? -1 : txinfo.getStartIndex();
	}

	public void setStartIndex(int startIndex) {
		if ((txinfo) != null) {
			txinfo.setStartIndex(startIndex);
		}
	}

	public int getEndIndex() {
		return (txinfo) == null ? -1 : txinfo.getEndIndex();
	}

	public void setEndIndex(int endIndex) {
		if ((txinfo) != null) {
			txinfo.setEndIndex(endIndex);
		}
	}

	public static List<HSLFHyperlink> find(HSLFTextShape shape) {
		return HSLFHyperlink.find(shape.getTextParagraphs());
	}

	@SuppressWarnings("resource")
	protected static List<HSLFHyperlink> find(List<HSLFTextParagraph> paragraphs) {
		List<HSLFHyperlink> lst = new ArrayList<>();
		if ((paragraphs == null) || (paragraphs.isEmpty()))
			return lst;

		HSLFTextParagraph firstPara = paragraphs.get(0);
		HSLFSlideShow ppt = firstPara.getSheet().getSlideShow();
		ExObjList exobj = ppt.getDocumentRecord().getExObjList(false);
		if (exobj != null) {
			Record[] records = firstPara.getRecords();
			HSLFHyperlink.find(Arrays.asList(records), exobj, lst);
		}
		return lst;
	}

	@SuppressWarnings("resource")
	protected static HSLFHyperlink find(HSLFShape shape) {
		HSLFSlideShow ppt = shape.getSheet().getSlideShow();
		ExObjList exobj = ppt.getDocumentRecord().getExObjList(false);
		return null;
	}

	private static void find(List<? extends Record> records, ExObjList exobj, List<HSLFHyperlink> out) {
		ListIterator<? extends Record> iter = records.listIterator();
		while (iter.hasNext()) {
			Record r = iter.next();
			if (!(r instanceof InteractiveInfo)) {
				continue;
			}
			InteractiveInfo hldr = ((InteractiveInfo) (r));
			InteractiveInfoAtom info = hldr.getInteractiveInfoAtom();
			if (info == null) {
				continue;
			}
			int id = info.getHyperlinkID();
			ExHyperlink exHyper = exobj.get(id);
			if (exHyper == null) {
				continue;
			}
			HSLFHyperlink link = new HSLFHyperlink(exHyper, hldr);
			out.add(link);
			if (iter.hasNext()) {
				r = iter.next();
				if (!(r instanceof TxInteractiveInfoAtom)) {
					iter.previous();
					continue;
				}
				link.setTextRunInfo(((TxInteractiveInfoAtom) (r)));
			}
		} 
	}
}

