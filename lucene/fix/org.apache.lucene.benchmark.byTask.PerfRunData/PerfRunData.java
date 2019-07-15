

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.benchmark.byTask.feeds.ContentItemsSource;
import org.apache.lucene.benchmark.byTask.feeds.ContentSource;
import org.apache.lucene.benchmark.byTask.feeds.DocMaker;
import org.apache.lucene.benchmark.byTask.feeds.FacetSource;
import org.apache.lucene.benchmark.byTask.feeds.QueryMaker;
import org.apache.lucene.benchmark.byTask.stats.Points;
import org.apache.lucene.benchmark.byTask.tasks.NewAnalyzerTask;
import org.apache.lucene.benchmark.byTask.tasks.ReadTask;
import org.apache.lucene.benchmark.byTask.utils.AnalyzerFactory;
import org.apache.lucene.benchmark.byTask.utils.Config;
import org.apache.lucene.facet.taxonomy.TaxonomyReader;
import org.apache.lucene.facet.taxonomy.TaxonomyWriter;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.IOUtils;


public class PerfRunData implements Closeable {
	private Points points;

	private Directory directory;

	private Map<String, AnalyzerFactory> analyzerFactories = new HashMap<>();

	private Analyzer analyzer;

	private DocMaker docMaker;

	private ContentSource contentSource;

	private FacetSource facetSource;

	private Locale locale;

	private Directory taxonomyDir;

	private TaxonomyWriter taxonomyWriter;

	private TaxonomyReader taxonomyReader;

	private HashMap<Class<? extends ReadTask>, QueryMaker> readTaskQueryMaker;

	private Class<? extends QueryMaker> qmkrClass;

	private DirectoryReader indexReader;

	private IndexSearcher indexSearcher;

	private IndexWriter indexWriter;

	private Config config;

	private long startTimeMillis;

	private final HashMap<String, Object> perfObjects = new HashMap<>();

	public PerfRunData(Config config) throws Exception {
		this.config = config;
		analyzer = NewAnalyzerTask.createAnalyzer(config.get("analyzer", "org.apache.lucene.analysis.standard.StandardAnalyzer"));
		String sourceClass = config.get("content.source", "org.apache.lucene.benchmark.byTask.feeds.SingleDocSource");
		contentSource = Class.forName(sourceClass).asSubclass(ContentSource.class).newInstance();
		contentSource.setConfig(config);
		docMaker = Class.forName(config.get("doc.maker", "org.apache.lucene.benchmark.byTask.feeds.DocMaker")).asSubclass(DocMaker.class).newInstance();
		docMaker.setConfig(config, contentSource);
		facetSource = Class.forName(config.get("facet.source", "org.apache.lucene.benchmark.byTask.feeds.RandomFacetSource")).asSubclass(FacetSource.class).newInstance();
		facetSource.setConfig(config);
		readTaskQueryMaker = new HashMap<>();
		qmkrClass = Class.forName(config.get("query.maker", "org.apache.lucene.benchmark.byTask.feeds.SimpleQueryMaker")).asSubclass(QueryMaker.class);
		reinit(false);
		points = new Points(config);
		if (Boolean.valueOf(config.get("log.queries", "false")).booleanValue()) {
			System.out.println("------------> queries:");
		}
	}

	@Override
	public void close() throws IOException {
		if ((indexWriter) != null) {
			indexWriter.close();
		}
		IOUtils.close(indexReader, directory, taxonomyWriter, taxonomyReader, taxonomyDir, docMaker, facetSource, contentSource);
		ArrayList<Closeable> perfObjectsToClose = new ArrayList<>();
		for (Object obj : perfObjects.values()) {
			if (obj instanceof Closeable) {
				perfObjectsToClose.add(((Closeable) (obj)));
			}
		}
		IOUtils.close(perfObjectsToClose);
	}

	public void reinit(boolean eraseIndex) throws Exception {
		if ((indexWriter) != null) {
			indexWriter.close();
		}
		IOUtils.close(indexReader, directory);
		indexWriter = null;
		indexReader = null;
		IOUtils.close(taxonomyWriter, taxonomyReader, taxonomyDir);
		taxonomyWriter = null;
		taxonomyReader = null;
		directory = createDirectory(eraseIndex, "index", "directory");
		taxonomyDir = createDirectory(eraseIndex, "taxo", "taxonomy.directory");
		resetInputs();
		System.runFinalization();
		System.gc();
		setStartTimeMillis();
	}

