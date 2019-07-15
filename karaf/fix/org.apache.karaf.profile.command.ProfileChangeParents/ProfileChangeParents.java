

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;


@Command(name = "change-parents", scope = "profile", description = "Replace the profile's parents with the specified list of parents")
@Service
public class ProfileChangeParents implements Action {}

