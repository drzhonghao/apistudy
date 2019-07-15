

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import org.apache.lucene.benchmark.byTask.feeds.ContentItemsSource;
import org.apache.lucene.benchmark.byTask.feeds.ContentSource;
import org.apache.lucene.benchmark.byTask.feeds.DocData;
import org.apache.lucene.benchmark.byTask.feeds.HTMLParser;
import org.apache.lucene.benchmark.byTask.feeds.NoMoreDataException;
import org.apache.lucene.benchmark.byTask.feeds.TrecDocParser;
import org.apache.lucene.benchmark.byTask.feeds.TrecGov2Parser;
import org.apache.lucene.benchmark.byTask.utils.Config;
import org.apache.lucene.benchmark.byTask.utils.StreamUtils;


public class TrecContentSource extends ContentSource {
	static final class DateFormatInfo {
		DateFormat[] dfs;

		ParsePosition pos;
	}

	public static final String DOCNO = "<DOCNO>";

	public static final String TERMINATING_DOCNO = "</DOCNO>";

	public static final String DOC = "<DOC>";

	public static final String TERMINATING_DOC = "</DOC>";

	public static final String NEW_LINE = System.getProperty("line.separator");

	private static final String[] DATE_FORMATS = new String[]{ "EEE, dd MMM yyyy kk:mm:ss z", "EEE MMM dd kk:mm:ss yyyy z", "EEE, dd-MMM-':'y kk:mm:ss z", "EEE, dd-MMM-yyy kk:mm:ss z", "EEE MMM dd kk:mm:ss yyyy", "dd MMM yyyy", "MMM dd, yyyy", "yyMMdd", "hhmm z.z.z. MMM dd, yyyy" };

	private ThreadLocal<TrecContentSource.DateFormatInfo> dateFormats = new ThreadLocal<>();

	private ThreadLocal<StringBuilder> trecDocBuffer = new ThreadLocal<>();

	private Path dataDir = null;

	private ArrayList<Path> inputFiles = new ArrayList<>();

	private int nextFile = 0;

	private Object lock = new Object();

	BufferedReader reader;

	int iteration = 0;

	HTMLParser htmlParser;

	private boolean excludeDocnameIteration;

	private TrecDocParser trecDocParser = new TrecGov2Parser();

	TrecDocParser.ParsePathType currPathType;

	private TrecContentSource.DateFormatInfo getDateFormatInfo() {
		TrecContentSource.DateFormatInfo dfi = dateFormats.get();
		if (dfi == null) {
			dfi = new TrecContentSource.DateFormatInfo();
			dfi.dfs = new SimpleDateFormat[TrecContentSource.DATE_FORMATS.length];
			for (int i = 0; i < (dfi.dfs.length); i++) {
				dfi.dfs[i] = new SimpleDateFormat(TrecContentSource.DATE_FORMATS[i], Locale.ENGLISH);
				dfi.dfs[i].setLenient(true);
			}
			dfi.pos = new ParsePosition(0);
			dateFormats.set(dfi);
		}
		return dfi;
	}

	private StringBuilder getDocBuffer() {
		StringBuilder sb = trecDocBuffer.get();
		if (sb == null) {
			sb = new StringBuilder();
			trecDocBuffer.set(sb);
		}
		return sb;
	}

	HTMLParser getHtmlParser() {
		return htmlParser;
	}

	private void read(StringBuilder buf, String lineStart, boolean collectMatchLine, boolean collectAll) throws IOException, NoMoreDataException {
		String sep = "";
		while (true) {
			String line = reader.readLine();
			if (line == null) {
				openNextFile();
				continue;
			}
			if ((lineStart != null) && (line.startsWith(lineStart))) {
				if (collectMatchLine) {
					buf.append(sep).append(line);
					sep = TrecContentSource.NEW_LINE;
				}
				return;
			}
			if (collectAll) {
				buf.append(sep).append(line);
				sep = TrecContentSource.NEW_LINE;
			}
		} 
	}

