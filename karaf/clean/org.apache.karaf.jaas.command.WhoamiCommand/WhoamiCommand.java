import org.apache.karaf.jaas.command.*;


import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.Principal;
import java.util.Set;

import javax.security.auth.Subject;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.Row;
import org.apache.karaf.shell.support.table.ShellTable;

@Command(scope = "jaas", name = "whoami", description = "List currently active principals according to JAAS.")
@Service
public class WhoamiCommand implements Action {
    private static final String USER_CLASS = "org.apache.karaf.jaas.boot.principal.UserPrincipal";
    private static final String GROUP_CLASS = "org.apache.karaf.jaas.boot.principal.GroupPrincipal";
    private static final String ROLE_CLASS = "org.apache.karaf.jaas.boot.principal.RolePrincipal";
    private static final String ALL_CLASS = "java.security.Principal";

	@Option(name = "-g", aliases = { "--groups" }, description = "Show groups instead of user.")
	boolean groups = false;
	
	@Option(name = "-r", aliases = { "--roles" }, description = "Show roles instead of user.")
	boolean roles = false;
	
    @Option(name = "-a", aliases = { "--all" }, description = "Show all JAAS principals regardless of type.")
	boolean all = false;

    @Option(name = "--no-format", description = "Disable table rendered output.")
	boolean noFormat = false;

    @Override
    public Object execute() throws Exception {
	ShellTable table = new ShellTable();

	// Get the currently-active JAAS Subject.
	AccessControlContext acc = AccessController.getContext();
	Subject subj = Subject.getSubject(acc);

	String classString = USER_CLASS;
	if (groups) {
	    classString = GROUP_CLASS;
	} else if (roles) {
	    classString = ROLE_CLASS;
	} else if (all) {
	    classString = ALL_CLASS;
	}

	Class c = Class.forName(classString);

	Set<Principal> principals = subj.getPrincipals(c);

	
	table.column("Name");
	if (all) {
	    table.column("Class");
	}

	for (Principal p : principals) {
	    Row row = table.addRow();
	    row.addContent(p.getName());
	    if (all) {
		row.addContent(p.getClass().getCanonicalName());
	    }
	}

	table.print(System.out, !noFormat);

	return null;
    }
}
