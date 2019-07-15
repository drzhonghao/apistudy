import org.apache.poi.xssf.usermodel.charts.*;


import org.apache.poi.ss.usermodel.charts.ChartSeries;
import org.apache.poi.ss.usermodel.charts.TitleType;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.util.Removal;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTSerTx;
import org.apache.poi.xddf.usermodel.chart.XDDFChartData.Series;;

/**
 * Base of all XSSF Chart Series
 *
 * @deprecated use {@link Series} instead
 */
@Deprecated
@Removal(version="4.2")
public abstract class AbstractXSSFChartSeries implements ChartSeries {

    private String titleValue;
    private CellReference titleRef;
    private TitleType titleType;

    @Override
    public void setTitle(CellReference titleReference) {
        titleType = TitleType.CELL_REFERENCE;
        titleRef = titleReference;
    }

    @Override
    public void setTitle(String title) {
        titleType = TitleType.STRING;
        titleValue = title;
    }

    @Override
    public CellReference getTitleCellReference() {
        if (TitleType.CELL_REFERENCE.equals(titleType)) {
            return titleRef;
        }
        throw new IllegalStateException("Title type is not CellReference.");
    }

    @Override
    public String getTitleString() {
        if (TitleType.STRING.equals(titleType)) {
            return titleValue;
        }
        throw new IllegalStateException("Title type is not String.");
    }

    @Override
    public TitleType getTitleType() {
        return titleType;
    }

    protected boolean isTitleSet() {
        return titleType != null;
    }

    protected CTSerTx getCTSerTx() {
        CTSerTx tx = CTSerTx.Factory.newInstance();
        switch (titleType) {
            case CELL_REFERENCE:
                tx.addNewStrRef().setF(titleRef.formatAsString());
                return tx;
            case STRING:
                tx.setV(titleValue);
                return tx;
            default:
                throw new IllegalStateException("Unkown title type: " + titleType);
        }
    }
}
