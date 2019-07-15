import org.apache.karaf.shell.ssh.*;


import org.apache.karaf.shell.api.action.lifecycle.Destroy;
import org.apache.karaf.shell.api.action.lifecycle.Init;
import org.apache.karaf.shell.api.action.lifecycle.Manager;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.console.SessionFactory;

public class ActivatorNoOsgi {

    @Reference
    SessionFactory sessionFactory;

    @Init
    public void init() {
        sessionFactory.getRegistry().getService(Manager.class).register(SshAction.class);
    }

    @Destroy
    public void destroy() {
        sessionFactory.getRegistry().getService(Manager.class).register(SshAction.class);
    }

}
