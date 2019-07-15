

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.felix.utils.collections.StringArrayMap;
import org.apache.felix.utils.manifest.Clause;
import org.apache.felix.utils.manifest.Parser;
import org.apache.felix.utils.repository.BaseRepository;
import org.apache.felix.utils.resource.CapabilityImpl;
import org.apache.felix.utils.resource.RequirementImpl;
import org.apache.felix.utils.resource.ResourceBuilder;
import org.apache.felix.utils.resource.ResourceImpl;
import org.apache.felix.utils.resource.SimpleFilter;
import org.apache.felix.utils.version.VersionRange;
import org.apache.felix.utils.version.VersionTable;
import org.apache.karaf.features.Blacklisting;
import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.Conditional;
import org.apache.karaf.features.Dependency;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesNamespaces;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.Library;
import org.apache.karaf.features.ScopeFilter;
import org.apache.karaf.features.Scoping;
import org.apache.karaf.features.internal.download.DownloadCallback;
import org.apache.karaf.features.internal.download.DownloadManager;
import org.apache.karaf.features.internal.download.Downloader;
import org.apache.karaf.features.internal.download.StreamProvider;
import org.apache.karaf.features.internal.region.RepositoryManager;
import org.apache.karaf.features.internal.region.SubsystemResolverCallback;
import org.apache.karaf.features.internal.resolver.FeatureResource;
import org.apache.karaf.features.internal.resolver.ResolverUtil;
import org.apache.karaf.features.internal.resolver.ResourceUtils;
import org.apache.karaf.features.internal.service.Overrides;
import org.apache.karaf.features.internal.util.MapUtils;
import org.eclipse.equinox.region.RegionFilter;
import org.osgi.framework.BundleException;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

import static org.apache.karaf.features.BundleInfo.BundleOverrideMode.OSGI;
import static org.apache.karaf.features.FeaturesService.ServiceRequirementsBehavior.Default;
import static org.apache.karaf.features.FeaturesService.ServiceRequirementsBehavior.Disable;


public class Subsystem extends ResourceImpl {
	private static final String ALL_FILTER = "(|(!(all=*))(all=*))";

	private static final String SUBSYSTEM_FILTER = String.format("(%s=%s)", IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE, ResourceUtils.TYPE_SUBSYSTEM);

	private static final String FEATURE_FILTER = String.format("(%s=%s)", IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE, ResourceUtils.TYPE_FEATURE);

	private static final String SUBSYSTEM_OR_FEATURE_FILTER = String.format("(|%s%s)", Subsystem.SUBSYSTEM_FILTER, Subsystem.FEATURE_FILTER);

	private static final Map<String, Set<String>> SHARE_ALL_POLICY = Collections.singletonMap(RegionFilter.VISIBLE_ALL_NAMESPACE, Collections.singleton(Subsystem.ALL_FILTER));

	private static final Map<String, Set<String>> SHARE_NONE_POLICY = Collections.singletonMap(IdentityNamespace.IDENTITY_NAMESPACE, Collections.singleton(Subsystem.SUBSYSTEM_FILTER));

	private final String name;

	private final boolean acceptDependencies;

	private final Subsystem parent;

	private final Feature feature;

	private final boolean mandatory;

	private final List<Subsystem> children = new ArrayList<>();

	private final Map<String, Set<String>> importPolicy;

	private final Map<String, Set<String>> exportPolicy;

	private final List<Resource> installable = new ArrayList<>();

	private final Map<String, Subsystem.DependencyInfo> dependencies = new HashMap<>();

	private final List<Requirement> dependentFeatures = new ArrayList<>();

	private final List<String> bundles = new ArrayList<>();

	public Subsystem(String name) {
		super(name, ResourceUtils.TYPE_SUBSYSTEM, Version.emptyVersion);
		this.name = name;
		this.parent = null;
		this.acceptDependencies = true;
		this.feature = null;
		this.importPolicy = Subsystem.SHARE_NONE_POLICY;
		this.exportPolicy = Subsystem.SHARE_NONE_POLICY;
		this.mandatory = true;
	}

