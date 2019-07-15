import org.apache.poi.xssf.usermodel.charts.*;


import org.apache.poi.ss.usermodel.charts.ChartDataFactory;
import org.apache.poi.util.Removal;

/**
 * @deprecated
 */
@Deprecated
@Removal(version="4.2")
public class XSSFChartDataFactory implements ChartDataFactory {

	private static XSSFChartDataFactory instance;

	private XSSFChartDataFactory() {
		super();
	}

	/**
	 * @return new scatter charts data instance
	 */
	@Override
    public XSSFScatterChartData createScatterChartData() {
		return new XSSFScatterChartData();
	}

	/**
	 * @return new line charts data instance
	 */
	@Override
    public XSSFLineChartData createLineChartData() {
		return new XSSFLineChartData();
	}

	/**
	 * @return factory instance
	 */
	public static XSSFChartDataFactory getInstance() {
		if (instance == null) {
			instance = new XSSFChartDataFactory();
		}
		return instance;
	}

}
