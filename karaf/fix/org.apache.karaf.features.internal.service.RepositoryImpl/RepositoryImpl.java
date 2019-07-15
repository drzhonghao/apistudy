

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.URI;
import java.net.URI[];
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Stream;
import org.apache.karaf.features.Repository;
import org.apache.karaf.features.internal.model.Feature;
import org.apache.karaf.features.internal.model.Features;
import org.apache.karaf.features.internal.model.JaxbUtil;
import org.apache.karaf.features.internal.service.FeaturesProcessor;


public class RepositoryImpl implements Repository {
	private final URI uri;

	private Features features;

	private boolean blacklisted;

	public RepositoryImpl(URI uri) {
		this(uri, false);
	}

	public RepositoryImpl(URI uri, boolean validate) {
		this.uri = uri;
		load(validate);
	}

	public RepositoryImpl(URI uri, Features features, boolean blacklisted) {
		this.uri = uri;
		this.features = features;
		this.blacklisted = blacklisted;
	}

	@Override
	public URI getURI() {
		return uri;
	}

	@Override
	public String getName() {
		return features.getName();
	}

	@Override
	public URI[] getRepositories() {
		return features.getRepository().stream().map(String::trim).map(URI::create).toArray(URI[]::new);
	}

	@Override
	public URI[] getResourceRepositories() {
		return features.getResourceRepository().stream().map(String::trim).map(URI::create).toArray(URI[]::new);
	}

	@Override
	public Feature[] getFeatures() {
		return features.getFeature().toArray(new Feature[features.getFeature().size()]);
	}

	public Features getFeaturesInternal() {
		return features;
	}

	@Override
	public boolean isBlacklisted() {
		return blacklisted;
	}

	public void setBlacklisted(boolean blacklisted) {
		this.blacklisted = blacklisted;
		features.setBlacklisted(blacklisted);
	}

	private void load(boolean validate) {
		if ((features) == null) {
			try (InputStream inputStream = new RepositoryImpl.InterruptibleInputStream(uri.toURL().openStream())) {
				features = JaxbUtil.unmarshal(uri.toASCIIString(), inputStream, validate);
			} catch (Exception e) {
				throw new RuntimeException((((e.getMessage()) + " : ") + (uri)), e);
			}
		}
	}

	public void processFeatures(FeaturesProcessor processor) {
		processor.process(features);
		if (blacklisted) {
			for (Feature feature : features.getFeature()) {
				feature.setBlacklisted(true);
			}
		}
	}

	static class InterruptibleInputStream extends FilterInputStream {
		InterruptibleInputStream(InputStream in) {
			super(in);
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			if (Thread.currentThread().isInterrupted()) {
				throw new InterruptedIOException();
			}
			return super.read(b, off, len);
		}
	}

	@Override
	public boolean equals(Object o) {
		if ((this) == o)
			return true;

		if ((o == null) || ((getClass()) != (o.getClass())))
			return false;

		RepositoryImpl that = ((RepositoryImpl) (o));
		return Objects.equals(uri, that.uri);
	}

	@Override
	public int hashCode() {
		return Objects.hash(uri);
	}

	@Override
	public String toString() {
		return getURI().toString();
	}
}

