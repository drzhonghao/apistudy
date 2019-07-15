

import java.io.PrintStream;
import org.apache.karaf.jaas.command.JaasCommandSupport;
import org.apache.karaf.jaas.command.completers.LoginModuleNameCompleter;
import org.apache.karaf.jaas.command.completers.RealmCompleter;
import org.apache.karaf.jaas.modules.BackingEngine;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;


@Command(scope = "jaas", name = "realm-manage", description = "Manage users and roles of a JAAS Realm")
@Service
public class ManageRealmCommand extends JaasCommandSupport {
	@Option(name = "--realm", description = "JAAS Realm", required = false, multiValued = false)
	@Completion(RealmCompleter.class)
	String realmName;

	@Option(name = "--index", description = "Realm Index", required = false, multiValued = false)
	int index;

	@Option(name = "--module", description = "JAAS Login Module Class Name", required = false, multiValued = false)
	@Completion(LoginModuleNameCompleter.class)
	String moduleName;

	@Option(name = "-f", aliases = { "--force" }, description = "Force the management of this realm, even if another one was under management", required = false, multiValued = false)
	boolean force;

	@Option(name = "-h", aliases = { "--hidden" }, description = "Manage hidden realms", required = false, multiValued = false)
	boolean hidden;

	@SuppressWarnings("unchecked")
	@Override
	public Object execute() throws Exception {
		if (((realmName) == null) && ((index) <= 0)) {
			System.err.println("A valid realm or the realm index need to be specified");
			return null;
		}
		return null;
	}

	@Override
	protected Object doExecute(BackingEngine engine) throws Exception {
		return null;
	}
}