	private Directory createDirectory(boolean eraseIndex, String dirName, String dirParam) throws IOException {
		if ("FSDirectory".equals(config.get(dirParam, "RAMDirectory"))) {
			Path workDir = Paths.get(config.get("work.dir", "work"));
			Path indexDir = workDir.resolve(dirName);
			if (eraseIndex && (Files.exists(indexDir))) {
				IOUtils.rm(indexDir);
			}
			Files.createDirectories(indexDir);
			return FSDirectory.open(indexDir);
		}
		return new RAMDirectory();
	}

	public synchronized Object getPerfObject(String key) {
		return perfObjects.get(key);
	}

	public synchronized void setPerfObject(String key, Object obj) {
		perfObjects.put(key, obj);
	}

	public long setStartTimeMillis() {
		startTimeMillis = System.currentTimeMillis();
		return startTimeMillis;
	}

	public long getStartTimeMillis() {
		return startTimeMillis;
	}

	public Points getPoints() {
		return points;
	}

	public Directory getDirectory() {
		return directory;
	}

	public void setDirectory(Directory directory) {
		this.directory = directory;
	}

	public Directory getTaxonomyDir() {
		return taxonomyDir;
	}

	public synchronized void setTaxonomyReader(TaxonomyReader taxoReader) throws IOException {
		if (taxoReader == (this.taxonomyReader)) {
			return;
		}
		if ((taxonomyReader) != null) {
			taxonomyReader.decRef();
		}
		if (taxoReader != null) {
			taxoReader.incRef();
		}
		this.taxonomyReader = taxoReader;
	}

	public synchronized TaxonomyReader getTaxonomyReader() {
		if ((taxonomyReader) != null) {
			taxonomyReader.incRef();
		}
		return taxonomyReader;
	}

	public void setTaxonomyWriter(TaxonomyWriter taxoWriter) {
		this.taxonomyWriter = taxoWriter;
	}

	public TaxonomyWriter getTaxonomyWriter() {
		return taxonomyWriter;
	}

	public synchronized DirectoryReader getIndexReader() {
		if ((indexReader) != null) {
			indexReader.incRef();
		}
		return indexReader;
	}

	public synchronized IndexSearcher getIndexSearcher() {
		if ((indexReader) != null) {
			indexReader.incRef();
		}
		return indexSearcher;
	}

	public synchronized void setIndexReader(DirectoryReader indexReader) throws IOException {
		if (indexReader == (this.indexReader)) {
			return;
		}
		if ((this.indexReader) != null) {
			this.indexReader.decRef();
		}
		this.indexReader = indexReader;
		if (indexReader != null) {
			indexReader.incRef();
			indexSearcher = new IndexSearcher(indexReader);
			indexSearcher.setQueryCache(null);
		}else {
			indexSearcher = null;
		}
	}

	public IndexWriter getIndexWriter() {
		return indexWriter;
	}

	public void setIndexWriter(IndexWriter indexWriter) {
		this.indexWriter = indexWriter;
	}

	public Analyzer getAnalyzer() {
		return analyzer;
	}

	public void setAnalyzer(Analyzer analyzer) {
		this.analyzer = analyzer;
	}

	public ContentSource getContentSource() {
		return contentSource;
	}

	public DocMaker getDocMaker() {
		return docMaker;
	}

	public FacetSource getFacetSource() {
		return facetSource;
	}

	public Locale getLocale() {
		return locale;
	}

	public void setLocale(Locale locale) {
		this.locale = locale;
	}

	public Config getConfig() {
		return config;
	}

	public void resetInputs() throws IOException {
		contentSource.resetInputs();
		docMaker.resetInputs();
		facetSource.resetInputs();
		for (final QueryMaker queryMaker : readTaskQueryMaker.values()) {
			try {
				queryMaker.resetInputs();
			} catch (IOException e) {
				throw e;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	public synchronized QueryMaker getQueryMaker(ReadTask readTask) {
		Class<? extends ReadTask> readTaskClass = readTask.getClass();
		QueryMaker qm = readTaskQueryMaker.get(readTaskClass);
		if (qm == null) {
			try {
				qm = qmkrClass.newInstance();
				qm.setConfig(config);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			readTaskQueryMaker.put(readTaskClass, qm);
		}
		return qm;
	}

	public Map<String, AnalyzerFactory> getAnalyzerFactories() {
		return analyzerFactories;
	}
}

