

import java.io.IOException;
import org.apache.lucene.benchmark.byTask.feeds.DocData;
import org.apache.lucene.benchmark.byTask.feeds.TrecContentSource;
import org.apache.lucene.benchmark.byTask.feeds.TrecDocParser;


public class TrecParserByPath extends TrecDocParser {
	@Override
	public DocData parse(DocData docData, String name, TrecContentSource trecSrc, StringBuilder docBuf, TrecDocParser.ParsePathType pathType) throws IOException {
		return null;
	}
}

