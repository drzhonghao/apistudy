import org.apache.poi.xwpf.usermodel.*;


import org.apache.poi.ooxml.POIXMLDocumentPart;
import org.apache.poi.openxml4j.opc.PackageRelationship;
import org.openxmlformats.schemas.drawingml.x2006.main.CTBlipFillProperties;
import org.openxmlformats.schemas.drawingml.x2006.picture.CTPicture;


/**
 * @author Philipp Epp
 */
public class XWPFPicture {

    private CTPicture ctPic;
    private String description;
    private XWPFRun run;

    public XWPFPicture(CTPicture ctPic, XWPFRun run) {
        this.run = run;
        this.ctPic = ctPic;
        description = ctPic.getNvPicPr().getCNvPr().getDescr();
    }

    /**
     * Link Picture with PictureData
     *
     * @param rel
     */
    public void setPictureReference(PackageRelationship rel) {
        ctPic.getBlipFill().getBlip().setEmbed(rel.getId());
    }

    /**
     * Return the underlying CTPicture bean that holds all properties for this picture
     *
     * @return the underlying CTPicture bean
     */
    public CTPicture getCTPicture() {
        return ctPic;
    }

    /**
     * Get the PictureData of the Picture, if present.
     * Note - not all kinds of picture have data
     */
    public XWPFPictureData getPictureData() {
        CTBlipFillProperties blipProps = ctPic.getBlipFill();

        if (blipProps == null || !blipProps.isSetBlip()) {
            // return null if Blip data is missing
            return null;
        }

        String blipId = blipProps.getBlip().getEmbed();
        POIXMLDocumentPart part = run.getParent().getPart();
        if (part != null) {
            POIXMLDocumentPart relatedPart = part.getRelationById(blipId);
            if (relatedPart instanceof XWPFPictureData) {
                return (XWPFPictureData) relatedPart;
            }
        }
        return null;
    }

    public String getDescription() {
        return description;
    }
}
