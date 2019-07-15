

import java.io.IOException;
import java.util.Date;
import org.apache.lucene.benchmark.byTask.feeds.DocData;
import org.apache.lucene.benchmark.byTask.feeds.TrecContentSource;
import org.apache.lucene.benchmark.byTask.feeds.TrecDocParser;


public class TrecGov2Parser extends TrecDocParser {
	private static final String DATE = "Date: ";

	private static final String DATE_END = TrecContentSource.NEW_LINE;

	private static final String DOCHDR = "<DOCHDR>";

	private static final String TERMINATING_DOCHDR = "</DOCHDR>";

	@Override
	public DocData parse(DocData docData, String name, TrecContentSource trecSrc, StringBuilder docBuf, TrecDocParser.ParsePathType pathType) throws IOException {
		Date date = null;
		int start = 0;
		final int h1 = docBuf.indexOf(TrecGov2Parser.DOCHDR);
		if (h1 >= 0) {
			final int h2 = docBuf.indexOf(TrecGov2Parser.TERMINATING_DOCHDR, h1);
			final String dateStr = TrecDocParser.extract(docBuf, TrecGov2Parser.DATE, TrecGov2Parser.DATE_END, h2, null);
			if (dateStr != null) {
				date = trecSrc.parseDate(dateStr);
			}
			start = h2 + (TrecGov2Parser.TERMINATING_DOCHDR.length());
		}
		final String html = docBuf.substring(start);
		return null;
	}
}

