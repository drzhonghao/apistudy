import org.apache.karaf.config.command.completers.*;


import java.util.List;

import org.apache.karaf.config.core.impl.MetaServiceCaller;
import org.apache.karaf.shell.api.action.lifecycle.Init;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.completers.StringsCompleter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class MetaCompleter implements Completer, BundleListener {

    private static final Logger LOG = LoggerFactory.getLogger(MetaCompleter.class);

    private final StringsCompleter delegate = new StringsCompleter();

    @Reference
    BundleContext context;
    
    @Init
    public void init() {
        try {
            updateMeta();
            context.registerService(BundleListener.class, this, null);
        } catch (Throwable e) {
            Throwable ncdfe = e;
            while (ncdfe != null && !(ncdfe instanceof NoClassDefFoundError)) {
                ncdfe = ncdfe.getCause();
            }
            if (ncdfe != null && ncdfe.getMessage().equals("org/osgi/service/metatype/MetaTypeService")) {
                if (LOG.isDebugEnabled()) {
                    LOG.warn("config:meta disabled because the org.osgi.service.metatype package is not wired", e);
                } else {
                    LOG.warn("config:meta disabled because the org.osgi.service.metatype package is not wired (enable debug logging for full stack trace).");
                }
            } else {
                throw e;
            }
        }
    }

    @Override
    public synchronized int complete(final Session session, final CommandLine commandLine, final List<String> candidates) {
        return delegate.complete(session, commandLine, candidates);
    }

    @Override
    public void bundleChanged(BundleEvent event) {
        updateMeta();
    }

    private synchronized void updateMeta() {
        List<String> pids = MetaServiceCaller.getPidsWithMetaInfo(context);
        if (pids != null) {
            delegate.getStrings().clear();
            delegate.getStrings().addAll(pids);
        }
    }

}