	void openNextFile() throws IOException, NoMoreDataException {
		close();
		currPathType = null;
		while (true) {
			if ((nextFile) >= (inputFiles.size())) {
				if (!(forever)) {
					throw new NoMoreDataException();
				}
				nextFile = 0;
				(iteration)++;
			}
			Path f = inputFiles.get(((nextFile)++));
			if (verbose) {
				System.out.println(((("opening: " + f) + " length: ") + (Files.size(f))));
			}
			try {
				InputStream inputStream = StreamUtils.inputStream(f);
				reader = new BufferedReader(new InputStreamReader(inputStream, encoding), StreamUtils.BUFFER_SIZE);
				currPathType = TrecDocParser.pathType(f);
				return;
			} catch (Exception e) {
				if (verbose) {
					System.out.println(((("Skipping 'bad' file " + (f.toAbsolutePath())) + " due to ") + (e.getMessage())));
					continue;
				}
				throw new NoMoreDataException();
			}
		} 
	}

	public Date parseDate(String dateStr) {
		dateStr = dateStr.trim();
		TrecContentSource.DateFormatInfo dfi = getDateFormatInfo();
		for (int i = 0; i < (dfi.dfs.length); i++) {
			DateFormat df = dfi.dfs[i];
			dfi.pos.setIndex(0);
			dfi.pos.setErrorIndex((-1));
			Date d = df.parse(dateStr, dfi.pos);
			if (d != null) {
				return d;
			}
		}
		if (verbose) {
			System.out.println(("failed to parse date (assigning 'now') for: " + dateStr));
		}
		return null;
	}

	@Override
	public void close() throws IOException {
		if ((reader) == null) {
			return;
		}
		try {
			reader.close();
		} catch (IOException e) {
			if (verbose) {
				System.out.println("failed to close reader !");
				e.printStackTrace(System.out);
			}
		}
		reader = null;
	}

	@Override
	public DocData getNextDocData(DocData docData) throws IOException, NoMoreDataException {
		String name = null;
		StringBuilder docBuf = getDocBuffer();
		TrecDocParser.ParsePathType parsedPathType;
		synchronized(lock) {
			if ((reader) == null) {
				openNextFile();
			}
			docBuf.setLength(0);
			read(docBuf, TrecContentSource.DOC, false, false);
			parsedPathType = currPathType;
			docBuf.setLength(0);
			read(docBuf, TrecContentSource.DOCNO, true, false);
			name = docBuf.substring(TrecContentSource.DOCNO.length(), docBuf.indexOf(TrecContentSource.TERMINATING_DOCNO, TrecContentSource.DOCNO.length())).trim();
			if (!(excludeDocnameIteration)) {
				name = (name + "_") + (iteration);
			}
			docBuf.setLength(0);
			read(docBuf, TrecContentSource.TERMINATING_DOC, false, true);
		}
		addBytes(docBuf.length());
		addItem();
		return docData;
	}

	@Override
	public void resetInputs() throws IOException {
		synchronized(lock) {
			super.resetInputs();
			close();
			nextFile = 0;
			iteration = 0;
		}
	}

	@Override
	public void setConfig(Config config) {
		super.setConfig(config);
		Path workDir = Paths.get(config.get("work.dir", "work"));
		String d = config.get("docs.dir", "trec");
		dataDir = Paths.get(d);
		if (!(dataDir.isAbsolute())) {
			dataDir = workDir.resolve(d);
		}
		try {
			collectFiles(dataDir, inputFiles);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		if ((inputFiles.size()) == 0) {
			throw new IllegalArgumentException(("No files in dataDir: " + (dataDir)));
		}
		try {
			String trecDocParserClassName = config.get("trec.doc.parser", "org.apache.lucene.benchmark.byTask.feeds.TrecGov2Parser");
			trecDocParser = Class.forName(trecDocParserClassName).asSubclass(TrecDocParser.class).newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		try {
			String htmlParserClassName = config.get("html.parser", "org.apache.lucene.benchmark.byTask.feeds.DemoHTMLParser");
			htmlParser = Class.forName(htmlParserClassName).asSubclass(HTMLParser.class).newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		if ((encoding) == null) {
			encoding = StandardCharsets.ISO_8859_1.name();
		}
		excludeDocnameIteration = config.get("content.source.excludeIteration", false);
	}
}

