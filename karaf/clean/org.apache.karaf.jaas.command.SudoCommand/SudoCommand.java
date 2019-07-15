import org.apache.karaf.jaas.command.*;


import java.security.PrivilegedExceptionAction;
import java.util.List;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginContext;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.util.jaas.JaasHelper;

@Command(scope = "jaas", name = "sudo", description = "Execute a command as another user")
@Service
public class SudoCommand implements Action {

    @Option(name = "--realm")
    String realm = "karaf";

    @Option(name = "--user")
    String user = "karaf";

    @Argument(multiValued = true)
    List<String> command;

    @Reference
    Session session;

    @Override
    public Object execute() throws Exception {
        Subject subject = new Subject();
        LoginContext loginContext = new LoginContext(realm, subject, callbacks -> {
            for (Callback callback : callbacks) {
                if (callback instanceof NameCallback) {
                    ((NameCallback) callback).setName(user);
                } else if (callback instanceof PasswordCallback) {
                    String password = SudoCommand.this.session.readLine("Password: ", '*');
                    ((PasswordCallback) callback).setPassword(password.toCharArray());
                } else {
                    throw new UnsupportedCallbackException(callback);
                }
            }
        });
        loginContext.login();

        final StringBuilder sb = new StringBuilder();
        for (String s : command) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(s);
        }
        JaasHelper.doAs(subject, (PrivilegedExceptionAction<Object>) () -> session.execute(sb));

        loginContext.logout();
        return null;
    }

}
