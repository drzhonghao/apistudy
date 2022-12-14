import org.apache.poi.hslf.record.Slide;
import org.apache.poi.hslf.usermodel.*;


import java.util.ArrayList;
import java.util.List;

import org.apache.poi.hslf.model.textproperties.TextPropCollection;
import org.apache.poi.hslf.record.SlideAtom;

/**
 * Title masters define the design template for slides with a Title Slide layout.
 */
public final class HSLFTitleMaster extends HSLFMasterSheet {
    private final List<List<HSLFTextParagraph>> _paragraphs = new ArrayList<>();

    /**
     * Constructs a TitleMaster
     *
     */
    public HSLFTitleMaster(org.apache.poi.hslf.record.Slide record, int sheetNo) {
        super(record, sheetNo);

        for (List<HSLFTextParagraph> l : HSLFTextParagraph.findTextParagraphs(getPPDrawing(), this)) {
            if (!_paragraphs.contains(l)) {
                _paragraphs.add(l);
            }
        }
    }

    /**
     * Returns an array of all the TextRuns found
     */
    @Override
    public List<List<HSLFTextParagraph>> getTextParagraphs() {
        return _paragraphs;
    }

    /**
     * Delegate the call to the underlying slide master.
     */
    @Override
    public TextPropCollection getPropCollection(final int txtype, final int level, final String name, final boolean isCharacter) {
        final HSLFMasterSheet master = getMasterSheet();
        return (master == null) ? null : master.getPropCollection(txtype, level, name, isCharacter);
    }
    
    /**
     * Returns the slide master for this title master.
     */
    @Override
    public HSLFMasterSheet getMasterSheet(){
        SlideAtom sa = ((org.apache.poi.hslf.record.Slide)getSheetContainer()).getSlideAtom();
        int masterId = sa.getMasterID();
        for (HSLFSlideMaster sm : getSlideShow().getSlideMasters()) {
            if (masterId == sm._getSheetNumber()) {
                return sm;
            }
        }
        return null;
    }
}
