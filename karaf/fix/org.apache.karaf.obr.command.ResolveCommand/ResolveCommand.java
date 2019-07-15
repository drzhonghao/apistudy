

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.felix.bundlerepository.Reason;
import org.apache.felix.bundlerepository.Repository;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.bundlerepository.Requirement;
import org.apache.felix.bundlerepository.Resolver;
import org.apache.felix.bundlerepository.Resource;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.osgi.framework.Version;


@Command(scope = "obr", name = "resolve", description = "Shows the resolution output for a given set of requirements.")
@Service
public class ResolveCommand {
	@Option(name = "-w", aliases = "--why", description = "Display the reason of the inclusion of the resource")
	boolean why;

	@Option(name = "-l", aliases = "--no-local", description = "Ignore local resources during resolution")
	boolean noLocal;

	@Option(name = "--no-remote", description = "Ignore remote resources during resolution")
	boolean noRemote;

	@Option(name = "--deploy", description = "Deploy the selected bundles")
	boolean deploy;

	@Option(name = "--start", description = "Deploy and start the selected bundles")
	boolean start;

	@Option(name = "--optional", description = "Resolve optional dependencies")
	boolean optional;

	@Argument(index = 0, name = "requirements", description = "Requirements", required = true, multiValued = true)
	List<String> requirements;

	protected void doExecute(RepositoryAdmin admin) throws Exception {
		List<Repository> repositories = new ArrayList<>();
		repositories.add(admin.getSystemRepository());
		if (!(noLocal)) {
			repositories.add(admin.getLocalRepository());
		}
		if (!(noRemote)) {
			repositories.addAll(Arrays.asList(admin.listRepositories()));
		}
		Resolver resolver = admin.resolver(repositories.toArray(new Repository[repositories.size()]));
		if (resolver.resolve((optional ? 0 : Resolver.NO_OPTIONAL_RESOURCES))) {
			Resource[] resources;
			resources = resolver.getRequiredResources();
			if ((resources != null) && ((resources.length) > 0)) {
				System.out.println("Required resource(s):");
				for (Resource resource : resources) {
					System.out.println((((("   " + (resource.getPresentationName())) + " (") + (resource.getVersion())) + ")"));
					if (why) {
						Reason[] req = resolver.getReason(resource);
						for (int reqIdx = 0; (req != null) && (reqIdx < (req.length)); reqIdx++) {
							if (!(req[reqIdx].getRequirement().isOptional())) {
								Resource r = req[reqIdx].getResource();
								if (r != null) {
									System.out.println(((((("      - " + (r.getPresentationName())) + " / ") + (req[reqIdx].getRequirement().getName())) + ":") + (req[reqIdx].getRequirement().getFilter())));
								}else {
									System.out.println(((("      - " + (req[reqIdx].getRequirement().getName())) + ":") + (req[reqIdx].getRequirement().getFilter())));
								}
							}
						}
					}
				}
			}
			resources = resolver.getOptionalResources();
			if ((resources != null) && ((resources.length) > 0)) {
				System.out.println();
				System.out.println("Optional resource(s):");
				for (Resource resource : resources) {
					System.out.println((((("   " + (resource.getPresentationName())) + " (") + (resource.getVersion())) + ")"));
					if (why) {
						Reason[] req = resolver.getReason(resource);
						for (int reqIdx = 0; (req != null) && (reqIdx < (req.length)); reqIdx++) {
							if (!(req[reqIdx].getRequirement().isOptional())) {
								Resource r = req[reqIdx].getResource();
								if (r != null) {
									System.out.println(((((("      - " + (r.getPresentationName())) + " / ") + (req[reqIdx].getRequirement().getName())) + ":") + (req[reqIdx].getRequirement().getFilter())));
								}else {
									System.out.println(((("      - " + (req[reqIdx].getRequirement().getName())) + ":") + (req[reqIdx].getRequirement().getFilter())));
								}
							}
						}
					}
				}
			}
			if ((deploy) || (start)) {
				try {
					System.out.print("\nDeploying...");
					resolver.deploy((start ? Resolver.START : 0));
					System.out.println("done.");
				} catch (IllegalStateException ex) {
					System.err.println(ex);
				}
			}
		}else {
			Reason[] reqs = resolver.getUnsatisfiedRequirements();
			if ((reqs != null) && ((reqs.length) > 0)) {
				System.out.println("Unsatisfied requirement(s):");
				for (Reason req : reqs) {
					System.out.println(((("   " + (req.getRequirement().getName())) + ":") + (req.getRequirement().getFilter())));
					System.out.println(("      " + (req.getResource().getPresentationName())));
				}
			}else {
				System.out.println("Could not resolve targets.");
			}
		}
	}
}

