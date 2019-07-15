import org.apache.karaf.jaas.config.impl.*;


import org.apache.karaf.jaas.boot.ProxyLoginModule;
import org.osgi.framework.BundleContext;

public class ProxyLoginModuleInitializer {

    private BundleContext bundleContext;

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public void init() {
        BundleContext context = bundleContext.getBundle(0).getBundleContext();
        ProxyLoginModule.init(context);
    }
}