	public Subsystem(String name, Feature feature, Subsystem parent, boolean mandatory) {
		super(name, ResourceUtils.TYPE_SUBSYSTEM, Version.emptyVersion);
		this.name = name;
		this.parent = parent;
		this.acceptDependencies = ((feature.getScoping()) != null) && (feature.getScoping().acceptDependencies());
		this.feature = feature;
		this.mandatory = mandatory;
		if ((feature.getScoping()) != null) {
			this.importPolicy = createPolicy(feature.getScoping().getImports());
			this.importPolicy.put(IdentityNamespace.IDENTITY_NAMESPACE, Collections.singleton(Subsystem.SUBSYSTEM_OR_FEATURE_FILTER));
			this.exportPolicy = createPolicy(feature.getScoping().getExports());
			this.exportPolicy.put(IdentityNamespace.IDENTITY_NAMESPACE, Collections.singleton(Subsystem.SUBSYSTEM_OR_FEATURE_FILTER));
		}else {
			this.importPolicy = Subsystem.SHARE_ALL_POLICY;
			this.exportPolicy = Subsystem.SHARE_ALL_POLICY;
		}
		ResourceUtils.addIdentityRequirement(this, feature.getName(), ResourceUtils.TYPE_FEATURE, new VersionRange(VersionTable.getVersion(feature.getVersion()), true));
	}

	public Subsystem(String name, Subsystem parent, boolean acceptDependencies, boolean mandatory) {
		super(name, ResourceUtils.TYPE_SUBSYSTEM, Version.emptyVersion);
		this.name = name;
		this.parent = parent;
		this.acceptDependencies = acceptDependencies;
		this.feature = null;
		this.mandatory = mandatory;
		this.importPolicy = Subsystem.SHARE_ALL_POLICY;
		this.exportPolicy = Subsystem.SHARE_NONE_POLICY;
	}

	public List<Resource> getInstallable() {
		return installable;
	}

	public String getName() {
		return name;
	}

	public Subsystem getParent() {
		return parent;
	}

	public Collection<Subsystem> getChildren() {
		return children;
	}

	public Subsystem getChild(String name) {
		for (Subsystem child : children) {
			if (child.getName().equals(name)) {
				return child;
			}
		}
		return null;
	}

	public boolean isAcceptDependencies() {
		return acceptDependencies;
	}

	public Map<String, Set<String>> getImportPolicy() {
		return importPolicy;
	}

	public Map<String, Set<String>> getExportPolicy() {
		return exportPolicy;
	}

	public Feature getFeature() {
		return feature;
	}

	public Subsystem createSubsystem(String name, boolean acceptDependencies) {
		if ((feature) != null) {
			throw new UnsupportedOperationException("Can not create application subsystems inside a feature subsystem");
		}
		String childName = ((getName()) + "/") + name;
		Subsystem as = new Subsystem(childName, this, acceptDependencies, true);
		children.add(as);
		ResourceUtils.addIdentityRequirement(this, childName, ResourceUtils.TYPE_SUBSYSTEM, ((VersionRange) (null)));
		installable.add(as);
		return as;
	}

	public void addSystemResource(Resource resource) {
		installable.add(resource);
	}

	public void requireFeature(String name, String range, boolean mandatory) {
		if (mandatory) {
			ResourceUtils.addIdentityRequirement(this, name, ResourceUtils.TYPE_FEATURE, range);
		}else {
			ResourceImpl res = new ResourceImpl();
			ResourceUtils.addIdentityRequirement(res, name, ResourceUtils.TYPE_FEATURE, range, false);
			dependentFeatures.addAll(res.getRequirements(null));
		}
	}

	public void require(String requirement) throws BundleException {
		int idx = requirement.indexOf(':');
		String type;
		String req;
		if (idx >= 0) {
			type = requirement.substring(0, idx);
			req = requirement.substring((idx + 1));
		}else {
			type = "feature";
			req = requirement;
		}
		switch (type) {
			case "feature" :
				addRequirement(ResourceUtils.toFeatureRequirement(req));
				break;
			case "requirement" :
				addRequirement(req);
				break;
			case "bundle" :
				bundles.add(req);
				break;
		}
	}

	protected void addRequirement(String requirement) throws BundleException {
		for (Requirement req : ResourceBuilder.parseRequirement(this, requirement)) {
			Object range = req.getAttributes().get(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE);
			if (range instanceof String) {
				req.getAttributes().put(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE, new VersionRange(((String) (range))));
			}
			addRequirement(req);
		}
	}

