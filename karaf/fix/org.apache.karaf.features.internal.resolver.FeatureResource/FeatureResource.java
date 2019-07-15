

import java.util.List;
import java.util.Map;
import org.apache.felix.utils.resource.RequirementImpl;
import org.apache.felix.utils.resource.ResourceBuilder;
import org.apache.felix.utils.resource.ResourceImpl;
import org.apache.felix.utils.version.VersionRange;
import org.apache.felix.utils.version.VersionTable;
import org.apache.karaf.features.Blacklisting;
import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.Capability;
import org.apache.karaf.features.Conditional;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.Requirement;
import org.apache.karaf.features.internal.model.Dependency;
import org.apache.karaf.features.internal.resolver.ResourceUtils;
import org.apache.karaf.features.internal.util.Macro;
import org.osgi.framework.BundleException;
import org.osgi.framework.Version;
import org.osgi.resource.Resource;


public final class FeatureResource extends ResourceImpl {
	public static final String REQUIREMENT_CONDITIONAL_DIRECTIVE = "condition";

	public static final String CONDITIONAL_TRUE = "true";

	private final Feature feature;

	private FeatureResource(Feature feature) {
		super(feature.getName(), ResourceUtils.TYPE_FEATURE, VersionTable.getVersion(feature.getVersion()));
		this.feature = feature;
	}

	public static FeatureResource build(Feature feature, Conditional conditional, String featureRange, Map<String, ? extends Resource> locToRes) throws BundleException {
		Feature fcond = conditional.asFeature();
		FeatureResource resource = FeatureResource.build(fcond, featureRange, locToRes);
		for (String cond : conditional.getCondition()) {
			if (cond.startsWith("req:")) {
				cond = cond.substring("req:".length());
			}else {
				Dependency dep = new Dependency();
				String[] p = cond.split("/");
				dep.setName(p[0]);
				if ((p.length) > 1) {
					dep.setVersion(p[1]);
				}
				FeatureResource.addDependency(resource, dep, featureRange, true);
			}
		}
		Dependency dep = new Dependency();
		dep.setName(feature.getName());
		dep.setVersion(feature.getVersion());
		FeatureResource.addDependency(resource, dep, featureRange, true);
		return resource;
	}

	public static FeatureResource build(Feature feature, String featureRange, Map<String, ? extends Resource> locToRes) throws BundleException {
		FeatureResource resource = new FeatureResource(feature);
		for (BundleInfo info : feature.getBundles()) {
			if ((!(info.isDependency())) && (!(info.isBlacklisted()))) {
				Resource res = locToRes.get(info.getLocation());
				if (res == null) {
					throw new IllegalStateException(("Resource not found for url " + (info.getLocation())));
				}
				ResourceUtils.addIdentityRequirement(resource, res);
			}
		}
		for (org.apache.karaf.features.Dependency dep : feature.getDependencies()) {
			if ((!(dep.isDependency())) && (!(dep.isBlacklisted()))) {
				FeatureResource.addDependency(resource, dep, featureRange);
			}
		}
		for (Capability cap : feature.getCapabilities()) {
			resource.addCapabilities(ResourceBuilder.parseCapability(resource, cap.getValue()));
		}
		for (Requirement req : feature.getRequirements()) {
			resource.addRequirements(ResourceBuilder.parseRequirement(resource, req.getValue()));
		}
		return resource;
	}

	protected static void addDependency(FeatureResource resource, org.apache.karaf.features.Dependency dep, String featureRange) {
		FeatureResource.addDependency(resource, dep, featureRange, false);
	}

	protected static void addDependency(FeatureResource resource, org.apache.karaf.features.Dependency dep, String featureRange, boolean condition) {
		String name = dep.getName();
		String version = dep.getVersion();
		if (version.equals("0.0.0")) {
			version = null;
		}else
			if ((!(version.startsWith("["))) && (!(version.startsWith("(")))) {
				version = Macro.transform(featureRange, version);
			}

		ResourceUtils.addIdentityRequirement(resource, name, ResourceUtils.TYPE_FEATURE, (version != null ? new VersionRange(version) : null), true, condition);
	}

	public Feature getFeature() {
		return feature;
	}
}

