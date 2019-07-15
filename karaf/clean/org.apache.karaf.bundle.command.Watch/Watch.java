import org.apache.karaf.bundle.command.*;


import java.util.List;

import org.apache.karaf.bundle.core.BundleWatcher;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

@Command(scope = "bundle", name = "watch", description = "Watches and updates bundles", detailedDescription = "Watches the local maven repo for changes in snapshot jars and redploys changed jars")
@Service
public class Watch implements Action {

    @Argument(index = 0, name = "urls", description = "The bundle IDs or URLs", required = false, multiValued = true)
    List<String> urls;

    @Option(name = "-i", aliases = {}, description = "Watch interval", required = false, multiValued = false)
    private long interval;

    @Option(name = "--start", description = "Starts watching the selected bundles", required = false, multiValued = false)
    protected boolean start;

    @Option(name = "--stop", description = "Stops watching all bundles", required = false, multiValued = false)
    protected boolean stop;

    @Option(name = "--remove", description = "Removes bundles from the watch list", required = false, multiValued = false)
    protected boolean remove;

    @Option(name = "--list", description = "Displays the watch list", required = false, multiValued = false)
    protected boolean list;

    @Reference
    private BundleWatcher bundleWatcher;

    public void setBundleWatcher(BundleWatcher bundleWatcher) {
        this.bundleWatcher = bundleWatcher;
    }

    @Override
    public Object execute() throws Exception {
        if (start && stop) {
            System.err.println("Please use only one of --start and --stop options!");
            return null;
        }

        if (interval > 0) {
            System.out.println("Setting watch interval to " + interval + " ms");
            bundleWatcher.setInterval(interval);
        }
        if (stop) {
            System.out.println("Stopping watch");
            bundleWatcher.stop();
        }
        if (urls != null) {
            if (remove) {
                for (String url : urls) {
                    bundleWatcher.remove(url);
                }
            } else {
                for (String url : urls) {
                    bundleWatcher.add(url);
                }
            }
        }
        if (start) {
            System.out.println("Starting watch");
            bundleWatcher.start();
        }

        if (list) { //List the watched bundles.
            String format = "%-40s %6s %-80s";
            System.out.println(String.format(format, "URL", "ID", "Bundle Name"));
            for (String url : bundleWatcher.getWatchURLs()) {

                List<Bundle> bundleList = bundleWatcher.getBundlesByURL(url);
                if (bundleList != null && bundleList.size() > 0) {
                    for (Bundle bundle : bundleList) {
                        System.out.println(String.format(format, url, bundle.getBundleId(), bundle.getHeaders().get(Constants.BUNDLE_NAME)));
                    }
                } else {
                    System.out.println(String.format(format, url, "", ""));
                }
            }
        } else {
            List<String> urls = bundleWatcher.getWatchURLs();
            if (urls != null && urls.size()>0) {
                System.out.println("Watched URLs/IDs: ");
                for (String url : bundleWatcher.getWatchURLs()) {
                    System.out.println(url);
                }
            } else {
                System.out.println("No watched URLs/IDs");
            }
        }

        return null;
    }

}