	public Map<String, BundleInfo> getBundleInfos() {
		Map<String, BundleInfo> infos = new HashMap<>();
		for (Subsystem.DependencyInfo di : dependencies.values()) {
			infos.put(di.getLocation(), di);
		}
		return infos;
	}

	@SuppressWarnings("InfiniteLoopStatement")
	public void build(Map<String, List<Feature>> allFeatures) throws Exception {
		doBuild(allFeatures, true);
	}

	private void doBuild(Map<String, List<Feature>> allFeatures, boolean mandatory) throws Exception {
		for (Subsystem child : children) {
			child.doBuild(allFeatures, true);
		}
		if ((feature) != null) {
			for (Dependency dep : feature.getDependencies()) {
				if (dep.isBlacklisted()) {
					continue;
				}
				Subsystem ss = this;
				while (!(ss.isAcceptDependencies())) {
					ss = ss.getParent();
				} 
				ss.requireFeature(dep.getName(), dep.getVersion(), false);
			}
			for (Conditional cond : feature.getConditional()) {
				if (cond.isBlacklisted()) {
					continue;
				}
				Feature fcond = cond.asFeature();
				String ssName = ((this.name) + "#") + (fcond.hasVersion() ? ((fcond.getName()) + "-") + (fcond.getVersion()) : fcond.getName());
				Subsystem fs = getChild(ssName);
				if (fs == null) {
					fs = new Subsystem(ssName, fcond, this, true);
					fs.doBuild(allFeatures, false);
					installable.add(fs);
					children.add(fs);
				}
			}
		}
		List<Requirement> processed = new ArrayList<>();
		while (true) {
			List<Requirement> requirements = getRequirements(IdentityNamespace.IDENTITY_NAMESPACE);
			requirements.addAll(dependentFeatures);
			requirements.removeAll(processed);
			if (requirements.isEmpty()) {
				break;
			}
			for (Requirement requirement : requirements) {
				String name = ((String) (requirement.getAttributes().get(IdentityNamespace.IDENTITY_NAMESPACE)));
				String type = ((String) (requirement.getAttributes().get(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE)));
				VersionRange range = ((VersionRange) (requirement.getAttributes().get(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE)));
				if ((ResourceUtils.TYPE_FEATURE.equals(type)) && (allFeatures.containsKey(name))) {
					for (Feature feature : allFeatures.get(name)) {
						if ((range == null) || (range.contains(VersionTable.getVersion(feature.getVersion())))) {
							if ((feature != (this.feature)) && (!(feature.isBlacklisted()))) {
								String ssName = ((this.name) + "#") + (feature.hasVersion() ? ((feature.getName()) + "-") + (feature.getVersion()) : feature.getName());
								Subsystem fs = getChild(ssName);
								if (fs == null) {
									fs.build(allFeatures);
									installable.add(fs);
									children.add(fs);
								}
							}
						}
					}
				}
				processed.add(requirement);
			}
		} 
	}

	public Set<String> collectPrerequisites() {
		Set<String> prereqs = new HashSet<>();
		doCollectPrerequisites(prereqs);
		return prereqs;
	}

	private void doCollectPrerequisites(Set<String> prereqs) {
		for (Subsystem child : children) {
			child.doCollectPrerequisites(prereqs);
		}
		if ((feature) != null) {
			boolean match = false;
			for (String prereq : prereqs) {
				String[] p = prereq.split("/");
				if ((feature.getName().equals(p[0])) && (VersionRange.parseVersionRange(p[1]).contains(Version.parseVersion(feature.getVersion())))) {
					match = true;
					break;
				}
			}
			if (!match) {
				for (Dependency dep : feature.getDependencies()) {
					if (dep.isPrerequisite()) {
						prereqs.add(dep.toString());
					}
				}
			}
		}
	}

