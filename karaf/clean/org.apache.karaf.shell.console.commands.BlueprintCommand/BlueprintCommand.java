import org.apache.karaf.shell.console.commands.*;


import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import org.apache.felix.gogo.commands.Action;
import org.apache.felix.service.command.CommandSession;
import org.apache.karaf.shell.commands.basic.AbstractCommand;
import org.apache.karaf.shell.commands.basic.ActionPreparator;
import org.apache.karaf.shell.commands.basic.DefaultActionPreparator;
import org.apache.karaf.shell.console.BlueprintContainerAware;
import org.apache.karaf.shell.console.BundleContextAware;
import org.apache.karaf.shell.console.CompletableFunction;
import org.apache.karaf.shell.console.Completer;
import org.osgi.framework.BundleContext;
import org.osgi.service.blueprint.container.BlueprintContainer;
import org.osgi.service.blueprint.container.Converter;

@Deprecated
public class
        BlueprintCommand extends AbstractCommand implements CompletableFunction
{

    protected BlueprintContainer blueprintContainer;
    protected Converter blueprintConverter;
    protected String actionId;
    protected String shell;
    protected List<Completer> completers;
    protected Map<String,Completer> optionalCompleters;

    public void setBlueprintContainer(BlueprintContainer blueprintContainer) {
        this.blueprintContainer = blueprintContainer;
    }

    public void setBlueprintConverter(Converter blueprintConverter) {
        this.blueprintConverter = blueprintConverter;
    }

    public void setActionId(String actionId) {
        this.actionId = actionId;
    }
    
    public void setShell(String shell) {
        this.shell = shell;
    }

    public List<Completer> getCompleters() {
        return completers;
    }

    public void setCompleters(List<Completer> completers) {
        this.completers = completers;
    }

    public Map<String, Completer> getOptionalCompleters() {
        return optionalCompleters;
    }

    public void setOptionalCompleters(Map<String, Completer> optionalCompleters) {
        this.optionalCompleters = optionalCompleters;
    }

    @Override
    protected ActionPreparator getPreparator() throws Exception {
        return new BlueprintActionPreparator();
    }

    protected class BlueprintActionPreparator extends DefaultActionPreparator {

        @Override
        protected Object convert(Action action, CommandSession commandSession, Object o, Type type) throws Exception {
            GenericType t = new GenericType(type);
            if (t.getRawClass() == String.class) {
                return o != null ? o.toString() : null;
            }
            return blueprintConverter.convert(o, t);
        }

    }

    public Action createNewAction() {
        Action action = (Action) blueprintContainer.getComponentInstance(actionId);
        if (action instanceof BlueprintContainerAware) {
            ((BlueprintContainerAware) action).setBlueprintContainer(blueprintContainer);
        }
        if (action instanceof BundleContextAware) {
            BundleContext context = (BundleContext) blueprintContainer.getComponentInstance("blueprintBundleContext");
            ((BundleContextAware) action).setBundleContext(context);
        }
        return action;
    }

}
