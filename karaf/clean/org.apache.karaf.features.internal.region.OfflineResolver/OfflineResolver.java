import org.apache.karaf.features.internal.region.*;


import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.felix.resolver.Logger;
import org.apache.felix.resolver.ResolverImpl;
import org.apache.felix.utils.repository.BaseRepository;
import org.apache.felix.utils.resource.RequirementImpl;
import org.apache.felix.utils.resource.ResourceBuilder;
import org.apache.felix.utils.resource.ResourceImpl;
import org.apache.felix.utils.resource.SimpleFilter;
import org.apache.karaf.util.json.JsonReader;
import org.osgi.framework.BundleException;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.osgi.resource.Wiring;
import org.osgi.service.repository.Repository;
import org.osgi.service.resolver.HostedCapability;
import org.osgi.service.resolver.ResolveContext;
import org.osgi.service.resolver.Resolver;

import static org.osgi.framework.Constants.RESOLUTION_DIRECTIVE;
import static org.osgi.framework.Constants.RESOLUTION_OPTIONAL;
import static org.osgi.framework.namespace.IdentityNamespace.IDENTITY_NAMESPACE;

public class OfflineResolver {

    public static void main(String[] args) throws Exception {
        if (args == null || args.length != 1) {
            throw new IllegalArgumentException("File path expected");
        }
        resolve(args[0]);
    }

    public static void resolve(String resolutionFile) throws Exception {
        Map<String, Object> resolution;
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(resolutionFile), StandardCharsets.UTF_8)) {
            resolution = (Map<String, Object>) JsonReader.read(reader);
        }

        final Repository globalRepository;
        if (resolution.containsKey("globalRepository")) {
            globalRepository = readRepository(resolution.get("globalRepository"));
        } else {
            globalRepository = null;
        }
        final Repository repository = readRepository(resolution.get("repository"));

        Resolver resolver = new ResolverImpl(new Logger(Logger.LOG_ERROR));
        Map<Resource, List<Wire>> wiring = resolver.resolve(new ResolveContext() {
            private final CandidateComparator candidateComparator = new CandidateComparator(r -> 0);

            @Override
            public Collection<Resource> getMandatoryResources() {
                List<Resource> resources = new ArrayList<>();
                Requirement req = new RequirementImpl(
                        null,
                        IDENTITY_NAMESPACE,
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        SimpleFilter.parse("(" + IDENTITY_NAMESPACE + "=root)"));
                Collection<Capability> identities = repository.findProviders(Collections.singleton(req)).get(req);
                for (Capability identity : identities) {
                    resources.add(identity.getResource());
                }
                return resources;
            }

            @Override
            public List<Capability> findProviders(Requirement requirement) {
                List<Capability> caps = new ArrayList<>();
                Map<Requirement, Collection<Capability>> resMap =
                        repository.findProviders(Collections.singleton(requirement));
                Collection<Capability> res = resMap != null ? resMap.get(requirement) : null;
                if (res != null && !res.isEmpty()) {
                    caps.addAll(res);
                } else if (globalRepository != null) {
                    // Only bring in external resources for non optional requirements
                    if (!RESOLUTION_OPTIONAL.equals(requirement.getDirectives().get(RESOLUTION_DIRECTIVE))) {
                        resMap = globalRepository.findProviders(Collections.singleton(requirement));
                        res = resMap != null ? resMap.get(requirement) : null;
                        if (res != null && !res.isEmpty()) {
                            caps.addAll(res);
                        }
                    }
                }

                // Sort caps
                Collections.sort(caps, candidateComparator);
                return caps;
            }

            @Override
            public int insertHostedCapability(List<Capability> capabilities, HostedCapability hostedCapability) {
                int idx = Collections.binarySearch(capabilities, hostedCapability, candidateComparator);
                if (idx < 0) {
                    idx = Math.abs(idx + 1);
                }
                capabilities.add(idx, hostedCapability);
                return idx;
            }

            @Override
            public boolean isEffective(Requirement requirement) {
                return true;
            }

            @Override
            public Map<Resource, Wiring> getWirings() {
                return Collections.emptyMap();
            }
        });
    }

    private static Repository readRepository(Object repository) throws BundleException {
        List<Resource> resources = new ArrayList<>();
        Collection<Map<String, List<String>>> metadatas;
        if (repository instanceof Map) {
            metadatas = ((Map<String, Map<String, List<String>>>) repository).values();
        } else {
            metadatas = (Collection<Map<String, List<String>>>) repository;
        }
        for (Map<String, List<String>> metadata : metadatas) {
            ResourceImpl res = new ResourceImpl();
            for (String cap : metadata.get("capabilities")) {
                res.addCapabilities(ResourceBuilder.parseCapability(res, cap));
            }
            if (metadata.containsKey("requirements")) {
                for (String req : metadata.get("requirements")) {
                    res.addRequirements(ResourceBuilder.parseRequirement(res, req));
                }
            }
            resources.add(res);
        }
        return new BaseRepository(resources);
    }

}