	@SuppressWarnings("InfiniteLoopStatement")
	public void downloadBundles(DownloadManager manager, String featureResolutionRange, final FeaturesService.ServiceRequirementsBehavior serviceRequirements, RepositoryManager repos, SubsystemResolverCallback callback) throws Exception {
		for (Subsystem child : children) {
			child.downloadBundles(manager, featureResolutionRange, serviceRequirements, repos, callback);
		}
		final Map<BundleInfo, Conditional> infos = new HashMap<>();
		final Downloader downloader = manager.createDownloader();
		if ((feature) != null) {
			for (Conditional cond : feature.getConditional()) {
				if (!(cond.isBlacklisted())) {
					for (final BundleInfo bi : cond.getBundles()) {
						infos.put(bi, cond);
					}
				}
			}
			for (BundleInfo bi : feature.getBundles()) {
				infos.put(bi, null);
			}
		}
		for (Iterator<BundleInfo> iterator = infos.keySet().iterator(); iterator.hasNext();) {
			BundleInfo bi = iterator.next();
			if (bi.isBlacklisted()) {
				iterator.remove();
				if (callback != null) {
					callback.bundleBlacklisted(bi);
				}
			}
		}
		final Map<String, ResourceImpl> bundles = new ConcurrentHashMap<>();
		final Map<String, ResourceImpl> overrides = new ConcurrentHashMap<>();
		boolean removeServiceRequirements = serviceRequirementsBehavior(feature, serviceRequirements);
		for (Map.Entry<BundleInfo, Conditional> entry : infos.entrySet()) {
			final BundleInfo bi = entry.getKey();
			final String loc = bi.getLocation();
			downloader.download(loc, ( provider) -> {
				ResourceImpl resource = createResource(loc, getMetadata(provider), removeServiceRequirements);
				bundles.put(loc, resource);
				if ((bi.isOverriden()) == (OSGI)) {
					downloader.download(bi.getOriginalLocation(), ( provider2) -> {
						ResourceImpl originalResource = createResource(bi.getOriginalLocation(), getMetadata(provider2), removeServiceRequirements);
						bundles.put(bi.getOriginalLocation(), originalResource);
						overrides.put(loc, originalResource);
					});
				}
			});
		}
		for (Clause bundle : Parser.parseClauses(this.bundles.toArray(new String[this.bundles.size()]))) {
			final String loc = bundle.getName();
			downloader.download(loc, ( provider) -> {
				bundles.put(loc, createResource(loc, getMetadata(provider), removeServiceRequirements));
			});
		}
		if ((feature) != null) {
			for (Library library : feature.getLibraries()) {
				if (library.isExport()) {
					final String loc = library.getLocation();
					downloader.download(loc, ( provider) -> {
						bundles.put(loc, createResource(loc, getMetadata(provider), removeServiceRequirements));
					});
				}
			}
		}
		downloader.await();
		Overrides.override(bundles, overrides);
		if ((feature) != null) {
			Map<Conditional, Resource> resConds = new HashMap<>();
			for (Conditional cond : feature.getConditional()) {
				if (cond.isBlacklisted()) {
					continue;
				}
				FeatureResource resCond = FeatureResource.build(feature, cond, featureResolutionRange, bundles);
				ResourceUtils.addIdentityRequirement(this, resCond, false);
				ResourceUtils.addIdentityRequirement(resCond, this, true);
				installable.add(resCond);
				resConds.put(cond, resCond);
			}
			FeatureResource resFeature = FeatureResource.build(feature, featureResolutionRange, bundles);
			ResourceUtils.addIdentityRequirement(resFeature, this);
			installable.add(resFeature);
			for (Map.Entry<BundleInfo, Conditional> entry : infos.entrySet()) {
				final BundleInfo bi = entry.getKey();
				final String loc = bi.getLocation();
				final Conditional cond = entry.getValue();
				ResourceImpl res = bundles.get(loc);
				int sl = ((bi.getStartLevel()) <= 0) ? feature.getStartLevel() : bi.getStartLevel();
				if (cond != null) {
					ResourceUtils.addIdentityRequirement(res, resConds.get(cond), true);
				}
				boolean mandatory = (!(bi.isDependency())) && (cond == null);
				if (bi.isDependency()) {
					addDependency(res, mandatory, bi.isStart(), sl, bi.isBlacklisted());
				}else {
					doAddDependency(res, mandatory, bi.isStart(), sl, bi.isBlacklisted());
				}
			}
			for (Library library : feature.getLibraries()) {
				if (library.isExport()) {
					final String loc = library.getLocation();
					ResourceImpl res = bundles.get(loc);
					addDependency(res, false, false, 0, false);
				}
			}
			for (String uri : feature.getResourceRepositories()) {
				BaseRepository repo = repos.getRepository(feature.getRepositoryUrl(), uri);
				for (Resource resource : repo.getResources()) {
					ResourceImpl res = cloneResource(resource);
					addDependency(res, false, true, 0, false);
				}
			}
		}
		for (Clause bundle : Parser.parseClauses(this.bundles.toArray(new String[this.bundles.size()]))) {
			final String loc = bundle.getName();
			boolean dependency = Boolean.parseBoolean(bundle.getAttribute("dependency"));
			boolean start = ((bundle.getAttribute("start")) == null) || (Boolean.parseBoolean(bundle.getAttribute("start")));
			boolean blacklisted = ((bundle.getAttribute("blacklisted")) != null) && (Boolean.parseBoolean(bundle.getAttribute("blacklisted")));
			int startLevel = 0;
			try {
				startLevel = Integer.parseInt(bundle.getAttribute("start-level"));
			} catch (NumberFormatException e) {
			}
			if (dependency) {
				addDependency(bundles.get(loc), false, start, startLevel, blacklisted);
			}else {
				doAddDependency(bundles.get(loc), true, start, startLevel, blacklisted);
				ResourceUtils.addIdentityRequirement(this, bundles.get(loc));
			}
		}
		for (Subsystem.DependencyInfo info : dependencies.values()) {
			installable.add(info.resource);
			ResourceUtils.addIdentityRequirement(info.resource, this, info.mandatory);
		}
	}

