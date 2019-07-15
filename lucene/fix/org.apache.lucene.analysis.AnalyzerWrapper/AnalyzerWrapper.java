

import java.io.Reader;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.util.AttributeFactory;


public abstract class AnalyzerWrapper extends Analyzer {
	protected AnalyzerWrapper(Analyzer.ReuseStrategy reuseStrategy) {
		super(reuseStrategy);
	}

	protected abstract Analyzer getWrappedAnalyzer(String fieldName);

	protected Analyzer.TokenStreamComponents wrapComponents(String fieldName, Analyzer.TokenStreamComponents components) {
		return components;
	}

	protected TokenStream wrapTokenStreamForNormalization(String fieldName, TokenStream in) {
		return in;
	}

	protected Reader wrapReader(String fieldName, Reader reader) {
		return reader;
	}

	protected Reader wrapReaderForNormalization(String fieldName, Reader reader) {
		return reader;
	}

	@Override
	protected final Analyzer.TokenStreamComponents createComponents(String fieldName) {
		return null;
	}

	@Override
	protected final TokenStream normalize(String fieldName, TokenStream in) {
		return null;
	}

	@Override
	public int getPositionIncrementGap(String fieldName) {
		return getWrappedAnalyzer(fieldName).getPositionIncrementGap(fieldName);
	}

	@Override
	public int getOffsetGap(String fieldName) {
		return getWrappedAnalyzer(fieldName).getOffsetGap(fieldName);
	}

	@Override
	public final Reader initReader(String fieldName, Reader reader) {
		return null;
	}

	@Override
	protected final Reader initReaderForNormalization(String fieldName, Reader reader) {
		return null;
	}

	@Override
	protected final AttributeFactory attributeFactory(String fieldName) {
		return null;
	}
}

