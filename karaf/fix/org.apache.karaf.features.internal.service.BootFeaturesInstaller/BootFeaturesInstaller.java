

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import org.apache.karaf.features.BootFinished;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.internal.service.FeaturesServiceImpl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.karaf.features.FeaturesService.Option.NoFailOnFeatureNotFound;


public class BootFeaturesInstaller {
	private static final Logger LOGGER = LoggerFactory.getLogger(BootFeaturesInstaller.class);

	private final FeaturesServiceImpl featuresService;

	private final BundleContext bundleContext;

	private final String[] repositories;

	private final String features;

	private final boolean asynchronous;

	private static final char UNIX_SEPARATOR = '/';

	private static final char WINDOWS_SEPARATOR = '\\';

	private static final char SYSTEM_SEPARATOR = File.separatorChar;

	public BootFeaturesInstaller(BundleContext bundleContext, FeaturesServiceImpl featuresService, String[] repositories, String features, boolean asynchronous) {
		this.bundleContext = bundleContext;
		this.featuresService = featuresService;
		this.repositories = repositories;
		this.features = features;
		this.asynchronous = asynchronous;
	}

	public void start() {
		if (asynchronous) {
			new Thread("Initial Features Provisioning") {
				public void run() {
					installBootFeatures();
				}
			}.start();
		}else {
			installBootFeatures();
		}
	}

	protected void installBootFeatures() {
		try {
			addRepositories();
			List<Set<String>> stagedFeatures = parseBootFeatures(features);
			for (Set<String> features : stagedFeatures) {
				featuresService.installFeatures(features, EnumSet.of(NoFailOnFeatureNotFound));
			}
			publishBootFinished();
		} catch (Throwable e) {
			if (e instanceof IllegalStateException) {
				try {
					bundleContext.getBundle();
				} catch (IllegalStateException ies) {
					return;
				}
			}
			BootFeaturesInstaller.LOGGER.error("Error installing boot features", e);
		}
	}

	private void addRepositories() {
		for (String repo : repositories) {
			repo = repo.trim();
			if (!(repo.isEmpty())) {
				repo = separatorsToUnix(repo);
				try {
					featuresService.addRepository(URI.create(repo));
				} catch (Exception e) {
					BootFeaturesInstaller.LOGGER.error(("Error installing boot feature repository " + repo), e);
				}
			}
		}
	}

	protected List<Set<String>> parseBootFeatures(String bootFeatures) {
		List<Set<String>> stages = new ArrayList<>();
		StringTokenizer tokenizer = new StringTokenizer(bootFeatures, " \t\r\n,()", true);
		int paren = 0;
		Set<String> stage = new LinkedHashSet<>();
		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			if (token.equals("(")) {
				if (paren == 0) {
					if (!(stage.isEmpty())) {
						stages.add(stage);
						stage = new LinkedHashSet<>();
					}
					paren++;
				}else {
					throw new IllegalArgumentException((("Bad syntax in boot features: '" + bootFeatures) + "'"));
				}
			}else
				if (token.equals(")")) {
					if (paren == 1) {
						if (!(stage.isEmpty())) {
							stages.add(stage);
							stage = new LinkedHashSet<>();
						}
						paren--;
					}else {
						throw new IllegalArgumentException((("Bad syntax in boot features: '" + bootFeatures) + "'"));
					}
				}else
					if (!(token.matches("[ \t\r\n]+|,"))) {
						stage.add(token);
					}


		} 
		if (!(stage.isEmpty())) {
			stages.add(stage);
		}
		return stages;
	}

	private void publishBootFinished() {
		if ((bundleContext) != null) {
			BootFinished bootFinished = new BootFinished() {};
			bundleContext.registerService(BootFinished.class, bootFinished, new Hashtable<String, String>());
		}
	}

	private String separatorsToUnix(String path) {
		if ((BootFeaturesInstaller.SYSTEM_SEPARATOR) == (BootFeaturesInstaller.WINDOWS_SEPARATOR)) {
			if ((path == null) || ((path.indexOf(BootFeaturesInstaller.WINDOWS_SEPARATOR)) == (-1))) {
				return path;
			}
			path = path.replace(BootFeaturesInstaller.WINDOWS_SEPARATOR, BootFeaturesInstaller.UNIX_SEPARATOR);
			BootFeaturesInstaller.LOGGER.debug("Converted path to unix separators: {}", path);
		}
		return path;
	}
}