	private boolean serviceRequirementsBehavior(Feature feature, FeaturesService.ServiceRequirementsBehavior serviceRequirements) {
		if ((Disable) == serviceRequirements) {
			return true;
		}else
			if ((feature != null) && ((Default) == serviceRequirements)) {
				return (((FeaturesNamespaces.URI_1_0_0.equals(feature.getNamespace())) || (FeaturesNamespaces.URI_1_1_0.equals(feature.getNamespace()))) || (FeaturesNamespaces.URI_1_2_0.equals(feature.getNamespace()))) || (FeaturesNamespaces.URI_1_2_1.equals(feature.getNamespace()));
			}else {
				return false;
			}

	}

	ResourceImpl cloneResource(Resource resource) {
		ResourceImpl res = new ResourceImpl();
		for (Capability cap : resource.getCapabilities(null)) {
			res.addCapability(new CapabilityImpl(res, cap.getNamespace(), new StringArrayMap<>(cap.getDirectives()), new StringArrayMap<>(cap.getAttributes())));
		}
		for (Requirement req : resource.getRequirements(null)) {
			SimpleFilter sf;
			if (req instanceof RequirementImpl) {
				sf = ((RequirementImpl) (req)).getFilter();
			}else
				if (req.getDirectives().containsKey(Namespace.REQUIREMENT_FILTER_DIRECTIVE)) {
					sf = SimpleFilter.parse(req.getDirectives().get(Namespace.REQUIREMENT_FILTER_DIRECTIVE));
				}else {
					sf = SimpleFilter.convert(req.getAttributes());
				}

			res.addRequirement(new RequirementImpl(res, req.getNamespace(), new StringArrayMap<>(req.getDirectives()), new StringArrayMap<>(req.getAttributes()), sf));
		}
		return res;
	}

