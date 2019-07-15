

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.apache.felix.utils.manifest.Clause;
import org.apache.felix.utils.manifest.Parser;
import org.apache.karaf.features.FeaturePattern;
import org.apache.karaf.features.LocationPattern;
import org.apache.karaf.features.internal.model.Features;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Blacklist {
	public static Logger LOG = LoggerFactory.getLogger(Blacklist.class);

	public static final String BLACKLIST_URL = "url";

	public static final String BLACKLIST_TYPE = "type";

	public static final String TYPE_FEATURE = "feature";

	public static final String TYPE_BUNDLE = "bundle";

	public static final String TYPE_REPOSITORY = "repository";

	private static final Logger LOGGER = LoggerFactory.getLogger(Blacklist.class);

	private Clause[] clauses;

	private List<LocationPattern> repositoryBlacklist = new LinkedList<>();

	private List<FeaturePattern> featureBlacklist = new LinkedList<>();

	private List<LocationPattern> bundleBlacklist = new LinkedList<>();

	public Blacklist() throws MalformedURLException {
		this(Collections.emptyList());
	}

	public Blacklist(List<String> blacklist) throws MalformedURLException {
		this.clauses = Parser.parseClauses(blacklist.toArray(new String[blacklist.size()]));
		compileClauses();
	}

	public Blacklist(String blacklistUrl) throws MalformedURLException {
		Set<String> blacklist = new HashSet<>();
		if (blacklistUrl != null) {
			try (InputStream is = new URL(blacklistUrl).openStream();BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
				reader.lines().map(String::trim).filter(( line) -> (!(line.isEmpty())) && (!(line.startsWith("#")))).forEach(blacklist::add);
			} catch (FileNotFoundException e) {
				Blacklist.LOGGER.debug("Unable to load blacklist bundles list", e.toString());
			} catch (Exception e) {
				Blacklist.LOGGER.debug("Unable to load blacklist bundles list", e);
			}
		}
		this.clauses = Parser.parseClauses(blacklist.toArray(new String[blacklist.size()]));
		compileClauses();
	}

	private void compileClauses() throws MalformedURLException {
		for (Clause c : clauses) {
			String type = c.getAttribute(Blacklist.BLACKLIST_TYPE);
			if (type == null) {
				String url = c.getAttribute(Blacklist.BLACKLIST_URL);
				if ((url != null) || (c.getName().startsWith("mvn:"))) {
					type = Blacklist.TYPE_BUNDLE;
				}else {
					type = Blacklist.TYPE_FEATURE;
				}
			}
			String location;
			switch (type) {
				case Blacklist.TYPE_REPOSITORY :
					location = c.getName();
					if ((c.getAttribute(Blacklist.BLACKLIST_URL)) != null) {
						location = c.getAttribute(Blacklist.BLACKLIST_URL);
					}
					if (location == null) {
						Blacklist.LOG.warn("Repository blacklist URI is empty. Ignoring.");
					}else {
						try {
							repositoryBlacklist.add(new LocationPattern(location));
						} catch (IllegalArgumentException e) {
							Blacklist.LOG.warn((((("Problem parsing repository blacklist URI \"" + location) + "\": ") + (e.getMessage())) + ". Ignoring."));
						}
					}
					break;
				case Blacklist.TYPE_FEATURE :
					try {
						featureBlacklist.add(new FeaturePattern(c.toString()));
					} catch (IllegalArgumentException e) {
						Blacklist.LOG.warn((((("Problem parsing blacklisted feature identifier \"" + (c.toString())) + "\": ") + (e.getMessage())) + ". Ignoring."));
					}
					break;
				case Blacklist.TYPE_BUNDLE :
					location = c.getName();
					if ((c.getAttribute(Blacklist.BLACKLIST_URL)) != null) {
						location = c.getAttribute(Blacklist.BLACKLIST_URL);
					}
					if (location == null) {
						Blacklist.LOG.warn("Bundle blacklist URI is empty. Ignoring.");
					}else {
						try {
							bundleBlacklist.add(new LocationPattern(location));
						} catch (IllegalArgumentException e) {
							Blacklist.LOG.warn((((("Problem parsing bundle blacklist URI \"" + location) + "\": ") + (e.getMessage())) + ". Ignoring."));
						}
					}
					break;
			}
		}
	}

	public boolean isRepositoryBlacklisted(String uri) throws MalformedURLException {
		for (LocationPattern pattern : repositoryBlacklist) {
			if (pattern.matches(uri)) {
				return true;
			}
		}
		return false;
	}

	public boolean isFeatureBlacklisted(String name, String version) {
		for (FeaturePattern pattern : featureBlacklist) {
			if (pattern.matches(name, version)) {
				return true;
			}
		}
		return false;
	}

	public boolean isBundleBlacklisted(String uri) {
		for (LocationPattern pattern : bundleBlacklist) {
			if (pattern.matches(uri)) {
				return true;
			}
		}
		return false;
	}

	public void merge(Blacklist others) {
		Clause[] ours = this.clauses;
		if (ours == null) {
			this.clauses = Arrays.copyOf(others.clauses, others.clauses.length);
		}else
			if ((others != null) && ((others.clauses.length) > 0)) {
				this.clauses = new Clause[(ours.length) + (others.clauses.length)];
				System.arraycopy(ours, 0, this.clauses, 0, ours.length);
				System.arraycopy(others.clauses, ours.length, this.clauses, 0, others.clauses.length);
			}

		if (others != null) {
			this.repositoryBlacklist.addAll(others.repositoryBlacklist);
			this.featureBlacklist.addAll(others.featureBlacklist);
			this.bundleBlacklist.addAll(others.bundleBlacklist);
		}
	}

	public Clause[] getClauses() {
		return clauses;
	}

	public void blacklist(Features featuresModel) {
	}

	public void blacklistRepository(LocationPattern locationPattern) {
		repositoryBlacklist.add(locationPattern);
	}

	public void blacklistFeature(FeaturePattern featurePattern) {
		featureBlacklist.add(featurePattern);
	}

	public void blacklistBundle(LocationPattern locationPattern) {
		bundleBlacklist.add(locationPattern);
	}

	public List<LocationPattern> getRepositoryBlacklist() {
		return repositoryBlacklist;
	}

	public List<FeaturePattern> getFeatureBlacklist() {
		return featureBlacklist;
	}

	public List<LocationPattern> getBundleBlacklist() {
		return bundleBlacklist;
	}
}

