import org.apache.poi.xddf.usermodel.chart.*;


import org.apache.poi.util.Beta;
import org.apache.poi.xddf.usermodel.text.TextContainer;
import org.apache.poi.xddf.usermodel.text.XDDFTextBody;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTTitle;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTTx;

/**
 * @since 4.0.1
 */
@Beta
public class XDDFTitle {
    private final CTTitle title;
    private final TextContainer parent;

    public XDDFTitle(TextContainer parent, CTTitle title) {
        this.parent = parent;
        this.title = title;
    }

    public XDDFTextBody getBody() {
        if (!title.isSetTx()) {
            title.addNewTx();
        }
        CTTx tx = title.getTx();
        if (tx.isSetStrRef()) {
            tx.unsetStrRef();
        }
        if (!tx.isSetRich()) {
            tx.addNewRich();
        }
        return new XDDFTextBody(parent, tx.getRich());
    }

    public void setText(String text) {
        if (!title.isSetLayout()) {
            title.addNewLayout();
        }
        getBody().setText(text);
    }

    public void setOverlay(Boolean overlay) {
        if (overlay == null) {
            if (title.isSetOverlay()) {
                title.unsetOverlay();
            }
        } else {
            if (title.isSetOverlay()) {
                title.getOverlay().setVal(overlay);
            } else {
                title.addNewOverlay().setVal(overlay);
            }
        }
    }

}