	Map<String, String> getMetadata(StreamProvider provider) throws IOException {
		try (ZipInputStream zis = new ZipInputStream(provider.open())) {
			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				if (JarFile.MANIFEST_NAME.equals(entry.getName())) {
					Attributes attributes = new Manifest(zis).getMainAttributes();
					Map<String, String> headers = new HashMap<>();
					for (Map.Entry attr : attributes.entrySet()) {
						headers.put(attr.getKey().toString(), attr.getValue().toString());
					}
					return headers;
				}
			} 
		}
		throw new IllegalArgumentException((("Resource " + (provider.getUrl())) + " does not contain a manifest"));
	}

	void addDependency(ResourceImpl resource, boolean mandatory, boolean start, int startLevel, boolean blacklisted) {
		if (isAcceptDependencies()) {
			doAddDependency(resource, mandatory, start, startLevel, blacklisted);
		}else {
			parent.addDependency(resource, mandatory, start, startLevel, blacklisted);
		}
	}

	private void doAddDependency(ResourceImpl resource, boolean mandatory, boolean start, int startLevel, boolean blacklisted) {
		String id = ((ResolverUtil.getSymbolicName(resource)) + "|") + (ResolverUtil.getVersion(resource));
		Subsystem.DependencyInfo info = new Subsystem.DependencyInfo(resource, mandatory, start, startLevel, blacklisted);
		dependencies.merge(id, info, this::merge);
	}

	private Subsystem.DependencyInfo merge(Subsystem.DependencyInfo di1, Subsystem.DependencyInfo di2) {
		Subsystem.DependencyInfo info = new Subsystem.DependencyInfo();
		if ((di1.resource) != (di2.resource)) {
			Requirement r1 = getFirstIdentityReq(di1.resource);
			Requirement r2 = getFirstIdentityReq(di2.resource);
			if (r1 == null) {
				info.resource = di1.resource;
			}else
				if (r2 == null) {
					info.resource = di2.resource;
				}else {
					String id = ((ResolverUtil.getSymbolicName(di1.resource)) + "/") + (ResolverUtil.getVersion(di1.resource));
					throw new IllegalStateException(((((((("Resource " + id) + " is duplicated on subsystem ") + (this.toString())) + ". First resource requires ") + r1) + " while the second requires ") + r2));
				}

		}else {
			info.resource = di1.resource;
		}
		info.mandatory = (di1.mandatory) | (di2.mandatory);
		info.start = (di1.start) | (di2.start);
		if (((di1.startLevel) > 0) && ((di2.startLevel) > 0)) {
			info.startLevel = Math.min(di1.startLevel, di2.startLevel);
		}else {
			info.startLevel = Math.max(di1.startLevel, di2.startLevel);
		}
		return info;
	}

	private RequirementImpl getFirstIdentityReq(ResourceImpl resource) {
		for (Requirement r : resource.getRequirements(null)) {
			if (IdentityNamespace.IDENTITY_NAMESPACE.equals(r.getNamespace())) {
				return ((RequirementImpl) (r));
			}
		}
		return null;
	}

	class DependencyInfo implements BundleInfo {
		ResourceImpl resource;

		boolean mandatory;

		boolean start;

		int startLevel;

		boolean blacklisted;

		BundleInfo.BundleOverrideMode overriden;

		public DependencyInfo() {
		}

		public DependencyInfo(ResourceImpl resource, boolean mandatory, boolean start, int startLevel, boolean blacklisted) {
			this.resource = resource;
			this.mandatory = mandatory;
			this.start = start;
			this.startLevel = startLevel;
			this.blacklisted = blacklisted;
		}

		@Override
		public boolean isStart() {
			return start;
		}

		@Override
		public int getStartLevel() {
			return startLevel;
		}

		@Override
		public String getLocation() {
			return ResourceUtils.getUri(resource);
		}

		@Override
		public String getOriginalLocation() {
			return ResourceUtils.getUri(resource);
		}

		@Override
		public boolean isDependency() {
			return !(mandatory);
		}

		@Override
		public boolean isBlacklisted() {
			return blacklisted;
		}

		@Override
		public BundleInfo.BundleOverrideMode isOverriden() {
			return overriden;
		}

		public void setOverriden(BundleInfo.BundleOverrideMode overriden) {
			this.overriden = overriden;
		}

		@Override
		public String toString() {
			return (("DependencyInfo{" + "resource=") + (resource)) + '}';
		}
	}

	Map<String, Set<String>> createPolicy(List<? extends ScopeFilter> filters) {
		Map<String, Set<String>> policy = new HashMap<>();
		for (ScopeFilter filter : filters) {
			MapUtils.addToMapSet(policy, filter.getNamespace(), filter.getFilter());
		}
		return policy;
	}

	ResourceImpl createResource(String uri, Map<String, String> headers, boolean removeServiceRequirements) throws Exception {
		try {
			return ResourceBuilder.build(uri, headers, removeServiceRequirements);
		} catch (BundleException e) {
			throw new Exception(("Unable to create resource for bundle " + uri), e);
		}
	}

	@Override
	public String toString() {
		return getName();
	}
}

