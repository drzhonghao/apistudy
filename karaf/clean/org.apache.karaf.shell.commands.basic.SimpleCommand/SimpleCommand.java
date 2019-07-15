import org.apache.karaf.shell.commands.basic.AbstractCommand;
import org.apache.karaf.shell.commands.basic.*;


import java.util.Hashtable;

import org.apache.felix.service.command.Function;
import org.apache.karaf.shell.commands.Action;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.CommandWithAction;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.BundleContext;

/**
 * A very simple {@link Function} which creates {@link Action} based on a class name.
 */
@Deprecated
public class SimpleCommand extends AbstractCommand {

    private Class<? extends Action> actionClass;

    public SimpleCommand()
    {
    }

    public SimpleCommand(Class<? extends Action> actionClass)
    {
        this.actionClass = actionClass;
    }

    public Class<? extends Action> getActionClass()
    {
        return actionClass;
    }

    public void setActionClass(Class<? extends Action> actionClass)
    {
        this.actionClass = actionClass;
    }

    public Action createNewAction() {
        try {
            return actionClass.newInstance();
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static ServiceRegistration export(BundleContext context, Class<? extends Action> actionClass)
    {
        Command cmd = actionClass.getAnnotation(Command.class);
        if (cmd == null)
        {
            throw new IllegalArgumentException("Action class is not annotated with @Command");
        }
        Hashtable<String, String> props = new Hashtable<>();
        props.put("osgi.command.scope", cmd.scope());
        props.put("osgi.command.function", cmd.name());
        SimpleCommand command = new SimpleCommand(actionClass);
        return context.registerService(
                new String[] { Function.class.getName(), CommandWithAction.class.getName() },
                command, props);
    }

}
