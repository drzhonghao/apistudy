import org.apache.karaf.instance.command.*;


import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.karaf.instance.core.Instance;
import org.apache.karaf.instance.core.InstanceService;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.lifecycle.Reference;

public abstract class InstanceCommandSupport implements Action {

    @Reference
    private InstanceService instanceService;

    public InstanceService getInstanceService() {
        return instanceService;
    }

    public void setInstanceService(InstanceService instanceService) {
        this.instanceService = instanceService;
    }

    protected Instance getExistingInstance(String name) {
        Instance i = instanceService.getInstance(name);
        if (i == null) {
            throw new IllegalArgumentException("Instances '" + name + "' does not exist");
        }
        return i;
    }

    protected List<Instance> getMatchingInstances(List<String> patterns) {
        List<Instance> instances = new ArrayList<>();
        Instance[] allInstances = instanceService.getInstances();
        for (Instance instance : allInstances) {
            if (match(instance.getName(), patterns)) {
                instances.add(instance);
            }
        }
        if (instances.isEmpty()) {
            throw new IllegalArgumentException("No matching instances");
        }
        return instances;
    }

    protected static Map<String, URL> getResources(List<String> resources) throws MalformedURLException {
        Map<String, URL> result = new HashMap<>();
        if (resources != null) {
            for (String resource : resources) {
                String path = resource.substring(0, resource.indexOf("="));
                String location = resource.substring(path.length() + 1);
                URL url = new URL(location);
                result.put(path, url);
            }
        }
        return result;
    }

    private boolean match(String name, List<String> patterns) {
        for (String pattern : patterns) {
            if (name.matches(pattern)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Object execute() throws Exception {
        return doExecute();
    }

    protected abstract Object doExecute() throws Exception;
}
