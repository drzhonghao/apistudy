

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;


@Service
@Command(scope = "booking", name = "get", description = "Get the booking by id")
public class GetCommand implements Action {}

